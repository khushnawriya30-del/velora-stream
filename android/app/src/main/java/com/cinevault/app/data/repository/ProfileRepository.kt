package com.cinevault.app.data.repository

import com.cinevault.app.data.model.*
import com.cinevault.app.data.remote.CineVaultApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(private val api: CineVaultApi) {

    suspend fun getProfiles(): Result<List<ProfileDto>> {
        return try {
            val response = api.getProfiles()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.message())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to load profiles")
        }
    }

    suspend fun createProfile(name: String, avatarUrl: String?, maturityRating: String): Result<ProfileDto> {
        return try {
            val request = CreateProfileRequest(
                displayName = name,
                avatarUrl = avatarUrl,
                maturityRating = maturityRating,
            )
            val response = api.createProfile(request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.message())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to create profile")
        }
    }

    suspend fun updateProfile(profileId: String, updates: Map<String, Any>): Result<ProfileDto> {
        return try {
            val stringUpdates = updates.mapValues { it.value.toString() }
            val response = api.updateProfile(profileId, stringUpdates)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.message())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to update profile")
        }
    }

    suspend fun deleteProfile(profileId: String): Result<Unit> {
        return try {
            val response = api.deleteProfile(profileId)
            if (response.isSuccessful) Result.Success(Unit)
            else Result.Error(response.message())
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to delete profile")
        }
    }

    suspend fun verifyPin(profileId: String, pin: String): Result<VerifyPinResponse> {
        return try {
            val response = api.verifyPin(profileId, VerifyPinRequest(pin))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.message())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Invalid PIN")
        }
    }
}
