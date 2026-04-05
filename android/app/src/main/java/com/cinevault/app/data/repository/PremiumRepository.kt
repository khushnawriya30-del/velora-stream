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
                    plan = result.user?.premiumPlan,
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
}
