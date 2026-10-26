package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class MockGpsRepository(
    private val database: AppDatabase,
    context: Context
) {
    private val routeDao = database.routeDao()
    private val favoriteDao = database.favoritePlaceDao()
    private val logDao = database.telemetryLogDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("mock_gps_engine_prefs", Context.MODE_PRIVATE)

    fun loadEngineConfig(): MockEngineConfig {
        return MockEngineConfig(
            updateIntervalMs = prefs.getLong("update_interval_ms", 500L),
            jitterEnabled = prefs.getBoolean("jitter_enabled", true),
            jitterRadiusMeters = prefs.getFloat("jitter_radius_meters", 2.5f),
            defaultAltitudeMeters = prefs.getFloat("default_altitude_meters", 18.0f).toDouble(),
            simulatedAccuracyMeters = prefs.getFloat("simulated_accuracy_meters", 2.5f),
            simulatedSatellitesCount = prefs.getInt("simulated_satellites_count", 14),
            generateNmea = prefs.getBoolean("generate_nmea", true),
            overlayJoystickEnabled = prefs.getBoolean("overlay_joystick_enabled", false)
        )
    }

    fun saveEngineConfig(config: MockEngineConfig) {
        prefs.edit().apply {
            putLong("update_interval_ms", config.updateIntervalMs)
            putBoolean("jitter_enabled", config.jitterEnabled)
            putFloat("jitter_radius_meters", config.jitterRadiusMeters)
            putFloat("default_altitude_meters", config.defaultAltitudeMeters.toFloat())
            putFloat("simulated_accuracy_meters", config.simulatedAccuracyMeters)
            putInt("simulated_satellites_count", config.simulatedSatellitesCount)
            putBoolean("generate_nmea", config.generateNmea)
            putBoolean("overlay_joystick_enabled", config.overlayJoystickEnabled)
            apply()
        }
    }

    fun loadMapTouchPanSpeed(): Float {
        return prefs.getFloat("map_touch_pan_speed", 3.0f)
    }

    fun saveMapTouchPanSpeed(speed: Float) {
        prefs.edit().putFloat("map_touch_pan_speed", speed).apply()
    }

    val savedRoutes: Flow<List<RouteItem>> = routeDao.getAllSavedRoutes().map { entities ->
        entities.map { entity ->
            RouteItem(
                id = entity.id,
                title = entity.title,
                description = entity.description,
                distanceKm = entity.distanceKm,
                waypoints = JsonHelper.jsonToWaypoints(entity.waypointsJson),
                isCustom = true
            )
        }
    }

    val favoritePlaces: Flow<List<FavoritePlaceEntity>> = favoriteDao.getAllFavorites()

    val telemetryLogs: Flow<List<TelemetryLog>> = logDao.getRecentLogs().map { entities ->
        entities.map { entity ->
            val levelEnum = try {
                LogLevel.valueOf(entity.level)
            } catch (e: Exception) {
                LogLevel.INFO
            }
            TelemetryLog(
                id = entity.id,
                timestamp = entity.timestamp,
                tag = entity.tag,
                level = levelEnum,
                message = entity.message
            )
        }
    }

    suspend fun saveRoute(title: String, description: String, distanceKm: Double, waypoints: List<Waypoint>) {
        val entity = SavedRouteEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            distanceKm = distanceKm,
            waypointsJson = JsonHelper.waypointsToJson(waypoints)
        )
        routeDao.insertRoute(entity)
    }

    suspend fun deleteRoute(id: String) {
        routeDao.deleteRouteById(id)
    }

    suspend fun saveFavorite(title: String, subtitle: String, lat: Double, lng: Double) {
        val entity = FavoritePlaceEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            subtitle = subtitle,
            latitude = lat,
            longitude = lng
        )
        favoriteDao.insertFavorite(entity)
    }

    suspend fun deleteFavorite(id: String) {
        favoriteDao.deleteFavoriteById(id)
    }

    suspend fun insertLog(log: TelemetryLog) {
        logDao.insertLog(
            TelemetryLogEntity(
                timestamp = log.timestamp,
                tag = log.tag,
                level = log.level.name,
                message = log.message
            )
        )
    }

    suspend fun clearLogs() {
        logDao.clearLogs()
    }

    val crashReports: Flow<List<CrashReportEntity>> = database.crashReportDao().getAllCrashReports()

    suspend fun clearCrashReports() {
        database.crashReportDao().clearCrashReports()
    }
}
