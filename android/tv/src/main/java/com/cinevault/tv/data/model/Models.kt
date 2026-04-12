package com.cinevault.tv.data.model

import com.google.gson.annotations.SerializedName

// ── Auth ──
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String?,
    val user: UserDto,
)

data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String?,
)

data class MessageResponse(val message: String)

// ── App Version ──
data class AppVersionResponse(
    val versionCode: Int,
    val versionName: String,
    val forceUpdate: Boolean = false,
    val apkUrl: String = "",
    val releaseNotes: String? = null,
)

// ── QR Login ──
data class QrGenerateResponse(
    val token: String,
    val expiresAt: String,
)

data class QrCheckResponse(
    val status: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val user: UserDto? = null,
)

// ── User ──
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val role: String,
    val authProvider: String? = null,
    val isEmailVerified: Boolean = false,
    val isPremium: Boolean = false,
    val premiumPlan: String? = null,
    val premiumExpiresAt: String? = null,
)

// ── Profile ──
data class ProfileDto(
    @SerializedName("_id") val id: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val maturityRating: String,
    val isActive: Boolean
)

// ── Movie / Content ──
data class MovieDto(
    @SerializedName("_id") val id: String,
    val title: String,
    val alternateTitle: String? = null,
    val synopsis: String? = null,
    val contentType: String,
    val genres: List<String> = emptyList(),
    val languages: List<String>? = null,
    val contentRating: String? = null,
    val status: String? = null,
    val releaseYear: Int? = null,
    val country: String? = null,
    val duration: Int? = null,
    val director: String? = null,
    val studio: String? = null,
    val cast: List<CastMemberDto>? = null,
    val posterUrl: String? = null,
    val bannerUrl: String? = null,
    val logoUrl: String? = null,
    val trailerUrl: String? = null,
    val streamingSources: List<StreamingSourceDto>? = null,
    val rating: Double? = null,
    val starRating: Double? = null,
    val voteCount: Int? = null,
    val viewCount: Int? = null,
    val popularityScore: Double? = null,
    val tags: List<String>? = null,
    val platformOrigin: String? = null,
    val isFeatured: Boolean? = null,
    val isPremium: Boolean? = false,
    val hasPremiumEpisode: Boolean? = false,
    val freeEpisodeCount: Int? = 0,
    val rankingLabel: String? = null,
    val videoQuality: String? = null,
    val hlsUrl: String? = null,
    val releaseDate: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val averageRating: Double get() = rating ?: starRating ?: 0.0
    val backdropUrl: String? get() = bannerUrl
    val description: String? get() = synopsis
    val isEffectivelyPremium: Boolean get() = isPremium == true || hasPremiumEpisode == true

    val languageLabel: String?
        get() {
            val langs = languages ?: return null
            if (langs.isEmpty()) return null
            if (langs.size >= 3) return "MULTILINGUAL"
            if (langs.size == 2) return "DUAL AUDIO"
            val cleaned = langs.first()
                .split(Regex("[–\\-|(/]"))
                .first()
                .trim()
            return cleaned.ifBlank { langs.first() }.uppercase()
        }
}

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

data class MoviesListResponse(
    val movies: List<MovieDto>,
    val total: Int,
    val page: Int,
    val pages: Int,
)

// ── Banner ──
data class BannerDto(
    @SerializedName("_id") val id: String,
    val contentId: Any? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val imageUrl: String,
    val logoUrl: String? = null,
    val tagline: String? = null,
    val genreTags: List<String>? = null,
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
) {
    val contentIdString: String?
        get() = when (contentId) {
            is String -> contentId
            is Map<*, *> -> (contentId as Map<*, *>)["_id"]?.toString()
            else -> null
        }
    val contentType: String?
        get() = when (contentId) {
            is Map<*, *> -> (contentId as Map<*, *>)["contentType"]?.toString()
            else -> null
        }
    val releaseYear: Int?
        get() = when (contentId) {
            is Map<*, *> -> (contentId as Map<*, *>)["releaseYear"]?.let {
                when (it) { is Number -> it.toInt(); is String -> it.toIntOrNull(); else -> null }
            }
            else -> null
        }
    val starRating: Double?
        get() = when (contentId) {
            is Map<*, *> -> (contentId as Map<*, *>)["starRating"]?.let {
                when (it) { is Number -> it.toDouble(); is String -> it.toDoubleOrNull(); else -> null }
            }
            else -> null
        }
}

