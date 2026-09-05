package com.example.fakeloc.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock

class MockLocationManager(private val context: Context) {

    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun isMockEnabled(): Boolean {
        return try {
            val provider = lm.getProvider(LocationManager.GPS_PROVIDER) ?: return false
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun pushLocation(lat: Double, lon: Double, accuracyMeters: Float = 5f): Boolean {
        val loc = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = lat
            longitude = lon
            altitude = 0.0
            accuracy = accuracyMeters
            time = System.currentTimeMillis()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }
        }
        return try {
            lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
            lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc)
            lm.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
            lm.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, loc)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun clear() {
        runCatching { lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false) }
        runCatching { lm.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, false) }
    }

    companion object {
        const val PREFS = "fakeloc_prefs"
        const val KEY_LAT = "last_lat"
        const val KEY_LON = "last_lon"
    }
}