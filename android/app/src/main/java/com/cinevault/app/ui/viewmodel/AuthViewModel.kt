package com.cinevault.app.ui.viewmodel

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.app.data.local.SessionManager
import com.cinevault.app.data.model.Result
import com.cinevault.app.data.repository.AuthRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val loginSuccess: Boolean = false,
    val registerSuccess: Boolean = false,
    val forgotPasswordSuccess: Boolean = false,
    val googleSignupSuccess: Boolean = false,
    // Phone OTP
    val phoneOtpSent: Boolean = false,
    val phoneLoginSuccess: Boolean = false,
    val devOtp: String? = null,  // non-null when SMS not configured (dev/testing mode)
    // Email OTP
    val emailOtpSent: Boolean = false,
    val emailLoginSuccess: Boolean = false,
    val emailDevOtp: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn: Flow<Boolean> = sessionManager.accessToken.map { it != null }
    val hasCompletedOnboarding: Flow<Boolean> = sessionManager.onboardingCompleted

    // Saved account flows for login suggestions
    val lastGoogleName: Flow<String?> = sessionManager.lastGoogleName
    val lastGoogleEmail: Flow<String?> = sessionManager.lastGoogleEmail
    val lastGoogleAvatar: Flow<String?> = sessionManager.lastGoogleAvatar
    val lastPhoneName: Flow<String?> = sessionManager.lastPhoneName
    val lastPhoneNumber: Flow<String?> = sessionManager.lastPhoneNumber

    /** Fetch pending referral by IP from backend (called on first launch) */
    fun fetchPendingReferralByIp() {
        viewModelScope.launch {
            authRepository.fetchPendingReferralByIp()
        }
    }

    /**
     * After login confirmed, check 3 sources for a referral code and apply:
     * 1. Clipboard (VELORA_REF:CODE copied by website on download)
     * 2. SessionManager pending referral (from deep link or IP check)
     * 3. SharedPreferences pending referral code (from onNewIntent)
     */
    fun checkAndApplyClipboardReferral(context: Context) {
        viewModelScope.launch {
            try {
                var referralCode: String? = null
                var fromClipboard = false

                // METHOD 1: Check clipboard for VELORA_REF: prefix (case-insensitive)
                try {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val clipText = clip.getItemAt(0).coerceToText(context).toString().trim()
                        Log.d("CineVaultReferral", "Clipboard raw content: '$clipText'")
                        val upper = clipText.uppercase()
                        referralCode = when {
                            upper.startsWith("VELORA_REF:") -> clipText.substring(11).trim()
                            upper.startsWith("VELORA-REF:") -> clipText.substring(11).trim()
                            upper.startsWith("VELORA REF:") -> clipText.substring(11).trim()
                            upper.startsWith("VALORA_REF:") -> clipText.substring(11).trim()
                            upper.startsWith("VALORA-REF:") -> clipText.substring(11).trim()
                            else -> null
                        }
                        if (!referralCode.isNullOrBlank()) {
                            Log.d("CineVaultReferral", "Found referral in clipboard: $referralCode")
                            fromClipboard = true
                            sessionManager.savePendingReferralCode(referralCode)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                clipboard.clearPrimaryClip()
                            }
                        } else {
                            Log.d("CineVaultReferral", "Clipboard has no referral prefix")
                        }
                    } else {
                        Log.d("CineVaultReferral", "Clipboard is empty")
                    }
                } catch (e: Exception) {
                    Log.w("CineVaultReferral", "Clipboard read failed: ${e.message}")
                }

                // METHOD 2: Check SessionManager for pending referral (from deep link, clipboard save, or IP check)
                if (referralCode.isNullOrBlank()) {
                    // Only skip if already applied AND no fresh clipboard code
                    val alreadyApplied = sessionManager.referralPromptShown.first()
                    if (alreadyApplied) {
                        Log.d("CineVaultReferral", "Referral already applied, no fresh clipboard code, skipping")
                        return@launch
                    }
                    val pending = sessionManager.pendingReferralCode.first()
                    if (!pending.isNullOrBlank()) {
                        referralCode = pending
                        Log.d("CineVaultReferral", "Found referral in SessionManager: $referralCode")
                    }
                }

                // Apply if found — with retry for auth timing issues (token may not be ready yet)
                if (!referralCode.isNullOrBlank()) {
                    if (fromClipboard) {
                        // Fresh clipboard code overrides any previous state
                        Log.d("CineVaultReferral", "Fresh clipboard code, resetting referral state")
                        sessionManager.resetReferralPromptShown()
                    }
                    var applied = false
                    for (attempt in 1..3) {
                        Log.d("CineVaultReferral", "Applying referral code (attempt $attempt/3): $referralCode")
                        when (val result = authRepository.applyReferralCode(referralCode)) {
                            is Result.Success -> {
                                Log.d("CineVaultReferral", "Referral applied successfully: ${result.data}")
                                sessionManager.setReferralPromptShown()
                                sessionManager.clearPendingReferralCode()
                                applied = true
                                break
                            }
                            is Result.Error -> {
                                Log.w("CineVaultReferral", "Referral apply failed: code=${result.code}, msg=${result.message}")
                                if (result.code == 409 || result.code == 400) {
                                    sessionManager.setReferralPromptShown()
                                    sessionManager.clearPendingReferralCode()
                                    applied = true
                                    break
                                }
                                if (attempt < 3) {
                                    Log.d("CineVaultReferral", "Retrying in ${attempt * 2}s...")
                                    kotlinx.coroutines.delay(attempt * 2000L)
                                }
                            }
                            is Result.Loading -> {}
                        }
                    }
                    if (!applied) {
                        Log.w("CineVaultReferral", "All retry attempts failed, keeping code for next app launch")
                    }
                } else {
                    Log.d("CineVaultReferral", "No referral code found in any source")
                }
            } catch (e: Exception) {
                Log.e("CineVaultReferral", "checkAndApplyClipboardReferral error: ${e.message}")
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.login(email, password)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun register(name: String, email: String, password: String, confirmPassword: String, referralCode: String? = null) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(error = "Passwords do not match") }
            return
        }
        if (password.length < 8) {
            _uiState.update { it.copy(error = "Password must be at least 8 characters") }
            return
        }
        if (!password.any { it.isUpperCase() } || !password.any { it.isLowerCase() } || !password.any { it.isDigit() }) {
            _uiState.update { it.copy(error = "Password must contain uppercase, lowercase, and a number") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.register(name, email, password, referralCode)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, registerSuccess = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your email") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.forgotPassword(email)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, forgotPasswordSuccess = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { AuthUiState() }
        }
    }

    fun googleLogin(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.googleLogin(idToken)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun googleSignup(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.googleSignup(idToken)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, googleSignupSuccess = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun sendPhoneOtp(phone: String) {
        val fullPhone = if (phone.startsWith("+91")) phone else "+91$phone"
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.sendPhoneOtp(fullPhone)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, phoneOtpSent = true, devOtp = result.data?.devOtp)
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun verifyPhoneOtp(phone: String, otp: String) {
        val fullPhone = if (phone.startsWith("+91")) phone else "+91$phone"
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.verifyPhoneOtp(fullPhone, otp)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, phoneLoginSuccess = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun firebasePhoneVerify(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.firebasePhoneVerify(idToken)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, phoneLoginSuccess = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun resetPhoneOtpSent() {
        _uiState.update { it.copy(phoneOtpSent = false) }
    }

    // ── Email OTP Authentication ──────────────────────────────────────────────

    fun sendEmailOtp(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your email") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.sendEmailOtp(email)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, emailOtpSent = true, emailDevOtp = result.data?.devOtp)
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun verifyEmailOtp(email: String, otp: String) {
        if (otp.isBlank()) {
            _uiState.update { it.copy(error = "Please enter the OTP") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.verifyEmailOtp(email, otp)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, emailLoginSuccess = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun resetEmailOtpSent() {
        _uiState.update { it.copy(emailOtpSent = false, emailDevOtp = null) }
    }

    fun onGoogleSignInError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    /**
     * Handles the auth callback from Chrome Custom Tab (velora://auth-callback?...)
     * Parses tokens + user JSON, saves to session, and triggers loginSuccess.
     */
    fun handleWebAuthCallback(uri: Uri) {
        val accessToken = uri.getQueryParameter("accessToken")
        val refreshToken = uri.getQueryParameter("refreshToken")
        val userJson = uri.getQueryParameter("user")
        if (accessToken == null || refreshToken == null || userJson == null) {
            Log.e("AuthViewModel", "Missing parameters in auth callback")
            _uiState.update { it.copy(error = "Google sign-in failed: missing data") }
            return
        }
        viewModelScope.launch {
            try {
                val user = Gson().fromJson(userJson, WebAuthUser::class.java)
                sessionManager.saveSession(
                    token = accessToken,
                    userId = user._id ?: user.id ?: "",
                    name = user.name ?: "",
                    email = user.email ?: "",
                    avatar = user.avatarUrl,
                    role = user.role ?: "user",
                )
                sessionManager.saveRefreshToken(refreshToken)
                sessionManager.saveGoogleAccount(user.name ?: "", user.email ?: "", user.avatarUrl)
                Log.d("AuthViewModel", "Web auth callback: session saved for ${user.email}")
                _uiState.update { it.copy(loginSuccess = true) }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to handle web auth callback", e)
                _uiState.update { it.copy(error = "Google sign-in failed: ${e.message}") }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            sessionManager.completeOnboarding()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetState() {
        _uiState.update { AuthUiState() }
    }

    // Referral prompt
    val referralPromptShown: Flow<Boolean> = sessionManager.referralPromptShown

    fun applyReferralCode(code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val result = authRepository.applyReferralCode(code)) {
                is Result.Success -> onResult(true, result.data ?: "Referral applied!")
                is Result.Error -> onResult(false, result.message)
                is Result.Loading -> {}
            }
        }
    }

    fun markReferralPromptShown() {
        viewModelScope.launch {
            sessionManager.setReferralPromptShown()
        }
    }
}

/** Minimal user model for parsing the web auth callback JSON */
private data class WebAuthUser(
    val _id: String? = null,
    val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val role: String? = null,
)
