package com.cinevault.app.data.model

import com.google.gson.annotations.SerializedName

// Auth
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val name: String, val email: String, val password: String, val referralCode: String? = null)
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String?,
    val user: UserDto
)
data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String?,
)
data class ForgotPasswordRequest(val email: String)
data class ResetPasswordRequest(val token: String, val password: String)
data class MessageResponse(val message: String, val devOtp: String? = null)
data class GoogleTokenRequest(val idToken: String, val referralCode: String? = null)

// Phone OTP Auth
data class SendPhoneOtpRequest(val phone: String)
data class VerifyPhoneOtpRequest(val phone: String, val otp: String, val referralCode: String? = null)
data class FirebasePhoneRequest(val idToken: String, val referralCode: String? = null)

// Email OTP Auth
data class SendEmailOtpRequest(val email: String)
data class VerifyEmailOtpRequest(val email: String, val otp: String, val referralCode: String? = null)

// App Version (for auto-update)
data class AppVersionResponse(
    val versionCode: Int,
    val versionName: String,
    val forceUpdate: Boolean,
    val apkUrl: String,
    val releaseNotes: String?
)

// User
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val role: String,
    val authProvider: String? = null,
    val isEmailVerified: Boolean,
    val isPremium: Boolean = false,
    val premiumPlan: String? = null,
    val premiumExpiresAt: String? = null,
)

// Profile
data class ProfileDto(
    @SerializedName("_id") val id: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val maturityRating: String,
    val isActive: Boolean
) {
    val name: String get() = displayName
}
data class CreateProfileRequest(
    val displayName: String,
    val avatarUrl: String? = null,
    val maturityRating: String? = null,
    val pin: String? = null
)
data class VerifyPinRequest(val pin: String)
data class VerifyPinResponse(val valid: Boolean)

// Movie / Content
data class MovieDto(
    @SerializedName("_id") val id: String,
    val title: String,
    val alternateTitle: String?,
    val synopsis: String?,
    val contentType: String,
    val genres: List<String>,
    val languages: List<String>?,
    val contentRating: String?,
    val status: String?,
    val releaseYear: Int?,
    val country: String?,
    val duration: Int?,
    val director: String?,
    val studio: String?,
    val cast: List<CastMemberDto>?,
    val posterUrl: String?,
    val bannerUrl: String?,
    val logoUrl: String?,
    val trailerUrl: String?,
    val cbfcCertificateUrl: String?,
    val streamingSources: List<StreamingSourceDto>?,
    val rating: Double?,
    val starRating: Double?,
    val voteCount: Int?,
    val viewCount: Int?,
    val popularityScore: Double?,
    val tags: List<String>?,
    val platformOrigin: String?,
    val isFeatured: Boolean?,
    val isPremium: Boolean? = false,
    val freeEpisodeCount: Int? = 0,
    val rankingLabel: String?,
    val videoQuality: String?,
    val hlsUrl: String?,
    val releaseDate: String?,
    val createdAt: String?,
    val updatedAt: String?
) {
    val averageRating: Double get() = rating ?: 0.0
    val backdropUrl: String? get() = bannerUrl
    val description: String? get() = synopsis

    val languageLabel: String?
        get() {
            val langs = languages ?: return null
            if (langs.isEmpty()) return null
            if (langs.size >= 3) return "MULTILINGUAL"
            if (langs.size == 2) return "DUAL AUDIO"
            // Strip website names / source info after separators (e.g. "Hindi – Vegamovies" → "Hindi")
            val cleaned = langs.first()
                .split(Regex("[–\\-|(/]"))
                .first()
                .trim()
            return cleaned.ifBlank { langs.first() }.uppercase()
        }
}

data class CastMemberDto(
    val name: String,
    val role: String?,
    val character: String?,
    val photoUrl: String?
)

data class StreamingSourceDto(
    val label: String,
    val url: String,
    val quality: String?,
    val priority: Int?
)

data class MoviesListResponse(
    val movies: List<MovieDto>,
    val total: Int,
    val page: Int,
    val pages: Int
)

// Banner
data class BannerDto(
    @SerializedName("_id") val id: String,
    val contentId: Any?,
    val title: String? = null,
    val subtitle: String? = null,
    val imageUrl: String,
    val logoUrl: String?,
    val tagline: String?,
    val genreTags: List<String>?,
    val displayOrder: Int,
    val isActive: Boolean
) {
    /** Extract movie ID from contentId which can be:
     *  - a plain String (unpopulated ObjectId)
     *  - a populated Map with "_id" key (when backend populates the ref)
     */
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
                when (it) {
                    is Number -> it.toInt()
                    is String -> it.toIntOrNull()
                    else -> null
                }
            }
            else -> null
        }

    val contentRating: String?
        get() = when (contentId) {
            is Map<*, *> -> (contentId as Map<*, *>)["contentRating"]?.toString()
            else -> null
        }

    val starRating: Double?
        get() = when (contentId) {
            is Map<*, *> -> (contentId as Map<*, *>)["starRating"]?.let {
                when (it) {
                    is Number -> it.toDouble()
                    is String -> it.toDoubleOrNull()
                    else -> null
                }
            }
            else -> null
        }
}

