package com.cinevault.tv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.tv.data.local.SessionManager
import com.cinevault.tv.data.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val contentRepository: ContentRepository,
) : ViewModel() {

    val isLoggedIn = sessionManager.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPremium = sessionManager.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userName = sessionManager.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userAvatar = sessionManager.userAvatar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        refreshPremiumStatus()
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
