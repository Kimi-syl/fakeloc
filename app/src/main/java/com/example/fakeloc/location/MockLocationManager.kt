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
            val providers = runCatching { lm.allProviders }.getOrNull().orEmpty().toSet()
            AppLogger.i("allProviders=$providers")
            candidates().any { name ->
                runCatching { lm.getProvider(name) }.getOrNull() != null
            }
        } catch (e: SecurityException) {
            AppLogger.e("isMockEnabled: SecurityException", e)
            false
        } catch (e: Throwable) {
            AppLogger.e("isMockEnabled: unexpected", e)
            false
        }
    }

    fun pushLocation(lat: Double, lon: Double, accuracyMeters: Float = 5f): Boolean {
        AppLogger.i("pushLocation: lat=$lat lon=$lon acc=$accuracyMeters")

        val names = candidates()
        AppLogger.i("pushLocation: candidate providers=$names")

        var anyOk = false
        try {
            for (name in names) {
                ensureTestProvider(name)
                val loc = Location(name).apply {
                    this.latitude = lat
                    this.longitude = lon
                    this.altitude = 0.0
                    this.accuracy = accuracyMeters
                    this.time = System.currentTimeMillis()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        this.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                    }
                }
                runCatching {
                    lm.setTestProviderEnabled(name, true)
                    lm.setTestProviderLocation(name, loc)
                    AppLogger.d("provider $name updated")
                    anyOk = true
                }.onFailure {
                    AppLogger.w("update $name failed: ${it.javaClass.simpleName}: ${it.message}")
                }
            }
        } catch (e: SecurityException) {
            AppLogger.e("pushLocation: SecurityException - this app is not the chosen mock provider", e)
            return false
        } catch (e: IllegalArgumentException) {
            AppLogger.e("pushLocation: IllegalArgumentException - bad arguments or provider missing", e)
            return false
        } catch (e: Throwable) {
            AppLogger.e("pushLocation: unexpected", e)
            return false
        }

        if (anyOk) {
            runCatching {
                val last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                AppLogger.i("getLastKnownLocation(gps)=lat=${last?.latitude} lon=${last?.longitude} time=${last?.time}")
                val lastN = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                AppLogger.i("getLastKnownLocation(network)=lat=${lastN?.latitude} lon=${lastN?.longitude} time=${lastN?.time}")
            }.onFailure { AppLogger.w("readback failed: ${it.message}") }
        }
        return anyOk
    }

    private fun candidates(): List<String> {
        val all = runCatching { lm.allProviders.toSet() }.getOrNull().orEmpty()
        val preferred = listOf("gps", "network", "fused")
        return preferred.filter { it in all }.ifEmpty {
            AppLogger.w("no preferred providers found, falling back to all=${all}")
            listOf("gps", "network")
        }
    }

    private fun ensureTestProvider(name: String) {
        AppLogger.d("ensureTestProvider $name: attempting add (will ignore 'already exists')")
        try {
            lm.addTestProvider(
                name,
                false, false, false, false,
                true, true, true,
                0, 5
            )
            lm.setTestProviderEnabled(name, true)
            AppLogger.i("addTestProvider $name succeeded")
        } catch (e: SecurityException) {
            AppLogger.e("addTestProvider $name: SecurityException - this app is not the chosen mock provider", e)
            throw e
        } catch (e: IllegalArgumentException) {
            val msg = e.message ?: ""
            if (msg.contains("already exists", ignoreCase = true)) {
                AppLogger.i("addTestProvider $name: already exists, treating as success")
                runCatching { lm.setTestProviderEnabled(name, true) }
                    .onFailure { AppLogger.w("setTestProviderEnabled($name) after already-exists: ${it.message}") }
            } else {
                AppLogger.e("addTestProvider $name failed: $msg", e)
                throw e
            }
        } catch (e: Throwable) {
            AppLogger.e("addTestProvider $name failed", e)
            throw e
        }
    }

    fun clear() {
        AppLogger.i("clear: disabling test providers")
        for (name in candidates()) {
            runCatching { lm.setTestProviderEnabled(name, false) }
                .onFailure { AppLogger.w("clear $name: ${it.javaClass.simpleName}: ${it.message}") }
        }
    }
}