// Home Feed
data class HomeSectionDto(
    val id: String,
    val title: String,
    val slug: String?,
    val type: String = "standard", // standard, large_card, mid_banner, trending
    val cardSize: String = "small", // small, medium, large
    val showViewMore: Boolean = true,
    val viewMoreText: String = "View More",
    val showTrendingNumbers: Boolean = false,
    val bannerImageUrl: String? = null,
    val contentId: String? = null, // For mid_banner: linked movie/show ID
    val isPremiumOnly: Boolean = false,
    val items: List<MovieDto> = emptyList()
)

// Season / Episode
data class SeasonDto(
    @SerializedName("_id") val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val title: String?,
    val synopsis: String?,
    val posterUrl: String?,
    val releaseYear: Int?,
    val episodeCount: Int
)

data class EpisodeDto(
    @SerializedName("_id") val id: String,
    val seasonId: String,
    val episodeNumber: Int,
    val title: String,
    val synopsis: String?,
    val duration: Int?,
    val airDate: String?,
    val thumbnailUrl: String?,
    val streamingSources: List<StreamingSourceDto>?,
    val skipIntro: SkipTimestampDto?,
    val skipRecap: SkipTimestampDto?,
    val subtitles: List<SubtitleTrackDto>?,
    val audioTracks: List<AudioTrackDto>?,
    val isPremium: Boolean = false,
)

data class SkipTimestampDto(val start: Int, val end: Int)
data class SubtitleTrackDto(val language: String, val url: String, val isDefault: Boolean)
data class AudioTrackDto(val language: String, val label: String?, val isDefault: Boolean)

// Watch Progress
data class WatchProgressDto(
    @SerializedName("_id") val id: String?,
    val contentId: String,
    val contentType: String,
    val currentTime: Int,
    val totalDuration: Int,
    val isCompleted: Boolean,
    val lastWatchedAt: String?,
    val episodeTitle: String?,
    val contentTitle: String?,
    val thumbnailUrl: String?,
    val seriesId: String? = null,  // populated when contentType == "episode"
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
    val thumbnailUrl: String? = null
)

// Watchlist
data class WatchlistItemDto(
    @SerializedName("_id") val id: String?,
    val contentId: MovieDto,
)
data class WatchlistCheckResponse(val inWatchlist: Boolean)

// Watch History paginated response
data class WatchHistoryResponse(
    val items: List<WatchProgressDto>,
    val total: Int,
)

// Review
data class ReviewDto(
    @SerializedName("_id") val id: String,
    val userId: Any?,
    val contentId: String,
    val rating: Int,
    val text: String?,
    val moderationStatus: String,
    val createdAt: String
) {
    val userName: String get() = (userId as? String) ?: "User"
}

data class CreateReviewRequest(
    val contentId: String,
    val rating: Int,
    val text: String? = null
)

data class ReviewsResponse(
    val reviews: List<ReviewDto>,
    val total: Int
)

// Search
data class SearchResponse(
    val results: List<MovieDto>,
    val total: Int,
    val page: Int,
    val pages: Int,
    val hasMore: Boolean
)

data class AutocompleteItem(
    @SerializedName("_id") val id: String,
    val title: String,
    val posterUrl: String?,
    val contentType: String?,
    val releaseYear: Int?
)

// Streaming
data class SignedUrlResponse(val url: String)

// Change Password
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

// OTP Verification
data class VerifyOtpRequest(val email: String, val otp: String)
data class VerifyOtpResponse(val message: String, val resetToken: String)

// Notification (user-facing)
data class NotificationDto(
    @SerializedName("_id") val id: String,
    val title: String,
    val body: String,
    val imageUrl: String?,
    val deepLink: String?,
    val isSent: Boolean,
    val sentAt: String?,
    val createdAt: String?,
)
data class NotificationsResponse(
    val notifications: List<NotificationDto>,
    val total: Int,
)

// Premium
data class ActivateCodeRequest(val code: String)
data class PremiumStatusResponse(
    val isPremium: Boolean,
    val plan: String?,
    val expiresAt: String?,
    val activatedAt: String?,
    val daysRemaining: Int?,
)
data class ActivateCodeResponse(
    val success: Boolean,
    val plan: String?,
    val expiresAt: String?,
    val durationDays: Int?,
)
data class PremiumPlanDto(
    val _id: String,
    val planId: String,
    val name: String,
    val months: Int,
    val price: Int,
    val originalPrice: Int,
    val discountPercent: Int,
    val badge: String? = null,
    val order: Int = 0,
    val isActive: Boolean = true,
)

