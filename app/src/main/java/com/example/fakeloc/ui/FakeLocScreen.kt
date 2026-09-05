package com.example.fakeloc.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fakeloc.AppLogger
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
fun FakeLocScreen(onShowLogs: () -> Unit) {
    val context = LocalContext.current
    val mock = remember { MockLocationManager(context) }
    val store = remember { SavedLocationsStore(context) }
    var mapError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val prefs = context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
            Configuration.getInstance().load(context, prefs)
            Configuration.getInstance().userAgentValue =
                "FakeLoc/1.0 (https://github.com/Kimi-syl/fakeloc; contact@example.com)"
            AppLogger.i("osmdroid configuration loaded")
        } catch (t: Throwable) {
            AppLogger.e("osmdroid init failed", t)
            mapError = "osmdroid init failed: ${t.message}"
        }
    }

    var pickedLat by remember { mutableStateOf<Double?>(null) }
    var pickedLon by remember { mutableStateOf<Double?>(null) }
    var spoofing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var spotName by remember { mutableStateOf("") }
    var manualLat by remember { mutableStateOf("") }
    var manualLon by remember { mutableStateOf("") }

    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { mapView?.onDetach() }
                .onFailure { AppLogger.w("mapView.onDetach failed: ${it.message}") }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (!mock.isMockEnabled()) "Mock locations NOT enabled"
                               else if (spoofing) "Spoofing: ${pickedLat?.format6()}, ${pickedLon?.format6()}"
                               else "Pick a spot on the map or type coordinates",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (statusMessage.isNotEmpty()) {
                        Text(text = statusMessage, style = MaterialTheme.typography.bodySmall)
                    }
                    if (mapError != null) {
                        Text(text = "Map: $mapError", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(380.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        try {
                            val mv = MapView(ctx).apply {
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
                            mapView = mv
                            AppLogger.i("MapView created")
                            mv
                        } catch (t: Throwable) {
                            AppLogger.e("MapView factory crashed", t)
                            mapError = "MapView failed: ${t.javaClass.simpleName}: ${t.message}"
                            throw t
                        }
                    },
                    update = { /* state lives in composable */ },
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
                            AppLogger.i("saved spot \"${spotName.trim()}\" at $lat,$lon")
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
                            AppLogger.i("user pressed Start spoofing, lat=$lat lon=$lon")
                            val ok = try {
                                mock.pushLocation(lat, lon)
                            } catch (t: Throwable) {
                                AppLogger.e("pushLocation threw unexpectedly", t)
                                false
                            }
                            spoofing = ok
                            statusMessage = if (ok) "Pushed location to system"
                                            else "Failed — check logs and Developer Options mock provider"
                        } else {
                            AppLogger.w("Start pressed with no coordinates selected")
                        }
                    },
                    enabled = pickedLat != null && !spoofing,
                    modifier = Modifier.weight(1f)
                ) { Text("Start spoofing") }

                OutlinedButton(
                    onClick = {
                        AppLogger.i("user pressed Stop")
                        try { mock.clear() } catch (t: Throwable) { AppLogger.e("clear threw", t) }
                        spoofing = false
                        statusMessage = "Stopped"
                    },
                    enabled = spoofing,
                    modifier = Modifier.weight(1f)
                ) { Text("Stop") }
            }

            Text("Or enter coordinates manually:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp))
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
                            mapView?.let { mv ->
                                runCatching {
                                    mv.controller.setCenter(GeoPoint(lat, lon))
                                    mv.controller.setZoom(13.0)
                                    mv.overlays.removeAll { it is Marker }
                                    mv.overlays.add(
                                        Marker(mv).apply {
                                            position = GeoPoint(lat, lon)
                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                            title = "Selected"
                                        }
                                    )
                                    mv.invalidate()
                                }.onFailure { AppLogger.e("manual set failed", it) }
                            }
                            statusMessage = "Set to $lat, $lon"
                            AppLogger.i("manual coordinates set: $lat, $lon")
                        } else {
                            statusMessage = "Invalid coordinates (lat -90..90, lon -180..180)"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Set") }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onShowLogs, modifier = Modifier.weight(1f)) {
                    Text("View logs")
                }
            }

            val saved = remember { store.list() }
            if (saved.isNotEmpty()) {
                Text("Saved spots:", style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp))
                saved.forEach { s ->
                    OutlinedButton(
                        onClick = {
                            pickedLat = s.lat; pickedLon = s.lon
                            mapView?.let { mv ->
                                runCatching {
                                    mv.controller.setCenter(GeoPoint(s.lat, s.lon))
                                    mv.overlays.removeAll { it is Marker }
                                    mv.overlays.add(
                                        Marker(mv).apply {
                                            position = GeoPoint(s.lat, s.lon)
                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                            title = s.name
                                        }
                                    )
                                    mv.invalidate()
                                }.onFailure { AppLogger.e("saved spot jump failed", it) }
                            }
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
    arrayOf("a.basemaps.cartocdn.com", "b.basemaps.cartocdn.com",
            "c.basemaps.cartocdn.com", "d.basemaps.cartocdn.com"),
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
