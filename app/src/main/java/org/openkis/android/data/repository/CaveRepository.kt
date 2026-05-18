package org.openkis.android.data.repository

import kotlinx.coroutines.flow.Flow
import org.openkis.android.data.local.dao.ArtificialDao
import org.openkis.android.data.local.dao.CaveDao
import org.openkis.android.data.local.dao.SpringDao
import org.openkis.android.data.local.dao.SurveyDao
import org.openkis.android.data.local.entity.ArtificialEntity
import org.openkis.android.data.local.entity.CaveEntity
import org.openkis.android.data.local.entity.SpringEntity
import org.openkis.android.data.local.entity.SurveyEntity
import org.openkis.android.data.remote.CsvParser
import org.openkis.android.data.remote.DevSiteApi
import org.openkis.android.data.remote.OpenKisApi
import org.openkis.android.data.remote.SurveyFetcher
import org.openkis.android.data.remote.dto.OpenKisItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaveRepository @Inject constructor(
    private val caveDao: CaveDao,
    private val springDao: SpringDao,
    private val artificialDao: ArtificialDao,
    private val api: OpenKisApi,
    private val devSiteApi: DevSiteApi,
    private val surveyDao: SurveyDao,
    private val surveyFetcher: SurveyFetcher
) {
    // Caves
    fun getAllCaves(): Flow<List<CaveEntity>> = caveDao.getAll()
    fun getCavesWithCoordinates(): Flow<List<CaveEntity>> = caveDao.getAllWithCoordinates()
    fun searchCaves(query: String): Flow<List<CaveEntity>> = caveDao.search(query)
    suspend fun getCaveByCode(code: String): CaveEntity? = caveDao.getByCode(code)
    fun getCavesInBounds(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double) =
        caveDao.getInBounds(minLat, maxLat, minLon, maxLon)

    // Springs
    fun getAllSprings(): Flow<List<SpringEntity>> = springDao.getAll()
    fun getSpringsWithCoordinates(): Flow<List<SpringEntity>> = springDao.getAllWithCoordinates()
    fun searchSprings(query: String): Flow<List<SpringEntity>> = springDao.search(query)
    suspend fun getSpringByCode(code: String): SpringEntity? = springDao.getByCode(code)

    // Artificials
    fun getAllArtificials(): Flow<List<ArtificialEntity>> = artificialDao.getAll()
    fun getArtificialsWithCoordinates(): Flow<List<ArtificialEntity>> = artificialDao.getAllWithCoordinates()
    fun searchArtificials(query: String): Flow<List<ArtificialEntity>> = artificialDao.search(query)
    suspend fun getArtificialByCode(code: String): ArtificialEntity? = artificialDao.getByCode(code)

    // Counts
    suspend fun getCaveCount(): Int = caveDao.count()
    suspend fun getSpringCount(): Int = springDao.count()
    suspend fun getArtificialCount(): Int = artificialDao.count()

    // Sync from server
    suspend fun syncCaves(serverUrl: String): Int {
        val response = api.getCaves()
        val entities = response.items.map { it.toCaveEntity(serverUrl) }
        caveDao.deleteByServerUrl(serverUrl)
        caveDao.insertAll(entities)
        return entities.size
    }

    suspend fun syncSprings(serverUrl: String): Int {
        val response = api.getSprings()
        val entities = response.items.map { it.toSpringEntity(serverUrl) }
        springDao.deleteByServerUrl(serverUrl)
        springDao.insertAll(entities)
        return entities.size
    }

    suspend fun syncArtificials(serverUrl: String): Int {
        val response = api.getArtificials()
        val entities = response.items.map { it.toArtificialEntity(serverUrl) }
        artificialDao.deleteByServerUrl(serverUrl)
        artificialDao.insertAll(entities)
        return entities.size
    }

    // --- Dev-site CSV sync (new format: /export/{entity}/csv) ---

    suspend fun syncCavesFromDevSite(serverUrl: String): Int {
        val rows = CsvParser.parse(devSiteApi.fetchCsv(serverUrl, "cavita-naturali"))
        val entities = rows.mapNotNull { it.toCaveEntity(serverUrl) }
        caveDao.deleteByServerUrl(serverUrl)
        caveDao.insertAll(entities)
        return entities.size
    }

    suspend fun syncCavesFromDevSiteContent(serverUrl: String, csvContent: String): Int {
        val rows = CsvParser.parse(csvContent)
        val entities = rows.mapNotNull { it.toCaveEntity(serverUrl) }
        caveDao.deleteByServerUrl(serverUrl)
        caveDao.insertAll(entities)
        return entities.size
    }

    suspend fun syncSpringsFromDevSite(serverUrl: String): Int {
        val rows = CsvParser.parse(devSiteApi.fetchCsv(serverUrl, "sorgenti"))
        val entities = rows.mapNotNull { it.toSpringEntity(serverUrl) }
        springDao.deleteByServerUrl(serverUrl)
        springDao.insertAll(entities)
        return entities.size
    }

    suspend fun syncArtificialsFromDevSite(serverUrl: String): Int {
        val rows = CsvParser.parse(devSiteApi.fetchCsv(serverUrl, "cavita-artificiali"))
        val entities = rows.mapNotNull { it.toArtificialEntityFromDevSite(serverUrl) }
        artificialDao.deleteByServerUrl(serverUrl)
        artificialDao.insertAll(entities)
        return entities.size
    }

    suspend fun clearByServer(serverUrl: String) {
        caveDao.deleteByServerUrl(serverUrl)
        springDao.deleteByServerUrl(serverUrl)
        artificialDao.deleteByServerUrl(serverUrl)
    }

    suspend fun clearAll() {
        caveDao.deleteAll()
        springDao.deleteAll()
        artificialDao.deleteAll()
    }

    // Surveys
    suspend fun getSurveysFromDb(serverUrl: String, entityType: String, dbId: String): List<SurveyEntity> =
        surveyDao.getByEntity(serverUrl, entityType, dbId)

    suspend fun fetchAndCacheSurveys(
        serverUrl: String,
        entityType: String,
        dbId: String
    ): List<SurveyEntity> {
        val fetched = surveyFetcher.fetchSurveys(serverUrl, entityType, dbId)
        surveyDao.deleteByEntity(serverUrl, entityType, dbId)
        val entities = fetched.mapIndexed { i, s ->
            SurveyEntity(
                serverUrl = serverUrl,
                entityType = entityType,
                dbId = dbId,
                surveyIndex = i,
                title = s.title,
                imageUrl = s.imageUrl,
                thumbnailUrl = s.thumbnailUrl,
                date = s.date,
                author = s.author,
                surveyors = s.surveyors,
                speleoGroups = s.speleoGroups,
                license = s.license,
                bibliography = s.bibliography
            )
        }
        surveyDao.insertAll(entities)
        return entities
    }

    fun getAllSurveys(): Flow<List<SurveyEntity>> = surveyDao.getAll()

    suspend fun getSurveyCount(): Int = surveyDao.count()
}

