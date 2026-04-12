package com.cinevault.tv.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.tv.data.model.*
import com.cinevault.tv.data.repository.ContentRepository
import com.cinevault.tv.data.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val banners: List<BannerDto> = emptyList(),
    val sections: List<HomeSectionDto> = emptyList(),
    val trending: List<MovieDto> = emptyList(),
    val continueWatching: List<WatchProgressDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contentRepo: ContentRepository,
    private val watchProgressRepo: WatchProgressRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private var currentSection: String? = null

    fun loadContent(section: String? = null) {
        if (section == currentSection && _state.value.sections.isNotEmpty()) return
        currentSection = section

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                // Map tab names to API section parameter
                val apiSection = when (section) {
                    "shows" -> "shows"
                    "movies" -> "movies"
                    "anime" -> "anime"
                    else -> null // Home tab loads default feed
                }

                val bannersDeferred = async { contentRepo.getBanners(apiSection).getOrElse { emptyList() } }
                val feedDeferred = async { contentRepo.getHomeFeed(apiSection).getOrElse { emptyList() } }
                val trendingDeferred = async { contentRepo.getTrending(limit = 20).getOrElse { emptyList() } }
                val continueDeferred = async { watchProgressRepo.getContinueWatching().getOrElse { emptyList() } }

                val banners = bannersDeferred.await()
                val feed = feedDeferred.await()
                val trending = trendingDeferred.await()
                val continueWatching = continueDeferred.await()

                _state.value = HomeState(
                    banners = banners,
                    sections = feed,
                    trending = trending,
                    continueWatching = continueWatching,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load content: ${e.message}",
                )
            }
        }
    }

    fun refresh() {
        currentSection = null // Force reload
        loadContent(currentSection)
    }
}
