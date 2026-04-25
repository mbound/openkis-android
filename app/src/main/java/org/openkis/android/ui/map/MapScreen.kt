package org.openkis.android.ui.map

import android.Manifest
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.hilt.navigation.compose.hiltViewModel
import org.openkis.android.R
import org.openkis.android.ui.theme.ArtificialMarker
import org.openkis.android.ui.theme.CaveMarker
import org.openkis.android.ui.theme.SpringMarker
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapScreen(
    onCaveClick: (type: String, code: String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val caves by viewModel.caves.collectAsState()
    val springs by viewModel.springs.collectAsState()
    val artificials by viewModel.artificials.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showLayerPanel by remember { mutableStateOf(false) }
    var locationEnabled by remember { mutableStateOf(false) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val locationOverlayRef = remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    Configuration.getInstance().userAgentValue = context.packageName

    // Location permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            locationEnabled = true
            locationOverlayRef.value?.enableMyLocation()
            locationOverlayRef.value?.enableFollowLocation()
        }
    }

    // Clean up location overlay when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            locationOverlayRef.value?.disableMyLocation()
            locationOverlayRef.value?.disableFollowLocation()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(6.0)
                    controller.setCenter(GeoPoint(42.5, 12.5))
                    minZoomLevel = 3.0
                    maxZoomLevel = 19.0

                    // Set up location overlay
                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    locationOverlayRef.value = locationOverlay
                    overlays.add(locationOverlay)

                    mapViewRef.value = this
                }
            },
            update = { mapView ->
                // Preserve the location overlay, clear everything else
                val locationOverlay = locationOverlayRef.value
                mapView.overlays.clear()
                if (locationOverlay != null) {
                    mapView.overlays.add(locationOverlay)
                }

                fun createIcon(drawableRes: Int, tintColor: Color): Drawable {
                    val drawable = ContextCompat.getDrawable(context, drawableRes)!!.mutate()
                    DrawableCompat.setTint(drawable, tintColor.toArgb())
                    return drawable
                }

                // Add cave markers
                if (uiState.showCaves) {
                    val caveIcon = createIcon(R.drawable.ic_cave, CaveMarker)
                    for (cave in caves) {
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(cave.latitude, cave.longitude)
                            title = "${cave.code} - ${cave.name}"
                            snippet = buildString {
                                append("Q.${cave.elevation} ")
                                append("SV.${cave.lengthTotal} ")
                                append("P.${cave.depthTotal}")
                            }
                            icon = caveIcon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { _, _ ->
                                viewModel.selectMarker("caves", cave.code)
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }
                }

                // Add spring markers
                if (uiState.showSprings) {
                    val springIcon = createIcon(R.drawable.ic_spring, SpringMarker)
                    for (spring in springs) {
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(spring.latitude, spring.longitude)
                            title = "${spring.code} - ${spring.name}"
                            snippet = "Q.${spring.elevation}"
                            icon = springIcon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { _, _ ->
                                viewModel.selectMarker("springs", spring.code)
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }
                }

                // Add artificial markers
                if (uiState.showArtificials) {
                    val artIcon = createIcon(R.drawable.ic_artificial, ArtificialMarker)
                    for (art in artificials) {
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(art.latitude, art.longitude)
                            title = "${art.code} - ${art.name}"
                            snippet = buildString {
                                append("Q.${art.elevation} ")
                                append("SV.${art.lengthTotal} ")
                                append("P.${art.depthTotal}")
                            }
                            icon = artIcon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { _, _ ->
                                viewModel.selectMarker("artificials", art.code)
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }
                }

                mapView.invalidate()
            }
        )

        // FAB column (layers + my location)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { showLayerPanel = !showLayerPanel },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(Icons.Default.Layers, "Layers")
            }

            SmallFloatingActionButton(
                onClick = {
                    if (locationEnabled) {
                        // Already enabled — center on current location
                        locationOverlayRef.value?.enableFollowLocation()
                        locationOverlayRef.value?.myLocation?.let { loc ->
                            mapViewRef.value?.controller?.animateTo(loc, 15.0, 1000L)
                        }
                    } else {
                        // Request permission
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                containerColor = if (locationEnabled)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    tint = if (locationEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Layer toggle panel
        if (showLayerPanel) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 72.dp, end = 16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    LayerToggle("Caves", CaveMarker, uiState.showCaves) { viewModel.toggleCaves() }
                    LayerToggle("Springs", SpringMarker, uiState.showSprings) { viewModel.toggleSprings() }
                    LayerToggle("Artificials", ArtificialMarker, uiState.showArtificials) { viewModel.toggleArtificials() }
                }
            }
        }

        // Selected marker info card
        if (uiState.selectedCode != null && uiState.selectedType != null) {
            val selectedName = when (uiState.selectedType) {
                "caves" -> caves.find { it.code == uiState.selectedCode }?.let { "${it.code} - ${it.name}" }
                "springs" -> springs.find { it.code == uiState.selectedCode }?.let { "${it.code} - ${it.name}" }
                "artificials" -> artificials.find { it.code == uiState.selectedCode }?.let { "${it.code} - ${it.name}" }
                else -> null
            }

            if (selectedName != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .clickable {
                            onCaveClick(uiState.selectedType!!, uiState.selectedCode!!)
                            viewModel.clearSelection()
                        },
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = selectedName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tap to view details",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerToggle(label: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (enabled) color else Color.Gray)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray
        )
    }
}
