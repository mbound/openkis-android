package org.openkis.android.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.openkis.android.data.debug.DebugLogger
import org.openkis.android.data.local.dao.ArtificialDao
import org.openkis.android.data.local.dao.CaveDao
import org.openkis.android.data.local.dao.SpringDao
import org.openkis.android.data.local.dao.SurveyDao
import org.openkis.android.data.local.dao.SurveyAnnotationDao
import org.openkis.android.data.local.entity.ArtificialEntity
import org.openkis.android.data.local.entity.CaveEntity
import org.openkis.android.data.local.entity.SpringEntity
import org.openkis.android.data.local.entity.SurveyEntity
import org.openkis.android.data.local.entity.SurveyAnnotationEntity
import org.openkis.android.data.remote.CsvParser
import org.openkis.android.data.remote.DevSiteApi
import org.openkis.android.data.remote.OpenKisApi
import org.openkis.android.data.remote.SurveyFetcher
import org.openkis.android.data.remote.dto.OpenKisItem
import java.io.File
import java.io.IOException
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
    private val surveyAnnotationDao: SurveyAnnotationDao,
    private val surveyFetcher: SurveyFetcher,
    private val json: Json,
    private val debugLogger: DebugLogger,
    @ApplicationContext private val context: Context
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

    suspend fun syncCavesFromLegacyContent(serverUrl: String, jsonContent: String): Int {
        val response = json.decodeFromString<org.openkis.android.data.remote.dto.OpenKisResponse>(jsonContent)
        val entities = response.items.map { it.toCaveEntity(serverUrl) }
        caveDao.deleteByServerUrl(serverUrl)
        caveDao.insertAll(entities)
        return entities.size
    }

    suspend fun syncSpringsFromLegacyContent(serverUrl: String, jsonContent: String): Int {
        val response = json.decodeFromString<org.openkis.android.data.remote.dto.OpenKisResponse>(jsonContent)
        val entities = response.items.map { it.toSpringEntity(serverUrl) }
        springDao.deleteByServerUrl(serverUrl)
        springDao.insertAll(entities)
        return entities.size
    }

    suspend fun syncArtificialsFromLegacyContent(serverUrl: String, jsonContent: String): Int {
        val response = json.decodeFromString<org.openkis.android.data.remote.dto.OpenKisResponse>(jsonContent)
        val entities = response.items.map { it.toArtificialEntity(serverUrl) }
        artificialDao.deleteByServerUrl(serverUrl)
        artificialDao.insertAll(entities)
        return entities.size
    }

    // --- Piemonte CSV sync (/export/{entity}/csv) ---

    suspend fun syncCavesFromDevSite(serverUrl: String): Int =
        syncCavesFromDevSiteContent(
            serverUrl,
            devSiteApi.fetchCsv(serverUrl, "cavita-naturali")
        )

    suspend fun syncCavesFromDevSiteContent(serverUrl: String, csvContent: String): Int {
        val entities = parseCsvEntities("caves", csvContent) { row ->
            row.toCaveEntity(serverUrl)
        }
        caveDao.deleteByServerUrl(serverUrl)
        caveDao.insertAll(entities)
        return entities.size
    }

    suspend fun syncSpringsFromDevSite(serverUrl: String): Int =
        syncSpringsFromDevSiteContent(
            serverUrl,
            devSiteApi.fetchCsv(serverUrl, "sorgenti")
        )

    suspend fun syncSpringsFromDevSiteContent(serverUrl: String, csvContent: String): Int {
        val entities = parseCsvEntities("springs", csvContent) { row ->
            row.toSpringEntity(serverUrl)
        }
        springDao.deleteByServerUrl(serverUrl)
        springDao.insertAll(entities)
        return entities.size
    }

    suspend fun syncArtificialsFromDevSite(serverUrl: String): Int =
        syncArtificialsFromDevSiteContent(
            serverUrl,
            devSiteApi.fetchCsv(serverUrl, "cavita-artificiali")
        )

    suspend fun syncArtificialsFromDevSiteContent(serverUrl: String, csvContent: String): Int {
        val entities = parseCsvEntities("artificials", csvContent) { row ->
            row.toArtificialEntityFromDevSite(serverUrl)
        }
        artificialDao.deleteByServerUrl(serverUrl)
        artificialDao.insertAll(entities)
        return entities.size
    }

    private fun <T> parseCsvEntities(
        entityType: String,
        csvContent: String,
        mapper: (Map<String, String>) -> T?
    ): List<T> {
        val parsed = CsvParser.parseDetailed(csvContent)
        val delimiterName = when (parsed.delimiter) {
            '\t' -> "TAB"
            ';' -> "semicolon"
            else -> "comma"
        }
        val headerSummary = parsed.headers.take(18).joinToString(" | ")

        debugLogger.i(
            "CsvSync",
            "$entityType CSV: delimiter=$delimiterName headers=${parsed.headers.size} rows=${parsed.rows.size} [$headerSummary]"
        )

        if (parsed.rows.isEmpty()) {
            val prefix = csvContent
                .replace("\r", " ")
                .replace("\n", " ")
                .take(220)
            throw IOException(
                "$entityType CSV contained no data rows; headers=[$headerSummary], prefix=$prefix"
            )
        }

        val entities = parsed.rows.mapNotNull(mapper)
        if (entities.isEmpty()) {
            throw IOException(
                "$entityType CSV has ${parsed.rows.size} rows but no recognizable object codes; headers=[$headerSummary]"
            )
        }

        debugLogger.i(
            "CsvSync",
            "$entityType CSV accepted ${entities.size}/${parsed.rows.size} rows"
        )
        return entities
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
        surveyDao.deleteAll()
        File(context.filesDir, "surveys").deleteRecursively()
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
        val host = Uri.parse(serverUrl).host?.replace(".", "_") ?: "server"
        val entities = fetched.mapIndexed { i, s ->
            val localPath = if (s.imageUrl.isNotBlank()) {
                val file = File(context.filesDir, "surveys/$host/$entityType/$dbId/$i.jpg")
                if (surveyFetcher.downloadImage(s.imageUrl, file)) file.absolutePath else ""
            } else ""
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
                bibliography = s.bibliography,
                localImagePath = localPath
            )
        }
        surveyDao.insertAll(entities)
        return entities
    }

    suspend fun clearSurveysForEntity(serverUrl: String, entityType: String, dbId: String) {
        surveyDao.deleteByEntity(serverUrl, entityType, dbId)
        val host = Uri.parse(serverUrl).host?.replace(".", "_") ?: "server"
        File(context.filesDir, "surveys/$host/$entityType/$dbId").deleteRecursively()
    }

    suspend fun clearSurveys() {
        surveyDao.deleteAll()
        File(context.filesDir, "surveys").deleteRecursively()
    }

    fun getAllSurveys(): Flow<List<SurveyEntity>> = surveyDao.getAll()

    suspend fun getSurveyCount(): Int = surveyDao.count()

    fun getAllSurveyAnnotations(): Flow<List<SurveyAnnotationEntity>> = surveyAnnotationDao.getAll()

    suspend fun getSurveyAnnotationCount(): Int = surveyAnnotationDao.count()
}

