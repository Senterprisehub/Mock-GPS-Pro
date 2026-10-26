package com.example.engine

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.LogLevel
import com.example.data.MockCoordinates
import com.example.data.MockEngineConfig
import com.example.data.PermissionsState
import com.example.data.TelemetryLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Locale

class MockLocationManager(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    private val _logEvents = MutableSharedFlow<TelemetryLog>(extraBufferCapacity = 64)
    val logEvents: SharedFlow<TelemetryLog> = _logEvents.asSharedFlow()

    private var isProviderRegistered = false
    private var isCurrentlyMocking = false

    private val prefs = context.getSharedPreferences("mock_gps_hw_real_loc", Context.MODE_PRIVATE)

    @Volatile
    private var cachedRealLocation: Location? = null

    init {
        try {
            val savedLat = prefs.getString("real_hw_lat", null)?.toDoubleOrNull()
            val savedLng = prefs.getString("real_hw_lng", null)?.toDoubleOrNull()
            val savedAlt = prefs.getString("real_hw_alt", "0.0")?.toDoubleOrNull() ?: 0.0
            val savedAcc = prefs.getFloat("real_hw_acc", 5.0f)
            val savedTime = prefs.getLong("real_hw_time", 0L)
            if (savedLat != null && savedLng != null) {
                cachedRealLocation = Location(LocationManager.GPS_PROVIDER).apply {
                    latitude = savedLat
                    longitude = savedLng
                    altitude = savedAlt
                    accuracy = savedAcc
                    time = savedTime
                }
            }
        } catch (e: Exception) {
            // Ignore prefs reading error
        }
    }

    private fun isRealHardwareLocation(location: Location?): Boolean {
        if (location == null) return false
        val isMocked = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                location.isMock
            } else {
                @Suppress("DEPRECATION")
                location.isFromMockProvider
            }
        } catch (e: Exception) {
            false
        }
        return !isMocked
    }

    private fun saveRealLocation(location: Location) {
        if (!isRealHardwareLocation(location)) return
        cachedRealLocation = location
        try {
            prefs.edit()
                .putString("real_hw_lat", location.latitude.toString())
                .putString("real_hw_lng", location.longitude.toString())
                .putString("real_hw_alt", location.altitude.toString())
                .putFloat("real_hw_acc", location.accuracy)
                .putLong("real_hw_time", location.time)
                .apply()
        } catch (e: Exception) {
            // Ignore prefs save error
        }
    }

    fun isMockLocationAppSelected(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val mode = appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    Process.myUid(),
                    context.packageName
                )
                mode == AppOpsManager.MODE_ALLOWED
            } else {
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ALLOW_MOCK_LOCATION
                ) != "0"
            }
        } catch (e: Exception) {
            false
        }
    }

    fun checkPermissionsState(): PermissionsState {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val bgGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        val batteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val mockAppSelected = isMockLocationAppSelected()

        return PermissionsState(
            fineLocationGranted = fineGranted,
            bgLocationGranted = bgGranted,
            overlayPermissionGranted = overlayGranted,
            fgsPermissionGranted = true,
            batteryOptimizationIgnored = batteryIgnored,
            mockLocationAppSelected = mockAppSelected,
            notificationPermissionGranted = notifGranted
        )
    }

    fun getRealDeviceLocation(): Location? {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return cachedRealLocation

        return try {
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            val locations = providers.mapNotNull { provider ->
                try {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null && isRealHardwareLocation(loc)) {
                        loc
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            val best = locations.maxByOrNull { it.time }
            if (best != null) {
                saveRealLocation(best)
                best
            } else {
                cachedRealLocation
            }
        } catch (e: Exception) {
            cachedRealLocation
        }
    }

    fun requestFreshLocation(onLocationReceived: (Location) -> Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // 1. Immediately return best verified non-mock location if available
        getRealDeviceLocation()?.let { onLocationReceived(it) }

        if (!hasFine && !hasCoarse) return

        // 2. Request active update from GPS and Network
        try {
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (isRealHardwareLocation(location)) {
                        saveRealLocation(location)
                        onLocationReceived(location)
                        try {
                            locationManager.removeUpdates(this)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L,
                    0f,
                    listener,
                    context.mainLooper
                )
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    500L,
                    0f,
                    listener,
                    context.mainLooper
                )
            }
        } catch (e: Exception) {
            Log.w("MockLocationManager", "Failed to request fresh location update: ${e.message}")
        }
    }

    @Synchronized
    fun startMocking(): Boolean {
        try {
            registerTestProvider(LocationManager.GPS_PROVIDER)
            try {
                registerTestProvider(LocationManager.NETWORK_PROVIDER)
            } catch (e: Exception) {
                // Ignore network provider if not available
            }
            isCurrentlyMocking = true
            emitLog("SERVICE_START", LogLevel.SUCCESS, "Mock Location Providers initialized and active.")
            return true
        } catch (e: SecurityException) {
            isCurrentlyMocking = false
            emitLog("MOCK_SECURITY_ERR", LogLevel.WARNING, "Mock app not set in Developer Options: ${e.message}")
            return false
        } catch (e: Exception) {
            isCurrentlyMocking = false
            emitLog("MOCK_INIT_ERR", LogLevel.ERROR, "Failed to start mock provider: ${e.message}")
            return false
        }
    }

    @Synchronized
    fun stopMocking() {
        try {
            removeTestProvider(LocationManager.GPS_PROVIDER)
            removeTestProvider(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            // Ignore
        }
        isCurrentlyMocking = false
        isProviderRegistered = false
        emitLog("SERVICE_STOP", LogLevel.INFO, "Mock Location injection stopped.")

        // Immediately request fresh real hardware fix after stopping injection
        try {
            requestFreshLocation { loc ->
                saveRealLocation(loc)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun registerTestProvider(providerName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                locationManager.addTestProvider(
                    providerName,
                    false,
                    false,
                    false,
                    false,
                    true,
                    true,
                    true,
                    ProviderProperties.POWER_USAGE_LOW,
                    ProviderProperties.ACCURACY_FINE
                )
            } else {
                @Suppress("DEPRECATION")
                locationManager.addTestProvider(
                    providerName,
                    "requiresNetwork" == "",
                    "requiresSatellite" == "",
                    "requiresCell" == "",
                    false,
                    true,
                    true,
                    true,
                    Criteria.POWER_LOW,
                    Criteria.ACCURACY_FINE
                )
            }
            locationManager.setTestProviderEnabled(providerName, true)
            isProviderRegistered = true
        } catch (e: SecurityException) {
            throw e
        } catch (e: IllegalArgumentException) {
            // Provider already added
            locationManager.setTestProviderEnabled(providerName, true)
            isProviderRegistered = true
        }
    }

    private fun removeTestProvider(providerName: String) {
        try {
            locationManager.setTestProviderEnabled(providerName, false)
            locationManager.removeTestProvider(providerName)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun injectLocation(
        coordinates: MockCoordinates,
        config: MockEngineConfig
    ) {
        var finalLat = coordinates.latitude
        var finalLng = coordinates.longitude

        // Apply satellite jitter if enabled
        if (config.jitterEnabled && config.jitterRadiusMeters > 0.05f) {
            val (jitterLat, jitterLng) = GeoUtils.generateJitter(config.jitterRadiusMeters)
            finalLat += jitterLat
            finalLng += jitterLng
        }

        val accuracy = if (config.jitterEnabled) {
            coordinates.accuracy + (Math.random().toFloat() * 1.2f - 0.6f)
        } else {
            coordinates.accuracy
        }

        val timeMs = System.currentTimeMillis()
        val elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()

        try {
            val location = Location(LocationManager.GPS_PROVIDER).apply {
                latitude = finalLat
                longitude = finalLng
                altitude = coordinates.altitude
                this.accuracy = accuracy
                speed = coordinates.speedMps
                bearing = coordinates.bearing
                time = timeMs
                this.elapsedRealtimeNanos = elapsedRealtimeNanos

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    verticalAccuracyMeters = 2.0f
                    speedAccuracyMetersPerSecond = 0.5f
                    bearingAccuracyDegrees = 3.0f
                }

                // Add satellite extras
                val extras = Bundle().apply {
                    putInt("satellites", config.simulatedSatellitesCount)
                    putFloat("hdop", 0.9f)
                    putFloat("vdop", 1.1f)
                }
                this.extras = extras
            }

            if (isCurrentlyMocking && isProviderRegistered) {
                locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location)
                try {
                    val netLocation = Location(LocationManager.NETWORK_PROVIDER).apply {
                        set(location)
                        provider = LocationManager.NETWORK_PROVIDER
                    }
                    locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, netLocation)
                } catch (e: Exception) {
                    // Ignore
                }
            }

            val msg = String.format(
                Locale.US,
                "Lat: %.6f, Lng: %.6f | Speed: %.1f km/h | Acc: ±%.1fm",
                finalLat, finalLng, coordinates.speedKmh, accuracy
            )
            emitLog("PROVIDER_INJECT", LogLevel.SUCCESS, msg)

            if (config.generateNmea) {
                val gpgga = GeoUtils.generateNmeaGpgga(finalLat, finalLng, coordinates.altitude, config.simulatedSatellitesCount)
                val gprmc = GeoUtils.generateNmeaGprmc(finalLat, finalLng, coordinates.speedKmh, coordinates.bearing)
                emitLog("NMEA", LogLevel.NMEA, gpgga)
                emitLog("NMEA", LogLevel.NMEA, gprmc)
            }

        } catch (e: SecurityException) {
            emitLog("PROVIDER_WARN", LogLevel.WARNING, "Provider call denied: set as mock app in Developer Options.")
        } catch (e: Exception) {
            emitLog("PROVIDER_ERR", LogLevel.ERROR, "Injection failed: ${e.localizedMessage}")
        }
    }

    private fun emitLog(tag: String, level: LogLevel, message: String) {
        _logEvents.tryEmit(
            TelemetryLog(
                timestamp = System.currentTimeMillis(),
                tag = tag,
                level = level,
                message = message
            )
        )
    }
}
