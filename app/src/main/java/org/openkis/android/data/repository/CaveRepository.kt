package org.openkis.android.data.repository

import kotlinx.coroutines.flow.Flow
import org.openkis.android.data.local.dao.ArtificialDao
import org.openkis.android.data.local.dao.CaveDao
import org.openkis.android.data.local.dao.SpringDao
import org.openkis.android.data.local.entity.ArtificialEntity
import org.openkis.android.data.local.entity.CaveEntity
import org.openkis.android.data.local.entity.SpringEntity
import org.openkis.android.data.remote.OpenKisApi
import org.openkis.android.data.remote.dto.OpenKisItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaveRepository @Inject constructor(
    private val caveDao: CaveDao,
    private val springDao: SpringDao,
    private val artificialDao: ArtificialDao,
    private val api: OpenKisApi
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
        caveDao.deleteAll()
        caveDao.insertAll(entities)
        return entities.size
    }

    suspend fun syncSprings(serverUrl: String): Int {
        val response = api.getSprings()
        val entities = response.items.map { it.toSpringEntity(serverUrl) }
        springDao.deleteAll()
        springDao.insertAll(entities)
        return entities.size
    }

    suspend fun syncArtificials(serverUrl: String): Int {
        val response = api.getArtificials()
        val entities = response.items.map { it.toArtificialEntity(serverUrl) }
        artificialDao.deleteAll()
        artificialDao.insertAll(entities)
        return entities.size
    }

    suspend fun clearAll() {
        caveDao.deleteAll()
        springDao.deleteAll()
        artificialDao.deleteAll()
    }
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
    serverUrl = serverUrl
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
    lengthTotal = lengthTotal,
    year = year,
    epoch = epoch,
    typology = typology,
    category = category,
    serverUrl = serverUrl
)
