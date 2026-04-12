package com.cinevault.tv.data.repository

import com.cinevault.tv.data.local.SessionManager
import com.cinevault.tv.data.model.*
import com.cinevault.tv.data.remote.CineVaultApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchProgressRepository @Inject constructor(
    private val api: CineVaultApi,
    private val session: SessionManager,
) {
    private suspend fun profileId() = session.getProfileIdSync() ?: session.getUserIdSync() ?: ""

    suspend fun updateProgress(request: UpdateProgressRequest): Result<WatchProgressDto> = apiCall {
        api.updateProgress(profileId(), request)
    }

    suspend fun getContinueWatching(): Result<List<WatchProgressDto>> = apiCall {
        api.getContinueWatching(profileId())
    }

    suspend fun getWatchHistory(page: Int = 1): Result<WatchHistoryResponse> = apiCall {
        api.getWatchHistory(profileId(), page)
    }

    suspend fun getProgress(contentId: String): Result<WatchProgressDto?> {
        return try {
            val response = api.getProgress(profileId(), contentId)
            if (response.isSuccessful) Result.success(response.body())
            else Result.success(null)
        } catch (e: Exception) {
            Result.success(null)
        }
    }

    suspend fun getLatestEpisodeForSeries(seriesId: String): Result<WatchProgressDto?> {
        return try {
            val response = api.getLatestEpisodeForSeries(profileId(), seriesId)
            if (response.isSuccessful) Result.success(response.body())
            else Result.success(null)
        } catch (e: Exception) {
            Result.success(null)
        }
    }
}
