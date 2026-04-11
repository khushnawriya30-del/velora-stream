package com.cinevault.app.data.repository

import com.cinevault.app.data.model.*
import com.cinevault.app.data.remote.CineVaultApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThematicCollectionRepository @Inject constructor(private val api: CineVaultApi) {

    suspend fun getCollection(profileId: String): Result<List<MovieDto>> {
        return try {
            val response = api.getThematicCollection(profileId)
            if (response.isSuccessful && response.body() != null) {
                val movies = response.body()!!.map { it.contentId }
                Result.Success(movies)
            } else {
                Result.Error(response.message())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to load thematic collection")
        }
    }

    suspend fun addToCollection(profileId: String, contentId: String): Result<Unit> {
        return try {
            val response = api.addToThematicCollection(profileId, contentId)
            if (response.isSuccessful) Result.Success(Unit)
            else Result.Error(response.message())
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to add to collection")
        }
    }

    suspend fun removeFromCollection(profileId: String, contentId: String): Result<Unit> {
        return try {
            val response = api.removeFromThematicCollection(profileId, contentId)
            if (response.isSuccessful) Result.Success(Unit)
            else Result.Error(response.message())
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to remove from collection")
        }
    }
}
