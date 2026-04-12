package com.cinevault.tv.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.tv.data.model.MovieDto
import com.cinevault.tv.data.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val query: String = "",
    val results: List<MovieDto> = emptyList(),
    val isLoading: Boolean = false,
    val selectedContentType: String? = null,
    val selectedGenre: String? = null,
    val selectedLanguage: String? = null,
    val hasSearched: Boolean = false,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val contentRepo: ContentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null

    val contentTypes = listOf("All", "movie", "series", "anime")

    private val _genres = MutableStateFlow(listOf(
        "All", "Action", "Adventure", "Animation", "Comedy", "Crime", "Documentary",
        "Drama", "Family", "Fantasy", "Horror", "Mystery", "Romance",
        "Sci-Fi", "Thriller", "War",
    ))
    val genres: List<String> get() = _genres.value

    private val _languages = MutableStateFlow(listOf(
        "All", "Hindi", "English", "Tamil", "Telugu", "Malayalam",
        "Kannada", "Bengali", "Marathi", "Punjabi", "Korean", "Japanese",
    ))
    val languages: List<String> get() = _languages.value

    init {
        loadFilters()
    }

    private fun loadFilters() {
        viewModelScope.launch {
            contentRepo.getGenres().getOrNull()?.let { genreList ->
                if (genreList.isNotEmpty()) {
                    _genres.value = listOf("All") + genreList
                }
            }
        }
        viewModelScope.launch {
            contentRepo.getLanguages().getOrNull()?.let { langList ->
                if (langList.isNotEmpty()) {
                    _languages.value = listOf("All") + langList
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            performSearch()
        }
    }

    fun onContentTypeChange(type: String?) {
        _state.value = _state.value.copy(
            selectedContentType = if (type == "All") null else type
        )
        performSearch()
    }

    fun onGenreChange(genre: String?) {
        _state.value = _state.value.copy(
            selectedGenre = if (genre == "All") null else genre
        )
        performSearch()
    }

    fun onLanguageChange(language: String?) {
        _state.value = _state.value.copy(
            selectedLanguage = if (language == "All") null else language
        )
        performSearch()
    }

    private fun performSearch() {
        viewModelScope.launch {
            val s = _state.value
            _state.value = s.copy(isLoading = true, hasSearched = true)
            val result = contentRepo.search(
                query = s.query.ifBlank { null },
                contentType = s.selectedContentType,
                genre = s.selectedGenre,
                language = s.selectedLanguage,
                limit = 40,
            )
            _state.value = _state.value.copy(
                results = result.getOrNull()?.results ?: emptyList(),
                isLoading = false,
            )
        }
    }
}
