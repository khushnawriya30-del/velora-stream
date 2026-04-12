package com.cinevault.tv.ui.screens.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.tv.data.model.EpisodeDto
import com.cinevault.tv.data.model.MovieDto
import com.cinevault.tv.data.model.SeasonDto
import com.cinevault.tv.data.remote.CineVaultApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerState(
    val movie: MovieDto? = null,
    val streamUrl: String? = null,
    val isHls: Boolean = false,
    val title: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val episodes: List<EpisodeDto> = emptyList(),
    val currentEpisodeIndex: Int = 0,
    val seasons: List<SeasonDto> = emptyList(),
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val api: CineVaultApi,
) : ViewModel() {

    private val movieId: String = savedStateHandle.get<String>("movieId") ?: ""

    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            _state.value = PlayerState(isLoading = true)
            try {
                val movieResponse = api.getMovie(movieId)
                val movie = movieResponse.body() ?: return@launch

                if (movie.contentType == "series") {
                    loadSeriesContent(movie)
                } else {
                    loadMovieContent(movie)
                }
            } catch (e: Exception) {
                _state.value = PlayerState(isLoading = false, error = "Failed to load content")
            }
        }
    }

    private suspend fun loadMovieContent(movie: MovieDto) {
        val streamUrl = resolveStreamUrl(movie.hlsUrl, movie.streamingSources?.firstOrNull()?.url)
        _state.value = PlayerState(
            movie = movie,
            streamUrl = streamUrl,
            isHls = movie.hlsUrl != null,
            title = movie.title,
            isLoading = false,
        )
    }

    private suspend fun loadSeriesContent(movie: MovieDto) {
        val seasonsResponse = api.getSeasons(movieId)
        val seasons = seasonsResponse.body() ?: emptyList()
        if (seasons.isEmpty()) {
            _state.value = PlayerState(isLoading = false, error = "No seasons available")
            return
        }

        val episodesResponse = api.getEpisodes(seasons[0].id)
        val episodes = episodesResponse.body() ?: emptyList()
        if (episodes.isEmpty()) {
            _state.value = PlayerState(isLoading = false, error = "No episodes available")
            return
        }

        val firstEp = episodes[0]
        val streamUrl = resolveStreamUrl(firstEp.hlsUrl, firstEp.streamingSources?.firstOrNull()?.url)

        _state.value = PlayerState(
            movie = movie,
            streamUrl = streamUrl,
            isHls = firstEp.hlsUrl != null,
            title = "Episode ${firstEp.episodeNumber}",
            isLoading = false,
            episodes = episodes,
            currentEpisodeIndex = 0,
            seasons = seasons,
        )
    }

    fun playEpisode(index: Int) {
        val episodes = _state.value.episodes
        if (index < 0 || index >= episodes.size) return

        viewModelScope.launch {
            val episode = episodes[index]
            val streamUrl = resolveStreamUrl(episode.hlsUrl, episode.streamingSources?.firstOrNull()?.url)
            _state.value = _state.value.copy(
                streamUrl = streamUrl,
                isHls = episode.hlsUrl != null,
                title = "Episode ${episode.episodeNumber}",
                currentEpisodeIndex = index,
            )
        }
    }

    fun playNextEpisode() {
        val next = _state.value.currentEpisodeIndex + 1
        if (next < _state.value.episodes.size) {
            playEpisode(next)
        }
    }

    private suspend fun resolveStreamUrl(hlsUrl: String?, sourceUrl: String?): String? {
        return when {
            hlsUrl != null -> hlsUrl
            sourceUrl != null -> {
                try {
                    val response = api.getStreamUrl(sourceUrl)
                    response.body()?.url ?: sourceUrl
                } catch (_: Exception) {
                    sourceUrl
                }
            }
            else -> null
        }
    }
}
