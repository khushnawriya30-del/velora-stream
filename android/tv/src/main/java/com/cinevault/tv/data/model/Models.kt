package com.cinevault.tv.data.model

import com.google.gson.annotations.SerializedName

// Auth
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String?,
    val user: UserDto,
)

data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val role: String,
    val isPremium: Boolean = false,
    val premiumPlan: String? = null,
    val premiumExpiresAt: String? = null,
)

data class MessageResponse(val message: String)

data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String?,
)

// QR Login
data class QrGenerateResponse(
    val token: String,
    val expiresAt: String,
)

data class QrCheckResponse(
    val status: String, // "pending" | "approved" | "expired"
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val user: UserDto? = null,
)

data class QrApproveRequest(val token: String)

// Content
data class MovieDto(
    @SerializedName("_id") val id: String,
    val title: String,
    val synopsis: String? = null,
    val contentType: String,
    val genres: List<String> = emptyList(),
    val languages: List<String>? = null,
    val contentRating: String? = null,
    val releaseYear: Int? = null,
    val duration: Int? = null,
    val director: String? = null,
    val cast: List<CastMemberDto>? = null,
    val posterUrl: String? = null,
    val bannerUrl: String? = null,
    val trailerUrl: String? = null,
    val streamingSources: List<StreamingSourceDto>? = null,
    val rating: Double? = null,
    val voteCount: Int? = null,
    val isPremium: Boolean? = false,
    val hasPremiumEpisode: Boolean? = false,
    val hlsUrl: String? = null,
    val ottPlatforms: List<String>? = null,
    val platformOrigin: String? = null,
)

data class CastMemberDto(
    val name: String,
    val role: String? = null,
    val character: String? = null,
    val photoUrl: String? = null,
)

data class StreamingSourceDto(
    val label: String,
    val url: String,
    val quality: String? = null,
    val priority: Int? = null,
)

data class HomeSectionDto(
    @SerializedName("_id") val id: String,
    val title: String,
    val type: String,
    val movies: List<MovieDto> = emptyList(),
    val order: Int = 0,
)

data class BannerDto(
    @SerializedName("_id") val id: String,
    val title: String? = null,
    val imageUrl: String,
    val movieId: String? = null,
    val linkUrl: String? = null,
    val isActive: Boolean = true,
)

data class SeasonDto(
    @SerializedName("_id") val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val title: String? = null,
    val episodeCount: Int,
)

data class EpisodeDto(
    @SerializedName("_id") val id: String,
    val seasonId: String,
    val episodeNumber: Int,
    val title: String,
    val duration: Int? = null,
    val streamingSources: List<StreamingSourceDto>? = null,
    val isPremium: Boolean = false,
    val hlsUrl: String? = null,
)

data class MoviesListResponse(
    val movies: List<MovieDto>,
    val total: Int,
    val page: Int,
    val totalPages: Int,
)

data class SearchResponse(
    val results: List<MovieDto>,
    val total: Int,
    val page: Int,
    val totalPages: Int,
)

data class SignedUrlResponse(val url: String)

data class PremiumStatusResponse(
    val isPremium: Boolean,
    val plan: String? = null,
    val expiresAt: String? = null,
)
