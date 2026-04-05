package com.cinevault.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.app.data.local.SessionManager
import com.cinevault.app.data.model.PremiumPlanDto
import com.cinevault.app.data.model.Result
import com.cinevault.app.data.repository.PremiumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PremiumUiState(
    val isPremium: Boolean = false,
    val plan: String? = null,
    val daysRemaining: Int? = null,
    val expiresAt: String? = null,
    val isActivating: Boolean = false,
    val activationSuccess: Boolean = false,
    val error: String? = null,
    val plans: List<PremiumPlanDto> = emptyList(),
    val isLoadingPlans: Boolean = false,
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val premiumRepository: PremiumRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    init {
        // Observe local premium status
        viewModelScope.launch {
            sessionManager.isPremium.collect { premium ->
                _uiState.update { it.copy(isPremium = premium) }
            }
        }
        // Fetch latest from server
        refreshStatus()
        fetchPlans()
    }

    fun fetchPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPlans = true) }
            when (val result = premiumRepository.getPremiumPlans()) {
                is Result.Success -> {
                    _uiState.update { it.copy(plans = result.data, isLoadingPlans = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingPlans = false) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            when (val result = premiumRepository.getPremiumStatus()) {
                is Result.Success -> {
                    val status = result.data
                    _uiState.update {
                        it.copy(
                            isPremium = status.isPremium,
                            plan = status.plan,
                            daysRemaining = status.daysRemaining,
                            expiresAt = status.expiresAt,
                        )
                    }
                }
                is Result.Error -> { /* silent — use cached */ }
                is Result.Loading -> {}
            }
        }
    }

    fun activateCode(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActivating = true, error = null, activationSuccess = false) }
            when (val result = premiumRepository.activateCode(code)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isActivating = false,
                            activationSuccess = true,
                            isPremium = true,
                        )
                    }
                    refreshStatus()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isActivating = false, error = result.message)
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
