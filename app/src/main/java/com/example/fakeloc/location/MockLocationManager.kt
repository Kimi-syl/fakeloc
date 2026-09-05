package com.example.fakeloc.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import com.example.fakeloc.AppLogger

class MockLocationManager(private val context: Context) {

    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun isMockEnabled(): Boolean {
        return try {
            AppLogger.d("isMockEnabled: checking providers")
            val gpsOk = runCatching { lm.getProvider(LocationManager.GPS_PROVIDER) }.getOrNull() != null
            val netOk = runCatching { lm.getProvider(LocationManager.NETWORK_PROVIDER) }.getOrNull() != null
            AppLogger.i("providers present: gps=$gpsOk network=$netOk")
            gpsOk || netOk
        } catch (e: SecurityException) {
            AppLogger.e("isMockEnabled: SecurityException", e)
            false
        } catch (e: Throwable) {
            AppLogger.e("isMockEnabled: unexpected", e)
            false
        }
    }

    fun pushLocation(lat: Double, lon: Double, accuracyMeters: Float = 5f): Boolean {
        val loc = Location(LocationManager.GPS_PROVIDER).apply {
            this.latitude = lat
            this.longitude = lon
            this.altitude = 0.0
            this.accuracy = accuracyMeters
            this.time = System.currentTimeMillis()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                this.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }
        }
        AppLogger.i("pushLocation: lat=$lat lon=$lon acc=$accuracyMeters")

        return try {
            ensureTestProvider(LocationManager.GPS_PROVIDER)
            ensureTestProvider(LocationManager.NETWORK_PROVIDER)

            lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
            lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc)
            AppLogger.d("gps provider updated")

            lm.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
            lm.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, loc)
            AppLogger.d("network provider updated")

            true
        } catch (e: SecurityException) {
            AppLogger.e("pushLocation: SecurityException - this app is not the chosen mock provider", e)
            false
        } catch (e: IllegalArgumentException) {
            AppLogger.e("pushLocation: IllegalArgumentException - bad arguments or provider missing", e)
            false
        } catch (e: Throwable) {
            AppLogger.e("pushLocation: unexpected", e)
            false
        }
    }

    private fun ensureTestProvider(name: String) {
        val exists = runCatching { lm.getProvider(name) }.getOrNull() != null
        if (exists) {
            AppLogger.d("provider $name already exists, skipping addTestProvider")
            return
        }
        AppLogger.w("provider $name missing, calling addTestProvider")
        try {
            lm.addTestProvider(
                name,                  // name
                false,                // requiresNetwork
                false,                // requiresSatellite
                false,                // requiresCell
                false,                // hasMonetaryCost
                true,                 // supportsAltitude
                true,                 // supportsSpeed
                true,                 // supportsBearing
                0,                    // powerRequirement
                5                     // accuracy (CRITERIA_HIGH)
            )
            lm.setTestProviderEnabled(name, true)
            AppLogger.i("addTestProvider $name succeeded")
        } catch (e: SecurityException) {
            AppLogger.e("addTestProvider $name: SecurityException - enable mock locations in Developer Options and pick this app", e)
            throw e
        } catch (e: Throwable) {
            AppLogger.e("addTestProvider $name failed", e)
            throw e
        }
    }

    fun clear() {
        AppLogger.i("clear: disabling test providers")
        runCatching { lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false) }
            .onFailure { AppLogger.w("clear gps: ${it.javaClass.simpleName}: ${it.message}") }
        runCatching { lm.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, false) }
            .onFailure { AppLogger.w("clear network: ${it.javaClass.simpleName}: ${it.message}") }
    }
}
