package org.openkis.android.ui.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openkis.android.data.export.GpxExporter
import org.openkis.android.data.export.JsonExporter
import org.openkis.android.data.export.KmlExporter
import org.openkis.android.data.export.SurveyExporter
import org.openkis.android.data.repository.CaveRepository
import java.io.File
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
    val surveyCount: Int = 0,
    val isExporting: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val repository: CaveRepository,
    private val kmlExporter: KmlExporter,
    private val gpxExporter: GpxExporter,
    private val jsonExporter: JsonExporter,
    private val surveyExporter: SurveyExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                caveCount = repository.getCaveCount(),
                springCount = repository.getSpringCount(),
                artificialCount = repository.getArtificialCount(),
                surveyCount = repository.getSurveyCount()
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

    fun share(context: Context) {
        val format = _uiState.value.selectedFormat
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, message = null)
            try {
                val caves = repository.getAllCaves().first()
                val springs = repository.getAllSprings().first()
                val artificials = repository.getAllArtificials().first()

                val file = withContext(Dispatchers.IO) {
                    val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
                    val f = File(dir, "openkis_export.${format.extension}")
                    f.outputStream().use { out ->
                        when (format) {
                            ExportFormat.KML -> kmlExporter.export(out, caves, springs, artificials)
                            ExportFormat.GPX -> gpxExporter.export(out, caves, springs, artificials)
                            ExportFormat.JSON -> jsonExporter.export(out, caves, springs, artificials)
                        }
                    }
                    f
                }

                val uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = format.mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share OpenKIS export"))

                val total = caves.size + springs.size + artificials.size
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Ready to share $total items"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun exportSurveys(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, message = null)
            try {
                val surveys = repository.getAllSurveys().first()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    surveyExporter.export(output, surveys)
                }
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Exported ${surveys.size} survey records"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Survey export failed: ${e.message}"
                )
            }
        }
    }

    fun shareSurveys(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, message = null)
            try {
                val surveys = repository.getAllSurveys().first()
                val file = withContext(Dispatchers.IO) {
                    val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
                    val f = File(dir, "openkis_surveys.json")
                    f.outputStream().use { out -> surveyExporter.export(out, surveys) }
                    f
                }
                val uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share survey data"))
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Ready to share ${surveys.size} survey records"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Survey export failed: ${e.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
