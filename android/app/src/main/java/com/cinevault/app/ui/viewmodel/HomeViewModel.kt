package com.cinevault.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.app.data.local.SessionManager
import com.cinevault.app.data.model.*
import com.cinevault.app.data.repository.ContentRepository
import com.cinevault.app.data.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val banners: List<BannerDto> = emptyList(),
    val tabBanners: List<BannerDto> = emptyList(),
    val homeSections: List<HomeSectionDto> = emptyList(),
    val continueWatching: List<WatchProgressDto> = emptyList(),
    val selectedTab: Int = 0, // 0=Home, 1=Shows, 2=Movies, 3=Anime
    val filteredMovies: List<MovieDto> = emptyList(),
    val isFilterLoading: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun selectTab(index: Int) {
        _uiState.update {
            it.copy(
                selectedTab = index,
                tabBanners = filterBannersForTab(index, it.banners)
            )
        }
        when (index) {
            0 -> { /* Home tab — data already loaded */ }
            1 -> loadFilteredContent("web_series,tv_show")
            2 -> loadFilteredContent("movie")
            3 -> loadFilteredContent("anime")
        }
    }

    private fun filterBannersForTab(tab: Int, banners: List<BannerDto>): List<BannerDto> {
        if (tab == 0) return banners
        val types = when (tab) {
            1 -> listOf("web_series", "tv_show")
            2 -> listOf("movie")
            3 -> listOf("anime")
            else -> return banners
        }
        val filtered = banners.filter { it.contentType in types }
        return filtered.ifEmpty { banners }
    }

    private fun loadFilteredContent(contentType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFilterLoading = true, filteredMovies = emptyList()) }
            // Load multiple types by splitting comma-separated
            val types = contentType.split(",")
            val allMovies = mutableListOf<MovieDto>()
            for (type in types) {
                when (val r = contentRepository.getMoviesByType(type.trim(), limit = 40)) {
                    is Result.Success -> allMovies.addAll(r.data)
                    else -> {}
                }
            }
            _uiState.update { it.copy(isFilterLoading = false, filteredMovies = allMovies) }
        }
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val bannersDeferred = async { contentRepository.getBanners() }
            val feedDeferred = async { contentRepository.getHomeFeed() }

            val profileId = sessionManager.activeProfileId.firstOrNull()
            val continueDeferred = if (profileId != null) {
                async { watchProgressRepository.getContinueWatching(profileId) }
            } else null

            val banners = when (val r = bannersDeferred.await()) {
                is Result.Success -> r.data
                else -> emptyList()
            }
            val sections = when (val r = feedDeferred.await()) {
                is Result.Success -> r.data
                else -> emptyList()
            }
            val continueWatching = continueDeferred?.let {
                when (val r = it.await()) {
                    is Result.Success -> r.data
                    else -> emptyList()
                }
            } ?: emptyList()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    banners = banners,
                    tabBanners = filterBannersForTab(it.selectedTab, banners),
                    homeSections = sections,
                    continueWatching = continueWatching,
                    error = if (banners.isEmpty() && sections.isEmpty()) "Failed to load content" else null,
                )
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadHome()
    }
}
