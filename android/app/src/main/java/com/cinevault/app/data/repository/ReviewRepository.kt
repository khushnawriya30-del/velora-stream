package com.cinevault.app.data.repository

import com.cinevault.app.data.model.*
import com.cinevault.app.data.remote.CineVaultApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(private val api: CineVaultApi) {

    suspend fun getReviews(contentId: String, page: Int = 1): Result<List<ReviewDto>> {
        return try {
            val response = api.getReviews(contentId, page)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.reviews)
            } else {
                Result.Error(response.message())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to load reviews")
        }
    }

    suspend fun createReview(contentId: String, rating: Double, text: String): Result<ReviewDto> {
        return try {
            val request = CreateReviewRequest(
                contentId = contentId,
                rating = rating.toInt(),
                text = text,
            )
            val response = api.createReview(request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.message())
            }
        } catch (e: Exception) {
            Result.Error(e.localizedMessage ?: "Failed to submit review")
        }
    }
}
