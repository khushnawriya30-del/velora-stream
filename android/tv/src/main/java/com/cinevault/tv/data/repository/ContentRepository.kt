package com.cinevault.tv.data.repository

import com.cinevault.tv.data.model.*
import com.cinevault.tv.data.remote.CineVaultApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepository @Inject constructor(private val api: CineVaultApi) {

    suspend fun getHomeFeed(section: String? = null): Result<List<HomeSectionDto>> = apiCall {
        api.getHomeFeed(section)
    }

    suspend fun getBanners(section: String? = null): Result<List<BannerDto>> = apiCall {
        api.getBanners(section)
    }

    suspend fun getMovies(
        page: Int = 1, limit: Int = 20, contentType: String? = null,
        genre: String? = null, language: String? = null, year: Int? = null,
        sort: String? = null,
    ): Result<MoviesListResponse> = apiCall {
        api.getMovies(page, limit, contentType, genre, language, year, sort = sort)
    }

    suspend fun getTrending(limit: Int = 20, contentType: String? = null): Result<List<MovieDto>> = apiCall {
        api.getTrending(limit, contentType)
    }

    suspend fun getNewReleases(limit: Int = 20): Result<List<MovieDto>> = apiCall {
        api.getNewReleases(limit)
    }

    suspend fun getTopRated(limit: Int = 20): Result<List<MovieDto>> = apiCall {
        api.getTopRated(limit)
    }

    suspend fun getMovie(id: String): Result<MovieDto> = apiCall {
        api.getMovie(id)
    }

    suspend fun getRelated(id: String): Result<List<MovieDto>> = apiCall {
        api.getRelated(id)
    }

    suspend fun trackView(id: String) {
        try { api.trackMovieView(id) } catch (_: Exception) {}
    }

    suspend fun getSeasons(seriesId: String): Result<List<SeasonDto>> = apiCall {
        api.getSeasons(seriesId)
    }

    suspend fun getEpisodes(seasonId: String): Result<List<EpisodeDto>> = apiCall {
        api.getEpisodes(seasonId)
    }

    suspend fun getEpisode(id: String): Result<EpisodeDto> = apiCall {
        api.getEpisode(id)
    }

    suspend fun search(
        query: String? = null, contentType: String? = null,
        genre: String? = null, language: String? = null,
        page: Int = 1, limit: Int = 20,
    ): Result<SearchResponse> = apiCall {
        api.search(query, contentType, genre, language, page = page, limit = limit)
    }

    suspend fun getGenres(): Result<List<String>> = apiCall { api.getGenres() }
    suspend fun getLanguages(): Result<List<String>> = apiCall { api.getLanguages() }

    suspend fun getStreamUrl(path: String): Result<SignedUrlResponse> = apiCall {
        api.getStreamUrl(path)
    }

    suspend fun getPremiumStatus(): Result<PremiumStatusResponse> = apiCall {
        api.getPremiumStatus()
    }

    suspend fun getMe(): Result<UserDto> = apiCall { api.getMe() }

    suspend fun getPremiumContent(limit: Int = 30): Result<List<MovieDto>> = apiCall {
        api.getPremiumContent(limit)
    }
}

suspend fun <T> apiCall(call: suspend () -> retrofit2.Response<T>): Result<T> {
    return try {
        val response = call()
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
