package com.cinevault.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.app.data.model.*
import com.cinevault.app.data.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val results: List<MovieDto> = emptyList(),
    val autocomplete: List<AutocompleteItem> = emptyList(),
    val trendingSearches: List<String> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val popularSearches: List<PopularSearchDto> = emptyList(),
    val recommendedContent: List<MovieDto> = emptyList(),
    val genres: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val selectedGenre: String? = null,
    val selectedLanguage: String? = null,
    val selectedContentType: String? = null,
    val hasMore: Boolean = false,
    val currentPage: Int = 1,
    val showFilters: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var autocompleteJob: Job? = null

    companion object {
        private const val PREFS_NAME = "search_history"
        private const val KEY_RECENT = "recent_searches"
        private const val MAX_RECENT = 15
    }

    init {
        loadFilterOptions()
        loadTrendingSearches()
    }

    fun loadSearchScreenData() {
        viewModelScope.launch {
            // Load popular searches
            when (val result = contentRepository.getMostPopularSearches()) {
                is Result.Success -> _uiState.update { it.copy(popularSearches = result.data) }
                else -> {}
            }
        }
        viewModelScope.launch {
            // Load recommended content
            when (val result = contentRepository.getRecommended()) {
                is Result.Success -> _uiState.update { it.copy(recommendedContent = result.data) }
                else -> {}
            }
        }
    }

    fun saveRecentSearch(context: Context) {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_RECENT, emptySet())?.toMutableList() ?: mutableListOf()
        existing.remove(query)
        existing.add(0, query)
        val trimmed = existing.take(MAX_RECENT)
        prefs.edit().putStringSet(KEY_RECENT, trimmed.toSet()).apply()
        _uiState.update { it.copy(recentSearches = trimmed) }
    }

    fun loadRecentSearches(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val recent = prefs.getStringSet(KEY_RECENT, emptySet())?.toList() ?: emptyList()
        _uiState.update { it.copy(recentSearches = recent) }
    }

    fun clearAllRecentSearches(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_RECENT).apply()
        _uiState.update { it.copy(recentSearches = emptyList()) }
    }

    fun removeRecentSearch(context: Context, query: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_RECENT, emptySet())?.toMutableSet() ?: mutableSetOf()
        existing.remove(query)
        prefs.edit().putStringSet(KEY_RECENT, existing).apply()
        _uiState.update { it.copy(recentSearches = existing.toList()) }
    }

    private fun loadTrendingSearches() {
        viewModelScope.launch {
            when (val result = contentRepository.getTrendingSearches()) {
                is Result.Success -> _uiState.update { it.copy(trendingSearches = result.data) }
                else -> {}
            }
        }
    }

    private fun loadFilterOptions() {
        viewModelScope.launch {
            val genres = when (val r = contentRepository.getGenres()) {
                is Result.Success -> r.data
                else -> emptyList()
            }
            val languages = when (val r = contentRepository.getLanguages()) {
                is Result.Success -> r.data
                else -> emptyList()
            }
            _uiState.update { it.copy(genres = genres, languages = languages) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        autocompleteJob?.cancel()
        if (query.length >= 2) {
            autocompleteJob = viewModelScope.launch {
                delay(300) // debounce
                when (val result = contentRepository.autocomplete(query)) {
                    is Result.Success -> _uiState.update { it.copy(autocomplete = result.data) }
                    else -> {}
                }
            }
        } else {
            _uiState.update { it.copy(autocomplete = emptyList()) }
        }
    }

    fun search(resetPage: Boolean = true) {
        searchJob?.cancel()
        val state = _uiState.value
        if (state.query.isBlank() && state.selectedGenre == null && state.selectedContentType == null) return

        val page = if (resetPage) 1 else state.currentPage + 1
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, autocomplete = emptyList()) }
            when (val result = contentRepository.search(
                query = state.query.takeIf { it.isNotBlank() },
                contentType = state.selectedContentType,
                genre = state.selectedGenre,
                language = state.selectedLanguage,
                page = page,
            )) {
                is Result.Success -> {
                    val data = result.data
                    val newResults = if (resetPage) data.results else state.results + data.results
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            results = newResults,
                            hasMore = data.hasMore,
                            currentPage = data.page,
                        )
                    }
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun loadMore() = search(resetPage = false)

    fun setGenreFilter(genre: String?) {
        _uiState.update { it.copy(selectedGenre = genre) }
        search()
    }

    fun setLanguageFilter(language: String?) {
        _uiState.update { it.copy(selectedLanguage = language) }
        search()
    }

    fun setContentTypeFilter(type: String?) {
        _uiState.update { it.copy(selectedContentType = type) }
        search()
    }

    fun toggleFilters() {
        _uiState.update { it.copy(showFilters = !it.showFilters) }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(selectedGenre = null, selectedLanguage = null, selectedContentType = null)
        }
        if (_uiState.value.query.isNotBlank()) search()
    }

    fun selectTrendingSearch(query: String) {
        _uiState.update { it.copy(query = query) }
        search()
    }
}
