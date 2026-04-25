package org.openkis.android.ui.caves

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openkis.android.data.repository.CaveRepository
import javax.inject.Inject

data class DetailData(
    val title: String,
    val subtitle: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val highlights: List<Pair<String, String>> = emptyList(),
    val fields: List<Pair<String, String>> = emptyList()
)

@HiltViewModel
class CaveDetailViewModel @Inject constructor(
    private val repository: CaveRepository
) : ViewModel() {

    private val _detail = MutableStateFlow<DetailData?>(null)
    val detail: StateFlow<DetailData?> = _detail.asStateFlow()

    private var loaded = false

    fun load(type: String, code: String) {
        if (loaded) return
        loaded = true

        viewModelScope.launch {
            _detail.value = when (type) {
                "caves" -> loadCave(code)
                "springs" -> loadSpring(code)
                "artificials" -> loadArtificial(code)
                else -> null
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
            )
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
            )
        )
    }
}
