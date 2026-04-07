package com.cinevault.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.ui.components.UpdateDialog
import com.cinevault.app.ui.navigation.CineVaultNavHost
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val _pendingAuthUri = MutableStateFlow<Uri?>(null)
    val pendingAuthUri: StateFlow<Uri?> = _pendingAuthUri

    fun clearPendingAuthUri() {
        _pendingAuthUri.value = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAuthIntent(intent)
        setContent {
            CineVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CineVaultTheme.colors.background
                ) {
                    val appViewModel: AppViewModel = hiltViewModel()
                    val updateInfo by appViewModel.updateInfo.collectAsState()

                    CineVaultNavHost()

                    updateInfo?.let { info ->
                        UpdateDialog(
                            info = info,
                            onDismiss = { appViewModel.dismissUpdate() },
                            onInstallClicked = { versionCode -> appViewModel.markUpdateInstalled(versionCode) }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "velora" && uri.host == "auth-callback") {
            Log.d("MainActivity", "Received auth callback: $uri")
            _pendingAuthUri.value = uri
        }
    }
}
