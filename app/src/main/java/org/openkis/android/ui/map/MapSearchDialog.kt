package org.openkis.android.ui.map

import android.location.Geocoder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openkis.android.R
import java.util.Locale

data class MapSearchObject(
    val type: String,
    val code: String,
    val name: String,
    val searchText: String,
    val latitude: Double,
    val longitude: Double
)

private data class MapPlaceResult(
    val label: String,
    val latitude: Double,
    val longitude: Double
)

@Suppress("DEPRECATION")
@Composable
fun MapSearchDialog(
    objects: List<MapSearchObject>,
    onDismiss: () -> Unit,
    onObjectSelected: (MapSearchObject) -> Unit,
    onPlaceSelected: (latitude: Double, longitude: Double) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var placeResults by remember { mutableStateOf<List<MapPlaceResult>>(emptyList()) }
    var placeError by remember { mutableStateOf<String?>(null) }
    var geocoding by remember { mutableStateOf(false) }

    val localResults = remember(query, objects) {
        val q = query.trim()
        if (q.length < 2) {
            emptyList()
        } else {
            objects.asSequence()
                .filter { it.searchText.contains(q, ignoreCase = true) }
                .take(10)
                .toList()
        }
    }

    fun searchPlaces() {
        val q = query.trim()
        if (q.length < 2 || geocoding) return

        geocoding = true
        placeError = null
        placeResults = emptyList()
        scope.launch {
            val results = withContext(Dispatchers.IO) {
                runCatching {
                    if (!Geocoder.isPresent()) return@runCatching emptyList()
                    Geocoder(context, Locale.getDefault())
                        .getFromLocationName(q, 6)
                        .orEmpty()
                        .map { address ->
                            MapPlaceResult(
                                label = address.getAddressLine(0)
                                    ?: address.featureName
                                    ?: address.locality
                                    ?: q,
                                latitude = address.latitude,
                                longitude = address.longitude
                            )
                        }
                }
            }
            geocoding = false
            results.onSuccess {
                placeResults = it
                if (it.isEmpty()) {
                    placeError = context.getString(R.string.map_search_no_places)
                }
            }.onFailure {
                placeError = context.getString(R.string.map_search_place_error)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.map_search_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        placeResults = emptyList()
                        placeError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    placeholder = {
                        Text(stringResource(R.string.map_search_hint))
                    }
                )

                OutlinedButton(
                    onClick = { searchPlaces() },
                    enabled = query.trim().length >= 2 && !geocoding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (geocoding) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text(stringResource(R.string.map_search_places))
                }

                if (localResults.isNotEmpty()) {
                    Text(
                        stringResource(R.string.map_search_objects),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(
                        localResults,
                        key = { "object_${it.type}_${it.code}" }
                    ) { item ->
                        ListItem(
                            headlineContent = {
                                Text(item.name.ifBlank { item.code })
                            },
                            supportingContent = {
                                Text(item.code)
                            },
                            modifier = Modifier.clickable {
                                onObjectSelected(item)
                            }
                        )
                    }

                    if (placeResults.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.map_search_places),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    top = 8.dp,
                                    bottom = 4.dp
                                )
                            )
                        }
                    }

                    items(
                        placeResults,
                        key = { "place_${it.latitude}_${it.longitude}_${it.label}" }
                    ) { place ->
                        ListItem(
                            headlineContent = { Text(place.label) },
                            leadingContent = {
                                Icon(Icons.Default.Place, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                onPlaceSelected(place.latitude, place.longitude)
                            }
                        )
                    }
                }

                placeError?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
