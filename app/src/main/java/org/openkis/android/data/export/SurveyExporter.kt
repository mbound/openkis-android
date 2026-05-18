package org.openkis.android.data.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.openkis.android.data.local.entity.SurveyEntity
import java.io.OutputStream
import javax.inject.Inject

class SurveyExporter @Inject constructor() {

    private val json = Json { prettyPrint = true }

    fun export(output: OutputStream, surveys: List<SurveyEntity>) {
        val exportData = surveys.map { it.toExport() }
        output.bufferedWriter().use { it.write(json.encodeToString(exportData)) }
    }
}

@Serializable
private data class SurveyExport(
    val serverUrl: String,
    val entityType: String,
    val entityDbId: String,
    val surveyIndex: Int,
    val title: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val date: String,
    val author: String,
    val surveyors: String,
    val speleoGroups: String,
    val license: String,
    val bibliography: String
)

private fun SurveyEntity.toExport() = SurveyExport(
    serverUrl = serverUrl,
    entityType = entityType,
    entityDbId = dbId,
    surveyIndex = surveyIndex,
    title = title,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    date = date,
    author = author,
    surveyors = surveyors,
    speleoGroups = speleoGroups,
    license = license,
    bibliography = bibliography
)
