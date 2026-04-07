package com.cinevault.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.app.data.model.*
import com.cinevault.app.data.remote.CineVaultApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EarnMoneyUiState(
    val isLoading: Boolean = true,
    val balance: Int = 0,
    val totalEarned: Int = 0,
    val totalWithdrawn: Int = 0,
    val totalReferrals: Int = 0,
    val canWithdraw: Boolean = false,
    val amountNeeded: Int = 0,
    val withdrawThreshold: Int = 100,
    val referralCode: String = "",
    val totalInvited: Int = 0,
    val earnings: List<EarningItem> = emptyList(),
    val withdrawals: List<WithdrawalHistoryItem> = emptyList(),
    val error: String? = null,
    val withdrawSuccess: Boolean = false,
    val withdrawError: String? = null,
    val isWithdrawing: Boolean = false,
)

@HiltViewModel
class EarnMoneyViewModel @Inject constructor(
    private val api: CineVaultApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EarnMoneyUiState())
    val uiState: StateFlow<EarnMoneyUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Load wallet balance
                val walletResp = api.getWalletBalance()
                if (walletResp.isSuccessful) {
                    val w = walletResp.body()!!
                    _uiState.value = _uiState.value.copy(
                        balance = w.balance,
                        totalEarned = w.totalEarned,
                        totalWithdrawn = w.totalWithdrawn,
                        totalReferrals = w.totalReferrals,
                        canWithdraw = w.canWithdraw,
                        amountNeeded = w.amountNeeded,
                        withdrawThreshold = w.withdrawThreshold,
                    )
                }

                // Load referral stats
                val refResp = api.getReferralStats()
                if (refResp.isSuccessful) {
                    val r = refResp.body()!!
                    _uiState.value = _uiState.value.copy(
                        referralCode = r.referralCode,
                        totalInvited = r.totalInvited,
                    )
                }

                // Load earnings history
                val earningsResp = api.getReferralEarnings()
                if (earningsResp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        earnings = earningsResp.body() ?: emptyList()
                    )
                }

                // Load withdrawal history
                val withdrawResp = api.getWithdrawalHistory()
                if (withdrawResp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        withdrawals = withdrawResp.body() ?: emptyList()
                    )
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Something went wrong"
                )
            }
        }
    }

    fun requestWithdrawal(amount: Int, upiId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWithdrawing = true,
                withdrawError = null,
                withdrawSuccess = false,
            )
            try {
                val resp = api.requestWithdrawal(WithdrawRequest(amount, upiId))
                if (resp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isWithdrawing = false,
                        withdrawSuccess = true,
                    )
                    loadAll() // Refresh data
                } else {
                    val errBody = resp.errorBody()?.string() ?: "Withdrawal failed"
                    _uiState.value = _uiState.value.copy(
                        isWithdrawing = false,
                        withdrawError = errBody,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isWithdrawing = false,
                    withdrawError = e.message ?: "Withdrawal failed",
                )
            }
        }
    }

    fun clearWithdrawState() {
        _uiState.value = _uiState.value.copy(
            withdrawSuccess = false,
            withdrawError = null,
        )
    }
}
