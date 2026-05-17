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
import org.openkis.android.data.debug.DebugLogger
import org.openkis.android.data.debug.LogEntry
import org.openkis.android.data.local.entity.ServerEntity
import org.openkis.android.data.repository.SyncManager
import org.openkis.android.data.repository.SyncResult
import javax.inject.Inject

data class SettingsUiState(
    val isSyncing: Boolean = false,
    val syncingServerUrl: String? = null,
    val syncMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val debugLogger: DebugLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val servers: StateFlow<List<ServerEntity>> = syncManager.servers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val offlineMode: StateFlow<Boolean> = syncManager.offlineMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val showCaves: StateFlow<Boolean> = syncManager.showCaves
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showSprings: StateFlow<Boolean> = syncManager.showSprings
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showArtificials: StateFlow<Boolean> = syncManager.showArtificials
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val debugLogEntries: StateFlow<List<LogEntry>> = debugLogger.entries
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun getDebugLogContent(): String = debugLogger.getContent()

    fun clearDebugLog() = debugLogger.clear()

    fun addServer(url: String, name: String = "") {
        viewModelScope.launch { syncManager.addServer(url, name) }
    }

    fun removeServer(url: String) {
        viewModelScope.launch { syncManager.removeServer(url) }
    }

    fun syncServer(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true, syncingServerUrl = url, syncMessage = null
            )
            when (val result = syncManager.syncServer(url)) {
                is SyncResult.Success -> _uiState.value = _uiState.value.copy(
                    isSyncing = false, syncingServerUrl = null,
                    syncMessage = "Synced ${result.count} items"
                )
                is SyncResult.Error -> _uiState.value = _uiState.value.copy(
                    isSyncing = false, syncingServerUrl = null,
                    syncMessage = "Sync failed: ${result.message}"
                )
            }
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true, syncingServerUrl = null, syncMessage = null
            )
            when (val result = syncManager.syncAll()) {
                is SyncResult.Success -> _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncMessage = "Sync complete: ${result.count} items"
                )
                is SyncResult.Error -> _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncMessage = "Sync failed: ${result.message}"
                )
            }
        }
    }

    fun setOfflineMode(enabled: Boolean) {
        viewModelScope.launch { syncManager.setOfflineMode(enabled) }
    }

    fun setShowCaves(enabled: Boolean) {
        viewModelScope.launch { syncManager.setShowCaves(enabled) }
    }

    fun setShowSprings(enabled: Boolean) {
        viewModelScope.launch { syncManager.setShowSprings(enabled) }
    }

    fun setShowArtificials(enabled: Boolean) {
        viewModelScope.launch { syncManager.setShowArtificials(enabled) }
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
