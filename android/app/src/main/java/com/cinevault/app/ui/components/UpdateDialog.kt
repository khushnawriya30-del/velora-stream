package com.cinevault.app.ui.components

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.cinevault.app.data.model.AppVersionResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

@Composable
fun UpdateDialog(info: AppVersionResponse, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var downloadId by remember { mutableLongStateOf(-1L) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isDownloadComplete by remember { mutableStateOf(false) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    val isDownloading = downloadId != -1L && !isDownloadComplete

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "progress"
    )

    // Poll download progress
    if (isDownloading) {
        LaunchedEffect(downloadId) {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            while (isActive && !isDownloadComplete) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    )
                    val bytesTotal = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    )
                    val status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                    )
                    if (bytesTotal > 0) {
                        progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                    }
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        progress = 1f
                        isDownloadComplete = true
                    }
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
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🎬 New Update Available!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Version ${info.versionName} is ready to install.",
                    fontSize = 14.sp,
                    color = Color(0xFFB0B0B0)
                )
                if (!info.releaseNotes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = info.releaseNotes,
                        fontSize = 13.sp,
                        color = Color(0xFF9A9A9A)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                when {
                    isDownloadComplete -> {
                        Text(
                            text = "Download complete!",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                downloadedFile?.let { installApk(context, it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Install Now", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    isDownloading -> {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Color(0xFFE50914),
                            trackColor = Color(0xFF333355),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Downloading... ${(animatedProgress * 100).toInt()}%",
                            color = Color(0xFFB0B0B0),
                            fontSize = 13.sp
                        )
                    }
                    else -> {
                        Button(
                            onClick = {
                                val fileName = "cinevault-${info.versionName}.apk"
                                val file = File(
                                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                                    fileName
                                )
                                if (file.exists()) file.delete()
                                downloadedFile = file

                                val request = DownloadManager.Request(Uri.parse(info.apkUrl))
                                    .setTitle("CineVault Update")
                                    .setDescription("Downloading v${info.versionName}")
                                    .setNotificationVisibility(
                                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                                    )
                                    .setDestinationUri(Uri.fromFile(file))
                                    .setAllowedOverMetered(true)
                                    .setAllowedOverRoaming(false)

                                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                downloadId = dm.enqueue(request)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Update Now", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        if (!info.forceUpdate) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onDismiss) {
                                Text("Skip for Now", color = Color(0xFF9A9A9A))
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


@Composable
fun UpdateDialog(info: AppVersionResponse, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var downloading by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!info.forceUpdate) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !info.forceUpdate,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🎬 New Update Available!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Version ${info.versionName} is ready to install.",
                    fontSize = 14.sp,
                    color = Color(0xFFB0B0B0)
                )
                if (!info.releaseNotes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = info.releaseNotes,
                        fontSize = 13.sp,
                        color = Color(0xFF9A9A9A)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                if (downloading) {
                    CircularProgressIndicator(color = Color(0xFFE50914))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Downloading update...", color = Color(0xFFB0B0B0), fontSize = 13.sp)
                } else {
                    Button(
                        onClick = {
                            downloading = true
                            downloadAndInstallApk(context, info.apkUrl, info.versionName) {
                                downloading = false
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Update Now", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    if (!info.forceUpdate) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onDismiss) {
                            Text("Skip for Now", color = Color(0xFF9A9A9A))
                        }
                    }
                }
            }
        }
    }
}

private fun downloadAndInstallApk(
    context: Context,
    apkUrl: String,
    versionName: String,
    onComplete: () -> Unit
) {
    val fileName = "cinevault-$versionName.apk"
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
    if (file.exists()) file.delete()

    val request = DownloadManager.Request(Uri.parse(apkUrl))
        .setTitle("CineVault Update")
        .setDescription("Downloading version $versionName")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationUri(Uri.fromFile(file))
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(false)

    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = dm.enqueue(request)

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                context.unregisterReceiver(this)
                onComplete()
                installApk(context, file)
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.registerReceiver(
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED
        )
    } else {
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.cinevault.app.BuildConfig
import com.cinevault.app.data.model.AppVersionResponse
import com.cinevault.app.data.remote.CineVaultApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun UpdateChecker(api: CineVaultApi) {
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<AppVersionResponse?>(null) }
    var dismissed by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) {
                val response = api.getAppVersion()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.versionCode > BuildConfig.VERSION_CODE) {
                        updateInfo = body
                    }
                }
            }
        }
    }

    val info = updateInfo
    if (info != null && !dismissed) {
        Dialog(
            onDismissRequest = { if (!info.forceUpdate) dismissed = true },
            properties = DialogProperties(dismissOnBackPress = !info.forceUpdate, dismissOnClickOutside = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A2E), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🎬 New Update Available!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Version ${info.versionName} is ready to install.",
                        fontSize = 14.sp,
                        color = Color(0xFFB0B0B0)
                    )
                    if (!info.releaseNotes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = info.releaseNotes,
                            fontSize = 13.sp,
                            color = Color(0xFF9A9A9A)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    if (downloading) {
                        CircularProgressIndicator(color = Color(0xFFE50914))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Downloading update...", color = Color(0xFFB0B0B0), fontSize = 13.sp)
                    } else {
                        Button(
                            onClick = {
                                downloading = true
                                downloadAndInstallApk(context, info.apkUrl, info.versionName) {
                                    downloading = false
                                    dismissed = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Update Now", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        if (!info.forceUpdate) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { dismissed = true }) {
                                Text("Skip for Now", color = Color(0xFF9A9A9A))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun downloadAndInstallApk(
    context: Context,
    apkUrl: String,
    versionName: String,
    onComplete: () -> Unit
) {
    val fileName = "cinevault-$versionName.apk"
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
    if (file.exists()) file.delete()

    val request = DownloadManager.Request(Uri.parse(apkUrl))
        .setTitle("CineVault Update")
        .setDescription("Downloading version $versionName")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationUri(Uri.fromFile(file))
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(false)

    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = dm.enqueue(request)

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                context.unregisterReceiver(this)
                onComplete()
                installApk(context, file)
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
    } else {
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
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