// ── Home Feed ──
data class HomeSectionDto(
    val id: String,
    val title: String,
    val slug: String? = null,
    val type: String = "standard",
    val cardSize: String = "small",
    val showViewMore: Boolean = true,
    val viewMoreText: String = "View More",
    val showTrendingNumbers: Boolean = false,
    val bannerImageUrl: String? = null,
    val contentId: String? = null,
    val isPremiumOnly: Boolean = false,
    val items: List<MovieDto> = emptyList(),
)

// ── Season / Episode ──
data class SeasonDto(
    @SerializedName("_id") val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val title: String? = null,
    val synopsis: String? = null,
    val posterUrl: String? = null,
    val releaseYear: Int? = null,
    val episodeCount: Int = 0,
)

data class EpisodeDto(
    @SerializedName("_id") val id: String,
    val seasonId: String,
    val episodeNumber: Int,
    val title: String,
    val synopsis: String? = null,
    val duration: Int? = null,
    val airDate: String? = null,
    val thumbnailUrl: String? = null,
    val streamingSources: List<StreamingSourceDto>? = null,
    val skipIntro: SkipTimestampDto? = null,
    val skipRecap: SkipTimestampDto? = null,
    val subtitles: List<SubtitleTrackDto>? = null,
    val audioTracks: List<AudioTrackDto>? = null,
    val isPremium: Boolean = false,
)

data class SkipTimestampDto(val start: Int, val end: Int)
data class SubtitleTrackDto(val language: String, val url: String, val isDefault: Boolean)
data class AudioTrackDto(val language: String, val label: String?, val isDefault: Boolean)

// ── Watch Progress ──
data class WatchProgressDto(
    @SerializedName("_id") val id: String?,
    val contentId: String,
    val contentType: String,
    val currentTime: Int,
    val totalDuration: Int,
    val isCompleted: Boolean,
    val lastWatchedAt: String? = null,
    val episodeTitle: String? = null,
    val contentTitle: String? = null,
    val thumbnailUrl: String? = null,
    val seriesId: String? = null,
) {
    val duration: Int get() = totalDuration
    val position: Int get() = currentTime
}

data class UpdateProgressRequest(
    val contentId: String,
    val contentType: String,
    val currentTime: Int,
    val totalDuration: Int,
    val seriesId: String? = null,
    val episodeTitle: String? = null,
    val contentTitle: String? = null,
    val thumbnailUrl: String? = null,
)

// ── Watchlist ──
data class WatchlistItemDto(
    @SerializedName("_id") val id: String?,
    val contentId: MovieDto,
)
data class WatchlistCheckResponse(val inWatchlist: Boolean)

// ── Watch History ──
data class WatchHistoryResponse(
    val items: List<WatchProgressDto>,
    val total: Int,
)

// ── Search ──
data class SearchResponse(
    val results: List<MovieDto>,
    val total: Int,
    val page: Int,
    val pages: Int,
    val hasMore: Boolean = false,
)

data class AutocompleteItem(
    @SerializedName("_id") val id: String,
    val title: String,
    val posterUrl: String? = null,
    val contentType: String? = null,
    val releaseYear: Int? = null,
)

// ── Streaming ──
data class SignedUrlResponse(val url: String)

// ── Premium ──
data class PremiumStatusResponse(
    val isPremium: Boolean,
    val plan: String? = null,
    val expiresAt: String? = null,
    val activatedAt: String? = null,
    val daysRemaining: Int? = null,
)
