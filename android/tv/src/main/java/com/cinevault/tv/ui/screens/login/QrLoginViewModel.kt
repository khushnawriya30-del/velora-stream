package com.cinevault.tv.ui.screens.login

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.tv.data.local.SessionManager
import com.cinevault.tv.data.model.QrCheckResponse
import com.cinevault.tv.data.remote.CineVaultApi
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QrLoginState(
    val qrBitmap: Bitmap? = null,
    val token: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val loginSuccess: Boolean = false,
    val isPremium: Boolean = false,
)

@HiltViewModel
class QrLoginViewModel @Inject constructor(
    private val api: CineVaultApi,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(QrLoginState())
    val state = _state.asStateFlow()

    private var pollingJob: Job? = null

    init {
        generateQrCode()
    }

    fun generateQrCode() {
        viewModelScope.launch {
            _state.value = QrLoginState(isLoading = true)
            try {
                val response = api.generateQrToken()
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val qrContent = "cinevault://tv-login?token=${data.token}"
                    val bitmap = generateQrBitmap(qrContent, 400)
                    _state.value = QrLoginState(
                        qrBitmap = bitmap,
                        token = data.token,
                        isLoading = false,
                    )
                    startPolling(data.token)
                } else {
                    _state.value = QrLoginState(
                        isLoading = false,
                        error = "Failed to generate QR code. Please try again.",
                    )
                }
            } catch (e: Exception) {
                _state.value = QrLoginState(
                    isLoading = false,
                    error = "Network error. Check your connection.",
                )
            }
        }
    }

    private fun startPolling(token: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 60 // 5 minutes at 5s intervals
            while (isActive && attempts < maxAttempts) {
                delay(5000)
                attempts++
                try {
                    val response = api.checkQrToken(mapOf("token" to token))
                    if (response.isSuccessful) {
                        val data = response.body()
                        when (data?.status) {
                            "approved" -> {
                                saveSession(data)
                                _state.value = _state.value.copy(
                                    loginSuccess = true,
                                    isPremium = data.user?.isPremium ?: false,
                                )
                                return@launch
                            }
                            "expired" -> {
                                _state.value = _state.value.copy(
                                    error = "QR code expired. Generating new one...",
                                )
                                delay(2000)
                                generateQrCode()
                                return@launch
                            }
                            // "pending" -> continue polling
                        }
                    }
                } catch (_: Exception) {
                    // Network hiccup, continue polling
                }
            }
            // Timed out
            _state.value = _state.value.copy(
                error = "Session timed out. Please try again.",
            )
        }
    }

    private suspend fun saveSession(data: QrCheckResponse) {
        if (data.accessToken != null && data.user != null) {
            sessionManager.saveSession(
                accessToken = data.accessToken,
                refreshToken = data.refreshToken,
                userId = data.user.id,
                name = data.user.name,
                email = data.user.email,
                avatar = data.user.avatarUrl,
                isPremium = data.user.isPremium,
                premiumPlan = data.user.premiumPlan,
                premiumExpiresAt = data.user.premiumExpiresAt,
            )
        }
    }

    private fun generateQrBitmap(content: String, size: Int): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bitmap
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