// Razorpay Payment
data class RazorpayCreateOrderRequest(val planId: String)
data class RazorpayCreateOrderResponse(
    val orderId: String,
    val amount: Int, // in paise
    val currency: String,
    val keyId: String,
    val planName: String,
    val plan: String,
)
data class RazorpayVerifyRequest(
    val razorpay_payment_id: String,
    val razorpay_order_id: String,
    val razorpay_signature: String,
)
data class RazorpayVerifyResponse(
    val success: Boolean,
    val message: String?,
    val premiumPlan: String?,
    val premiumExpiresAt: String?,
    val daysRemaining: Int?,
    val alreadyActivated: Boolean? = null,
)

// UPI Payment
data class CreateOrderRequest(val planId: String, val deviceInfo: String? = null)
data class CreateOrderResponse(
    val orderId: String,
    val plan: String,
    val amount: Int,
    val upiId: String,
    val upiLink: String,
    val expiresAt: String,
)
data class CreatePaymentSessionRequest(val planId: String)
data class CreatePaymentSessionResponse(
    val paymentId: String,
    val amount: Double,
    val plan: String,
    val planName: String,
    val expiresAt: String,
    val upiId: String,
    val upiLink: String,
)
data class SubmitUtrRequest(val orderId: String, val utrId: String)
data class SubmitUtrResponse(
    val orderId: String,
    val status: String,
    val message: String,
)
data class OrderStatusResponse(
    val orderId: String,
    val plan: String,
    val amount: Int,
    val status: String,
    val utrId: String?,
    val activationCode: String?,
    val rejectionReason: String?,
    val createdAt: String,
)
data class VerifyPaymentRequest(
    val orderId: String,
    val status: String,
    val txnId: String? = null,
    val responseCode: String? = null,
    val approvalRefNo: String? = null,
)
data class VerifyPaymentResponse(
    val success: Boolean,
    val message: String?,
    val orderId: String?,
    val plan: String?,
    val premiumPlan: String?,
    val premiumExpiresAt: String?,
    val daysRemaining: Int?,
    val alreadyVerified: Boolean? = null,
)

// ── Wallet / Referral / Withdrawal ──
data class WalletBalanceResponse(
    val balance: Int,
    val totalEarned: Int,
    val totalWithdrawn: Int,
    val totalReferrals: Int,
    val withdrawThreshold: Int,
    val canWithdraw: Boolean,
    val amountNeeded: Int,
)

data class ReferralStatsResponse(
    val referralCode: String,
    val totalInvited: Int,
    val totalEarned: Int,
    val referrals: List<ReferralItem>,
)

data class CheckPendingReferralResponse(
    val referralCode: String?,
)

data class ReferralItem(
    val id: String,
    val amount: Int,
    val status: String,
    val createdAt: String,
)

data class EarningItem(
    val id: String,
    val type: String,
    val amount: Int,
    val description: String,
    val createdAt: String,
)

// Saved bank details (stored on wallet, filled once)
data class SaveBankDetailsRequest(
    val bankName: String,
    val accountNumber: String,
    val ifscCode: String,
    val accountHolderName: String,
    val phoneNumber: String,
    val email: String,
)

data class BankDetailsResponse(
    val bankName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val accountHolderName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val hasBankDetails: Boolean = false,
)

data class WithdrawRequest(
    val amount: Int,
    val upiId: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val accountHolderName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
)

data class WithdrawResponse(
    val id: String,
    val amount: Int,
    val upiId: String? = null,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val ifscCode: String? = null,
    val accountHolderName: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val status: String,
    val createdAt: String,
)

data class WithdrawalHistoryItem(
    val id: String,
    val amount: Int,
    val upiId: String? = null,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val ifscCode: String? = null,
    val accountHolderName: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val status: String,
    val rejectionReason: String? = null,
    val createdAt: String,
)

// ── Premium Offers ──
data class PremiumOfferDto(
    @SerializedName("_id") val id: String,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val bannerText: String? = null,
    val originalPrice: Int,
    val discountPrice: Int,
    val discountPercent: Int,
    val badgeText: String? = null,
    val planId: String = "1m",
    val durationMonths: Int = 1,
    val targetUserType: String = "non_premium",
    val offerType: String = "subscription",
    val isVisible: Boolean = true,
    val showAsPopup: Boolean = false,
    val order: Int = 0,
)

// ── Invite Settings ──
data class InviteSettingsDto(
    val targetAmount: Int = 100,
    val defaultBalance: Int = 80,
    val rewardPerInvite: Int = 1,
    val earnWindowDays: Int = 60,
    val isActive: Boolean = true,
)
