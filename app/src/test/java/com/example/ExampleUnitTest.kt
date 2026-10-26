package com.example

import com.example.engine.GeoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testHaversineDistance() {
        // San Francisco to San Jose (~67 km)
        val sfLat = 37.7749
        val sfLng = -122.4194
        val sjLat = 37.3382
        val sjLng = -121.8863

        val distanceMeters = GeoUtils.calculateDistanceMeters(sfLat, sfLng, sjLat, sjLng)
        assertTrue("Distance should be roughly 65-72km", distanceMeters in 65000.0..72000.0)
    }

    @Test
    fun testBearingCalculation() {
        // Due North
        val bNorth = GeoUtils.calculateBearing(0.0, 0.0, 1.0, 0.0)
        assertEquals(0.0, bNorth.toDouble(), 1.0)

        // Due East
        val bEast = GeoUtils.calculateBearing(0.0, 0.0, 0.0, 1.0)
        assertEquals(90.0, bEast.toDouble(), 1.0)
    }

    @Test
    fun testNmeaSentenceGeneration() {
        val lat = 37.7749
        val lng = -122.4194
        val gga = GeoUtils.generateNmeaGpgga(lat, lng, 15.0, 10)
        val rmc = GeoUtils.generateNmeaGprmc(lat, lng, 12.0f, 45.0f)

        assertTrue(gga.startsWith("\$GPGGA"))
        assertTrue(gga.contains("*"))
        assertTrue(rmc.startsWith("\$GPRMC"))
        assertTrue(rmc.contains("*"))
    }

    @Test
    fun testCoordinateOffset() {
        val startLat = 37.7749
        val startLng = -122.4194
        val (nextLat, nextLng) = GeoUtils.calculateDestination(startLat, startLng, 100.0, 90f) // 100m East

        assertTrue(nextLat in 37.77..37.78)
        assertTrue(nextLng > startLng)
    }

    @Test
    fun testCopilotActionParsing() {
        val rawResponse = """
            Here is your custom route from Central Park to Times Square!
            
            [ACTION:LOAD_ROUTE|title=Central Park to Times Square|waypoints=40.7850,-73.9682,Central Park;40.7710,-73.9780,Midtown Checkpoint;40.7580,-73.9855,Times Square]
            [ACTION:SET_SPEED|speed=15.0|label=Cycling Pace (15 km/h)]
        """.trimIndent()

        val parsed = com.example.service.GeminiCopilotService.parseResponseToChatMessage(rawResponse)
        assertEquals(2, parsed.actions.size)
        assertTrue(parsed.actions[0] is com.example.service.CopilotAction.LoadRoute)
        val routeAction = parsed.actions[0] as com.example.service.CopilotAction.LoadRoute
        assertEquals("Central Park to Times Square", routeAction.title)
        assertEquals(3, routeAction.waypoints.size)
        assertEquals("Central Park", routeAction.waypoints[0].name)
        assertEquals("Times Square", routeAction.waypoints[2].name)

        val speedAction = parsed.actions[1] as com.example.service.CopilotAction.SetSpeed
        assertEquals(15f, speedAction.speedKmh)
    }
}


