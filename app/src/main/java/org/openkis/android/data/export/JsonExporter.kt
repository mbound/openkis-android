package org.openkis.android.data.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.openkis.android.data.local.entity.ArtificialEntity
import org.openkis.android.data.local.entity.CaveEntity
import org.openkis.android.data.local.entity.SpringEntity
import java.io.OutputStream
import javax.inject.Inject

class JsonExporter @Inject constructor() {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun export(
        output: OutputStream,
        caves: List<CaveEntity>,
        springs: List<SpringEntity>,
        artificials: List<ArtificialEntity>
    ) {
        val exportData = ExportData(
            caves = caves.map { it.toExport() },
            springs = springs.map { it.toExport() },
            artificials = artificials.map { it.toExport() }
        )
        output.bufferedWriter().use { it.write(json.encodeToString(exportData)) }
    }
}

@Serializable
private data class ExportData(
    val caves: List<CaveExport>,
    val springs: List<SpringExport>,
    val artificials: List<ArtificialExport>
)

@Serializable
private data class CaveExport(
    val serverUrl: String,
    val code: String,
    val name: String,
    val synonyms: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: String,
    val depthTotal: String,
    val depthNegative: String,
    val depthPositive: String,
    val lengthTotal: String,
    val hydrology: String,
    val meteorology: String,
    val region: String,
    val province: String,
    val municipality: String,
    val locality: String,
    val closed: String,
    val description: String,
    val photoUrl: String
)

@Serializable
private data class SpringExport(
    val serverUrl: String,
    val code: String,
    val name: String,
    val caveCode: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: String,
    val flowMax: String,
    val flowMin: String,
    val flowAverage: String,
    val usage: String,
    val utilization: String,
    val region: String,
    val province: String,
    val municipality: String,
    val description: String,
    val photoUrl: String
)

@Serializable
private data class ArtificialExport(
    val serverUrl: String,
    val code: String,
    val name: String,
    val synonyms: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: String,
    val depthTotal: String,
    val lengthTotal: String,
    val year: String,
    val epoch: String,
    val typology: String,
    val category: String,
    val address: String,
    val region: String,
    val province: String,
    val municipality: String,
    val locality: String,
    val description: String,
    val photoUrl: String
)

private fun CaveEntity.toExport() = CaveExport(
    serverUrl = serverUrl, code = code, name = name, synonyms = synonyms,
    latitude = latitude, longitude = longitude, elevation = elevation,
    depthTotal = depthTotal, depthNegative = depthNegative, depthPositive = depthPositive,
    lengthTotal = lengthTotal, hydrology = hydrology, meteorology = meteorology,
    region = region, province = province, municipality = municipality, locality = locality,
    closed = closed, description = description, photoUrl = photoUrl
)

private fun SpringEntity.toExport() = SpringExport(
    serverUrl = serverUrl, code = code, name = name, caveCode = caveCode,
    latitude = latitude, longitude = longitude, elevation = elevation,
    flowMax = flowMax, flowMin = flowMin, flowAverage = flowAverage,
    usage = usage, utilization = utilization,
    region = region, province = province, municipality = municipality,
    description = description, photoUrl = photoUrl
)

private fun ArtificialEntity.toExport() = ArtificialExport(
    serverUrl = serverUrl, code = code, name = name, synonyms = synonyms,
    latitude = latitude, longitude = longitude, elevation = elevation,
    depthTotal = depthTotal, lengthTotal = lengthTotal,
    year = year, epoch = epoch, typology = typology, category = category,
    address = address, region = region, province = province,
    municipality = municipality, locality = locality,
    description = description, photoUrl = photoUrl
)
