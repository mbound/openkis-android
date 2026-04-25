package org.openkis.android.ui.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.openkis.android.data.export.GpxExporter
import org.openkis.android.data.export.JsonExporter
import org.openkis.android.data.export.KmlExporter
import org.openkis.android.data.repository.CaveRepository
import javax.inject.Inject

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    KML("KML", "kml", "application/vnd.google-earth.kml+xml"),
    GPX("GPX", "gpx", "application/gpx+xml"),
    JSON("JSON", "json", "application/json")
}

data class ExportUiState(
    val selectedFormat: ExportFormat = ExportFormat.KML,
    val caveCount: Int = 0,
    val springCount: Int = 0,
    val artificialCount: Int = 0,
    val isExporting: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val repository: CaveRepository,
    private val kmlExporter: KmlExporter,
    private val gpxExporter: GpxExporter,
    private val jsonExporter: JsonExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                caveCount = repository.getCaveCount(),
                springCount = repository.getSpringCount(),
                artificialCount = repository.getArtificialCount()
            )
        }
    }

    fun setFormat(format: ExportFormat) {
        _uiState.value = _uiState.value.copy(selectedFormat = format)
    }

    fun export(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, message = null)
            try {
                val caves = repository.getAllCaves().first()
                val springs = repository.getAllSprings().first()
                val artificials = repository.getAllArtificials().first()

                context.contentResolver.openOutputStream(uri)?.use { output ->
                    when (_uiState.value.selectedFormat) {
                        ExportFormat.KML -> kmlExporter.export(output, caves, springs, artificials)
                        ExportFormat.GPX -> gpxExporter.export(output, caves, springs, artificials)
                        ExportFormat.JSON -> jsonExporter.export(output, caves, springs, artificials)
                    }
                }

                val total = caves.size + springs.size + artificials.size
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Exported $total items successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
