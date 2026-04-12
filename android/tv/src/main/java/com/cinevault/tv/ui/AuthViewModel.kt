package com.cinevault.tv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.tv.BuildConfig
import com.cinevault.tv.data.local.SessionManager
import com.cinevault.tv.data.model.AppVersionResponse
import com.cinevault.tv.data.remote.CineVaultApi
import com.cinevault.tv.data.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val contentRepository: ContentRepository,
    private val api: CineVaultApi,
) : ViewModel() {

    val isLoggedIn = sessionManager.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPremium = sessionManager.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userName = sessionManager.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userAvatar = sessionManager.userAvatar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _updateInfo = MutableStateFlow<AppVersionResponse?>(null)
    val updateInfo: StateFlow<AppVersionResponse?> = _updateInfo

    init {
        refreshPremiumStatus()
        checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            try {
                val resp = api.getAppVersion()
                if (resp.isSuccessful) {
                    val body = resp.body() ?: return@launch
                    val installed = sessionManager.installedVersionCode.first()
                    val current = maxOf(BuildConfig.VERSION_CODE, installed)
                    if (body.versionCode > current) {
                        _updateInfo.value = body
                    }
                }
            } catch (_: Exception) { /* silent */ }
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }

    fun markUpdateInstalled(versionCode: Int) {
        viewModelScope.launch {
            sessionManager.saveInstalledVersionCode(versionCode)
        }
    }

    fun refreshPremiumStatus() {
        viewModelScope.launch {
            contentRepository.getPremiumStatus().onSuccess { status ->
                sessionManager.updatePremiumStatus(status.isPremium, status.plan, status.expiresAt)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
        }
    }
}
