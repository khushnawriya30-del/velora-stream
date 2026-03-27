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
                body.refreshToken?.let { sessionManager.saveRefreshToken(it) }
                ensureActiveProfile()
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
                body.refreshToken?.let { sessionManager.saveRefreshToken(it) }
                ensureActiveProfile()
                Result.Success(body.user)
            } else {
                Result.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "An error occurred")
        }
    }

    /**
     * After login/register, ensure there's an active profile set.
     * If no profiles exist, create a default one.
     */
    private suspend fun ensureActiveProfile() {
        try {
            val profilesResponse = api.getProfiles()
            if (profilesResponse.isSuccessful) {
                val profiles = profilesResponse.body() ?: emptyList()
                val profile = profiles.firstOrNull()
                if (profile != null) {
                    sessionManager.setActiveProfileId(profile.id)
                    android.util.Log.d("CineVaultAuth", "Active profile set: ${profile.id}")
                } else {
                    // No profiles exist — create a default one
                    val createResponse = api.createProfile(
                        CreateProfileRequest(
                            displayName = "Default",
                            avatarUrl = null,
                            maturityRating = "PG",
                        )
                    )
                    if (createResponse.isSuccessful && createResponse.body() != null) {
                        val newProfile = createResponse.body()!!
                        sessionManager.setActiveProfileId(newProfile.id)
                        android.util.Log.d("CineVaultAuth", "Default profile created & set: ${newProfile.id}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CineVaultAuth", "Failed to ensure active profile", e)
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
