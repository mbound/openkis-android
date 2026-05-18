package org.openkis.android.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "surveys",
    primaryKeys = ["serverUrl", "entityType", "dbId", "surveyIndex"]
)
data class SurveyEntity(
    val serverUrl: String,
    val entityType: String,
    val dbId: String,
    val surveyIndex: Int,
    val title: String = "",
    val imageUrl: String = "",
    val thumbnailUrl: String = "",
    val date: String = "",
    val author: String = "",
    val surveyors: String = "",
    val speleoGroups: String = "",
    val license: String = "",
    val bibliography: String = "",
    val fetchedAt: Long = System.currentTimeMillis()
)
