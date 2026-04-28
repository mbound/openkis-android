package org.openkis.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.openkis.android.data.local.entity.CaveEntity

@Dao
interface CaveDao {

    @Query("SELECT * FROM caves ORDER BY code ASC")
    fun getAll(): Flow<List<CaveEntity>>

    @Query("SELECT * FROM caves WHERE latitude != 0.0 AND longitude != 0.0")
    fun getAllWithCoordinates(): Flow<List<CaveEntity>>

    @Query("SELECT * FROM caves WHERE code = :code")
    suspend fun getByCode(code: String): CaveEntity?

    @Query("""
        SELECT * FROM caves
        WHERE name LIKE '%' || :query || '%'
           OR code LIKE '%' || :query || '%'
           OR synonyms LIKE '%' || :query || '%'
        ORDER BY code ASC
    """)
    fun search(query: String): Flow<List<CaveEntity>>

    @Query("""
        SELECT * FROM caves
        WHERE latitude BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLon AND :maxLon
          AND latitude != 0.0 AND longitude != 0.0
    """)
    fun getInBounds(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): Flow<List<CaveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(caves: List<CaveEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cave: CaveEntity)

    @Query("DELETE FROM caves WHERE serverUrl = :serverUrl")
    suspend fun deleteByServerUrl(serverUrl: String)

    @Query("DELETE FROM caves")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM caves")
    suspend fun count(): Int
}
