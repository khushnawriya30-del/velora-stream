package com.cinevault.app.ui.components

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
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
import java.io.File

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
