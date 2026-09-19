package org.openkis.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "survey_annotations",
    indices = [
        Index(value = ["serverUrl", "entityType", "dbId", "surveyKey"])
    ]
)
data class SurveyAnnotationEntity(
    @PrimaryKey val id: String,
    val serverUrl: String,
    val entityType: String,
    val dbId: String,
    val surveyKey: String,
    val normalizedX: Float,
    val normalizedY: Float,
    val title: String = "",
    val category: String = "",
    val notes: String = "",
    val metadata: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun SurveyEntity.annotationKey(): String =
    imageUrl.ifBlank { thumbnailUrl }.ifBlank {
        "$surveyIndex|$title|$date"
    }
