package com.cinevault.app.ui.viewmodel

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
    val availableQualities: List<String> = listOf("auto", "1080p", "720p", "480p"),
    val playbackSpeed: Float = 1.0f,
    val isFullscreen: Boolean = false,
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
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val movieResult = contentRepository.getMovie(contentId)) {
                is Result.Success -> {
                    val movie = movieResult.data
                    _uiState.update { it.copy(movie = movie) }
                    loadStreamingUrl()
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = movieResult.message) }
                is Result.Loading -> {}
            }
        }
    }

    private fun loadStreamingUrl() {
        viewModelScope.launch {
            val movie = _uiState.value.movie
            val streamPath = movie?.streamingSources?.firstOrNull()?.url ?: contentId
            when (val result = watchProgressRepository.getStreamingUrl(streamPath)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, streamingUrl = result.data.url)
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
        _uiState.update { it.copy(selectedQuality = quality) }
        loadStreamingUrl()
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
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
