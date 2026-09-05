package com.example.fakeloc.ui

import android.preference.PreferenceManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fakeloc.data.SavedLocationsStore
import com.example.fakeloc.location.MockLocationManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun FakeLocScreen() {
    val context = LocalContext.current
    val mock = remember { MockLocationManager(context) }
    val store = remember { SavedLocationsStore(context) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context)
        )
        Configuration.getInstance().userAgentValue =
            "FakeLoc/1.0 (https://github.com/example/fakeloc; contact@example.com)"
    }

    var pickedLat by remember { mutableStateOf<Double?>(null) }
    var pickedLon by remember { mutableStateOf<Double?>(null) }
    var spoofing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var spotName by remember { mutableStateOf("") }
    var manualLat by remember { mutableStateOf("") }
    var manualLon by remember { mutableStateOf("") }

    val mapView = remember { MapView(context) }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (!mock.isMockEnabled()) "Mock locations NOT enabled"
                               else if (spoofing) "Spoofing: ${pickedLat?.format6()}, ${pickedLon?.format6()}"
                               else "Pick a spot on the map",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (statusMessage.isNotEmpty()) {
                        Text(text = statusMessage, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(420.dp)
            ) {
                AndroidView(
                    factory = {
                        mapView.apply {
                            setTileSource(CartoLight)
                            setMultiTouchControls(true)
                            setHorizontalMapRepetitionEnabled(false)
                            setVerticalMapRepetitionEnabled(false)
                            isTilesScaledToDpi = true
                            controller.setZoom(4.0)
                            controller.setCenter(GeoPoint(20.0, 0.0))

                            val tap = MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    if (p == null) return false
                                    pickedLat = p.latitude
                                    pickedLon = p.longitude
                                    overlays.removeAll { it is Marker }
                                    val m = Marker(this@apply).apply {
                                        position = p
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        title = "Selected"
                                    }
                                    overlays.add(m)
                                    invalidate()
                                    return true
                                }
                                override fun longPressHelper(p: GeoPoint?): Boolean = false
                            })
                            overlays.add(tap)
                        }
                    },
                    update = { /* no-op; state lives in composable */ },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = spotName,
                    onValueChange = { spotName = it },
                    label = { Text("Name") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        val lat = pickedLat; val lon = pickedLon
                        if (lat != null && lon != null && spotName.isNotBlank()) {
                            store.add(SavedLocationsStore.Spot(spotName.trim(), lat, lon))
                            statusMessage = "Saved \"${spotName.trim()}\""
                        }
                    },
                    enabled = pickedLat != null && spotName.isNotBlank()
                ) { Text("Save") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val lat = pickedLat; val lon = pickedLon
                        if (lat != null && lon != null) {
                            val ok = mock.pushLocation(lat, lon)
                            spoofing = ok
                            statusMessage = if (ok) "Pushed location to system"
                                            else "Failed — enable Mock Locations in Developer Options and pick this app"
                        }
                    },
                    enabled = pickedLat != null && !spoofing,
                    modifier = Modifier.weight(1f)
                ) { Text("Start spoofing") }

                OutlinedButton(
                    onClick = {
                        mock.clear()
                        spoofing = false
                        statusMessage = "Stopped"
                    },
                    enabled = spoofing,
                    modifier = Modifier.weight(1f)
                ) { Text("Stop") }
            }

            Text("Or enter coordinates manually:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = manualLat,
                    onValueChange = { manualLat = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                    label = { Text("Latitude") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = manualLon,
                    onValueChange = { manualLon = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                    label = { Text("Longitude") },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val lat = manualLat.toDoubleOrNull()
                        val lon = manualLon.toDoubleOrNull()
                        if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                            pickedLat = lat; pickedLon = lon
                            mapView.controller.setCenter(GeoPoint(lat, lon))
                            mapView.controller.setZoom(13.0)
                            mapView.overlays.removeAll { it is Marker }
                            mapView.overlays.add(
                                Marker(mapView).apply {
                                    position = GeoPoint(lat, lon)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = "Selected"
                                }
                            )
                            mapView.invalidate()
                            statusMessage = "Set to $lat, $lon"
                        } else {
                            statusMessage = "Invalid coordinates (lat -90..90, lon -180..180)"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Set") }
            }

            val saved = remember { store.list() }
            if (saved.isNotEmpty()) {
                Text("Saved spots:", style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp))
                saved.forEach { s ->
                    OutlinedButton(
                        onClick = {
                            pickedLat = s.lat; pickedLon = s.lon
                            mapView.controller.setCenter(GeoPoint(s.lat, s.lon))
                            mapView.overlays.removeAll { it is Marker }
                            mapView.overlays.add(
                                Marker(mapView).apply {
                                    position = GeoPoint(s.lat, s.lon)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = s.name
                                }
                            )
                            mapView.invalidate()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) { Text("${s.name}: ${s.lat.format6()}, ${s.lon.format6()}") }
                }
            }
        }
    }
}

private fun Double.format6(): String = "%.6f".format(this)

private val CartoLight = object : OnlineTileSourceBase(
    "CartoDB Positron",
    0, 19, 256, ".png",
    arrayOf("a.basemaps.cartocdn.com", "b.basemaps.cartocdn.com", "c.basemaps.cartocdn.com", "d.basemaps.cartocdn.com"),
    "https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
    TileSourcePolicy(2, TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL)
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        return baseUrl
            .replace("{z}", MapTileIndex.getZoom(pMapTileIndex).toString())
            .replace("{x}", MapTileIndex.getX(pMapTileIndex).toString())
            .replace("{y}", MapTileIndex.getY(pMapTileIndex).toString())
    }
}