// Parses a decimal-degree coordinate string; returns 0.0 if the value is outside
// the valid range (e.g. UTM northings/eastings that cannot be displayed on the map).
private fun String?.toWgs84OrZero(): Double {
    val v = this?.toDoubleOrNull() ?: return 0.0
    return if (v in -180.0..180.0) v else 0.0
}

private fun normalizeCsvFieldName(value: String): String =
    value.trim()
        .lowercase()
        .replace('à', 'a')
        .replace('á', 'a')
        .replace('è', 'e')
        .replace('é', 'e')
        .replace('ì', 'i')
        .replace('í', 'i')
        .replace('ò', 'o')
        .replace('ó', 'o')
        .replace('ù', 'u')
        .replace('ú', 'u')
        .filter { it.isLetterOrDigit() }

private fun Map<String, String>.csvValue(vararg aliases: String): String {
    aliases.forEach { alias ->
        get(alias)?.takeIf { it.isNotBlank() }?.let { return it }
    }

    val wanted = aliases
        .map(::normalizeCsvFieldName)
        .filter { it.isNotBlank() }
        .toSet()

    // First prefer exact normalized matches.
    entries.firstOrNull { normalizeCsvFieldName(it.key) in wanted }
        ?.value
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    // Export labels may include units or explanatory suffixes, e.g.
    // "Sviluppo totale [m]" or "Dislivello totale (m)".
    // After normalization these become "sviluppototalem" etc.; accept
    // a known alias as the leading field name, but avoid very short
    // aliases such as "id", "lat" or "lon" to limit false matches.
    entries.firstOrNull { entry ->
        val header = normalizeCsvFieldName(entry.key)
        wanted.any { alias ->
            alias.length >= 5 && header.startsWith(alias)
        }
    }
        ?.value
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    return ""
}

