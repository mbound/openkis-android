package org.openkis.android.ui.map

import org.openkis.android.data.local.entity.ArtificialEntity
import org.openkis.android.data.local.entity.CaveEntity
import org.openkis.android.data.local.entity.SpringEntity

/**
 * Maps entity metadata to the ordered list of PNG layer names used by IconCompositor.
 * Logic mirrors openkis_GetIcon() in the PHP backend (extra/openkis.inc.php lines 175–338).
 */
object MarkerIconResolver {

    fun caveIconLayers(cave: CaveEntity): List<String> = buildList {
        add("caves")

        // Trend: compare absolute depth values
        val neg = cave.depthNegative.toDoubleOrNull()?.let { kotlin.math.abs(it) } ?: 0.0
        val pos = cave.depthPositive.toDoubleOrNull()?.let { kotlin.math.abs(it) } ?: 0.0
        when {
            neg == 0.0 && pos == 0.0 -> add("hori")
            neg >= pos -> add("desc")
            else -> add("asc")
        }

        // Hydrology type (hydrology field takes precedence over name/synonyms)
        val hydro = cave.hydrology.lowercase()
        val nameText = "${cave.name} ${cave.synonyms}".lowercase()
        when {
            hydro.contains("torrents") || hydro.contains("siphons") || hydro.contains("lakes") ->
                add("water")
            hydro.contains("emitting") || hydro.contains("issuing") ->
                add("emitting")
            nameText.contains("risorgenza") || nameText.contains("sorgente") ->
                add("emitting")
            nameText.contains("inghiottitoio") ->
                add("absorbent")
        }

        // Meteorology
        val validMeteo = setOf(
            "suck_during_cold", "blow_during_heat", "blow_during_cold", "suck_during_heat"
        )
        if (cave.meteorology in validMeteo) add(cave.meteorology)

        // Closure (fauna/bats deferred — no fauna field in CaveEntity yet)
        if (cave.closed.isNotBlank() && cave.closed != "N") add("closed")
    }

    fun springIconLayers(@Suppress("UNUSED_PARAMETER") spring: SpringEntity): List<String> =
        listOf("springs")

    fun artificialIconLayers(@Suppress("UNUSED_PARAMETER") art: ArtificialEntity): List<String> =
        listOf("artificials")
}
