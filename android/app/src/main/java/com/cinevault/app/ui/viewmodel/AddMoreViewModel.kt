package com.cinevault.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.app.data.local.SessionManager
import com.cinevault.app.data.model.MovieDto
import com.cinevault.app.data.model.Result
import com.cinevault.app.data.repository.ContentRepository
import com.cinevault.app.data.repository.ThematicCollectionRepository
import com.cinevault.app.data.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddMoreUiState(
    val isLoading: Boolean = false,
    val allContent: List<MovieDto> = emptyList(),
    val addedIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val hasMore: Boolean = true,
    val currentPage: Int = 1,
    val type: String = "watchlist",
) {
    val filteredContent: List<MovieDto>
        get() {
            if (searchQuery.isBlank()) return allContent
            val normalized = searchQuery.lowercase().replace(Regex("[:\\-_/]"), " ").replace(Regex("\\s+"), " ").trim()
            val words = normalized.split(" ").filter { it.isNotEmpty() }
            return allContent.filter { movie ->
                val normalizedTitle = movie.title.lowercase().replace(Regex("[:\\-_/]"), " ").replace(Regex("\\s+"), " ").trim()
                words.all { word -> normalizedTitle.contains(word) }
            }
        }
}

@HiltViewModel
class AddMoreViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val watchlistRepository: WatchlistRepository,
    private val thematicCollectionRepository: ThematicCollectionRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMoreUiState())
    val uiState: StateFlow<AddMoreUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun initialize(type: String) {
        _uiState.update { it.copy(type = type) }
        loadExistingItems(type)
        loadContent(page = 1)
    }

    private fun loadExistingItems(type: String) {
        viewModelScope.launch {
            val profileId = sessionManager.activeProfileId.firstOrNull() ?: return@launch
            val ids = when (type) {
                "watchlist" -> {
                    when (val r = watchlistRepository.getWatchlist(profileId)) {
                        is Result.Success -> r.data.map { it.id }.toSet()
                        else -> emptySet()
                    }
                }
                "collection" -> {
                    when (val r = thematicCollectionRepository.getCollection(profileId)) {
                        is Result.Success -> r.data.map { it.id }.toSet()
                        else -> emptySet()
                    }
                }
                else -> emptySet()
            }
            _uiState.update { it.copy(addedIds = ids) }
        }
    }

    private fun loadContent(page: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = contentRepository.getAllContent(page = page, limit = 50)) {
                is Result.Success -> {
                    val data = result.data
                    val newContent = if (page == 1) data.movies else _uiState.value.allContent + data.movies
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            allContent = newContent,
                            hasMore = data.page < data.pages,
                            currentPage = data.page,
                        )
                    }
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false) }
                is Result.Loading -> {}
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.isLoading && state.hasMore) {
            loadContent(state.currentPage + 1)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun addContent(contentId: String) {
        viewModelScope.launch {
            val profileId = sessionManager.activeProfileId.firstOrNull() ?: return@launch
            val type = _uiState.value.type
            val result = when (type) {
                "watchlist" -> watchlistRepository.addToWatchlist(profileId, contentId)
                "collection" -> thematicCollectionRepository.addToCollection(profileId, contentId)
                else -> return@launch
            }
            if (result is Result.Success) {
                _uiState.update { it.copy(addedIds = it.addedIds + contentId) }
            }
        }
    }

    fun removeContent(contentId: String) {
        viewModelScope.launch {
            val profileId = sessionManager.activeProfileId.firstOrNull() ?: return@launch
            val type = _uiState.value.type
            val result = when (type) {
                "watchlist" -> watchlistRepository.removeFromWatchlist(profileId, contentId)
                "collection" -> thematicCollectionRepository.removeFromCollection(profileId, contentId)
                else -> return@launch
            }
            if (result is Result.Success) {
                _uiState.update { it.copy(addedIds = it.addedIds - contentId) }
            }
        }
    }
}
