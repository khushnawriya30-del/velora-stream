package com.cinevault.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.app.BuildConfig
import com.cinevault.app.data.local.SessionManager
import com.cinevault.app.data.model.CreateOrderResponse
import com.cinevault.app.data.model.OrderStatusResponse
import com.cinevault.app.data.model.PremiumPlanDto
import com.cinevault.app.data.model.RazorpayCreateOrderResponse
import com.cinevault.app.data.model.RazorpayVerifyResponse
import com.cinevault.app.data.model.Result
import com.cinevault.app.data.model.VerifyPaymentResponse
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
    val userName: String? = null,
    val userId: String? = null,
    // UPI Payment
    val isCreatingOrder: Boolean = false,
    val currentOrder: CreateOrderResponse? = null,
    val paymentUrl: String? = null,
    // Auto-verify via UPI intent
    val isVerifyingPayment: Boolean = false,
    val paymentVerified: Boolean = false,
    val paymentFailed: Boolean = false,
    val verifyMessage: String? = null,
    val verifyResponse: VerifyPaymentResponse? = null,
    // Orders
    val myOrders: List<OrderStatusResponse> = emptyList(),
    val isLoadingOrders: Boolean = false,
    // Razorpay
    val razorpayOrder: RazorpayCreateOrderResponse? = null,
    val isCreatingRazorpayOrder: Boolean = false,
    val isVerifyingRazorpay: Boolean = false,
    val razorpaySuccess: Boolean = false,
    val razorpayFailed: Boolean = false,
    val razorpayMessage: String? = null,
    val razorpayVerifyResponse: RazorpayVerifyResponse? = null,
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val premiumRepository: PremiumRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.isPremium.collect { premium ->
                _uiState.update { it.copy(isPremium = premium) }
            }
        }
        viewModelScope.launch {
            sessionManager.userName.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
        viewModelScope.launch {
            sessionManager.userId.collect { id ->
                _uiState.update { it.copy(userId = id) }
            }
        }
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
                is Result.Error -> {}
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

    fun createPaymentSession(planId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingOrder = true, error = null, paymentUrl = null) }
            when (val result = premiumRepository.createPaymentSession(planId)) {
                is Result.Success -> {
                    val rootUrl = BuildConfig.BASE_URL.replace("api/v1/", "")
                    val paymentUrl = "${rootUrl}pay/${result.data.paymentId}"
                    _uiState.update { it.copy(isCreatingOrder = false, paymentUrl = paymentUrl) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isCreatingOrder = false, error = result.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun createOrder(planId: String, deviceInfo: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingOrder = true, error = null, currentOrder = null) }
            when (val result = premiumRepository.createUpiOrder(planId, deviceInfo)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isCreatingOrder = false, currentOrder = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isCreatingOrder = false, error = result.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun verifyPayment(
        orderId: String,
        status: String,
        txnId: String? = null,
        responseCode: String? = null,
        approvalRefNo: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isVerifyingPayment = true,
                    paymentVerified = false,
                    paymentFailed = false,
                    verifyMessage = null,
                    error = null,
                )
            }
            when (val result = premiumRepository.verifyPayment(orderId, status, txnId, responseCode, approvalRefNo)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isVerifyingPayment = false,
                            paymentVerified = true,
                            verifyMessage = result.data.message,
                            verifyResponse = result.data,
                            isPremium = result.data.premiumPlan != null,
                        )
                    }
                    refreshStatus()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isVerifyingPayment = false,
                            paymentFailed = true,
                            verifyMessage = result.message,
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun fetchMyOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOrders = true) }
            when (val result = premiumRepository.getMyOrders()) {
                is Result.Success -> {
                    _uiState.update { it.copy(myOrders = result.data, isLoadingOrders = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingOrders = false) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearPaymentState() {
        _uiState.update {
            it.copy(
                currentOrder = null,
                paymentUrl = null,
                paymentVerified = false,
                paymentFailed = false,
                verifyMessage = null,
                verifyResponse = null,
                razorpayOrder = null,
                razorpaySuccess = false,
                razorpayFailed = false,
                razorpayMessage = null,
                razorpayVerifyResponse = null,
            )
        }
    }

    // ── Razorpay ──

    fun createRazorpayOrder(planId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCreatingRazorpayOrder = true,
                    error = null,
                    razorpayOrder = null,
                    razorpaySuccess = false,
                    razorpayFailed = false,
                )
            }
            when (val result = premiumRepository.createRazorpayOrder(planId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isCreatingRazorpayOrder = false, razorpayOrder = result.data)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isCreatingRazorpayOrder = false, error = result.message)
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun verifyRazorpayPayment(paymentId: String, orderId: String, signature: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isVerifyingRazorpay = true,
                    razorpaySuccess = false,
                    razorpayFailed = false,
                    razorpayMessage = null,
                    error = null,
                )
            }
            when (val result = premiumRepository.verifyRazorpayPayment(paymentId, orderId, signature)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isVerifyingRazorpay = false,
                            razorpaySuccess = true,
                            razorpayMessage = result.data.message,
                            razorpayVerifyResponse = result.data,
                            isPremium = result.data.premiumPlan != null,
                        )
                    }
                    refreshStatus()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isVerifyingRazorpay = false,
                            razorpayFailed = true,
                            razorpayMessage = result.message,
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }
}
