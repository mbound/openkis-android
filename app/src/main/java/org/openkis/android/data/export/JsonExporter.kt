package org.openkis.android.data.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.openkis.android.data.local.entity.ArtificialEntity
import org.openkis.android.data.local.entity.CaveEntity
import org.openkis.android.data.local.entity.SpringEntity
import java.io.OutputStream
import javax.inject.Inject

class JsonExporter @Inject constructor() {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun export(
        output: OutputStream,
        caves: List<CaveEntity>,
        springs: List<SpringEntity>,
        artificials: List<ArtificialEntity>
    ) {
        val exportData = ExportData(
            caves = caves.filter { it.latitude != 0.0 || it.longitude != 0.0 }
                .map { it.toExportItem() },
            springs = springs.filter { it.latitude != 0.0 || it.longitude != 0.0 }
                .map { it.toExportItem() },
            artificials = artificials.filter { it.latitude != 0.0 || it.longitude != 0.0 }
                .map { it.toExportItem() }
        )
        output.bufferedWriter().use { writer ->
            writer.write(json.encodeToString(exportData))
        }
    }
}

@Serializable
private data class ExportData(
    val caves: List<ExportItem>,
    val springs: List<ExportItem>,
    val artificials: List<ExportItem>
)

@Serializable
private data class ExportItem(
    val code: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: String = "",
    val depthTotal: String = "",
    val lengthTotal: String = "",
    val synonyms: String = "",
    val description: String = ""
)

private fun CaveEntity.toExportItem() = ExportItem(
    code = code, name = name,
    latitude = latitude, longitude = longitude,
    elevation = elevation, depthTotal = depthTotal,
    lengthTotal = lengthTotal, synonyms = synonyms,
    description = description
)

private fun SpringEntity.toExportItem() = ExportItem(
    code = code, name = name,
    latitude = latitude, longitude = longitude,
    elevation = elevation, description = description
)

private fun ArtificialEntity.toExportItem() = ExportItem(
    code = code, name = name,
    latitude = latitude, longitude = longitude,
    elevation = elevation, depthTotal = depthTotal,
    lengthTotal = lengthTotal, synonyms = synonyms,
    description = description
)
