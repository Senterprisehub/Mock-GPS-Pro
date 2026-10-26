package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

data class MockCoordinates(
    val latitude: Double = 37.774900,
    val longitude: Double = -122.419400,
    val altitude: Double = 15.0,
    val accuracy: Float = 2.5f,
    val speedKmh: Float = 25f,
    val bearing: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    val speedMps: Float
        get() = speedKmh / 3.6f
}

data class Waypoint(
    val latitude: Double,
    val longitude: Double,
    val name: String = "",
    val index: Int = 0
)

data class RouteItem(
    val id: String,
    val title: String,
    val description: String,
    val distanceKm: Double,
    val waypoints: List<Waypoint>,
    val isCustom: Boolean = false
)

data class MockEngineConfig(
    val jitterEnabled: Boolean = true,
    val jitterRadiusMeters: Float = 2.5f,
    val updateIntervalMs: Long = 500L,
    val defaultAltitudeMeters: Double = 18.0,
    val simulatedAccuracyMeters: Float = 2.5f,
    val simulatedSatellitesCount: Int = 14,
    val generateNmea: Boolean = true,
    val overlayJoystickEnabled: Boolean = false
)

enum class LogLevel {
    INFO, SUCCESS, WARNING, ERROR, NMEA
}

data class TelemetryLog(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val level: LogLevel = LogLevel.INFO,
    val message: String
)

data class PermissionsState(
    val fineLocationGranted: Boolean = false,
    val bgLocationGranted: Boolean = false,
    val overlayPermissionGranted: Boolean = false,
    val fgsPermissionGranted: Boolean = true,
    val batteryOptimizationIgnored: Boolean = false,
    val mockLocationAppSelected: Boolean = false,
    val notificationPermissionGranted: Boolean = true
)

@Entity(tableName = "saved_routes")
data class SavedRouteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val distanceKm: Double,
    val waypointsJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_places")
data class FavoritePlaceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "telemetry_logs")
data class TelemetryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val tag: String,
    val level: String,
    val message: String
)

@Entity(tableName = "crash_reports")
data class CrashReportEntity(
    @PrimaryKey val id: String,
    val timestamp: String,
    val errorType: String,
    val message: String,
    val stackTrace: String,
    val lastAction: String,
    val lastCoordinates: String,
    val memoryUsageMb: String,
    val androidVersion: String,
    val deviceModel: String,
    val createdAt: Long = System.currentTimeMillis()
)

object JsonHelper {
    fun waypointsToJson(list: List<Waypoint>): String {
        return try {
            val jsonArray = org.json.JSONArray()
            for (wp in list) {
                val obj = org.json.JSONObject().apply {
                    put("latitude", wp.latitude)
                    put("longitude", wp.longitude)
                    put("name", wp.name)
                    put("index", wp.index)
                }
                jsonArray.put(obj)
            }
            jsonArray.toString()
        } catch (e: Exception) {
            "[]"
        }
    }

    fun jsonToWaypoints(json: String): List<Waypoint> {
        if (json.isBlank()) return emptyList()
        return try {
            val jsonArray = org.json.JSONArray(json)
            val list = ArrayList<Waypoint>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Waypoint(
                        latitude = obj.optDouble("latitude", 0.0),
                        longitude = obj.optDouble("longitude", 0.0),
                        name = obj.optString("name", "Waypoint ${i + 1}"),
                        index = obj.optInt("index", i)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
