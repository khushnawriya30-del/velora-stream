package com.cinevault.tv.ui.components

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.cinevault.tv.data.model.AppVersionResponse
import com.cinevault.tv.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvUpdateDialog(
    info: AppVersionResponse,
    onDismiss: () -> Unit,
    onInstallClicked: (versionCode: Int) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val d = LocalTvDimens.current
    var downloadId by remember { mutableLongStateOf(-1L) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isDownloadComplete by remember { mutableStateOf(false) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var isResolving by remember { mutableStateOf(false) }

    val isDownloading = downloadId != -1L && !isDownloadComplete && downloadError == null

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "progress",
    )

    suspend fun resolveRedirects(url: String): String = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.requestMethod = "HEAD"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.connect()
            val code = conn.responseCode
            val location = conn.getHeaderField("Location")
            conn.disconnect()
            if (code in 301..303 && !location.isNullOrBlank()) location else url
        } catch (_: Exception) {
            url
        }
    }

    fun startDownload() {
        downloadError = null
        progress = 0f
        isDownloadComplete = false
        isResolving = true

        scope.launch {
            val directUrl = resolveRedirects(info.apkUrl)
            val fileName = "velora-tv-${info.versionName}.apk"
            val file = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                fileName,
            )
            if (file.exists()) file.delete()
            downloadedFile = file

            val request = DownloadManager.Request(Uri.parse(directUrl))
                .setTitle("VELORA TV Update")
                .setDescription("Downloading v${info.versionName}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(file))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setMimeType("application/vnd.android.package-archive")

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            isResolving = false
            downloadId = dm.enqueue(request)
        }
    }

    if (isDownloading) {
        LaunchedEffect(downloadId) {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            while (isActive && !isDownloadComplete && downloadError == null) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                    )
                    val bytesTotal = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                    )
                    val status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS),
                    )
                    if (bytesTotal > 0) progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            progress = 1f
                            isDownloadComplete = true
                        }
                        DownloadManager.STATUS_FAILED -> {
                            downloadError = "Download failed. Check your connection."
                            downloadId = -1L
                        }
                    }
                } else {
                    downloadError = "Download cancelled."
                    downloadId = -1L
                }
                cursor.close()
                delay(300)
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!info.forceUpdate && !isDownloading) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !info.forceUpdate && !isDownloading,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .background(TvSurface, RoundedCornerShape(d.padLarge))
                .padding(d.padXXL),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "New Update Available!",
                    fontSize = d.fontXXL,
                    fontWeight = FontWeight.Bold,
                    color = TvOnSurface,
                )
                Spacer(Modifier.height(d.padSmall))
                Text(
                    text = "Version ${info.versionName} is ready.",
                    fontSize = d.fontBody,
                    color = TvOnSurfaceVariant,
                )
                if (!info.releaseNotes.isNullOrBlank()) {
                    Spacer(Modifier.height(d.padSmall))
                    Text(
                        text = info.releaseNotes,
                        fontSize = d.fontSmall,
                        color = TvTextMuted,
                    )
                }
                Spacer(Modifier.height(d.padXL))

                when {
                    isDownloadComplete -> {
                        Text(
                            "Download complete!",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = d.fontBody,
                        )
                        Spacer(Modifier.height(d.padMedium))
                        Button(
                            onClick = {
                                onInstallClicked(info.versionCode)
                                downloadedFile?.let { installApk(context, it) }
                            },
                            colors = ButtonDefaults.colors(containerColor = Color(0xFF4CAF50)),
                        ) {
                            Text("Install Now", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    downloadError != null -> {
                        Text(
                            downloadError!!,
                            color = TvPrimary,
                            fontSize = d.fontSmall,
                        )
                        Spacer(Modifier.height(d.padMedium))
                        Button(
                            onClick = { startDownload() },
                            colors = ButtonDefaults.colors(containerColor = TvPrimary),
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        if (!info.forceUpdate) {
                            Spacer(Modifier.height(d.padSmall))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.colors(containerColor = Color.Transparent),
                            ) {
                                Text("Skip", color = TvTextMuted)
                            }
                        }
                    }
                    isResolving -> {
                        Text("Preparing download...", color = TvTextMuted, fontSize = d.fontSmall)
                    }
                    isDownloading -> {
                        // Simple progress text
                        val pct = if (progress > 0f) "${(animatedProgress * 100).toInt()}%" else "..."
                        Text(
                            "Downloading... $pct",
                            color = TvOnSurfaceVariant,
                            fontSize = d.fontBody,
                        )
                    }
                    else -> {
                        Button(
                            onClick = { startDownload() },
                            colors = ButtonDefaults.colors(containerColor = TvPrimary),
                        ) {
                            Text("Update Now", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        if (!info.forceUpdate) {
                            Spacer(Modifier.height(d.padSmall))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.colors(containerColor = Color.Transparent),
                            ) {
                                Text("Skip for Now", color = TvTextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun installApk(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}
