package org.openkis.android.data.export

import org.openkis.android.data.local.entity.ArtificialEntity
import org.openkis.android.data.local.entity.CaveEntity
import org.openkis.android.data.local.entity.SpringEntity
import java.io.OutputStream
import javax.inject.Inject

class KmlExporter @Inject constructor() {

    fun export(
        output: OutputStream,
        caves: List<CaveEntity>,
        springs: List<SpringEntity>,
        artificials: List<ArtificialEntity>
    ) {
        output.bufferedWriter().use { writer ->
            writer.write("""<?xml version="1.0" encoding="UTF-8"?>""")
            writer.newLine()
            writer.write("""<kml xmlns="http://www.opengis.net/kml/2.2">""")
            writer.newLine()
            writer.write("<Document>")
            writer.newLine()
            writer.write("<name>OpenKIS Export</name>")
            writer.newLine()

            // Cave style
            writer.write("""
                |<Style id="cave-style">
                |  <IconStyle><color>ff0000ff</color><scale>0.3</scale>
                |    <Icon><href>http://maps.google.com/mapfiles/kml/shapes/triangle.png</href></Icon>
                |  </IconStyle>
                |</Style>
            """.trimMargin())
            writer.newLine()

            // Spring style
            writer.write("""
                |<Style id="spring-style">
                |  <IconStyle><color>ffff0000</color><scale>0.3</scale>
                |    <Icon><href>http://maps.google.com/mapfiles/kml/shapes/water.png</href></Icon>
                |  </IconStyle>
                |</Style>
            """.trimMargin())
            writer.newLine()

            // Artificial style
            writer.write("""
                |<Style id="artificial-style">
                |  <IconStyle><color>ff800080</color><scale>0.3</scale>
                |    <Icon><href>http://maps.google.com/mapfiles/kml/shapes/square.png</href></Icon>
                |  </IconStyle>
                |</Style>
            """.trimMargin())
            writer.newLine()

            // Caves folder
            if (caves.isNotEmpty()) {
                writer.write("<Folder><name>Caves</name>")
                writer.newLine()
                for (cave in caves) {
                    if (cave.latitude == 0.0 && cave.longitude == 0.0) continue
                    writePlacemark(writer, cave.code, cave.name,
                        buildCaveDescription(cave),
                        cave.longitude, cave.latitude, cave.elevation, "#cave-style")
                }
                writer.write("</Folder>")
                writer.newLine()
            }

            // Springs folder
            if (springs.isNotEmpty()) {
                writer.write("<Folder><name>Springs</name>")
                writer.newLine()
                for (spring in springs) {
                    if (spring.latitude == 0.0 && spring.longitude == 0.0) continue
                    writePlacemark(writer, spring.code, spring.name,
                        buildSpringDescription(spring),
                        spring.longitude, spring.latitude, spring.elevation, "#spring-style")
                }
                writer.write("</Folder>")
                writer.newLine()
            }

            // Artificials folder
            if (artificials.isNotEmpty()) {
                writer.write("<Folder><name>Artificial Cavities</name>")
                writer.newLine()
                for (art in artificials) {
                    if (art.latitude == 0.0 && art.longitude == 0.0) continue
                    writePlacemark(writer, art.code, art.name,
                        buildArtificialDescription(art),
                        art.longitude, art.latitude, art.elevation, "#artificial-style")
                }
                writer.write("</Folder>")
                writer.newLine()
            }

            writer.write("</Document>")
            writer.newLine()
            writer.write("</kml>")
        }
    }

    private fun writePlacemark(
        writer: java.io.BufferedWriter,
        code: String, name: String, description: String,
        lon: Double, lat: Double, elevation: String, styleUrl: String
    ) {
        val elev = elevation.toDoubleOrNull() ?: 0.0
        val escapedName = escapeXml("$code - $name")
        val escapedDesc = escapeXml(description)
        writer.write("""
            |<Placemark>
            |  <name>$escapedName</name>
            |  <description><![CDATA[$escapedDesc]]></description>
            |  <styleUrl>$styleUrl</styleUrl>
            |  <Point><coordinates>$lon,$lat,$elev</coordinates></Point>
            |</Placemark>
        """.trimMargin())
        writer.newLine()
    }

    private fun buildCaveDescription(cave: CaveEntity): String = buildString {
        if (cave.synonyms.isNotBlank()) append("Synonyms: ${cave.synonyms}\n")
        append("Elevation: ${cave.elevation}m\n")
        append("Length: ${cave.lengthTotal}m\n")
        append("Depth: ${cave.depthTotal}m\n")
        if (cave.hydrology.isNotBlank()) append("Hydrology: ${cave.hydrology}\n")
        if (cave.meteorology.isNotBlank()) append("Meteorology: ${cave.meteorology}\n")
    }

    private fun buildSpringDescription(spring: SpringEntity): String = buildString {
        append("Elevation: ${spring.elevation}m\n")
        if (spring.flowMax.isNotBlank()) append("Flow max: ${spring.flowMax}\n")
        if (spring.flowMin.isNotBlank()) append("Flow min: ${spring.flowMin}\n")
        if (spring.flowAverage.isNotBlank()) append("Flow avg: ${spring.flowAverage}\n")
        if (spring.usage.isNotBlank()) append("Usage: ${spring.usage}\n")
    }

    private fun buildArtificialDescription(art: ArtificialEntity): String = buildString {
        if (art.synonyms.isNotBlank()) append("Synonyms: ${art.synonyms}\n")
        append("Elevation: ${art.elevation}m\n")
        append("Length: ${art.lengthTotal}m\n")
        append("Depth: ${art.depthTotal}m\n")
        if (art.year.isNotBlank()) append("Year: ${art.year}\n")
        if (art.typology.isNotBlank()) append("Type: ${art.typology}\n")
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
