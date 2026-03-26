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
    val homeSections: List<HomeSectionDto> = emptyList(),
    val continueWatching: List<WatchProgressDto> = emptyList(),
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
