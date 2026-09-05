package com.example.fakeloc.data

import android.content.Context

class SavedLocationsStore(context: Context) {
    private val prefs = context.getSharedPreferences("fakeloc_saved", Context.MODE_PRIVATE)

    data class Spot(val name: String, val lat: Double, val lon: Double)

    fun list(): List<Spot> {
        val raw = prefs.getString("spots", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size == 3) {
                val lat = parts[1].toDoubleOrNull()
                val lon = parts[2].toDoubleOrNull()
                if (lat != null && lon != null) Spot(parts[0], lat, lon) else null
            } else null
        }
    }

    fun add(spot: Spot) {
        val current = list().toMutableList()
        current.removeAll { it.lat == spot.lat && it.lon == spot.lon }
        current.add(0, spot)
        val raw = current.joinToString("\n") { "${it.name}|${it.lat}|${it.lon}" }
        prefs.edit().putString("spots", raw).apply()
    }
}