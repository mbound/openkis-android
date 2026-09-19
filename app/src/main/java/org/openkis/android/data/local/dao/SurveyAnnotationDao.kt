package org.openkis.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.openkis.android.data.local.entity.SurveyAnnotationEntity

@Dao
interface SurveyAnnotationDao {

    @Query(
        """
        SELECT * FROM survey_annotations
        WHERE serverUrl = :serverUrl
          AND entityType = :entityType
          AND dbId = :dbId
          AND surveyKey = :surveyKey
        ORDER BY createdAt ASC
        """
    )
    fun observeBySurvey(
        serverUrl: String,
        entityType: String,
        dbId: String,
        surveyKey: String
    ): Flow<List<SurveyAnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(annotation: SurveyAnnotationEntity)

    @Delete
    suspend fun delete(annotation: SurveyAnnotationEntity)
}
