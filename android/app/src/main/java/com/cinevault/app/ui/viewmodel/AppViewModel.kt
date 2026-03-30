package com.cinevault.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinevault.app.BuildConfig
import com.cinevault.app.data.model.AppVersionResponse
import com.cinevault.app.data.remote.CineVaultApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val api: CineVaultApi
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<AppVersionResponse?>(null)
    val updateInfo: StateFlow<AppVersionResponse?> = _updateInfo

    init {
        checkForUpdate()
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            runCatching {
                val response = api.getAppVersion()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.versionCode > BuildConfig.VERSION_CODE) {
                        _updateInfo.value = body
                    }
                }
            }
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }
}
