package com.cinevault.tv.ui.screens.detail

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

data class DetailState(
    val movie: MovieDto? = null,
    val seasons: List<SeasonDto> = emptyList(),
    val episodes: List<EpisodeDto> = emptyList(),
    val related: List<MovieDto> = emptyList(),
    val selectedSeasonIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val api: CineVaultApi,
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
                val movieResponse = api.getMovie(movieId)
                val movie = movieResponse.body()
                val relatedResponse = api.getRelated(movieId)

                var seasons: List<SeasonDto> = emptyList()
                var episodes: List<EpisodeDto> = emptyList()

                if (movie != null && movie.contentType == "series") {
                    val seasonsResponse = api.getSeasons(movieId)
                    seasons = seasonsResponse.body() ?: emptyList()
                    if (seasons.isNotEmpty()) {
                        val episodesResponse = api.getEpisodes(seasons[0].id)
                        episodes = episodesResponse.body() ?: emptyList()
                    }
                }

                _state.value = DetailState(
                    movie = movie,
                    seasons = seasons,
                    episodes = episodes,
                    related = relatedResponse.body() ?: emptyList(),
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
            try {
                val episodesResponse = api.getEpisodes(seasons[index].id)
                _state.value = _state.value.copy(
                    episodes = episodesResponse.body() ?: emptyList()
                )
            } catch (_: Exception) {}
        }
    }
}
