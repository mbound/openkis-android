package org.openkis.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.openkis.android.data.local.entity.ArtificialEntity

@Dao
interface ArtificialDao {

    @Query("SELECT * FROM artificials ORDER BY code ASC")
    fun getAll(): Flow<List<ArtificialEntity>>

    @Query("SELECT * FROM artificials WHERE latitude != 0.0 AND longitude != 0.0")
    fun getAllWithCoordinates(): Flow<List<ArtificialEntity>>

    @Query("SELECT * FROM artificials WHERE code = :code")
    suspend fun getByCode(code: String): ArtificialEntity?

    @Query("""
        SELECT * FROM artificials
        WHERE name LIKE '%' || :query || '%'
           OR code LIKE '%' || :query || '%'
           OR synonyms LIKE '%' || :query || '%'
        ORDER BY code ASC
    """)
    fun search(query: String): Flow<List<ArtificialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(artificials: List<ArtificialEntity>)

    @Query("DELETE FROM artificials")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM artificials")
    suspend fun count(): Int
}
