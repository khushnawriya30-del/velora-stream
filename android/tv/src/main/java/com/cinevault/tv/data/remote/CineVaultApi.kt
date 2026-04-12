package com.cinevault.tv.data.remote

import com.cinevault.tv.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface CineVaultApi {

    // QR Login
    @POST("auth/tv/qr-generate")
    suspend fun generateQrToken(): Response<QrGenerateResponse>

    @POST("auth/tv/qr-check")
    suspend fun checkQrToken(@Body body: Map<String, String>): Response<QrCheckResponse>

    // Home & Content
    @GET("home/feed")
    suspend fun getHomeFeed(@Query("section") section: String? = null): Response<List<HomeSectionDto>>

    @GET("banners")
    suspend fun getBanners(@Query("section") section: String? = null): Response<List<BannerDto>>

    @GET("movies")
    suspend fun getMovies(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("contentType") contentType: String? = null,
        @Query("genre") genre: String? = null,
        @Query("language") language: String? = null,
        @Query("year") year: Int? = null,
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

    // Series
    @GET("series/{seriesId}/seasons")
    suspend fun getSeasons(@Path("seriesId") seriesId: String): Response<List<SeasonDto>>

    @GET("series/seasons/{seasonId}/episodes")
    suspend fun getEpisodes(@Path("seasonId") seasonId: String): Response<List<EpisodeDto>>

    // Search
    @GET("search")
    suspend fun search(
        @Query("q") query: String? = null,
        @Query("contentType") contentType: String? = null,
        @Query("genre") genre: String? = null,
        @Query("language") language: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<SearchResponse>

    // Streaming
    @GET("streaming/url")
    suspend fun getStreamUrl(@Query("path") path: String): Response<SignedUrlResponse>

    // Premium
    @GET("premium/status")
    suspend fun getPremiumStatus(): Response<PremiumStatusResponse>

    // User
    @GET("users/me")
    suspend fun getMe(): Response<UserDto>

    // Token refresh
    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: Map<String, String>): Response<RefreshResponse>
}
