package org.openkis.android.ui.caves

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openkis.android.data.local.entity.SurveyEntity
import org.openkis.android.data.repository.CaveRepository
import javax.inject.Inject

data class DetailData(
    val title: String,
    val subtitle: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val highlights: List<Pair<String, String>> = emptyList(),
    val fields: List<Pair<String, String>> = emptyList(),
    val serverUrl: String = "",
    val dbId: String = "",
    val entityType: String = ""
)

sealed class SurveysState {
    object Idle : SurveysState()
    object Loading : SurveysState()
    data class Loaded(val surveys: List<SurveyEntity>) : SurveysState()
    data class Error(val message: String) : SurveysState()
}

@HiltViewModel
class CaveDetailViewModel @Inject constructor(
    private val repository: CaveRepository
) : ViewModel() {

    private val _detail = MutableStateFlow<DetailData?>(null)
    val detail: StateFlow<DetailData?> = _detail.asStateFlow()

    private val _surveysState = MutableStateFlow<SurveysState>(SurveysState.Idle)
    val surveysState: StateFlow<SurveysState> = _surveysState.asStateFlow()

    private var loaded = false

    fun load(type: String, code: String) {
        if (loaded) return
        loaded = true

        viewModelScope.launch {
            val data = when (type) {
                "caves" -> loadCave(code)
                "springs" -> loadSpring(code)
                "artificials" -> loadArtificial(code)
                else -> null
            }
            _detail.value = data

            // Check for cached surveys and pre-populate state
            if (data != null && data.dbId.isNotBlank() && data.entityType.isNotBlank()) {
                val cached = repository.getSurveysFromDb(data.serverUrl, data.entityType, data.dbId)
                if (cached.isNotEmpty()) {
                    _surveysState.value = SurveysState.Loaded(cached)
                }
            } else if (data != null && data.entityType.isNotBlank() && data.dbId.isBlank()) {
                _surveysState.value = SurveysState.Error("Survey download not available (no server ID for this entry)")
            }
        }
    }

    fun downloadSurveys() {
        val d = _detail.value ?: return
        if (d.dbId.isBlank()) {
            _surveysState.value = SurveysState.Error("Survey download not available (no server ID for this entry)")
            return
        }
        viewModelScope.launch {
            _surveysState.value = SurveysState.Loading
            try {
                val surveys = repository.fetchAndCacheSurveys(d.serverUrl, d.entityType, d.dbId)
                _surveysState.value = SurveysState.Loaded(surveys)
            } catch (e: Exception) {
                _surveysState.value = SurveysState.Error(e.message ?: "Fetch failed")
            }
        }
    }

    private suspend fun loadCave(code: String): DetailData? {
        val cave = repository.getCaveByCode(code) ?: return null
        return DetailData(
            title = "${cave.code} - ${cave.name}",
            subtitle = cave.synonyms,
            latitude = cave.latitude,
            longitude = cave.longitude,
            highlights = listOfNotNull(
                cave.elevation.takeIf { it.isNotBlank() }?.let { "Elevation" to "${it}m" },
                cave.lengthTotal.takeIf { it.isNotBlank() }?.let { "Length" to "${it}m" },
                cave.depthTotal.takeIf { it.isNotBlank() }?.let { "Depth" to "${it}m" }
            ),
            fields = listOf(
                "Code" to cave.code,
                "Name" to cave.name,
                "Synonyms" to cave.synonyms,
                "Region" to cave.region,
                "Province" to cave.province,
                "Municipality" to cave.municipality,
                "Locality" to cave.locality,
                "Depth +" to cave.depthPositive,
                "Depth -" to cave.depthNegative,
                "Hydrology" to cave.hydrology,
                "Meteorology" to cave.meteorology,
                "Description" to cave.description,
                "Closed" to cave.closed
            ),
            serverUrl = cave.serverUrl,
            dbId = cave.dbId,
            entityType = "caves"
        )
    }

    private suspend fun loadSpring(code: String): DetailData? {
        val spring = repository.getSpringByCode(code) ?: return null
        return DetailData(
            title = "${spring.code} - ${spring.name}",
            latitude = spring.latitude,
            longitude = spring.longitude,
            highlights = listOfNotNull(
                spring.elevation.takeIf { it.isNotBlank() }?.let { "Elevation" to "${it}m" },
                spring.flowAverage.takeIf { it.isNotBlank() }?.let { "Avg Flow" to it },
                spring.flowMax.takeIf { it.isNotBlank() }?.let { "Max Flow" to it }
            ),
            fields = listOf(
                "Code" to spring.code,
                "Name" to spring.name,
                "Linked Cave" to spring.caveCode,
                "Region" to spring.region,
                "Province" to spring.province,
                "Municipality" to spring.municipality,
                "Flow Min" to spring.flowMin,
                "Flow Max" to spring.flowMax,
                "Flow Average" to spring.flowAverage,
                "Usage" to spring.usage,
                "Utilization" to spring.utilization,
                "Description" to spring.description
            )
            // Springs have no survey pages; entityType left blank → no surveys section shown
        )
    }

    private suspend fun loadArtificial(code: String): DetailData? {
        val art = repository.getArtificialByCode(code) ?: return null
        return DetailData(
            title = "${art.code} - ${art.name}",
            subtitle = art.synonyms,
            latitude = art.latitude,
            longitude = art.longitude,
            highlights = listOfNotNull(
                art.elevation.takeIf { it.isNotBlank() }?.let { "Elevation" to "${it}m" },
                art.lengthTotal.takeIf { it.isNotBlank() }?.let { "Length" to "${it}m" },
                art.depthTotal.takeIf { it.isNotBlank() }?.let { "Depth" to "${it}m" }
            ),
            fields = listOf(
                "Code" to art.code,
                "Name" to art.name,
                "Synonyms" to art.synonyms,
                "Year" to art.year,
                "Epoch" to art.epoch,
                "Typology" to art.typology,
                "Category" to art.category,
                "Address" to art.address,
                "Region" to art.region,
                "Province" to art.province,
                "Municipality" to art.municipality,
                "Locality" to art.locality,
                "Description" to art.description
            ),
            serverUrl = art.serverUrl,
            dbId = art.dbId,
            entityType = "artificials"
        )
    }
}
