package org.openkis.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.openkis.android.data.local.entity.SpringEntity

@Dao
interface SpringDao {

    @Query("SELECT * FROM springs ORDER BY code ASC")
    fun getAll(): Flow<List<SpringEntity>>

    @Query("SELECT * FROM springs WHERE latitude != 0.0 AND longitude != 0.0")
    fun getAllWithCoordinates(): Flow<List<SpringEntity>>

    @Query("SELECT * FROM springs WHERE code = :code")
    suspend fun getByCode(code: String): SpringEntity?

    @Query("""
        SELECT * FROM springs
        WHERE name LIKE '%' || :query || '%'
           OR code LIKE '%' || :query || '%'
        ORDER BY code ASC
    """)
    fun search(query: String): Flow<List<SpringEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(springs: List<SpringEntity>)

    @Query("DELETE FROM springs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM springs")
    suspend fun count(): Int
}
