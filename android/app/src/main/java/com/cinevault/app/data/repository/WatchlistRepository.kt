package com.cinevault.app.data.repository

import com.cinevault.app.data.model.*
import com.cinevault.app.data.remote.CineVaultApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepository @Inject constructor(private val api: CineVaultApi) {

    suspend fun getWatchlist(profileId: String): Result<List<MovieDto>> {
        return try {
            val response = api.getWatchlist(profileId)
            if (response.isSuccessful && response.body() != null) {
                val movies = response.body()!!.map { it.contentId }
                Result.Success(movies)
            } else {
                Result.Error(response.message())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to load watchlist")
        }
    }

    suspend fun addToWatchlist(profileId: String, contentId: String): Result<Unit> {
        return try {
            val response = api.addToWatchlist(profileId, contentId)
            if (response.isSuccessful) Result.Success(Unit)
            else Result.Error(response.message())
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to add to watchlist")
        }
    }

    suspend fun removeFromWatchlist(profileId: String, contentId: String): Result<Unit> {
        return try {
            val response = api.removeFromWatchlist(profileId, contentId)
            if (response.isSuccessful) Result.Success(Unit)
            else Result.Error(response.message())
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to remove from watchlist")
        }
    }

    suspend fun isInWatchlist(profileId: String, contentId: String): Result<Boolean> {
        return try {
            val response = api.checkWatchlist(profileId, contentId)
            if (response.isSuccessful) Result.Success(response.body()?.inWatchlist == true)
            else Result.Error(response.message())
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to check watchlist")
        }
    }
}
