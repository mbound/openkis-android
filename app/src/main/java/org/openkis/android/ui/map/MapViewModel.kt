package org.openkis.android.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openkis.android.data.local.entity.ArtificialEntity
import org.openkis.android.data.local.entity.CaveEntity
import org.openkis.android.data.local.entity.SpringEntity
import org.openkis.android.data.repository.CaveRepository
import org.openkis.android.data.repository.SyncManager
import org.openkis.android.ui.filter.ObjectNumericFilters
import javax.inject.Inject

data class MapUiState(
    val showCaves: Boolean = true,
    val showSprings: Boolean = true,
    val showArtificials: Boolean = true,
    val selectedCode: String? = null,
    val selectedType: String? = null,
    val numericFilters: ObjectNumericFilters = ObjectNumericFilters()
)

data class MapCameraState(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double
)

@HiltViewModel
class MapViewModel @Inject constructor(
    repository: CaveRepository,
    private val syncManager: SyncManager,
    val compositor: IconCompositor
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var savedCameraState: MapCameraState? = null
    val cameraState: MapCameraState?
        get() = savedCameraState

    val caves: StateFlow<List<CaveEntity>> = combine(
        repository.getCavesWithCoordinates(),
        syncManager.visibleServerUrls,
        _uiState
    ) { entities, visibleUrls, state ->
        entities.filter {
            it.serverUrl in visibleUrls &&
                state.numericFilters.matches(it.depthTotal, it.lengthTotal, it.elevation)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val springs: StateFlow<List<SpringEntity>> = combine(
        repository.getSpringsWithCoordinates(),
        syncManager.visibleServerUrls,
        _uiState
    ) { entities, visibleUrls, state ->
        entities.filter {
            it.serverUrl in visibleUrls &&
                state.numericFilters.matches(null, null, it.elevation)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val artificials: StateFlow<List<ArtificialEntity>> = combine(
        repository.getArtificialsWithCoordinates(),
        syncManager.visibleServerUrls,
        _uiState
    ) { entities, visibleUrls, state ->
        entities.filter {
            it.serverUrl in visibleUrls &&
                state.numericFilters.matches(it.depthTotal, it.lengthTotal, it.elevation)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Load persisted visibility preferences
        viewModelScope.launch {
            syncManager.showCaves.collect { show ->
                _uiState.value = _uiState.value.copy(showCaves = show)
            }
        }
        viewModelScope.launch {
            syncManager.showSprings.collect { show ->
                _uiState.value = _uiState.value.copy(showSprings = show)
            }
        }
        viewModelScope.launch {
            syncManager.showArtificials.collect { show ->
                _uiState.value = _uiState.value.copy(showArtificials = show)
            }
        }
    }

    fun toggleCaves() {
        val newValue = !_uiState.value.showCaves
        _uiState.value = _uiState.value.copy(showCaves = newValue)
        viewModelScope.launch { syncManager.setShowCaves(newValue) }
    }

    fun toggleSprings() {
        val newValue = !_uiState.value.showSprings
        _uiState.value = _uiState.value.copy(showSprings = newValue)
        viewModelScope.launch { syncManager.setShowSprings(newValue) }
    }

    fun toggleArtificials() {
        val newValue = !_uiState.value.showArtificials
        _uiState.value = _uiState.value.copy(showArtificials = newValue)
        viewModelScope.launch { syncManager.setShowArtificials(newValue) }
    }

    fun setNumericFilters(filters: ObjectNumericFilters) {
        _uiState.value = _uiState.value.copy(numericFilters = filters)
    }

    fun saveCamera(latitude: Double, longitude: Double, zoom: Double) {
        savedCameraState = MapCameraState(latitude, longitude, zoom)
    }

    fun selectMarker(type: String, code: String) {
        _uiState.value = _uiState.value.copy(selectedCode = code, selectedType = type)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedCode = null, selectedType = null)
    }
}
