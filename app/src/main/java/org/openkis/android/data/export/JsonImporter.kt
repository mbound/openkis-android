package org.openkis.android.data.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openkis.android.data.local.dao.ArtificialDao
import org.openkis.android.data.local.dao.CaveDao
import org.openkis.android.data.local.dao.SpringDao
import org.openkis.android.data.local.entity.ArtificialEntity
import org.openkis.android.data.local.entity.CaveEntity
import org.openkis.android.data.local.entity.SpringEntity
import java.io.InputStream
import javax.inject.Inject

data class ImportResult(val caves: Int, val springs: Int, val artificials: Int)

class JsonImporter @Inject constructor(
    private val caveDao: CaveDao,
    private val springDao: SpringDao,
    private val artificialDao: ArtificialDao
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun import(input: InputStream): ImportResult {
        val raw = input.bufferedReader().readText()
        val data = json.decodeFromString<ImportData>(raw)

        val caves = data.caves.map { it.toEntity() }
        val springs = data.springs.map { it.toEntity() }
        val artificials = data.artificials.map { it.toEntity() }

        if (caves.isNotEmpty()) caveDao.insertAll(caves)
        if (springs.isNotEmpty()) springDao.insertAll(springs)
        if (artificials.isNotEmpty()) artificialDao.insertAll(artificials)

        return ImportResult(caves.size, springs.size, artificials.size)
    }
}

@Serializable
private data class ImportData(
    val caves: List<CaveImport> = emptyList(),
    val springs: List<SpringImport> = emptyList(),
    val artificials: List<ArtificialImport> = emptyList()
)

@Serializable
private data class CaveImport(
    val serverUrl: String = "",
    val code: String = "",
    val name: String = "",
    val synonyms: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val elevation: String = "",
    val depthTotal: String = "",
    val depthNegative: String = "",
    val depthPositive: String = "",
    val lengthTotal: String = "",
    val hydrology: String = "",
    val meteorology: String = "",
    val region: String = "",
    val province: String = "",
    val municipality: String = "",
    val locality: String = "",
    val closed: String = "",
    val description: String = "",
    val photoUrl: String = ""
)

@Serializable
private data class SpringImport(
    val serverUrl: String = "",
    val code: String = "",
    val name: String = "",
    val caveCode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val elevation: String = "",
    val flowMax: String = "",
    val flowMin: String = "",
    val flowAverage: String = "",
    val usage: String = "",
    val utilization: String = "",
    val region: String = "",
    val province: String = "",
    val municipality: String = "",
    val description: String = "",
    val photoUrl: String = ""
)

@Serializable
private data class ArtificialImport(
    val serverUrl: String = "",
    val code: String = "",
    val name: String = "",
    val synonyms: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val elevation: String = "",
    val depthTotal: String = "",
    val depthNegative: String = "",
    val depthPositive: String = "",
    val lengthTotal: String = "",
    val closed: String = "",
    val year: String = "",
    val epoch: String = "",
    val typology: String = "",
    val category: String = "",
    val address: String = "",
    val region: String = "",
    val province: String = "",
    val municipality: String = "",
    val locality: String = "",
    val description: String = "",
    val photoUrl: String = ""
)

private fun CaveImport.toEntity() = CaveEntity(
    serverUrl = serverUrl, code = code, name = name, synonyms = synonyms,
    latitude = latitude, longitude = longitude, elevation = elevation,
    depthTotal = depthTotal, depthNegative = depthNegative, depthPositive = depthPositive,
    lengthTotal = lengthTotal, hydrology = hydrology, meteorology = meteorology,
    region = region, province = province, municipality = municipality, locality = locality,
    closed = closed, description = description, photoUrl = photoUrl
)

private fun SpringImport.toEntity() = SpringEntity(
    serverUrl = serverUrl, code = code, name = name, caveCode = caveCode,
    latitude = latitude, longitude = longitude, elevation = elevation,
    flowMax = flowMax, flowMin = flowMin, flowAverage = flowAverage,
    usage = usage, utilization = utilization,
    region = region, province = province, municipality = municipality,
    description = description, photoUrl = photoUrl
)

private fun ArtificialImport.toEntity() = ArtificialEntity(
    serverUrl = serverUrl, code = code, name = name, synonyms = synonyms,
    latitude = latitude, longitude = longitude, elevation = elevation,
    depthTotal = depthTotal, depthNegative = depthNegative, depthPositive = depthPositive,
    lengthTotal = lengthTotal, closed = closed,
    year = year, epoch = epoch, typology = typology, category = category,
    address = address, region = region, province = province,
    municipality = municipality, locality = locality,
    description = description, photoUrl = photoUrl
)
