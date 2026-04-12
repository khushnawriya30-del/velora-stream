package com.cinevault.tv.data.remote

import com.cinevault.tv.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface CineVaultApi {

    // ── App Version ──
    @GET("app-version?platform=tv")
    suspend fun getAppVersion(): Response<AppVersionResponse>

    // ── QR Login ──
    @POST("auth/tv/qr-generate")
    suspend fun generateQrToken(): Response<QrGenerateResponse>

    @POST("auth/tv/qr-check")
    suspend fun checkQrToken(@Body body: Map<String, String>): Response<QrCheckResponse>

    // ── Token Refresh ──
    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: Map<String, String>): Response<RefreshResponse>

    // ── Logout ──
    @POST("auth/logout")
    suspend fun logout(): Response<MessageResponse>

    // ── User ──
    @GET("users/me")
    suspend fun getMe(): Response<UserDto>

    // ── Profiles ──
    @GET("profiles")
    suspend fun getProfiles(): Response<List<ProfileDto>>

    // ── Home & Banners ──
    @GET("home/feed")
    suspend fun getHomeFeed(@Query("section") section: String? = null): Response<List<HomeSectionDto>>

    @GET("banners")
    suspend fun getBanners(@Query("section") section: String? = null): Response<List<BannerDto>>

    // ── Movies ──
    @GET("movies")
    suspend fun getMovies(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("contentType") contentType: String? = null,
        @Query("genre") genre: String? = null,
        @Query("language") language: String? = null,
        @Query("year") year: Int? = null,
        @Query("rating") rating: Double? = null,
        @Query("sort") sort: String? = null,
    ): Response<MoviesListResponse>

    @GET("movies/trending")
    suspend fun getTrending(
        @Query("limit") limit: Int = 20,
        @Query("contentType") contentType: String? = null,
    ): Response<List<MovieDto>>

    @GET("movies/new-releases")
    suspend fun getNewReleases(@Query("limit") limit: Int = 20): Response<List<MovieDto>>

    @GET("movies/top-rated")
    suspend fun getTopRated(@Query("limit") limit: Int = 20): Response<List<MovieDto>>

    @GET("movies/{id}")
    suspend fun getMovie(@Path("id") id: String): Response<MovieDto>

    @GET("movies/{id}/related")
    suspend fun getRelated(@Path("id") id: String): Response<List<MovieDto>>

    @POST("movies/{id}/view")
    suspend fun trackMovieView(@Path("id") id: String): Response<MessageResponse>

    @GET("movies/premium")
    suspend fun getPremiumContent(@Query("limit") limit: Int = 30): Response<List<MovieDto>>

    // ── Series ──
    @GET("series/{seriesId}/seasons")
    suspend fun getSeasons(@Path("seriesId") seriesId: String): Response<List<SeasonDto>>

    @GET("series/seasons/{seasonId}/episodes")
    suspend fun getEpisodes(@Path("seasonId") seasonId: String): Response<List<EpisodeDto>>

    @GET("series/episodes/{id}")
    suspend fun getEpisode(@Path("id") id: String): Response<EpisodeDto>

    @POST("series/episodes/{id}/view")
    suspend fun trackEpisodeView(@Path("id") id: String): Response<MessageResponse>

    // ── Search ──
    @GET("search")
    suspend fun search(
        @Query("q") query: String? = null,
        @Query("contentType") contentType: String? = null,
        @Query("genre") genre: String? = null,
        @Query("language") language: String? = null,
        @Query("yearMin") yearMin: Int? = null,
        @Query("yearMax") yearMax: Int? = null,
        @Query("ratingMin") ratingMin: Double? = null,
        @Query("sort") sort: String? = null,
        @Query("platform") platform: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<SearchResponse>

    @GET("search/autocomplete")
    suspend fun autocomplete(@Query("q") query: String): Response<List<AutocompleteItem>>

    @GET("search/genres")
    suspend fun getGenres(): Response<List<String>>

    @GET("search/languages")
    suspend fun getLanguages(): Response<List<String>>

    // ── Watch Progress ──
    @POST("watch-progress")
    suspend fun updateProgress(
        @Header("x-profile-id") profileId: String,
        @Body request: UpdateProgressRequest,
    ): Response<WatchProgressDto>

    @GET("watch-progress/continue-watching")
    suspend fun getContinueWatching(
        @Header("x-profile-id") profileId: String,
    ): Response<List<WatchProgressDto>>

    @GET("watch-progress/history")
    suspend fun getWatchHistory(
        @Header("x-profile-id") profileId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<WatchHistoryResponse>

    @GET("watch-progress/latest-for-series/{seriesId}")
    suspend fun getLatestEpisodeForSeries(
        @Header("x-profile-id") profileId: String,
        @Path("seriesId") seriesId: String,
    ): Response<WatchProgressDto?>

    @GET("watch-progress/{contentId}")
    suspend fun getProgress(
        @Header("x-profile-id") profileId: String,
        @Path("contentId") contentId: String,
    ): Response<WatchProgressDto?>

    // ── Watchlist ──
    @GET("watchlist")
    suspend fun getWatchlist(
        @Header("x-profile-id") profileId: String,
    ): Response<List<WatchlistItemDto>>

    @POST("watchlist/{contentId}")
    suspend fun addToWatchlist(
        @Header("x-profile-id") profileId: String,
        @Path("contentId") contentId: String,
    ): Response<MessageResponse>

    @DELETE("watchlist/{contentId}")
    suspend fun removeFromWatchlist(
        @Header("x-profile-id") profileId: String,
        @Path("contentId") contentId: String,
    ): Response<MessageResponse>

    @GET("watchlist/{contentId}/check")
    suspend fun checkWatchlist(
        @Header("x-profile-id") profileId: String,
        @Path("contentId") contentId: String,
    ): Response<WatchlistCheckResponse>

    // ── Streaming ──
    @GET("streaming/url")
    suspend fun getStreamUrl(@Query("path") path: String): Response<SignedUrlResponse>

    // ── Premium ──
    @GET("premium/status")
    suspend fun getPremiumStatus(): Response<PremiumStatusResponse>
}