private fun Map<String, String>.toCaveEntity(serverUrl: String): CaveEntity? {
    val code = csvValue(
        "Codice", "code", "codice", "Codice catasto", "cave_code", "cavecode"
    ).takeIf { it.isNotBlank() } ?: return null

    return CaveEntity(
        code = code,
        name = csvValue("Nome", "name", "nome"),
        synonyms = csvValue("Sinonimi", "synonyms", "sinonimi"),
        latitude = csvValue(
            "Latitudine", "latitude", "lat", "latitudine WGS84", "wgs84_latitude"
        ).replace(",", ".").toWgs84OrZero(),
        longitude = csvValue(
            "Longitudine", "longitude", "lon", "lng", "longitudine WGS84", "wgs84_longitude"
        ).replace(",", ".").toWgs84OrZero(),
        elevation = csvValue("Altitudine", "Quota", "elevation", "altitude", "quota"),
        depthPositive = csvValue(
            "Dislivello positivo", "depth_positive", "dislivello_positivo", "depthPositive"
        ),
        depthNegative = csvValue(
            "Dislivello negativo", "depth_negative", "dislivello_negativo", "depthNegative"
        ),
        depthTotal = csvValue(
            "Dislivello totale", "Profondità totale", "depth_total",
            "dislivello_totale", "profondita_totale", "depthTotal"
        ),
        lengthTotal = csvValue(
            "Lunghezza totale", "Sviluppo", "Sviluppo totale", "length_total",
            "lenght_total", "lunghezza_totale", "sviluppo_totale"
        ),
        locality = csvValue("Località", "localita", "locality"),
        region = csvValue("Regione", "regione", "region"),
        province = csvValue("Provincia", "provincia", "province"),
        municipality = csvValue("Comune", "comune", "municipality"),
        closed = csvValue("Chiuso", "chiuso", "closed"),
        serverUrl = serverUrl,
        dbId = csvValue("id", "ID", "db_id", "dbid")
    )
}

private fun Map<String, String>.toSpringEntity(serverUrl: String): SpringEntity? {
    val code = csvValue(
        "Codice", "code", "codice", "Codice catasto", "spring_code", "springcode"
    ).takeIf { it.isNotBlank() } ?: return null

    return SpringEntity(
        code = code,
        name = csvValue("Nome", "name", "nome"),
        latitude = csvValue(
            "Latitudine", "latitude", "lat", "latitudine WGS84", "wgs84_latitude"
        ).replace(",", ".").toWgs84OrZero(),
        longitude = csvValue(
            "Longitudine", "longitude", "lon", "lng", "longitudine WGS84", "wgs84_longitude"
        ).replace(",", ".").toWgs84OrZero(),
        elevation = csvValue("Altitudine", "Quota", "elevation", "altitude", "quota"),
        flowMax = csvValue("Portata in piena", "flow_max", "portata_massima", "flowMax"),
        flowMin = csvValue("Portata minima", "flow_min", "portata_minima", "flowMin"),
        flowAverage = csvValue("Portata media", "flow_average", "portata_media", "flowAverage"),
        usage = csvValue("Uso", "use", "usage", "uso"),
        utilization = csvValue("Tipo", "type", "utilization", "tipo"),
        region = csvValue("Regione", "regione", "region"),
        province = csvValue("Provincia", "provincia", "province"),
        municipality = csvValue("Comune", "comune", "municipality"),
        serverUrl = serverUrl
    )
}

private fun Map<String, String>.toArtificialEntityFromDevSite(serverUrl: String): ArtificialEntity? {
    val code = csvValue(
        "Codice", "code", "codice", "Codice catasto", "artificial_code", "artificialcode"
    ).takeIf { it.isNotBlank() } ?: return null

    return ArtificialEntity(
        code = code,
        name = csvValue("Nome", "name", "nome"),
        synonyms = csvValue("Sinonimi", "synonyms", "sinonimi"),
        latitude = csvValue(
            "Latitudine", "latitude", "lat", "latitudine WGS84", "wgs84_latitude"
        ).replace(",", ".").toWgs84OrZero(),
        longitude = csvValue(
            "Longitudine", "longitude", "lon", "lng", "longitudine WGS84", "wgs84_longitude"
        ).replace(",", ".").toWgs84OrZero(),
        elevation = csvValue("Quota", "Altitudine", "elevation", "altitude", "quota"),
        depthTotal = csvValue(
            "Profondità totale", "Dislivello totale", "depth_total",
            "profondita_totale", "dislivello_totale", "depthTotal"
        ),
        depthNegative = csvValue(
            "Dislivello negativo", "depth_negative", "dislivello_negativo", "depthNegative"
        ),
        depthPositive = csvValue(
            "Dislivello positivo", "depth_positive", "dislivello_positivo", "depthPositive"
        ),
        lengthTotal = csvValue(
            "Lunghezza totale", "Sviluppo", "Sviluppo totale", "length_total",
            "lenght_total", "lunghezza_totale", "sviluppo_totale"
        ),
        closed = csvValue("Chiuso", "chiuso", "closed", "Distrutta", "distrutta"),
        year = csvValue("Anno", "year", "anno"),
        epoch = csvValue("Epoca", "epoch", "epoca"),
        typology = csvValue("Tipologia", "typology", "tipologia", "Tipo", "type"),
        category = csvValue("Categoria", "category", "categoria"),
        address = csvValue("Indirizzo", "address", "indirizzo"),
        locality = csvValue("Località", "localita", "locality"),
        region = csvValue("Regione", "regione", "region"),
        province = csvValue("Provincia", "provincia", "province"),
        municipality = csvValue("Comune", "comune", "municipality"),
        serverUrl = serverUrl,
        dbId = csvValue("id", "ID", "db_id", "dbid")
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
