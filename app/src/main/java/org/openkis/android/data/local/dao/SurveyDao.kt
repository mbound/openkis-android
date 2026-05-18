package org.openkis.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.openkis.android.data.local.entity.SurveyEntity

@Dao
interface SurveyDao {

    @Query("SELECT * FROM surveys WHERE serverUrl = :url AND entityType = :type AND dbId = :id ORDER BY surveyIndex")
    suspend fun getByEntity(url: String, type: String, id: String): List<SurveyEntity>

    @Query("SELECT * FROM surveys ORDER BY fetchedAt DESC")
    fun getAll(): Flow<List<SurveyEntity>>

    @Query("SELECT COUNT(*) FROM surveys")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(surveys: List<SurveyEntity>)

    @Query("DELETE FROM surveys WHERE serverUrl = :url AND entityType = :type AND dbId = :id")
    suspend fun deleteByEntity(url: String, type: String, id: String)
}
