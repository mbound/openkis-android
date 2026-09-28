package org.openkis.android.ui.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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

data class ObjectNumericFilters(
    val minDepthMeters: String = "",
    val minLengthMeters: String = "",
    val minElevationMeters: String = "",
    val maxElevationMeters: String = ""
) {
    val isActive: Boolean
        get() = minDepthMeters.isNotBlank() ||
            minLengthMeters.isNotBlank() ||
            minElevationMeters.isNotBlank() ||
            maxElevationMeters.isNotBlank()

    fun matches(
        depth: String?,
        length: String?,
        elevation: String?
    ): Boolean {
        val minDepth = minDepthMeters.toFilterNumber()
        val minLength = minLengthMeters.toFilterNumber()
        val minElevation = minElevationMeters.toFilterNumber()
        val maxElevation = maxElevationMeters.toFilterNumber()

        if (minDepth != null) {
            val value = depth.toMeasurementNumber()?.let(::abs) ?: return false
            if (value < minDepth) return false
        }
        if (minLength != null) {
            val value = length.toMeasurementNumber() ?: return false
            if (value < minLength) return false
        }
        if (minElevation != null) {
            val value = elevation.toMeasurementNumber() ?: return false
            if (value < minElevation) return false
        }
        if (maxElevation != null) {
            val value = elevation.toMeasurementNumber() ?: return false
            if (value > maxElevation) return false
        }
        return true
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
    var depth by remember(initial) { mutableStateOf(initial.minDepthMeters) }
    var length by remember(initial) { mutableStateOf(initial.minLengthMeters) }
    var minElevation by remember(initial) { mutableStateOf(initial.minElevationMeters) }
    var maxElevation by remember(initial) { mutableStateOf(initial.maxElevationMeters) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_measurements_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.filter_measurements_desc))
                OutlinedTextField(
                    value = depth,
                    onValueChange = { depth = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.filter_min_depth)) },
                    suffix = { Text("m") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = length,
                    onValueChange = { length = it },
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
                            minDepthMeters = depth.trim(),
                            minLengthMeters = length.trim(),
                            minElevationMeters = minElevation.trim(),
                            maxElevationMeters = maxElevation.trim()
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
