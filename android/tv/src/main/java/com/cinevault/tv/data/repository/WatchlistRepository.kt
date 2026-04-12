package com.cinevault.tv.data.repository

import com.cinevault.tv.data.local.SessionManager
import com.cinevault.tv.data.model.*
import com.cinevault.tv.data.remote.CineVaultApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepository @Inject constructor(
    private val api: CineVaultApi,
    private val session: SessionManager,
) {
    private suspend fun profileId() = session.getProfileIdSync() ?: session.getUserIdSync() ?: ""

    suspend fun getWatchlist(): Result<List<WatchlistItemDto>> = apiCall {
        api.getWatchlist(profileId())
    }

    suspend fun addToWatchlist(contentId: String): Result<MessageResponse> = apiCall {
        api.addToWatchlist(profileId(), contentId)
    }

    suspend fun removeFromWatchlist(contentId: String): Result<MessageResponse> = apiCall {
        api.removeFromWatchlist(profileId(), contentId)
    }

    suspend fun checkWatchlist(contentId: String): Boolean {
        return try {
            val res = api.checkWatchlist(profileId(), contentId)
            res.body()?.inWatchlist == true
        } catch (_: Exception) {
            false
        }
    }
}
