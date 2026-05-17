package org.openkis.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.openkis.android.data.local.entity.ServerEntity

@Dao
interface ServerDao {

    @Query("SELECT * FROM servers ORDER BY addedAt ASC")
    fun getAll(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE url = :url")
    suspend fun getByUrl(url: String): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(server: ServerEntity)

    @Query("DELETE FROM servers WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("UPDATE servers SET lastSync = :timestamp WHERE url = :url")
    suspend fun updateLastSync(url: String, timestamp: Long)

    @Query("UPDATE servers SET visible = :visible WHERE url = :url")
    suspend fun updateVisible(url: String, visible: Boolean)

    @Query("UPDATE servers SET syncCaves = :caves, syncSprings = :springs, syncArtificials = :artificials WHERE url = :url")
    suspend fun updateSyncTypes(url: String, caves: Boolean, springs: Boolean, artificials: Boolean)

    @Query("SELECT COUNT(*) FROM servers")
    suspend fun count(): Int
}
