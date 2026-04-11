package com.cinevault.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.app.data.local.SessionManager
import com.cinevault.app.data.model.*
import com.cinevault.app.data.remote.CineVaultApi
import com.cinevault.app.data.repository.PremiumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val daysRemaining: Int = 60,
    val earnings: List<EarningItem> = emptyList(),
    val withdrawals: List<WithdrawalHistoryItem> = emptyList(),
    val error: String? = null,
    val withdrawSuccess: Boolean = false,
    val withdrawError: String? = null,
    val isWithdrawing: Boolean = false,
    val rewardPerInvite: Int = 1,
    val inviteSettings: InviteSettingsDto? = null,
    // Bank details
    val savedBankDetails: BankDetailsResponse? = null,
    val isSavingBank: Boolean = false,
    val bankSaveSuccess: Boolean = false,
    val bankSaveError: String? = null,
)

@HiltViewModel
class EarnMoneyViewModel @Inject constructor(
    private val api: CineVaultApi,
    private val sessionManager: SessionManager,
    private val premiumRepository: PremiumRepository,
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
                // Initialize earn start timestamp if not set
                sessionManager.initEarnStartTimestamp()
                val startTs = sessionManager.earnStartTimestamp.first()
                val elapsedDays = ((System.currentTimeMillis() - startTs) / (24 * 60 * 60 * 1000)).toInt()
                val remaining = (60 - elapsedDays).coerceIn(0, 60)

                _uiState.value = _uiState.value.copy(daysRemaining = remaining)

                // Load invite settings from backend
                try {
                    when (val settingsResult = premiumRepository.getInviteSettings()) {
                        is Result.Success -> {
                            val s = settingsResult.data
                            _uiState.value = _uiState.value.copy(
                                inviteSettings = s,
                                rewardPerInvite = s.rewardPerInvite,
                            )
                        }
                        else -> {}
                    }
                } catch (_: Exception) {}

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
                try {
                    val refResp = api.getReferralStats()
                    if (refResp.isSuccessful) {
                        val r = refResp.body()
                        if (r != null && r.referralCode.isNotEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                referralCode = r.referralCode,
                                totalInvited = r.totalInvited,
                            )
                        }
                    }
                } catch (_: Exception) {}

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

                // Load saved bank details
                try {
                    val bankResp = api.getBankDetails()
                    if (bankResp.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            savedBankDetails = bankResp.body()
                        )
                    }
                } catch (_: Exception) {}

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Something went wrong"
                )
            }
        }
    }

    fun saveBankDetails(bankName: String, accountNumber: String, ifscCode: String, accountHolderName: String, phoneNumber: String, email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingBank = true, bankSaveError = null, bankSaveSuccess = false)
            try {
                val resp = api.saveBankDetails(SaveBankDetailsRequest(
                    bankName = bankName,
                    accountNumber = accountNumber,
                    ifscCode = ifscCode,
                    accountHolderName = accountHolderName,
                    phoneNumber = phoneNumber,
                    email = email,
                ))
                if (resp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isSavingBank = false,
                        bankSaveSuccess = true,
                        savedBankDetails = BankDetailsResponse(
                            bankName = bankName,
                            accountNumber = accountNumber,
                            ifscCode = ifscCode,
                            accountHolderName = accountHolderName,
                            phoneNumber = phoneNumber,
                            email = email,
                            hasBankDetails = true,
                        ),
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSavingBank = false,
                        bankSaveError = resp.errorBody()?.string() ?: "Failed to save",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSavingBank = false,
                    bankSaveError = e.message ?: "Failed to save bank details",
                )
            }
        }
    }

    fun requestWithdrawal(amount: Int, upiId: String = "", bankName: String = "", accountNumber: String = "", ifscCode: String = "", accountHolderName: String = "", phoneNumber: String = "", email: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWithdrawing = true,
                withdrawError = null,
                withdrawSuccess = false,
            )
            try {
                val resp = api.requestWithdrawal(WithdrawRequest(
                    amount = amount,
                    upiId = upiId,
                    bankName = bankName,
                    accountNumber = accountNumber,
                    ifscCode = ifscCode,
                    accountHolderName = accountHolderName,
                    phoneNumber = phoneNumber,
                    email = email,
                ))
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
