package com.cinevault.tv.ui.screens.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.tv.data.model.EpisodeDto
import com.cinevault.tv.data.model.MovieDto
import com.cinevault.tv.data.model.SeasonDto
import com.cinevault.tv.data.model.UpdateProgressRequest
import com.cinevault.tv.data.repository.ContentRepository
import com.cinevault.tv.data.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerState(
    val movie: MovieDto? = null,
    val streamUrl: String? = null,
    val isHls: Boolean = false,
    val title: String = "",
    val episodeTitle: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val episodes: List<EpisodeDto> = emptyList(),
    val currentEpisodeIndex: Int = 0,
    val seasons: List<SeasonDto> = emptyList(),
    val currentEpisodeId: String? = null,
    val resumePositionMs: Long = 0,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepo: ContentRepository,
    private val progressRepo: WatchProgressRepository,
) : ViewModel() {

    private val movieId: String = savedStateHandle.get<String>("movieId") ?: ""
    private val initialEpisodeId: String? = savedStateHandle.get<String>("episodeId")?.takeIf { it.isNotBlank() }
    private val initialSeasonId: String? = savedStateHandle.get<String>("seasonId")?.takeIf { it.isNotBlank() }

    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    private var progressSaveJob: Job? = null

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            _state.value = PlayerState(isLoading = true)
            try {
                val movie = contentRepo.getMovie(movieId).getOrNull() ?: run {
                    _state.value = PlayerState(isLoading = false, error = "Failed to load content")
                    return@launch
                }

                if (movie.contentType == "series" || movie.contentType == "anime") {
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

        // Check for resume position
        val progress = progressRepo.getProgress(movieId).getOrNull()
        val resumeMs = progress?.let { it.currentTime.toLong() * 1000 } ?: 0L

        _state.value = PlayerState(
            movie = movie,
            streamUrl = streamUrl,
            isHls = streamUrl?.contains(".m3u8") == true,
            title = movie.title,
            isLoading = false,
            resumePositionMs = resumeMs,
        )
    }

    private suspend fun loadSeriesContent(movie: MovieDto) {
        val seasons = contentRepo.getSeasons(movieId).getOrElse { emptyList() }
        if (seasons.isEmpty()) {
            _state.value = PlayerState(isLoading = false, error = "No seasons available")
            return
        }

        // Determine which season to load
        val targetSeasonId = initialSeasonId ?: seasons[0].id
        val episodes = contentRepo.getEpisodes(targetSeasonId).getOrElse { emptyList() }
        if (episodes.isEmpty()) {
            _state.value = PlayerState(isLoading = false, error = "No episodes available")
            return
        }

        // Determine which episode to play
        val targetEpIndex = if (initialEpisodeId != null) {
            episodes.indexOfFirst { it.id == initialEpisodeId }.coerceAtLeast(0)
        } else 0

        val episode = episodes[targetEpIndex]
        val streamUrl = resolveStreamUrl(null, episode.streamingSources?.firstOrNull()?.url)

        // Resume position
        val progress = progressRepo.getProgress(episode.id).getOrNull()
        val resumeMs = progress?.let { it.currentTime.toLong() * 1000 } ?: 0L

        _state.value = PlayerState(
            movie = movie,
            streamUrl = streamUrl,
            isHls = streamUrl?.contains(".m3u8") == true,
            title = "S${seasons.indexOfFirst { it.id == targetSeasonId } + 1}:E${episode.episodeNumber}",
            episodeTitle = episode.title,
            isLoading = false,
            episodes = episodes,
            currentEpisodeIndex = targetEpIndex,
            currentEpisodeId = episode.id,
            seasons = seasons,
            resumePositionMs = resumeMs,
        )
    }

    fun playEpisode(index: Int) {
        val episodes = _state.value.episodes
        if (index < 0 || index >= episodes.size) return

        viewModelScope.launch {
            val episode = episodes[index]
            val streamUrl = resolveStreamUrl(null, episode.streamingSources?.firstOrNull()?.url)
            _state.value = _state.value.copy(
                streamUrl = streamUrl,
                isHls = streamUrl?.contains(".m3u8") == true,
                title = "E${episode.episodeNumber}",
                episodeTitle = episode.title,
                currentEpisodeIndex = index,
                currentEpisodeId = episode.id,
                resumePositionMs = 0,
            )
        }
    }

    fun playNextEpisode() {
        val next = _state.value.currentEpisodeIndex + 1
        if (next < _state.value.episodes.size) {
            playEpisode(next)
        }
    }

    fun playPreviousEpisode() {
        val prev = _state.value.currentEpisodeIndex - 1
        if (prev >= 0) {
            playEpisode(prev)
        }
    }

    fun saveProgress(currentPositionMs: Long, totalDurationMs: Long) {
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            delay(3000) // Debounce — save every 3 seconds
            val s = _state.value
            val contentId = s.currentEpisodeId ?: movieId
            val contentType = if (s.episodes.isNotEmpty()) "episode" else (s.movie?.contentType ?: "movie")

            progressRepo.updateProgress(
                UpdateProgressRequest(
                    contentId = contentId,
                    contentType = contentType,
                    currentTime = (currentPositionMs / 1000).toInt(),
                    totalDuration = (totalDurationMs / 1000).toInt(),
                    seriesId = if (s.episodes.isNotEmpty()) movieId else null,
                    episodeTitle = s.episodeTitle,
                    contentTitle = s.movie?.title,
                )
            )
        }
    }

    private suspend fun resolveStreamUrl(hlsUrl: String?, sourceUrl: String?): String? {
        return when {
            hlsUrl != null -> hlsUrl
            sourceUrl != null -> {
                try {
                    contentRepo.getStreamUrl(sourceUrl).getOrNull()?.url ?: sourceUrl
                } catch (_: Exception) { sourceUrl }
            }
            else -> null
        }
    }
}
