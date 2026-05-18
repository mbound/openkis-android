package org.openkis.android.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.openkis.android.R

object MetadataLabels {

    val meteorology: Map<String, Int> = mapOf(
        "blow_during_heat" to R.string.meteo_blow_during_heat,
        "blow_during_cold" to R.string.meteo_blow_during_cold,
        "suck_during_heat" to R.string.meteo_suck_during_heat,
        "suck_during_cold" to R.string.meteo_suck_during_cold,
        "none" to R.string.meteo_none,
        "blow_always" to R.string.meteo_blow_always,
        "suck_always" to R.string.meteo_suck_always,
        "none_in_heat" to R.string.meteo_none_in_heat,
        "none_in_cold" to R.string.meteo_none_in_cold
    )

    val hydrology: Map<String, Int> = mapOf(
        "temporary flooding" to R.string.hydro_temporary_flooding,
        "absorbent" to R.string.hydro_absorbent,
        "emitting" to R.string.hydro_emitting,
        "issuing" to R.string.hydro_emitting,
        "permanent absorbent" to R.string.hydro_permanent_absorbent,
        "temporary absorbent" to R.string.hydro_temporary_absorbent,
        "permanent issuing" to R.string.hydro_permanent_issuing,
        "temporary issuing" to R.string.hydro_temporary_issuing,
        "lakes" to R.string.hydro_lakes,
        "permanent lakes" to R.string.hydro_permanent_lakes,
        "temporary lakes" to R.string.hydro_temporary_lakes,
        "slight flows" to R.string.hydro_slight_flows,
        "dry" to R.string.hydro_dry,
        "siphons" to R.string.hydro_siphons,
        "permanent siphons" to R.string.hydro_permanent_siphons,
        "temporary siphons" to R.string.hydro_temporary_siphons,
        "only dripping" to R.string.hydro_only_dripping,
        "torrents" to R.string.hydro_torrents,
        "permanent torrents" to R.string.hydro_permanent_torrents,
        "temporary torrents" to R.string.hydro_temporary_torrents,
        "temporary ice" to R.string.hydro_temporary_ice,
        "permanent ice" to R.string.hydro_permanent_ice,
        "snow wells" to R.string.hydro_snow_wells
    )

    val epoch: Map<String, Int> = mapOf(
        "x" to R.string.epoch_not_determined,
        "a" to R.string.epoch_prehistoric,
        "b" to R.string.epoch_protohistoric,
        "c" to R.string.epoch_ancient,
        "d" to R.string.epoch_medieval,
        "e" to R.string.epoch_modern,
        "f" to R.string.epoch_contemporary,
        "g" to R.string.epoch_twentieth
    )
}

@Composable
private fun resolveMultiValue(raw: String, lookup: Map<String, Int>): String {
    val codes = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    if (codes.isEmpty()) return ""
    val parts = ArrayList<String>(codes.size)
    for (code in codes) {
        val resId = lookup[code]
        parts.add(if (resId != null) stringResource(resId) else code)
    }
    return parts.joinToString(", ")
}

@Composable
fun resolveFieldValue(@StringRes labelRes: Int, raw: String): String = when (labelRes) {
    R.string.label_meteorology -> resolveMultiValue(raw, MetadataLabels.meteorology)
    R.string.label_hydrology -> resolveMultiValue(raw, MetadataLabels.hydrology)
    R.string.label_epoch -> MetadataLabels.epoch[raw.trim()]?.let { stringResource(it) } ?: raw
    R.string.label_closed -> when (raw.uppercase()) {
        "X" -> stringResource(R.string.closed_yes)
        "N", "" -> if (raw.isBlank()) "" else stringResource(R.string.closed_no)
        else -> raw
    }
    R.string.label_usage -> when (raw) {
        "free" -> stringResource(R.string.usage_free)
        "captured" -> stringResource(R.string.usage_captured)
        else -> raw
    }
    R.string.label_utilization -> when (raw) {
        "drinking" -> stringResource(R.string.utilization_drinking)
        "agricoltural", "agricultural" -> stringResource(R.string.utilization_agricultural)
        "industrial" -> stringResource(R.string.utilization_industrial)
        else -> raw
    }
    else -> raw
}
