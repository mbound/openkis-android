package org.openkis.android.ui.caves

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openkis.android.R
import org.openkis.android.data.local.entity.SurveyEntity
import org.openkis.android.data.repository.CaveRepository
import javax.inject.Inject

data class DetailData(
    val title: String,
    val subtitle: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val highlights: List<Pair<Int, String>> = emptyList(),
    val fields: List<Pair<Int, String>> = emptyList(),
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

    fun clearSurveys() {
        val d = _detail.value ?: return
        viewModelScope.launch {
            repository.clearSurveysForEntity(d.serverUrl, d.entityType, d.dbId)
            _surveysState.value = SurveysState.Idle
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
                cave.elevation.takeIf { it.isNotBlank() }?.let { R.string.highlight_elevation to "${it}m" },
                cave.lengthTotal.takeIf { it.isNotBlank() }?.let { R.string.highlight_length to "${it}m" },
                cave.depthTotal.takeIf { it.isNotBlank() }?.let { R.string.highlight_depth to "${it}m" }
            ),
            fields = listOf(
                R.string.label_code to cave.code,
                R.string.label_name to cave.name,
                R.string.label_synonyms to cave.synonyms,
                R.string.label_region to cave.region,
                R.string.label_province to cave.province,
                R.string.label_municipality to cave.municipality,
                R.string.label_locality to cave.locality,
                R.string.label_depth_positive to cave.depthPositive,
                R.string.label_depth_negative to cave.depthNegative,
                R.string.label_hydrology to cave.hydrology,
                R.string.label_meteorology to cave.meteorology,
                R.string.label_description to cave.description,
                R.string.label_closed to cave.closed
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
                spring.elevation.takeIf { it.isNotBlank() }?.let { R.string.highlight_elevation to "${it}m" },
                spring.flowAverage.takeIf { it.isNotBlank() }?.let { R.string.highlight_avg_flow to it },
                spring.flowMax.takeIf { it.isNotBlank() }?.let { R.string.highlight_max_flow to it }
            ),
            fields = listOf(
                R.string.label_code to spring.code,
                R.string.label_name to spring.name,
                R.string.label_linked_cave to spring.caveCode,
                R.string.label_region to spring.region,
                R.string.label_province to spring.province,
                R.string.label_municipality to spring.municipality,
                R.string.label_flow_min to spring.flowMin,
                R.string.label_flow_max to spring.flowMax,
                R.string.label_flow_avg to spring.flowAverage,
                R.string.label_usage to spring.usage,
                R.string.label_utilization to spring.utilization,
                R.string.label_description to spring.description
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
                art.elevation.takeIf { it.isNotBlank() }?.let { R.string.highlight_elevation to "${it}m" },
                art.lengthTotal.takeIf { it.isNotBlank() }?.let { R.string.highlight_length to "${it}m" },
                art.depthTotal.takeIf { it.isNotBlank() }?.let { R.string.highlight_depth to "${it}m" }
            ),
            fields = listOf(
                R.string.label_code to art.code,
                R.string.label_name to art.name,
                R.string.label_synonyms to art.synonyms,
                R.string.label_year to art.year,
                R.string.label_epoch to art.epoch,
                R.string.label_typology to art.typology,
                R.string.label_category to art.category,
                R.string.label_address to art.address,
                R.string.label_region to art.region,
                R.string.label_province to art.province,
                R.string.label_municipality to art.municipality,
                R.string.label_locality to art.locality,
                R.string.label_closed to art.closed,
                R.string.label_description to art.description
            ),
            serverUrl = art.serverUrl,
            dbId = art.dbId,
            entityType = "artificials"
        )
    }
}
