package com.cinevault.app.data.repository

import com.cinevault.app.data.model.*
import com.cinevault.app.data.remote.CineVaultApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchProgressRepository @Inject constructor(private val api: CineVaultApi) {

    suspend fun updateProgress(contentId: String, profileId: String, position: Long, duration: Long): Result<WatchProgressDto> {
        return try {
            val request = UpdateProgressRequest(
                contentId = contentId,
                contentType = "movie",
                currentTime = position.toInt(),
                totalDuration = duration.toInt(),
            )
            val response = api.updateProgress(profileId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.message())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to update progress")
        }
    }

    suspend fun getContinueWatching(profileId: String): Result<List<WatchProgressDto>> {
        return try {
            val response = api.getContinueWatching(profileId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.message())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to load continue watching")
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getWatchHistory(profileId: String): Result<List<WatchProgressDto>> {
        return try {
            val response = api.getWatchHistory(profileId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val items = (body["items"] as? List<*>)?.filterIsInstance<WatchProgressDto>() ?: emptyList()
                Result.Success(items)
            } else {
                Result.Error(response.message())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to load watch history")
        }
    }

    suspend fun getStreamingUrl(path: String): Result<SignedUrlResponse> {
        return try {
            val response = api.getStreamUrl(path)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.message())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to get streaming URL")
        }
    }
}
