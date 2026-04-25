package org.openkis.android.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import org.openkis.android.data.local.entity.ArtificialEntity
import org.openkis.android.data.local.entity.CaveEntity
import org.openkis.android.data.local.entity.SpringEntity
import org.openkis.android.data.repository.CaveRepository
import javax.inject.Inject

data class MapUiState(
    val showCaves: Boolean = true,
    val showSprings: Boolean = true,
    val showArtificials: Boolean = true,
    val selectedCode: String? = null,
    val selectedType: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    repository: CaveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    val caves: StateFlow<List<CaveEntity>> = repository.getCavesWithCoordinates()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val springs: StateFlow<List<SpringEntity>> = repository.getSpringsWithCoordinates()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val artificials: StateFlow<List<ArtificialEntity>> = repository.getArtificialsWithCoordinates()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun toggleCaves() {
        _uiState.value = _uiState.value.copy(showCaves = !_uiState.value.showCaves)
    }

    fun toggleSprings() {
        _uiState.value = _uiState.value.copy(showSprings = !_uiState.value.showSprings)
    }

    fun toggleArtificials() {
        _uiState.value = _uiState.value.copy(showArtificials = !_uiState.value.showArtificials)
    }

    fun selectMarker(type: String, code: String) {
        _uiState.value = _uiState.value.copy(selectedCode = code, selectedType = type)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedCode = null, selectedType = null)
    }
}
