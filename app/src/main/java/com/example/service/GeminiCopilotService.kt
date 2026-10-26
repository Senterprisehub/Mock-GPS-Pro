package com.example.service

import android.util.Log
import com.example.BuildConfig
import com.example.data.CityCatalog
import com.example.data.GeocodingRepository
import com.example.data.RoadRoutingRepository
import com.example.data.SearchPlaceResult
import com.example.data.Waypoint
import com.example.engine.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.math.cos
import kotlin.math.sin

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actions: List<CopilotAction> = emptyList(),
    val isError: Boolean = false
)

sealed class CopilotAction {
    data class SetLocation(val lat: Double, val lng: Double, val name: String) : CopilotAction()
    data class SetSpeed(val speedKmh: Float, val label: String) : CopilotAction()
    data class LoadRoute(val title: String, val waypoints: List<Waypoint>) : CopilotAction()
    object StartMock : CopilotAction()
    object StopMock : CopilotAction()
    object OpenSettings : CopilotAction()
    object OpenRoutes : CopilotAction()
}

object GeminiCopilotService {
    private const val TAG = "GeminiCopilotService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private val API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getSystemInstruction(currentLat: Double, currentLng: Double): String = """
You are "Mock GPS Pro AI Copilot", an expert AI assistant built directly into the Mock GPS Pro Android application.
Current user coordinates: Latitude $currentLat, Longitude $currentLng.

YOUR CAPABILITIES & DOMAIN EXPERTISE:
1. Coordinate Navigation & Teleportation: You know precise GPS coordinates for world landmarks, cities, stadiums, tourist attractions, and addresses worldwide.
2. Route Planning & Waypoints Generation: When the user asks for a route from their current location or between two places, generate a complete multi-waypoint path with realistic geographic coordinates and smooth curves.
3. GPS Mechanics & Telemetry: You understand Android Mock Location Provider architecture, NMEA sentences (${"$" + "GPGGA"}, ${"$" + "GPRMC"}, ${"$" + "GPVTG"}, ${"$" + "GPGSA"}), satellite constellations (GPS, GLONASS, Galileo, BeiDou), HDOP/VDOP accuracy, speed curves, altitude variance, and realistic jitter.
4. Android System Guidance: You guide users on Developer Options setup (Settings > About Phone > Tap Build Number 7 times > Developer Options > Select mock location app > Choose Mock GPS Pro), battery optimization exemptions, and security questions.

IMPORTANT: IN-APP ACTION COMMANDS
Whenever you recommend teleporting to a place, setting a speed, or creating a route, you MUST include structured action tags at the very bottom of your response so the app can display 1-tap execution buttons to the user:
- To set mock coordinates: [ACTION:SET_LOCATION|lat=LATITUDE|lng=LONGITUDE|name=LOCATION_NAME]
- To set speed: [ACTION:SET_SPEED|speed=SPEED_IN_KMH|label=SPEED_LABEL]
- To generate and load a route: [ACTION:LOAD_ROUTE|title=ROUTE_TITLE|waypoints=lat1,lng1,Name1;lat2,lng2,Name2;lat3,lng3,Name3]
- To start mocking: [ACTION:START_MOCK]
- To stop mocking: [ACTION:STOP_MOCK]
- To open settings: [ACTION:OPEN_SETTINGS]
- To open routes: [ACTION:OPEN_ROUTES]

Example response format:
"Here is a scenic route from your current location to Tokyo Tower! It spans 6 waypoints with smooth turns.

[ACTION:LOAD_ROUTE|title=Current Location to Tokyo Tower|waypoints=35.6586,139.7454,Current Location;35.6580,139.7480,Checkpoint 1;35.6585,139.7513,Tokyo Tower]
[ACTION:SET_SPEED|speed=40.0|label=City Driving (40 km/h)]"

Keep responses friendly, helpful, concise, and nicely formatted with bullet points and bold titles.
"""

    suspend fun sendMessage(
        history: List<ChatMessage>,
        userMessage: String,
        currentLat: Double = 37.7749,
        currentLng: Double = -122.4194
    ): ChatMessage = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        // If no API key is provided or offline, use the comprehensive built-in zero-key AI engine
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER")) {
            val intelligentReply = processWithLocalCopilotEngine(userMessage, currentLat, currentLng)
            return@withContext parseResponseToChatMessage(intelligentReply)
        }

        try {
            val jsonBody = JSONObject().apply {
                // System Instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", getSystemInstruction(currentLat, currentLng)))
                    })
                })

                // Contents conversation history
                val contentsArray = JSONArray()

                val recentHistory = history.takeLast(6)
                for (msg in recentHistory) {
                    contentsArray.put(JSONObject().apply {
                        put("role", if (msg.role == "user") "user" else "model")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", msg.content))
                        })
                    })
                }

                // Current user message
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", userMessage))
                    })
                })

                put("contents", contentsArray)

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
                    put("topK", 40)
                })
            }

            val request = Request.Builder()
                .url("$API_ENDPOINT?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini API returned ${response.code}, falling back to built-in smart engine")
                val fallback = processWithLocalCopilotEngine(userMessage, currentLat, currentLng)
                return@withContext parseResponseToChatMessage(fallback)
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val contentObj = candidate.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val rawText = parts.getJSONObject(0).optString("text", "")
                    return@withContext parseResponseToChatMessage(rawText)
                }
            }

            val fallback = processWithLocalCopilotEngine(userMessage, currentLat, currentLng)
            return@withContext parseResponseToChatMessage(fallback)
        } catch (e: Exception) {
            Log.w(TAG, "Exception contacting Gemini cloud, serving from on-device engine: ${e.message}")
            val fallback = processWithLocalCopilotEngine(userMessage, currentLat, currentLng)
            return@withContext parseResponseToChatMessage(fallback)
        }
    }

    fun parseResponseToChatMessage(rawText: String): ChatMessage {
        val actions = mutableListOf<CopilotAction>()
        val cleanLines = mutableListOf<String>()

        val lines = rawText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[ACTION:") && trimmed.endsWith("]")) {
                val actionContent = trimmed.removePrefix("[ACTION:").removeSuffix("]")
                val parsedAction = parseActionTag(actionContent)
                if (parsedAction != null) {
                    actions.add(parsedAction)
                }
            } else {
                cleanLines.add(line)
            }
        }

        return ChatMessage(
            role = "model",
            content = cleanLines.joinToString("\n").trim(),
            actions = actions
        )
    }

    private fun parseActionTag(actionStr: String): CopilotAction? {
        try {
            val parts = actionStr.split("|")
            val type = parts.firstOrNull() ?: return null
            val params = mutableMapOf<String, String>()

            for (i in 1 until parts.size) {
                val kv = parts[i].split("=", limit = 2)
                if (kv.size == 2) {
                    params[kv[0].trim()] = kv[1].trim()
                }
            }

            return when (type) {
                "SET_LOCATION" -> {
                    val lat = params["lat"]?.toDoubleOrNull() ?: return null
                    val lng = params["lng"]?.toDoubleOrNull() ?: return null
                    val name = params["name"] ?: "Selected Location"
                    CopilotAction.SetLocation(lat, lng, name)
                }
                "SET_SPEED" -> {
                    val speed = params["speed"]?.toFloatOrNull() ?: 25f
                    val label = params["label"] ?: "${speed.toInt()} km/h"
                    CopilotAction.SetSpeed(speed, label)
                }
                "LOAD_ROUTE" -> {
                    val title = params["title"] ?: "AI Generated Route"
                    val waypointsStr = params["waypoints"] ?: return null
                    val waypointsList = mutableListOf<Waypoint>()
                    val wpEntries = waypointsStr.split(";")
                    for ((idx, entry) in wpEntries.withIndex()) {
                        val tokens = entry.split(",")
                        if (tokens.size >= 2) {
                            val wLat = tokens[0].toDoubleOrNull() ?: continue
                            val wLng = tokens[1].toDoubleOrNull() ?: continue
                            val wName = if (tokens.size >= 3) tokens[2] else "Point ${idx + 1}"
                            waypointsList.add(Waypoint(wLat, wLng, wName, idx))
                        }
                    }
                    if (waypointsList.isNotEmpty()) {
                        CopilotAction.LoadRoute(title, waypointsList)
                    } else null
                }
                "START_MOCK" -> CopilotAction.StartMock
                "STOP_MOCK" -> CopilotAction.StopMock
                "OPEN_SETTINGS" -> CopilotAction.OpenSettings
                "OPEN_ROUTES" -> CopilotAction.OpenRoutes
                else -> null
            }
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Complete zero-API-key, 100% free forever on-device Intelligence Engine.
     * Searches OpenStreetMap Nominatim live, resolves Origin and Destination points,
     * synthesizes realistic multi-waypoint road/walking curves, and produces 1-tap executable action cards.
     */
    suspend fun processWithLocalCopilotEngine(
        prompt: String,
        currentLat: Double,
        currentLng: Double
    ): String = withContext(Dispatchers.IO) {
        val lower = prompt.lowercase().trim()

        // 1. Explicit Coordinates in prompt (e.g. "35.6895, 139.6917")
        val coordPattern = Pattern.compile("([-+]?\\d{1,2}\\.\\d+)[,\\s]+([-+]?\\d{1,3}\\.\\d+)")
        val matcher = coordPattern.matcher(prompt)
        if (matcher.find() && !lower.contains("route") && !lower.contains("from") && !lower.contains("to")) {
            val lat = matcher.group(1)?.toDoubleOrNull()
            val lng = matcher.group(2)?.toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                return@withContext "📍 **Coordinates Identified**\n\n" +
                        "Target Coordinates:\n" +
                        "• **Latitude**: ${String.format(Locale.US, "%.5f", lat)}°\n" +
                        "• **Longitude**: ${String.format(Locale.US, "%.5f", lng)}°\n\n" +
                        "Tap **Teleport** below to immediately place your mock location pin here.\n\n" +
                        "[ACTION:SET_LOCATION|lat=$lat|lng=$lng|name=Coordinates: ${String.format(Locale.US, "%.4f, %.4f", lat, lng)}]"
            }
        }

        // 2. Start / Stop Mock Location Intent
        if (lower.startsWith("start") && (lower.contains("mock") || lower.contains("gps") || lower.contains("simulat") || lower.contains("run") || lower.contains("engine"))) {
            return@withContext "▶️ **Starting Mock GPS Service**\n\n" +
                    "Injecting simulated GPS coordinates and real-time NMEA satellite telemetry into Android LocationManager.\n\n" +
                    "[ACTION:START_MOCK]"
        }

        if (lower.startsWith("stop") && (lower.contains("mock") || lower.contains("gps") || lower.contains("simulat") || lower.contains("pause") || lower.contains("halt") || lower.contains("engine"))) {
            return@withContext "⏹️ **Stopping Mock GPS Service**\n\n" +
                    "Halting simulated GPS broadcasts. Android OS will resume using physical device hardware GPS.\n\n" +
                    "[ACTION:STOP_MOCK]"
        }

        // 3. Waypoint Route Generation & Route Finding Intent
        val isRouteIntent = lower.contains("route") || lower.contains("waypoint") ||
                lower.contains("path") || lower.contains("walk") || lower.contains("tour") ||
                lower.contains("trail") || lower.contains("drive from") || lower.contains("from ") ||
                lower.contains("directions") || lower.contains("navigate") || lower.contains("between")

        if (isRouteIntent) {
            val routeResponse = generateIntelligentWaypointRoute(prompt, currentLat, currentLng)
            if (routeResponse != null) {
                return@withContext routeResponse
            }
        }

        // 4. Speed Setting Intent
        val speedNumMatch = Pattern.compile("(\\d+)\\s*(?:km/?h|kmh|kph|speed)").matcher(lower)
        if (speedNumMatch.find()) {
            val sVal = speedNumMatch.group(1)?.toFloatOrNull() ?: 25f
            val clamped = sVal.coerceIn(1f, 300f)
            return@withContext "🚗 **Simulation Speed Adjusted**\n\n" +
                    "Setting target simulation velocity to **${clamped.toInt()} km/h** (${String.format(Locale.US, "%.1f", clamped / 3.6f)} m/s).\n\n" +
                    "[ACTION:SET_SPEED|speed=$clamped|label=${clamped.toInt()} km/h]"
        }

        if (lower.contains("speed") || lower.contains("walking speed") || lower.contains("driving speed") || lower.contains("highway speed")) {
            val speed = when {
                lower.contains("walk") -> 5f
                lower.contains("run") || lower.contains("jog") -> 10f
                lower.contains("bike") || lower.contains("cycl") -> 20f
                lower.contains("highway") || lower.contains("fast") -> 100f
                else -> 50f
            }
            val label = when {
                lower.contains("walk") -> "Walking Pace (5 km/h)"
                lower.contains("run") || lower.contains("jog") -> "Jogging Pace (10 km/h)"
                lower.contains("bike") || lower.contains("cycl") -> "Cycling (20 km/h)"
                lower.contains("highway") || lower.contains("fast") -> "Highway Speed (100 km/h)"
                else -> "City Driving (50 km/h)"
            }
            return@withContext "🚗 **Simulation Velocity Preset**\n\n" +
                    "Selected profile: **$label**.\n" +
                    "• **Velocity**: ${speed.toInt()} km/h (${String.format(Locale.US, "%.1f", speed / 3.6f)} m/s)\n\n" +
                    "[ACTION:SET_SPEED|speed=$speed|label=$label]"
        }

        // 5. Landmark, Place, Address, or City Geocoding Search
        val cleanSearchQuery = prompt
            .replace("teleport to", "", ignoreCase = true)
            .replace("take me to", "", ignoreCase = true)
            .replace("go to", "", ignoreCase = true)
            .replace("find", "", ignoreCase = true)
            .replace("search for", "", ignoreCase = true)
            .replace("show me", "", ignoreCase = true)
            .replace("where is", "", ignoreCase = true)
            .replace("pin", "", ignoreCase = true)
            .trim()

        if (cleanSearchQuery.length >= 3) {
            val foundPlace = GeocodingRepository.findBestLocation(cleanSearchQuery, currentLat, currentLng)
            if (foundPlace != null) {
                val dist = if (!currentLat.isNaN()) GeoUtils.calculateDistanceMeters(currentLat, currentLng, foundPlace.latitude, foundPlace.longitude) / 1000.0 else 0.0
                return@withContext "📍 **${foundPlace.category.icon} ${foundPlace.title}**\n\n" +
                        "Located in: ${foundPlace.subtitle}\n\n" +
                        "• **Coordinates**: ${String.format(Locale.US, "%.5f, %.5f", foundPlace.latitude, foundPlace.longitude)}\n" +
                        (if (dist > 0.1) "• **Distance from here**: ${String.format(Locale.US, "%.1f km", dist)}\n\n" else "\n") +
                        "Tap **Teleport** below to instantly drop your mock GPS target pin at this location!\n\n" +
                        "[ACTION:SET_LOCATION|lat=${foundPlace.latitude}|lng=${foundPlace.longitude}|name=${foundPlace.title}]"
            }
        }

        // 6. Navigation / Screen shortcuts
        if (lower.contains("setting") || lower.contains("permission") || lower.contains("developer") || lower.contains("setup")) {
            return@withContext "⚙️ **Developer Settings & System Configuration**\n\n" +
                    "To enable mock location on Android:\n" +
                    "1. Open **Android Settings** > **About Phone**\n" +
                    "2. Tap **Build Number** 7 times\n" +
                    "3. Open **Developer Options** > **Select mock location app** > Choose **Mock GPS Pro**\n\n" +
                    "[ACTION:OPEN_SETTINGS]"
        }

        if (lower.contains("route library") || lower.contains("saved routes") || lower.contains("routes tab") || lower.contains("my routes")) {
            return@withContext "🗺️ **Saved Routes Library**\n\n" +
                    "Opening the Routes tab where you can browse preloaded world city routes or inspect all your custom waypoint tracks.\n\n" +
                    "[ACTION:OPEN_ROUTES]"
        }

        // 7. Security / Mock Detection Q&A
        if (lower.contains("untraceable") || lower.contains("detect") || lower.contains("hide") || lower.contains("safe") || lower.contains("fake")) {
            return@withContext "🛡️ **Location Realism & OS Telemetry Architecture**\n\n" +
                    "Mock GPS Pro produces authentic hardware-grade GPS signals:\n\n" +
                    "• **NMEA 0183 Telemetry**: Streams genuine \$GPGGA, \$GPRMC, \$GPVTG, and \$GPGSA sentences.\n" +
                    "• **Active Constellations**: Injects 14+ simulated GPS, GLONASS, and Galileo satellites with dynamic Signal-to-Noise Ratios (SNR).\n" +
                    "• **HDOP / VDOP Variance**: Micro-dilution of precision for true satellite realism.\n" +
                    "• **Natural Jitter**: 1–2 meter drift simulation replicating physical GPS hardware.\n\n" +
                    "*Note: On standard non-rooted Android, the OS flags mock locations with `isFromMockProvider()`. Completely masking this flag requires root framework hooks (e.g. LSPosed). Our app maximizes signal realism to minimize behavioral detection.*"
        }

        // 8. General Greeting & Capabilities Guide
        val currentCity = CityCatalog.getNearestArea(currentLat, currentLng).name
        return@withContext "👋 **Hello! I am your Mock GPS AI Copilot**\n\n" +
                "I can find any location on earth and generate custom waypoint routes for you:\n\n" +
                "• **Create Waypoint Routes**: *\"Make a waypoint route from current location to Tokyo Tower\"* or *\"Route from Central Park to Times Square\"*\n" +
                "• **Teleport to Places**: *\"Take me to Shibuya Crossing, Tokyo\"* or *\"Find Mayo Clinic\"*\n" +
                "• **Speed Control**: *\"Set speed to 45 km/h for city driving\"*\n" +
                "• **Mock Controls**: *\"Start mock location\"* or *\"Stop mock location\"*\n\n" +
                "Current position: **$currentCity** (${String.format(Locale.US, "%.4f, %.4f", currentLat, currentLng)})\n\n" +
                "[ACTION:LOAD_ROUTE|title=$currentCity Scenic Tour|waypoints=${currentLat},${currentLng},Start Point;${currentLat + 0.006},${currentLng + 0.005},Checkpoint 1;${currentLat + 0.003},${currentLng + 0.010},Checkpoint 2;${currentLat - 0.004},${currentLng + 0.007},Checkpoint 3;${currentLat},${currentLng},Return Loop]\n" +
                "[ACTION:SET_SPEED|speed=15.0|label=Cycling (15 km/h)]"
    }

    /**
     * Resolves Origin & Destination, performs geocoding, generates smooth intermediate waypoints,
     * calculates distance, selects optimal simulation velocity, and prepares the route for saving.
     */
    private suspend fun generateIntelligentWaypointRoute(
        prompt: String,
        currentLat: Double,
        currentLng: Double
    ): String? {
        val lower = prompt.lowercase()

        var originQuery = ""
        var destQuery = ""

        // Pattern 1: "from X to Y" or "from X until Y"
        val fromToPattern = Pattern.compile("from\\s+(.+?)\\s+(?:to|towards|until|ending at)\\s+(.+)", Pattern.CASE_INSENSITIVE)
        val fromToMatcher = fromToPattern.matcher(prompt)

        // Pattern 2: "between X and Y"
        val betweenPattern = Pattern.compile("between\\s+(.+?)\\s+and\\s+(.+)", Pattern.CASE_INSENSITIVE)
        val betweenMatcher = betweenPattern.matcher(prompt)

        // Pattern 3: "to Y" or "route to Y" or "waypoint to Y"
        val toPattern = Pattern.compile("(?:route\\s+to|waypoint\\s+route\\s+to|directions\\s+to|navigate\\s+to|go\\s+to)\\s+(.+)", Pattern.CASE_INSENSITIVE)
        val toMatcher = toPattern.matcher(prompt)

        when {
            fromToMatcher.find() -> {
                originQuery = fromToMatcher.group(1)?.trim() ?: ""
                destQuery = fromToMatcher.group(2)?.trim() ?: ""
            }
            betweenMatcher.find() -> {
                originQuery = betweenMatcher.group(1)?.trim() ?: ""
                destQuery = betweenMatcher.group(2)?.trim() ?: ""
            }
            toMatcher.find() -> {
                originQuery = "current location"
                destQuery = toMatcher.group(1)?.trim() ?: ""
            }
            else -> {
                // Check if user named a specific city or landmark to tour around
                val clean = prompt
                    .replace("make a waypoint route", "", ignoreCase = true)
                    .replace("create a route", "", ignoreCase = true)
                    .replace("make a route", "", ignoreCase = true)
                    .replace("generate a route", "", ignoreCase = true)
                    .replace("route in", "", ignoreCase = true)
                    .replace("around", "", ignoreCase = true)
                    .replace("tour of", "", ignoreCase = true)
                    .trim()
                if (clean.length >= 3) {
                    destQuery = clean
                    originQuery = clean
                }
            }
        }

        // Clean up common sentence fillers
        destQuery = destQuery.replace(Regex("(?i)\\b(please|for me|now|in the map|in map|on the map)\\b"), "").trim()
        originQuery = originQuery.replace(Regex("(?i)\\b(please|for me|now|in the map|in map|on the map)\\b"), "").trim()

        if (destQuery.isEmpty() && originQuery.isEmpty()) {
            return null
        }

        // Resolve Origin
        val originPlace = if (originQuery.isEmpty() || originQuery.contains("current") || originQuery.contains("here") || originQuery.contains("my location")) {
            val area = CityCatalog.getNearestArea(currentLat, currentLng).name
            SearchPlaceResult("Current Location ($area)", "Active Mock Target", currentLat, currentLng, com.example.data.PlaceCategory.GENERAL)
        } else {
            GeocodingRepository.findBestLocation(originQuery, currentLat, currentLng)
        }

        // Resolve Destination
        val destPlace = if (destQuery.isEmpty() || destQuery == originQuery) {
            originPlace
        } else {
            GeocodingRepository.findBestLocation(destQuery, originPlace?.latitude ?: currentLat, originPlace?.longitude ?: currentLng)
        }

        if (originPlace == null && destPlace == null) {
            return null
        }

        val start = originPlace ?: destPlace!!
        val end = destPlace ?: originPlace!!

        // Query real-world road network navigation from OpenStreetMap OSRM
        val roadResult = RoadRoutingRepository.calculateRoadNavigationRoute(
            originLat = start.latitude,
            originLng = start.longitude,
            destLat = end.latitude,
            destLng = end.longitude,
            originName = start.title,
            destName = end.title
        )

        val routeTitle = "${start.title.take(20)} to ${end.title.take(20)}"
        val waypoints = roadResult.waypoints

        // Determine recommended speed based on road distance
        val speedKmh: Float
        val speedLabel: String
        when {
            lower.contains("walk") || lower.contains("foot") || lower.contains("jog") -> {
                speedKmh = 5f
                speedLabel = "Walking Pace (5 km/h)"
            }
            lower.contains("bike") || lower.contains("cycle") -> {
                speedKmh = 18f
                speedLabel = "Bicycle (18 km/h)"
            }
            roadResult.distanceKm > 40.0 -> {
                speedKmh = 90f
                speedLabel = "Highway Speed (90 km/h)"
            }
            roadResult.distanceKm > 10.0 -> {
                speedKmh = 60f
                speedLabel = "Arterial Road (60 km/h)"
            }
            else -> {
                speedKmh = 35f
                speedLabel = "City Street Driving (35 km/h)"
            }
        }

        // Construct waypoints string for ACTION tag
        // Filter down to at most 60 high-fidelity key road vertices to keep the tag clean while preserving curve fidelity
        val sampledWaypoints = if (waypoints.size > 60) {
            val step = (waypoints.size / 60.0).coerceAtLeast(1.0)
            val filtered = mutableListOf<Waypoint>()
            filtered.add(waypoints.first())
            var nextIndex = step
            while (nextIndex < waypoints.size - 1) {
                filtered.add(waypoints[nextIndex.toInt()])
                nextIndex += step
            }
            filtered.add(waypoints.last())
            filtered.mapIndexed { idx, wp -> wp.copy(index = idx) }
        } else {
            waypoints
        }

        val waypointsStr = sampledWaypoints.joinToString(";") { wp ->
            "${String.format(Locale.US, "%.6f", wp.latitude)},${String.format(Locale.US, "%.6f", wp.longitude)},${wp.name.replace(";", "").replace("|", "").replace(",", " ")}"
        }

        val stepsSummary = if (roadResult.steps.isNotEmpty()) {
            val previewSteps = roadResult.steps.take(4).mapIndexed { i, step ->
                "${i + 1}. ${step.instruction}"
            }.joinToString("\n")
            "\n\n🛣️ **Turn-by-Turn Navigation:**\n$previewSteps"
        } else ""

        return "🧭 **Real Road Navigation Route Snapped to Map**\n\n" +
                "I have calculated the driving route following the actual streets and roads:\n\n" +
                "• **From**: ${start.title}\n" +
                "• **To**: ${end.title}\n" +
                "• **Corridor**: ${roadResult.summaryRoads}\n" +
                "• **Road Distance**: ${roadResult.distanceKm} km\n" +
                "• **Est. Duration**: ${roadResult.durationMinutes} mins\n" +
                "• **Waypoints**: ${sampledWaypoints.size} road checkpoints\n" +
                "• **Recommended Speed**: $speedLabel" +
                stepsSummary + "\n\n" +
                "✨ **Ready to Navigate**: Tap **Load Route on Map** to display the Google Maps-style navigation polyline and start moving along actual roads.\n\n" +
                "[ACTION:LOAD_ROUTE|title=$routeTitle|waypoints=$waypointsStr]\n" +
                "[ACTION:SET_SPEED|speed=$speedKmh|label=$speedLabel]"
    }
}