// Parses a decimal-degree coordinate string; returns 0.0 if the value is outside
// the valid range (e.g. UTM northings/eastings that cannot be displayed on the map).
private fun String?.toWgs84OrZero(): Double {
    val v = this?.toDoubleOrNull() ?: return 0.0
    return if (v in -180.0..180.0) v else 0.0
}

private fun Map<String, String>.toCaveEntity(serverUrl: String): CaveEntity? {
    val code = get("Codice")?.takeIf { it.isNotBlank() } ?: return null
    return CaveEntity(
        code = code,
        name = get("Nome") ?: "",
        synonyms = get("Sinonimi") ?: "",
        latitude = get("Latitudine").toWgs84OrZero(),
        longitude = get("Longitudine").toWgs84OrZero(),
        elevation = get("Altitudine") ?: "",
        depthPositive = get("Dislivello positivo") ?: "",
        depthNegative = get("Dislivello negativo") ?: "",
        depthTotal = get("Dislivello totale") ?: "",
        lengthTotal = get("Lunghezza totale") ?: "",
        locality = get("Località") ?: "",
        region = get("Regione") ?: "",
        province = get("Provincia") ?: "",
        municipality = get("Comune") ?: "",
        closed = get("Chiuso") ?: "",
        serverUrl = serverUrl,
        dbId = getOrDefault("id", "")
    )
}

