package org.openkis.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openkis.android.data.repository.SyncManager
import org.openkis.android.data.repository.SyncResult
import javax.inject.Inject

data class SettingsUiState(
    val isSyncing: Boolean = false,
    val syncMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val serverUrl: StateFlow<String> = syncManager.serverUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val lastSync: StateFlow<Long> = syncManager.lastSync
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val offlineMode: StateFlow<Boolean> = syncManager.offlineMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setServerUrl(url: String) {
        viewModelScope.launch {
            syncManager.setServerUrl(url)
        }
    }

    fun setOfflineMode(enabled: Boolean) {
        viewModelScope.launch {
            syncManager.setOfflineMode(enabled)
        }
    }

    fun sync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
            when (val result = syncManager.syncAll()) {
                is SyncResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncMessage = "Sync complete: ${result.count} items"
                    )
                }
                is SyncResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncMessage = "Sync failed: ${result.message}"
                    )
                }
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            syncManager.clearCache()
            _uiState.value = _uiState.value.copy(syncMessage = "Cache cleared")
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(syncMessage = null)
    }
}
