package com.cinevault.app.data.repository

import com.cinevault.app.data.local.SessionManager
import com.cinevault.app.data.model.*
import com.cinevault.app.data.remote.CineVaultApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: CineVaultApi,
    private val sessionManager: SessionManager,
) {
    suspend fun login(email: String, password: String): Result<UserDto> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                sessionManager.saveSession(
                    token = body.accessToken,
                    userId = body.user.id,
                    name = body.user.name,
                    email = body.user.email,
                    avatar = body.user.avatarUrl,
                    role = body.user.role,
                )
                Result.Success(body.user)
            } else {
                Result.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "An error occurred")
        }
    }

    suspend fun register(name: String, email: String, password: String): Result<UserDto> {
        return try {
            val response = api.register(RegisterRequest(name, email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                sessionManager.saveSession(
                    token = body.accessToken,
                    userId = body.user.id,
                    name = body.user.name,
                    email = body.user.email,
                    avatar = body.user.avatarUrl,
                    role = body.user.role,
                )
                Result.Success(body.user)
            } else {
                Result.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "An error occurred")
        }
    }

    suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val response = api.forgotPassword(ForgotPasswordRequest(email))
            if (response.isSuccessful) {
                Result.Success(response.body()?.message ?: "Reset link sent")
            } else {
                Result.Error(response.message())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "An error occurred")
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            api.logout()
            sessionManager.clearSession()
            Result.Success(Unit)
        } catch (e: Exception) {
            sessionManager.clearSession()
            Result.Success(Unit)
        }
    }
}
