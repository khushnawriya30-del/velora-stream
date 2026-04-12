package com.cinevault.tv.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.tv.data.model.BannerDto
import com.cinevault.tv.data.model.HomeSectionDto
import com.cinevault.tv.data.model.MovieDto
import com.cinevault.tv.data.remote.CineVaultApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val banners: List<BannerDto> = emptyList(),
    val sections: List<HomeSectionDto> = emptyList(),
    val trending: List<MovieDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: CineVaultApi,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        loadContent()
    }

    fun loadContent() {
        viewModelScope.launch {
            _state.value = HomeState(isLoading = true)
            try {
                val bannersResponse = api.getBanners()
                val homeFeedResponse = api.getHomeFeed()
                val trendingResponse = api.getTrending(limit = 20)

                _state.value = HomeState(
                    banners = bannersResponse.body() ?: emptyList(),
                    sections = homeFeedResponse.body() ?: emptyList(),
                    trending = trendingResponse.body() ?: emptyList(),
                    isLoading = false,
                )
            } catch (e: Exception) {
                _state.value = HomeState(
                    isLoading = false,
                    error = "Failed to load content",
                )
            }
        }
    }
}