private fun Map<String, String>.toSpringEntity(serverUrl: String): SpringEntity? {
    val code = get("Codice")?.takeIf { it.isNotBlank() } ?: return null
    return SpringEntity(
        code = code,
        name = get("Nome") ?: "",
        latitude = get("Latitudine").toWgs84OrZero(),
        longitude = get("Longitudine").toWgs84OrZero(),
        elevation = get("Altitudine") ?: get("Quota") ?: "",
        flowMax = get("Portata in piena") ?: "",
        flowMin = get("Portata minima") ?: "",
        flowAverage = get("Portata media") ?: "",
        usage = get("Uso") ?: "",
        utilization = get("Tipo") ?: "",
        region = get("Regione") ?: "",
        province = get("Provincia") ?: "",
        municipality = get("Comune") ?: "",
        serverUrl = serverUrl
    )
}

private fun Map<String, String>.toArtificialEntityFromDevSite(serverUrl: String): ArtificialEntity? {
    val code = get("Codice")?.takeIf { it.isNotBlank() } ?: return null
    return ArtificialEntity(
        code = code,
        name = get("Nome") ?: "",
        synonyms = get("Sinonimi") ?: "",
        latitude = get("Latitudine").toWgs84OrZero(),
        longitude = get("Longitudine").toWgs84OrZero(),
        elevation = get("Quota") ?: get("Altitudine") ?: "",
        depthTotal = get("Profondità totale") ?: "",
        depthNegative = get("Dislivello negativo") ?: "",
        depthPositive = get("Dislivello positivo") ?: "",
        lengthTotal = get("Lunghezza totale") ?: "",
        closed = get("Chiuso") ?: "",
        year = get("Anno") ?: "",
        epoch = get("epoch") ?: "",
        typology = get("Tipologia") ?: "",
        category = get("Categoria") ?: "",
        address = get("Indirizzo") ?: "",
        locality = get("Località") ?: "",
        region = get("Regione") ?: "",
        province = get("Provincia") ?: "",
        municipality = get("Comune") ?: "",
        serverUrl = serverUrl,
        dbId = getOrDefault("id", "")
    )
}

private fun OpenKisItem.toCaveEntity(serverUrl: String) = CaveEntity(
    code = code,
    name = name,
    synonyms = synonyms,
    latitude = lat.toDoubleOrNull() ?: latitude.replace(",", ".").toDoubleOrNull() ?: 0.0,
    longitude = lon.toDoubleOrNull() ?: longitude.replace(",", ".").toDoubleOrNull() ?: 0.0,
    elevation = elevation,
    depthTotal = depthTotal,
    depthNegative = depthNegative,
    depthPositive = depthPositive,
    lengthTotal = lengthTotal,
    hydrology = hydrology,
    meteorology = meteorology,
    closed = closed,
    serverUrl = serverUrl,
    dbId = id
)

private fun OpenKisItem.toSpringEntity(serverUrl: String) = SpringEntity(
    code = code,
    name = name,
    latitude = lat.toDoubleOrNull() ?: latitude.replace(",", ".").toDoubleOrNull() ?: 0.0,
    longitude = lon.toDoubleOrNull() ?: longitude.replace(",", ".").toDoubleOrNull() ?: 0.0,
    elevation = elevation,
    flowMax = flowMax,
    flowMin = flowMin,
    flowAverage = flowAverage,
    usage = usage,
    utilization = utilization,
    serverUrl = serverUrl
)

private fun OpenKisItem.toArtificialEntity(serverUrl: String) = ArtificialEntity(
    code = code,
    name = name,
    synonyms = synonyms,
    latitude = lat.toDoubleOrNull() ?: latitude.replace(",", ".").toDoubleOrNull() ?: 0.0,
    longitude = lon.toDoubleOrNull() ?: longitude.replace(",", ".").toDoubleOrNull() ?: 0.0,
    elevation = elevation,
    depthTotal = depthTotal,
    depthNegative = depthNegative,
    depthPositive = depthPositive,
    lengthTotal = lengthTotal,
    closed = closed,
    year = year,
    epoch = epoch,
    typology = typology,
    category = category,
    serverUrl = serverUrl,
    dbId = id
)
