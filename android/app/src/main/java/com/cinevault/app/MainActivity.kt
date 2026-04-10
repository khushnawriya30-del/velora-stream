package com.cinevault.app

import android.content.ClipboardManager
import android.content.Context
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
import androidx.lifecycle.lifecycleScope
import com.cinevault.app.data.local.SessionManager
import com.cinevault.app.ui.components.UpdateDialog
import com.cinevault.app.ui.navigation.CineVaultNavHost
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.AppViewModel
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Razorpay payment result sealed class
 */
sealed class RazorpayPaymentResult {
    data class Success(val paymentId: String, val orderId: String, val signature: String) : RazorpayPaymentResult()
    data class Error(val code: Int, val message: String) : RazorpayPaymentResult()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    @Inject lateinit var sessionManager: SessionManager

    private val _pendingAuthUri = MutableStateFlow<Uri?>(null)
    val pendingAuthUri: StateFlow<Uri?> = _pendingAuthUri

    private val _razorpayResult = MutableSharedFlow<RazorpayPaymentResult>(extraBufferCapacity = 1)
    val razorpayResult: SharedFlow<RazorpayPaymentResult> = _razorpayResult.asSharedFlow()

    fun clearPendingAuthUri() {
        _pendingAuthUri.value = null
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?, data: PaymentData?) {
        Log.d("Razorpay", "Payment success: id=$razorpayPaymentID, order=${data?.orderId}, sig=${data?.signature}")
        if (razorpayPaymentID != null && data?.orderId != null && data.signature != null) {
            _razorpayResult.tryEmit(
                RazorpayPaymentResult.Success(razorpayPaymentID, data.orderId, data.signature)
            )
        }
    }

    override fun onPaymentError(code: Int, response: String?, data: PaymentData?) {
        Log.e("Razorpay", "Payment error: code=$code, response=$response")
        _razorpayResult.tryEmit(
            RazorpayPaymentResult.Error(code, response ?: "Payment cancelled or failed")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAuthIntent(intent)
        checkClipboardForReferral()
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

    private fun checkClipboardForReferral() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (!clipboard.hasPrimaryClip()) {
                Log.d("ReferralClipboard", "Clipboard is empty")
                return
            }

            val clip = clipboard.primaryClip ?: return
            if (clip.itemCount == 0) return

            val text = clip.getItemAt(0).coerceToText(this).toString().trim()
            Log.d("ReferralClipboard", "Clipboard content: '$text'")

            val upper = text.uppercase()
            val code = when {
                upper.startsWith("VELORA_REF:") -> text.substring(11).trim()
                upper.startsWith("VELORA-REF:") -> text.substring(11).trim()
                upper.startsWith("VELORA REF:") -> text.substring(11).trim()
                upper.startsWith("VALORA_REF:") -> text.substring(11).trim()
                upper.startsWith("VALORA-REF:") -> text.substring(11).trim()
                else -> null
            }

            if (!code.isNullOrEmpty()) {
                Log.d("ReferralClipboard", "Found referral code in clipboard: $code")
                lifecycleScope.launch {
                    sessionManager.savePendingReferralCode(code)
                    // Reset referral applied flag so the new code can be applied
                    sessionManager.resetReferralPromptShown()
                    Log.d("ReferralClipboard", "Saved referral code to sessionManager: $code")
                }
                // Clear clipboard so we don't re-read the same code next launch
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    clipboard.clearPrimaryClip()
                }
            } else {
                Log.d("ReferralClipboard", "No VELORA_REF prefix found in clipboard")
            }
        } catch (e: Exception) {
            Log.e("ReferralClipboard", "Error reading clipboard: ${e.message}")
        }
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "velora") {
            when (uri.host) {
                "auth-callback" -> {
                    Log.d("MainActivity", "Received auth callback: $uri")
                    _pendingAuthUri.value = uri
                }
                "referral" -> {
                    val code = uri.getQueryParameter("code")
                    if (!code.isNullOrBlank()) {
                        Log.d("MainActivity", "Received referral code from deep link: $code")
                        lifecycleScope.launch {
                            sessionManager.savePendingReferralCode(code)
                        }
                    }
                }
            }
        }
    }
}
