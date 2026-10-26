package com.example.engine

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object GeoUtils {
    const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates distance in meters between two lat/lng points using Haversine formula
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1.isNaN() || lon1.isNaN() || lat2.isNaN() || lon2.isNaN()) return 0.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = (sin(dLat / 2) * sin(dLat / 2) +
                cos(rLat1) * cos(rLat2) * sin(dLon / 2) * sin(dLon / 2)).coerceIn(0.0, 1.0)
        val c = 2 * atan2(sqrt(a), sqrt(1.0 - a))
        val res = EARTH_RADIUS_METERS * c
        return if (res.isNaN()) 0.0 else res
    }

    /**
     * Calculates initial bearing from point 1 to point 2 in degrees (0..360)
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)

        val y = sin(dLon) * cos(rLat2)
        val x = cos(rLat1) * sin(rLat2) - sin(rLat1) * cos(rLat2) * cos(dLon)
        val initialBearing = Math.toDegrees(atan2(y, x))
        return ((initialBearing + 360) % 360).toFloat()
    }

    /**
     * Standard Linear Interpolation (LERP) between two scalar values
     */
    fun interpolateLinear(start: Double, end: Double, fraction: Double): Double {
        val f = fraction.coerceIn(0.0, 1.0)
        return start + (end - start) * f
    }

    /**
     * High-Precision Geodetic Linear Interpolation between two lat/lon coordinates.
     * Accurately handles shortest-path angular delta for longitude across the international date line (-180°/+180°).
     * Eliminates map jumping, coordinate jitter, and abrupt waypoint snapping.
     */
    fun interpolateCoordinates(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
        fraction: Double
    ): Pair<Double, Double> {
        val f = fraction.coerceIn(0.0, 1.0)
        val lat = lat1 + (lat2 - lat1) * f

        // Shortest angular distance across anti-meridian (-180 / +180)
        var dLon = lon2 - lon1
        if (dLon > 180.0) dLon -= 360.0
        if (dLon < -180.0) dLon += 360.0

        val lon = lon1 + dLon * f
        // Normalize longitude into standard [-180, 180] range
        val normalizedLon = ((lon + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
        val clampedLat = lat.coerceIn(-85.0511, 85.0511)

        return Pair(clampedLat, normalizedLon)
    }

    /**
     * Smooth Shortest-Angle Angular Interpolation between two compass bearings (in degrees 0..360).
     * Prevents the direction compass/marker from spinning wildly when passing through North (0°/360°).
     */
    fun interpolateBearing(bearing1: Float, bearing2: Float, fraction: Float): Float {
        val f = fraction.coerceIn(0f, 1f)
        var diff = (bearing2 - bearing1) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        val res = (bearing1 + diff * f) % 360f
        return if (res < 0f) res + 360f else res
    }

    /**
     * Computes the destination point given start point, distance in meters, and bearing in degrees
     */
    fun calculateDestination(lat: Double, lon: Double, distanceMeters: Double, bearingDegrees: Float): Pair<Double, Double> {
        val distRatio = distanceMeters / EARTH_RADIUS_METERS
        val rLat = Math.toRadians(lat)
        val rLon = Math.toRadians(lon)
        val rBearing = Math.toRadians(bearingDegrees.toDouble())

        val destLat = Math.asin(
            sin(rLat) * cos(distRatio) + cos(rLat) * sin(distRatio) * cos(rBearing)
        )
        val destLon = rLon + atan2(
            sin(rBearing) * sin(distRatio) * cos(rLat),
            cos(distRatio) - sin(rLat) * sin(destLat)
        )

        return Pair(Math.toDegrees(destLat), Math.toDegrees(destLon))
    }

    /**
     * Computes Gaussian 2D jitter offset in meters
     */
    fun generateJitter(radiusMeters: Float): Pair<Double, Double> {
        if (radiusMeters <= 0.1f) return Pair(0.0, 0.0)
        // Box-Muller transform for 2D Gaussian distribution
        val u1 = Random.nextDouble().coerceAtLeast(0.0001)
        val u2 = Random.nextDouble()
        val r = radiusMeters * sqrt(-2.0 * kotlin.math.ln(u1)) * 0.4
        val theta = 2.0 * Math.PI * u2

        val dX = r * cos(theta) // meters east
        val dY = r * sin(theta) // meters north

        val dLat = dY / EARTH_RADIUS_METERS * (180.0 / Math.PI)
        val dLon = dX / (EARTH_RADIUS_METERS * cos(Math.toRadians(37.0))) * (180.0 / Math.PI)
        return Pair(dLat, dLon)
    }

    /**
     * Generates standard NMEA GPGGA sentence with calculated checksum
     */
    fun generateNmeaGpgga(lat: Double, lon: Double, alt: Double, satellites: Int = 12): String {
        val timeFormat = SimpleDateFormat("HHmmss.SS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val utcTime = timeFormat.format(Date())

        val latDeg = lat.toInt().let { kotlin.math.abs(it) }
        val latMin = (kotlin.math.abs(lat) - latDeg) * 60.0
        val latDir = if (lat >= 0) "N" else "S"
        val latStr = String.format(Locale.US, "%02d%07.4f,%s", latDeg, latMin, latDir)

        val lonDeg = lon.toInt().let { kotlin.math.abs(it) }
        val lonMin = (kotlin.math.abs(lon) - lonDeg) * 60.0
        val lonDir = if (lon >= 0) "E" else "W"
        val lonStr = String.format(Locale.US, "%03d%07.4f,%s", lonDeg, lonMin, lonDir)

        val sentenceWithoutChecksum = String.format(
            Locale.US,
            "GPGGA,%s,%s,%s,1,%02d,1.0,%.1f,M,0.0,M,,",
            utcTime, latStr, lonStr, satellites, alt
        )

        val checksum = calculateNmeaChecksum(sentenceWithoutChecksum)
        return "$$sentenceWithoutChecksum*$checksum"
    }

    /**
     * Generates standard NMEA GPRMC sentence
     */
    fun generateNmeaGprmc(lat: Double, lon: Double, speedKmh: Float, bearing: Float): String {
        val timeFormat = SimpleDateFormat("HHmmss.SS", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val dateFormat = SimpleDateFormat("ddMMyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val now = Date()
        val utcTime = timeFormat.format(now)
        val utcDate = dateFormat.format(now)

        val speedKnots = speedKmh * 0.539957f

        val latDeg = lat.toInt().let { kotlin.math.abs(it) }
        val latMin = (kotlin.math.abs(lat) - latDeg) * 60.0
        val latDir = if (lat >= 0) "N" else "S"
        val latStr = String.format(Locale.US, "%02d%07.4f,%s", latDeg, latMin, latDir)

        val lonDeg = lon.toInt().let { kotlin.math.abs(it) }
        val lonMin = (kotlin.math.abs(lon) - lonDeg) * 60.0
        val lonDir = if (lon >= 0) "E" else "W"
        val lonStr = String.format(Locale.US, "%03d%07.4f,%s", lonDeg, lonMin, lonDir)

        val sentenceWithoutChecksum = String.format(
            Locale.US,
            "GPRMC,%s,A,%s,%s,%.2f,%.2f,%s,,,A",
            utcTime, latStr, lonStr, speedKnots, bearing, utcDate
        )

        val checksum = calculateNmeaChecksum(sentenceWithoutChecksum)
        return "$$sentenceWithoutChecksum*$checksum"
    }

    private fun calculateNmeaChecksum(sentence: String): String {
        var checksum = 0
        for (c in sentence) {
            checksum = checksum xor c.code
        }
        return String.format(Locale.US, "%02X", checksum)
    }
}
