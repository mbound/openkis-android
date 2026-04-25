package org.openkis.android.data.export

import org.openkis.android.data.local.entity.ArtificialEntity
import org.openkis.android.data.local.entity.CaveEntity
import org.openkis.android.data.local.entity.SpringEntity
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class GpxExporter @Inject constructor() {

    fun export(
        output: OutputStream,
        caves: List<CaveEntity>,
        springs: List<SpringEntity>,
        artificials: List<ArtificialEntity>
    ) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val now = dateFormat.format(Date())

        // Compute bounds
        val allPoints = mutableListOf<Pair<Double, Double>>()
        caves.filter { it.latitude != 0.0 || it.longitude != 0.0 }
            .forEach { allPoints.add(it.latitude to it.longitude) }
        springs.filter { it.latitude != 0.0 || it.longitude != 0.0 }
            .forEach { allPoints.add(it.latitude to it.longitude) }
        artificials.filter { it.latitude != 0.0 || it.longitude != 0.0 }
            .forEach { allPoints.add(it.latitude to it.longitude) }

        val minLat = allPoints.minOfOrNull { it.first } ?: 0.0
        val maxLat = allPoints.maxOfOrNull { it.first } ?: 0.0
        val minLon = allPoints.minOfOrNull { it.second } ?: 0.0
        val maxLon = allPoints.maxOfOrNull { it.second } ?: 0.0

        output.bufferedWriter().use { writer ->
            writer.write("""<?xml version="1.0" encoding="UTF-8"?>""")
            writer.newLine()
            writer.write("""<gpx version="1.1" creator="OpenKIS Android" """)
            writer.write("""xmlns="http://www.topografix.com/GPX/1/1" """)
            writer.write("""xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" """)
            writer.write("""xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">""")
            writer.newLine()

            writer.write("""<metadata>""")
            writer.newLine()
            writer.write("""  <name>OpenKIS Export</name>""")
            writer.newLine()
            writer.write("""  <time>$now</time>""")
            writer.newLine()
            writer.write("""  <bounds minlat="$minLat" minlon="$minLon" maxlat="$maxLat" maxlon="$maxLon"/>""")
            writer.newLine()
            writer.write("""</metadata>""")
            writer.newLine()

            // Caves as waypoints
            for (cave in caves) {
                if (cave.latitude == 0.0 && cave.longitude == 0.0) continue
                val elev = cave.elevation.toDoubleOrNull() ?: 0.0
                writer.write("""<wpt lat="${cave.latitude}" lon="${cave.longitude}">""")
                writer.newLine()
                writer.write("""  <ele>$elev</ele>""")
                writer.newLine()
                writer.write("""  <name>${escapeXml("${cave.code}-${cave.name}")}</name>""")
                writer.newLine()
                writer.write("""  <desc>${escapeXml(buildCaveDesc(cave))}</desc>""")
                writer.newLine()
                writer.write("""  <type>Cave</type>""")
                writer.newLine()
                writer.write("""</wpt>""")
                writer.newLine()
            }

            // Springs
            for (spring in springs) {
                if (spring.latitude == 0.0 && spring.longitude == 0.0) continue
                val elev = spring.elevation.toDoubleOrNull() ?: 0.0
                writer.write("""<wpt lat="${spring.latitude}" lon="${spring.longitude}">""")
                writer.newLine()
                writer.write("""  <ele>$elev</ele>""")
                writer.newLine()
                writer.write("""  <name>${escapeXml("${spring.code}-${spring.name}")}</name>""")
                writer.newLine()
                writer.write("""  <desc>${escapeXml(buildSpringDesc(spring))}</desc>""")
                writer.newLine()
                writer.write("""  <type>Spring</type>""")
                writer.newLine()
                writer.write("""</wpt>""")
                writer.newLine()
            }

            // Artificials
            for (art in artificials) {
                if (art.latitude == 0.0 && art.longitude == 0.0) continue
                val elev = art.elevation.toDoubleOrNull() ?: 0.0
                writer.write("""<wpt lat="${art.latitude}" lon="${art.longitude}">""")
                writer.newLine()
                writer.write("""  <ele>$elev</ele>""")
                writer.newLine()
                writer.write("""  <name>${escapeXml("${art.code}-${art.name}")}</name>""")
                writer.newLine()
                writer.write("""  <desc>${escapeXml(buildArtificialDesc(art))}</desc>""")
                writer.newLine()
                writer.write("""  <type>Artificial</type>""")
                writer.newLine()
                writer.write("""</wpt>""")
                writer.newLine()
            }

            writer.write("</gpx>")
        }
    }

    private fun buildCaveDesc(cave: CaveEntity) = buildString {
        append("${cave.name}\n")
        append("Q.${cave.elevation} SV.${cave.lengthTotal} P.${cave.depthTotal}\n")
    }

    private fun buildSpringDesc(spring: SpringEntity) = buildString {
        append("${spring.name}\n")
        append("Q.${spring.elevation}\n")
        if (spring.flowAverage.isNotBlank()) append("Flow: ${spring.flowAverage}\n")
    }

    private fun buildArtificialDesc(art: ArtificialEntity) = buildString {
        append("${art.name}\n")
        append("Q.${art.elevation} SV.${art.lengthTotal} P.${art.depthTotal}\n")
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
