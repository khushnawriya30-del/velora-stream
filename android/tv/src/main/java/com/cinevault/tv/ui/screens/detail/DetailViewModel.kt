package com.cinevault.tv.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.tv.data.model.EpisodeDto
import com.cinevault.tv.data.model.MovieDto
import com.cinevault.tv.data.model.SeasonDto
import com.cinevault.tv.data.repository.ContentRepository
import com.cinevault.tv.data.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailState(
    val movie: MovieDto? = null,
    val seasons: List<SeasonDto> = emptyList(),
    val episodes: List<EpisodeDto> = emptyList(),
    val related: List<MovieDto> = emptyList(),
    val selectedSeasonIndex: Int = 0,
    val isInWatchlist: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepo: ContentRepository,
    private val watchlistRepo: WatchlistRepository,
) : ViewModel() {

    private val movieId: String = savedStateHandle.get<String>("movieId") ?: ""

    private val _state = MutableStateFlow(DetailState())
    val state = _state.asStateFlow()

    init {
        loadMovie()
    }

    private fun loadMovie() {
        viewModelScope.launch {
            _state.value = DetailState(isLoading = true)
            try {
                val movieDef = async { contentRepo.getMovie(movieId).getOrNull() }
                val relatedDef = async { contentRepo.getRelated(movieId).getOrElse { emptyList() } }
                val watchlistDef = async { watchlistRepo.checkWatchlist(movieId) }

                val movie = movieDef.await()
                val related = relatedDef.await()
                val inWatchlist = watchlistDef.await()

                var seasons: List<SeasonDto> = emptyList()
                var episodes: List<EpisodeDto> = emptyList()

                if (movie != null && (movie.contentType == "series" || movie.contentType == "anime")) {
                    seasons = contentRepo.getSeasons(movieId).getOrElse { emptyList() }
                    if (seasons.isNotEmpty()) {
                        episodes = contentRepo.getEpisodes(seasons[0].id).getOrElse { emptyList() }
                    }
                }

                // Track view
                contentRepo.trackView(movieId)

                _state.value = DetailState(
                    movie = movie,
                    seasons = seasons,
                    episodes = episodes,
                    related = related,
                    isInWatchlist = inWatchlist,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _state.value = DetailState(isLoading = false, error = "Failed to load details")
            }
        }
    }

    fun selectSeason(index: Int) {
        val seasons = _state.value.seasons
        if (index < 0 || index >= seasons.size) return

        _state.value = _state.value.copy(selectedSeasonIndex = index)

        viewModelScope.launch {
            val episodes = contentRepo.getEpisodes(seasons[index].id).getOrElse { emptyList() }
            _state.value = _state.value.copy(episodes = episodes)
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            val isIn = _state.value.isInWatchlist
            if (isIn) {
                watchlistRepo.removeFromWatchlist(movieId)
            } else {
                watchlistRepo.addToWatchlist(movieId)
            }
            _state.value = _state.value.copy(isInWatchlist = !isIn)
        }
    }
}
