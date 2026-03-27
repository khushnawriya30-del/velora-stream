package com.cinevault.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.app.data.local.SessionManager
import com.cinevault.app.data.model.*
import com.cinevault.app.data.repository.ContentRepository
import com.cinevault.app.data.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val movie: MovieDto? = null,
    val streamingUrl: String? = null,
    val episodes: List<EpisodeDto> = emptyList(),
    val currentEpisodeIndex: Int = 0,
    val showControls: Boolean = true,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val totalDuration: Long = 0L,
    val selectedQuality: String = "auto",
    val availableQualities: List<String> = listOf("auto"),
    val isAdaptive: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val isFullscreen: Boolean = true,
    val availableAudioTracks: List<String> = emptyList(),
    val selectedAudioTrack: String = "default",
    val isSpeedOverride: Boolean = false,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val contentId: String = savedStateHandle.get<String>("contentId") ?: ""
    private val episodeId: String? = savedStateHandle.get<String>("episodeId")

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null

    init {
        if (contentId.isNotEmpty()) loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            Log.d("CineVaultPlayer", "Loading content: $contentId")
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val movieResult = contentRepository.getMovie(contentId)) {
                is Result.Success -> {
                    val movie = movieResult.data
                    Log.d("CineVaultPlayer", "Movie loaded: ${movie.title}, hlsUrl=${movie.hlsUrl}, sources=${movie.streamingSources?.size ?: 0}")
                    movie.streamingSources?.forEachIndexed { i, src ->
                        Log.d("CineVaultPlayer", "  Source[$i]: quality=${src.quality}, url=${src.url.take(100)}")
                    }
                    _uiState.update { it.copy(movie = movie) }
                    loadStreamingUrl()
                }
                is Result.Error -> {
                    Log.e("CineVaultPlayer", "Failed to load movie: ${movieResult.message}")
                    _uiState.update { it.copy(isLoading = false, error = movieResult.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun loadStreamingUrl(resumePosition: Long = -1L) {
        viewModelScope.launch {
            val movie = _uiState.value.movie

            // Prefer HLS URL for adaptive streaming (auto quality based on internet speed)
            val hlsUrl = movie?.hlsUrl
            if (!hlsUrl.isNullOrBlank()) {
                val qualities = listOf("auto", "1080p", "720p", "480p", "360p")
                _uiState.update { it.copy(
                    isLoading = false,
                    streamingUrl = hlsUrl,
                    availableQualities = qualities,
                    isAdaptive = true,
                    currentPosition = if (resumePosition >= 0) resumePosition else it.currentPosition,
                ) }
                return@launch
            }

            // Fallback: use direct streaming source URL (single file — no quality switching)
            val sources = movie?.streamingSources ?: emptyList()

            // For multiple sources with different qualities, allow switching between them
            val hasMultipleSources = sources.size > 1
            val qualities = if (hasMultipleSources) {
                listOf("auto") + sources.mapNotNull { it.quality }.distinct()
            } else {
                // Single file: just show the detected quality
                val sourceQuality = sources.firstOrNull()?.quality ?: "Original"
                listOf(sourceQuality)
            }

            // Pick source based on selected quality
            val selectedQuality = _uiState.value.selectedQuality
            val source = if (selectedQuality == "auto" || !hasMultipleSources) {
                sources.minByOrNull { it.priority ?: Int.MAX_VALUE }
            } else {
                // Try exact match first, then fall back to best available
                sources.find { it.quality?.lowercase() == selectedQuality.lowercase() }
                    ?: sources.minByOrNull { it.priority ?: Int.MAX_VALUE }
            }

            val streamUrl = source?.url ?: ""

            if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://")) {
                _uiState.update { it.copy(
                    isLoading = false,
                    streamingUrl = streamUrl,
                    availableQualities = qualities,
                    isAdaptive = hasMultipleSources,
                    selectedQuality = if (!hasMultipleSources) (sources.firstOrNull()?.quality ?: "Original") else it.selectedQuality,
                    currentPosition = if (resumePosition >= 0) resumePosition else it.currentPosition,
                ) }
                return@launch
            }

            val streamPath = streamUrl.ifEmpty { contentId }
            when (val result = watchProgressRepository.getStreamingUrl(streamPath)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        streamingUrl = result.data.url,
                        availableQualities = qualities,
                        currentPosition = if (resumePosition >= 0) resumePosition else it.currentPosition,
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    fun onPlaybackStateChange(isPlaying: Boolean) {
        _uiState.update { it.copy(isPlaying = isPlaying) }
    }

    fun onPositionChange(position: Long, duration: Long) {
        _uiState.update { it.copy(currentPosition = position, totalDuration = duration) }
        scheduleProgressUpdate(position, duration)
    }

    private fun scheduleProgressUpdate(position: Long, duration: Long) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            delay(10_000) // save every 10 seconds
            val profileId = sessionManager.activeProfileId.firstOrNull() ?: return@launch
            watchProgressRepository.updateProgress(contentId, profileId, position, duration)
        }
    }

    fun saveProgressNow() {
        viewModelScope.launch {
            val state = _uiState.value
            val profileId = sessionManager.activeProfileId.firstOrNull() ?: return@launch
            if (state.totalDuration > 0) {
                watchProgressRepository.updateProgress(
                    contentId, profileId, state.currentPosition, state.totalDuration
                )
            }
        }
    }

    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    fun setQuality(quality: String) {
        val oldQuality = _uiState.value.selectedQuality
        _uiState.update { it.copy(selectedQuality = quality) }
        // For multiple sources (non-HLS), reload URL with new quality source
        val movie = _uiState.value.movie
        if (movie?.hlsUrl.isNullOrBlank() && (movie?.streamingSources?.size ?: 0) > 1 && quality != oldQuality) {
            val currentPos = _uiState.value.currentPosition
            loadStreamingUrl(resumePosition = currentPos)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setSpeedOverride(active: Boolean) {
        _uiState.update { it.copy(isSpeedOverride = active) }
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun seekTo(positionMs: Long) {
        _uiState.update { it.copy(currentPosition = positionMs) }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
    }
}
