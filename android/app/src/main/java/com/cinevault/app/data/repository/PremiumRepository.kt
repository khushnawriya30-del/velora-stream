package com.cinevault.app.data.repository

import com.cinevault.app.data.local.SessionManager
import com.cinevault.app.data.model.*
import com.cinevault.app.data.remote.CineVaultApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumRepository @Inject constructor(
    private val api: CineVaultApi,
    private val sessionManager: SessionManager,
) {
    suspend fun getPremiumPlans(): Result<List<PremiumPlanDto>> {
        return try {
            val response = api.getPremiumPlans()
            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Failed to fetch plans")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getPremiumStatus(): Result<PremiumStatusResponse> {
        return try {
            val response = api.getPremiumStatus()
            if (response.isSuccessful) {
                val status = response.body()!!
                sessionManager.savePremiumStatus(
                    isPremium = status.isPremium,
                    plan = status.plan,
                    expiresAt = status.expiresAt,
                )
                Result.Success(status)
            } else {
                Result.Error("Failed to fetch premium status")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun activateCode(code: String): Result<ActivateCodeResponse> {
        return try {
            val response = api.activatePremiumCode(ActivateCodeRequest(code))
            if (response.isSuccessful) {
                val result = response.body()!!
                // Update local premium status
                sessionManager.savePremiumStatus(
                    isPremium = true,
                    plan = result.plan,
                    expiresAt = result.expiresAt,
                )
                Result.Success(result)
            } else {
                val errorBody = response.errorBody()?.string()
                val message = try {
                    com.google.gson.Gson().fromJson(errorBody, MessageResponse::class.java).message
                } catch (_: Exception) {
                    "Invalid activation code"
                }
                Result.Error(message)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun createUpiOrder(planId: String, deviceInfo: String? = null): Result<CreateOrderResponse> {
        return try {
            val response = api.createUpiOrder(CreateOrderRequest(planId, deviceInfo))
            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val message = try {
                    com.google.gson.Gson().fromJson(errorBody, MessageResponse::class.java).message
                } catch (_: Exception) {
                    "Failed to create order"
                }
                Result.Error(message)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun submitUtr(orderId: String, utrId: String): Result<SubmitUtrResponse> {
        return try {
            val response = api.submitUtr(SubmitUtrRequest(orderId, utrId))
            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val message = try {
                    com.google.gson.Gson().fromJson(errorBody, MessageResponse::class.java).message
                } catch (_: Exception) {
                    "Failed to submit UTR"
                }
                Result.Error(message)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getOrderStatus(orderId: String): Result<OrderStatusResponse> {
        return try {
            val response = api.getOrderStatus(orderId)
            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Failed to get order status")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMyOrders(): Result<List<OrderStatusResponse>> {
        return try {
            val response = api.getMyOrders()
            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Failed to get orders")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun verifyPayment(
        orderId: String,
        status: String,
        txnId: String? = null,
        responseCode: String? = null,
        approvalRefNo: String? = null,
    ): Result<VerifyPaymentResponse> {
        return try {
            val response = api.verifyPayment(
                VerifyPaymentRequest(orderId, status, txnId, responseCode, approvalRefNo)
            )
            if (response.isSuccessful) {
                val result = response.body()!!
                if (result.success && result.premiumPlan != null) {
                    // Update local premium status
                    sessionManager.savePremiumStatus(
                        isPremium = true,
                        plan = result.premiumPlan,
                        expiresAt = result.premiumExpiresAt,
                    )
                }
                Result.Success(result)
            } else {
                val errorBody = response.errorBody()?.string()
                val message = try {
                    com.google.gson.Gson().fromJson(errorBody, MessageResponse::class.java).message
                } catch (_: Exception) {
                    "Payment verification failed"
                }
                Result.Error(message)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
