package org.openkis.android.ui.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.openkis.android.R
import kotlin.math.abs

enum class FilterMatchMode {
    ALL,
    ANY
}

data class ObjectNumericFilters(
    val minVerticalExtentMeters: String = "",
    val minDevelopmentMeters: String = "",
    val minElevationMeters: String = "",
    val maxElevationMeters: String = "",
    val matchMode: FilterMatchMode = FilterMatchMode.ALL
) {
    private val minVerticalExtent = minVerticalExtentMeters.toFilterNumber()
    private val minDevelopment = minDevelopmentMeters.toFilterNumber()
    private val minElevation = minElevationMeters.toFilterNumber()
    private val maxElevation = maxElevationMeters.toFilterNumber()

    val isActive: Boolean
        get() = minVerticalExtent != null ||
            minDevelopment != null ||
            minElevation != null ||
            maxElevation != null

    fun matches(
        verticalExtent: String?,
        development: String?,
        elevation: String?
    ): Boolean {
        val criteria = ArrayList<Boolean>(4)

        minVerticalExtent?.let { threshold ->
            val value = verticalExtent.toMeasurementNumber()?.let(::abs)
            criteria += value != null && value >= threshold
        }
        minDevelopment?.let { threshold ->
            val value = development.toMeasurementNumber()
            criteria += value != null && value >= threshold
        }
        minElevation?.let { threshold ->
            val value = elevation.toMeasurementNumber()
            criteria += value != null && value >= threshold
        }
        maxElevation?.let { threshold ->
            val value = elevation.toMeasurementNumber()
            criteria += value != null && value <= threshold
        }

        if (criteria.isEmpty()) return true
        return when (matchMode) {
            FilterMatchMode.ALL -> criteria.all { it }
            FilterMatchMode.ANY -> criteria.any { it }
        }
    }
}

private val NUMBER_REGEX = Regex("""[-+]?\d+(?:[.,]\d+)?""")

private fun String?.toMeasurementNumber(): Double? {
    if (this.isNullOrBlank()) return null
    return NUMBER_REGEX.find(this)?.value?.replace(',', '.')?.toDoubleOrNull()
}

private fun String.toFilterNumber(): Double? =
    trim().replace(',', '.').toDoubleOrNull()

@Composable
fun NumericFilterDialog(
    initial: ObjectNumericFilters,
    onDismiss: () -> Unit,
    onApply: (ObjectNumericFilters) -> Unit
) {
    var verticalExtent by remember(initial) { mutableStateOf(initial.minVerticalExtentMeters) }
    var development by remember(initial) { mutableStateOf(initial.minDevelopmentMeters) }
    var minElevation by remember(initial) { mutableStateOf(initial.minElevationMeters) }
    var maxElevation by remember(initial) { mutableStateOf(initial.maxElevationMeters) }
    var matchMode by remember(initial) { mutableStateOf(initial.matchMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_measurements_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.filter_measurements_desc))
                Text(
                    text = stringResource(R.string.filter_match_mode),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { matchMode = FilterMatchMode.ALL }
                ) {
                    RadioButton(
                        selected = matchMode == FilterMatchMode.ALL,
                        onClick = { matchMode = FilterMatchMode.ALL }
                    )
                    Text(
                        text = stringResource(R.string.filter_match_all),
                        modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { matchMode = FilterMatchMode.ANY }
                ) {
                    RadioButton(
                        selected = matchMode == FilterMatchMode.ANY,
                        onClick = { matchMode = FilterMatchMode.ANY }
                    )
                    Text(
                        text = stringResource(R.string.filter_match_any),
                        modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)
                    )
                }
                OutlinedTextField(
                    value = verticalExtent,
                    onValueChange = { verticalExtent = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.filter_min_depth)) },
                    suffix = { Text("m") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = development,
                    onValueChange = { development = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.filter_min_length)) },
                    suffix = { Text("m") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = minElevation,
                    onValueChange = { minElevation = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.filter_min_elevation)) },
                    suffix = { Text("m") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxElevation,
                    onValueChange = { maxElevation = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.filter_max_elevation)) },
                    suffix = { Text("m") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(
                        ObjectNumericFilters(
                            minVerticalExtentMeters = verticalExtent.trim(),
                            minDevelopmentMeters = development.trim(),
                            minElevationMeters = minElevation.trim(),
                            maxElevationMeters = maxElevation.trim(),
                            matchMode = matchMode
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onApply(ObjectNumericFilters())
                }
            ) {
                Text(stringResource(R.string.clear))
            }
        }
    )
}
