package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CrashReportEntity
import com.example.data.CrashTracker
import com.example.data.FavoritePlaceEntity
import com.example.data.GeocodingRepository
import com.example.data.LogLevel
import com.example.data.MockCoordinates
import com.example.data.MockEngineConfig
import com.example.data.MockGpsRepository
import com.example.data.PermissionsState
import com.example.data.RoadRoutingRepository
import com.example.data.RouteItem
import com.example.data.SearchPlaceResult
import com.example.data.TelemetryLog
import com.example.data.Waypoint
import com.example.engine.GeoUtils
import com.example.engine.MockLocationManager
import com.example.service.FloatingJoystickService
import com.example.service.MockLocationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

import com.example.service.ChatMessage
import com.example.service.CopilotAction
import com.example.service.GeminiCopilotService

enum class AppTab {
    MAP, SETTINGS, ROUTES, LOGS, AI_COPILOT
}

data class PresetLocation(
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double
)

class MockGpsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = MockGpsRepository(database, application)
    val locationManager = MockLocationManager(application)

    // Current State
    private val _currentTab = MutableStateFlow(AppTab.MAP)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _coordinates = MutableStateFlow(MockCoordinates())
    val coordinates: StateFlow<MockCoordinates> = _coordinates.asStateFlow()

    // Real Hardware GPS Coordinates
    private val _realCoordinates = MutableStateFlow<MockCoordinates?>(null)
    val realCoordinates: StateFlow<MockCoordinates?> = _realCoordinates.asStateFlow()

    private val _isMocking = MutableStateFlow(false)
    val isMocking: StateFlow<Boolean> = _isMocking.asStateFlow()

    private val _engineConfig = MutableStateFlow(repository.loadEngineConfig())
    val engineConfig: StateFlow<MockEngineConfig> = _engineConfig.asStateFlow()

    private val _permissionsState = MutableStateFlow(locationManager.checkPermissionsState())
    val permissionsState: StateFlow<PermissionsState> = _permissionsState.asStateFlow()

    // Live Search Results & Reverse Geocoding
    private val _searchResults = MutableStateFlow<List<SearchPlaceResult>>(emptyList())
    val searchResults: StateFlow<List<SearchPlaceResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _currentAreaName = MutableStateFlow("Detecting area...")
    val currentAreaName: StateFlow<String> = _currentAreaName.asStateFlow()

    // Map Touch Movement / Drag Speed Multiplier (Controls how fast map pans when swiping with finger)
    private val _mapTouchPanSpeed = MutableStateFlow(repository.loadMapTouchPanSpeed())
    val mapTouchPanSpeed: StateFlow<Float> = _mapTouchPanSpeed.asStateFlow()

    private var searchDebounceJob: Job? = null
    private var reverseGeocodeJob: Job? = null

    // Live Telemetry Logs in memory and Room
    private val _inMemoryLogs = MutableStateFlow<List<TelemetryLog>>(listOf(
        TelemetryLog(
            tag = "SYSTEM_INIT",
            level = LogLevel.SUCCESS,
            message = "MockGPS Pro Provider Engine initialized."
        ),
        TelemetryLog(
            tag = "PROVIDER_INFO",
            level = LogLevel.INFO,
            message = "GPS_PROVIDER & NETWORK_PROVIDER mock injection channels ready."
        )
    ))
    val inMemoryLogs: StateFlow<List<TelemetryLog>> = _inMemoryLogs.asStateFlow()

    val savedRoutes: StateFlow<List<RouteItem>> = repository.savedRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritePlaces: StateFlow<List<FavoritePlaceEntity>> = repository.favoritePlaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val crashReports: StateFlow<List<CrashReportEntity>> = repository.crashReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Route Replay State
    private val _activeRoute = MutableStateFlow<RouteItem?>(null)
    val activeRoute: StateFlow<RouteItem?> = _activeRoute.asStateFlow()

    private val _isRoutePlaying = MutableStateFlow(false)
    val isRoutePlaying: StateFlow<Boolean> = _isRoutePlaying.asStateFlow()

    private val _routeProgress = MutableStateFlow(0f)
    val routeProgress: StateFlow<Float> = _routeProgress.asStateFlow()

    private val _routeSpeedMultiplier = MutableStateFlow(1f)
    val routeSpeedMultiplier: StateFlow<Float> = _routeSpeedMultiplier.asStateFlow()

    private val _isLoopingRoute = MutableStateFlow(false)
    val isLoopingRoute: StateFlow<Boolean> = _isLoopingRoute.asStateFlow()

    private val _recallCoordinates = MutableStateFlow<MockCoordinates?>(null)
    val recallCoordinates: StateFlow<MockCoordinates?> = _recallCoordinates.asStateFlow()

    // Route Creator State
    private val _isCreatingRoute = MutableStateFlow(false)
    val isCreatingRoute: StateFlow<Boolean> = _isCreatingRoute.asStateFlow()

    private val _newRouteWaypoints = MutableStateFlow<List<Waypoint>>(emptyList())
    val newRouteWaypoints: StateFlow<List<Waypoint>> = _newRouteWaypoints.asStateFlow()

    // Predefined Routes (not required)
    val predefinedRoutes: List<RouteItem> = emptyList()

    // Preset Instant Locations
    val presetLocations = listOf(
        PresetLocation("San Francisco", "Union Square & Market St", 37.774900, -122.419400),
        PresetLocation("Tokyo", "Shibuya Crossing, Japan", 35.659500, 139.700500),
        PresetLocation("New York City", "Times Square, Manhattan", 40.758000, -73.985500),
        PresetLocation("Paris", "Eiffel Tower & Champ de Mars", 48.858400, 2.294500),
        PresetLocation("London", "Big Ben & Westminster", 51.500700, -0.124600),
        PresetLocation("Sydney", "Opera House & Harbor Bridge", -33.856800, 151.215300),
        PresetLocation("Singapore", "Marina Bay Sands", 1.283800, 103.859100)
    )

    private var injectionJob: Job? = null
    private var routeReplayJob: Job? = null

    init {
        // Collect logs from MockLocationManager
        viewModelScope.launch {
            locationManager.logEvents.collect { log ->
                addLog(log)
            }
        }

        // Connect floating joystick callback
        FloatingJoystickService.onJoystickStep = { dLat, dLng, bearing ->
            stepJoystick(dLat, dLng, bearing)
        }

        // Fetch real device hardware location on startup
        fetchRealDeviceLocation(updateMockTarget = true)
    }

    fun fetchRealDeviceLocation(updateMockTarget: Boolean = false, onLocated: ((Double, Double) -> Unit)? = null) {
        locationManager.requestFreshLocation { realLoc ->
            val realMock = MockCoordinates(
                latitude = realLoc.latitude,
                longitude = realLoc.longitude,
                altitude = realLoc.altitude,
                accuracy = if (realLoc.accuracy > 0) realLoc.accuracy else 5.0f,
                speedKmh = realLoc.speed * 3.6f,
                bearing = realLoc.bearing,
                timestamp = realLoc.time
            )
            _realCoordinates.value = realMock
            onLocated?.invoke(realLoc.latitude, realLoc.longitude)

            if (updateMockTarget) {
                _coordinates.value = _coordinates.value.copy(
                    latitude = realLoc.latitude,
                    longitude = realLoc.longitude,
                    altitude = realLoc.altitude,
                    timestamp = System.currentTimeMillis()
                )
                addLog(
                    TelemetryLog(
                        tag = "GPS_REAL_LOC",
                        level = LogLevel.SUCCESS,
                        message = String.format(
                            Locale.US,
                            "Device GPS position restored: %.6f, %.6f (±%.1fm)",
                            realLoc.latitude,
                            realLoc.longitude,
                            realLoc.accuracy
                        )
                    )
                )
            }
        }
    }

    fun locateCurrentDevice(updatePinIfStopped: Boolean = true, onLocated: ((Double, Double) -> Unit)? = null) {
        val shouldUpdatePin = updatePinIfStopped && !_isMocking.value
        fetchRealDeviceLocation(updateMockTarget = shouldUpdatePin) { lat, lng ->
            onLocated?.invoke(lat, lng)
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun refreshPermissions() {
        _permissionsState.value = locationManager.checkPermissionsState()
        fetchRealDeviceLocation(updateMockTarget = false)
    }

    fun searchPlacesDebounced(query: String, mapCenterLat: Double, mapCenterLng: Double) {
        searchDebounceJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        _isSearching.value = true
        searchDebounceJob = viewModelScope.launch {
            delay(300) // 300ms debounce to prevent spamming
            try {
                val results = GeocodingRepository.searchPlaces(
                    query = trimmed,
                    fromLat = mapCenterLat,
                    fromLng = mapCenterLng,
                    limit = 15
                )
                _searchResults.value = results
            } catch (e: Exception) {
                // Keep existing or empty
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun reverseGeocodeMapCenter(lat: Double, lng: Double) {
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = viewModelScope.launch {
            delay(600) // Debounce when panning
            try {
                val areaName = GeocodingRepository.reverseGeocode(lat, lng)
                if (!areaName.isNullOrBlank()) {
                    _currentAreaName.value = areaName
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun clearSearchResults() {
        searchDebounceJob?.cancel()
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun updateCoordinates(lat: Double, lng: Double, reason: String = "MAP_PIN_DRAG") {
        _coordinates.value = _coordinates.value.copy(
            latitude = lat,
            longitude = lng,
            timestamp = System.currentTimeMillis()
        )
        addLog(
            TelemetryLog(
                tag = reason,
                level = LogLevel.INFO,
                message = String.format(Locale.US, "Target set to Lat: %.6f, Lng: %.6f", lat, lng)
            )
        )
        if (_isMocking.value) {
            try {
                MockLocationService.updateNotification(
                    getApplication(),
                    lat,
                    lng,
                    _coordinates.value.speedKmh
                )
            } catch (e: Exception) {
                // Ignore notification update failures
            }
        }
    }

    fun updateSpeed(speedKmh: Float) {
        _coordinates.value = _coordinates.value.copy(speedKmh = speedKmh)
    }

    fun updateMapTouchPanSpeed(speedMultiplier: Float) {
        val clamped = speedMultiplier.coerceIn(0.5f, 30.0f)
        _mapTouchPanSpeed.value = clamped
        repository.saveMapTouchPanSpeed(clamped)
    }

    fun increaseMapTouchPanSpeed(delta: Float) {
        updateMapTouchPanSpeed(_mapTouchPanSpeed.value + delta)
    }

    fun decreaseMapTouchPanSpeed(delta: Float) {
        updateMapTouchPanSpeed(_mapTouchPanSpeed.value - delta)
    }

    fun updateBearing(bearing: Float) {
        _coordinates.value = _coordinates.value.copy(bearing = bearing)
    }

    fun updateConfig(config: MockEngineConfig) {
        _engineConfig.value = config
        repository.saveEngineConfig(config)
        val newAccuracy = if (config.jitterEnabled) config.jitterRadiusMeters else 1.0f
        _coordinates.value = _coordinates.value.copy(accuracy = newAccuracy)
        addLog(
            TelemetryLog(
                tag = "CONFIG_UPDATE",
                level = LogLevel.INFO,
                message = "Engine parameters updated: Jitter=${config.jitterEnabled} (±${config.jitterRadiusMeters}m), Rate=${config.updateIntervalMs}ms"
            )
        )
    }

    fun toggleMocking() {
        if (_isMocking.value) {
            stopMocking()
        } else {
            startMocking()
        }
    }

    fun startMocking() {
        if (_recallCoordinates.value == null) {
            _recallCoordinates.value = _coordinates.value
        }
        val success = locationManager.startMocking()
        _isMocking.value = true

        try {
            MockLocationService.startService(
                getApplication(),
                _coordinates.value.latitude,
                _coordinates.value.longitude,
                _coordinates.value.speedKmh
            )
        } catch (e: Exception) {
            addLog(
                TelemetryLog(
                    tag = "SERVICE_WARN",
                    level = LogLevel.WARNING,
                    message = "Foreground notification service not allowed in current background state: ${e.message}"
                )
            )
        }

        if (_engineConfig.value.overlayJoystickEnabled && _permissionsState.value.overlayPermissionGranted) {
            try {
                FloatingJoystickService.start(getApplication())
            } catch (e: Exception) {
                // Ignore
            }
        }

        startInjectionLoop()
    }

    fun recallMockTargetPin() {
        val recallTarget = _recallCoordinates.value ?: _realCoordinates.value
        if (recallTarget != null) {
            _coordinates.value = _coordinates.value.copy(
                latitude = recallTarget.latitude,
                longitude = recallTarget.longitude,
                timestamp = System.currentTimeMillis()
            )
            if (_isMocking.value) {
                try {
                    MockLocationService.updateNotification(
                        getApplication(),
                        recallTarget.latitude,
                        recallTarget.longitude,
                        _coordinates.value.speedKmh
                    )
                } catch (e: Exception) {
                    // Ignore
                }
            }
            addLog(
                TelemetryLog(
                    tag = "PIN_RECALLED",
                    level = LogLevel.SUCCESS,
                    message = String.format(
                        Locale.US,
                        "Mock target pin recalled back to start point (%.6f, %.6f)",
                        recallTarget.latitude,
                        recallTarget.longitude
                    )
                )
            )
        }
    }

    fun stopMocking() {
        _isMocking.value = false
        try {
            locationManager.stopMocking()
        } catch (e: Exception) {
            // Ignore
        }
        try {
            MockLocationService.stopService(getApplication())
        } catch (e: Exception) {
            // Ignore
        }
        try {
            FloatingJoystickService.stop(getApplication())
        } catch (e: Exception) {
            // Ignore
        }
        stopRouteReplay()
        injectionJob?.cancel()
        injectionJob = null

        // Immediately refresh real hardware GPS location when mock injection is stopped
        fetchRealDeviceLocation(updateMockTarget = false)
    }

    private fun startInjectionLoop() {
        injectionJob?.cancel()
        injectionJob = viewModelScope.launch {
            while (isActive && _isMocking.value) {
                try {
                    locationManager.injectLocation(_coordinates.value, _engineConfig.value)
                } catch (e: Exception) {
                    // Prevent injection crashes
                }
                delay(_engineConfig.value.updateIntervalMs)
            }
        }
    }

    fun onJoystickDrag(deltaX: Float, deltaY: Float, maxRadius: Float) {
        if (maxRadius <= 0) return
        val distance = kotlin.math.hypot(deltaX, deltaY)
        val intensity = (distance / maxRadius).coerceIn(0f, 1f)
        if (intensity < 0.05f) return

        val angleRad = kotlin.math.atan2(deltaY.toDouble(), deltaX.toDouble())
        val angleDeg = ((Math.toDegrees(angleRad) + 90 + 360) % 360).toFloat()

        // Calculate step in meters based on speed and time
        val speedMps = _coordinates.value.speedMps
        val stepMeters = (speedMps * (_engineConfig.value.updateIntervalMs / 1000.0) * intensity).coerceAtLeast(0.2)

        val (nextLat, nextLng) = GeoUtils.calculateDestination(
            _coordinates.value.latitude,
            _coordinates.value.longitude,
            stepMeters,
            angleDeg
        )

        _coordinates.value = _coordinates.value.copy(
            latitude = nextLat,
            longitude = nextLng,
            bearing = angleDeg,
            timestamp = System.currentTimeMillis()
        )

        if (_isMocking.value) {
            MockLocationService.updateNotification(
                getApplication(),
                nextLat,
                nextLng,
                _coordinates.value.speedKmh
            )
        }
    }

    private fun stepJoystick(dLat: Double, dLng: Double, bearing: Float) {
        val nextLat = _coordinates.value.latitude + dLat
        val nextLng = _coordinates.value.longitude + dLng
        _coordinates.value = _coordinates.value.copy(
            latitude = nextLat,
            longitude = nextLng,
            bearing = bearing,
            timestamp = System.currentTimeMillis()
        )
        if (_isMocking.value) {
            MockLocationService.updateNotification(
                getApplication(),
                nextLat,
                nextLng,
                _coordinates.value.speedKmh
            )
        }
    }

    // --- Route Replay with Smooth Linear Interpolation ---

    fun startRouteReplay(route: RouteItem) {
        if (route.waypoints.isEmpty()) return
        if (route.waypoints.size == 1) {
            val single = route.waypoints.first()
            _coordinates.value = _coordinates.value.copy(
                latitude = single.latitude,
                longitude = single.longitude,
                timestamp = System.currentTimeMillis()
            )
            return
        }

        // Save origin starting waypoint for Recall Pin capability
        _recallCoordinates.value = route.waypoints.firstOrNull()?.let {
            MockCoordinates(latitude = it.latitude, longitude = it.longitude)
        } ?: _coordinates.value

        _activeRoute.value = route
        _isRoutePlaying.value = true
        _routeProgress.value = 0f

        if (!_isMocking.value) {
            startMocking()
        }

        addLog(
            TelemetryLog(
                tag = "ROUTE_START",
                level = LogLevel.SUCCESS,
                message = "Started route simulation with Linear Interpolation: ${route.title} (${route.distanceKm} km)"
            )
        )

        routeReplayJob?.cancel()
        routeReplayJob = viewModelScope.launch {
            val waypoints = route.waypoints

            // Pre-calculate cumulative distance along the entire path for precise global progress
            val legDistances = DoubleArray(waypoints.size)
            var totalPathDistanceMeters = 0.0
            for (i in 0 until waypoints.size - 1) {
                val d = GeoUtils.calculateDistanceMeters(
                    waypoints[i].latitude, waypoints[i].longitude,
                    waypoints[i + 1].latitude, waypoints[i + 1].longitude
                )
                legDistances[i] = d
                totalPathDistanceMeters += d
            }
            if (_isLoopingRoute.value && waypoints.size > 2) {
                val loopClosingDist = GeoUtils.calculateDistanceMeters(
                    waypoints.last().latitude, waypoints.last().longitude,
                    waypoints.first().latitude, waypoints.first().longitude
                )
                legDistances[waypoints.size - 1] = loopClosingDist
                totalPathDistanceMeters += loopClosingDist
            }
            if (totalPathDistanceMeters <= 0.0) totalPathDistanceMeters = 1.0

            var currentLegIndex = 0
            var legDistanceTraveledMeters = 0.0
            var globalDistanceTraveledMeters = 0.0
            var lastTickTimeNs = System.nanoTime()

            // High-frequency sub-stepping tick (30ms = ~33 FPS smooth animation)
            val subTickIntervalMs = 30L

            while (isActive && _isRoutePlaying.value) {
                val nowNs = System.nanoTime()
                val deltaSeconds = ((nowNs - lastTickTimeNs) / 1_000_000_000.0).coerceIn(0.001, 0.2)
                lastTickTimeNs = nowNs

                val currentWp = waypoints[currentLegIndex]
                val nextIndex = (currentLegIndex + 1) % waypoints.size

                // If non-looping and reached end of final waypoint: STOP at destination and hold coordinates
                if (currentLegIndex >= waypoints.size - 1 && !_isLoopingRoute.value) {
                    val finalWp = waypoints.last()
                    _coordinates.value = _coordinates.value.copy(
                        latitude = finalWp.latitude,
                        longitude = finalWp.longitude,
                        timestamp = System.currentTimeMillis()
                    )
                    _routeProgress.value = 1f
                    _isRoutePlaying.value = false
                    routeReplayJob?.cancel()
                    routeReplayJob = null
                    addLog(
                        TelemetryLog(
                            tag = "ROUTE_COMPLETE",
                            level = LogLevel.SUCCESS,
                            message = String.format(
                                Locale.US,
                                "Reached final waypoint (%d/%d: %.6f, %.6f). Mock target pin holding position at destination.",
                                waypoints.size,
                                waypoints.size,
                                finalWp.latitude,
                                finalWp.longitude
                            )
                        )
                    )
                    break
                }

                val targetWp = waypoints[nextIndex]
                val legLengthMeters = legDistances[currentLegIndex].coerceAtLeast(0.0)

                // Handle degenerate leg with zero distance
                if (legLengthMeters < 0.1) {
                    currentLegIndex = nextIndex
                    legDistanceTraveledMeters = 0.0
                    delay(subTickIntervalMs)
                    continue
                }

                // Dynamic speed calculation
                val effectiveSpeedKmh = _coordinates.value.speedKmh * _routeSpeedMultiplier.value
                val effectiveSpeedMps = (effectiveSpeedKmh / 3.6).coerceAtLeast(0.1)

                // Advance distance along current leg using high-precision delta time
                val distanceIncrement = effectiveSpeedMps * deltaSeconds
                legDistanceTraveledMeters += distanceIncrement
                globalDistanceTraveledMeters += distanceIncrement

                // Leg completion check
                if (legDistanceTraveledMeters >= legLengthMeters) {
                    // Carry over remaining distance into next leg to maintain zero velocity jitter
                    val surplusDistance = legDistanceTraveledMeters - legLengthMeters
                    currentLegIndex = nextIndex

                    if (currentLegIndex >= waypoints.size - 1 && !_isLoopingRoute.value) {
                        val finalWp = waypoints.last()
                        _coordinates.value = _coordinates.value.copy(
                            latitude = finalWp.latitude,
                            longitude = finalWp.longitude,
                            timestamp = System.currentTimeMillis()
                        )
                        _routeProgress.value = 1f
                        _isRoutePlaying.value = false
                        routeReplayJob?.cancel()
                        routeReplayJob = null
                        addLog(
                            TelemetryLog(
                                tag = "ROUTE_COMPLETE",
                                level = LogLevel.SUCCESS,
                                message = String.format(
                                    Locale.US,
                                    "Arrived at destination waypoint (%d/%d: %.6f, %.6f). Holding position.",
                                    waypoints.size,
                                    waypoints.size,
                                    finalWp.latitude,
                                    finalWp.longitude
                                )
                            )
                        )
                        break
                    }

                    legDistanceTraveledMeters = surplusDistance
                    if (currentLegIndex == 0 && _isLoopingRoute.value) {
                        globalDistanceTraveledMeters = surplusDistance
                    }
                    delay(subTickIntervalMs)
                    continue
                }

                // Linear Interpolation (LERP) fraction strictly in [0.0, 1.0]
                val fraction = (legDistanceTraveledMeters / legLengthMeters).coerceIn(0.0, 1.0)

                // 1. High-precision Geodetic Coordinate Linear Interpolation (with meridian wrap protection)
                val (interpolatedLat, interpolatedLng) = GeoUtils.interpolateCoordinates(
                    lat1 = currentWp.latitude,
                    lon1 = currentWp.longitude,
                    lat2 = targetWp.latitude,
                    lon2 = targetWp.longitude,
                    fraction = fraction
                )

                // 2. Smooth Bearing Interpolation (shortest angular arc)
                val currentLegBearing = GeoUtils.calculateBearing(
                    currentWp.latitude, currentWp.longitude,
                    targetWp.latitude, targetWp.longitude
                )

                val followingIndex = (nextIndex + 1) % waypoints.size
                val nextTargetWp = waypoints[followingIndex]
                val nextLegBearing = GeoUtils.calculateBearing(
                    targetWp.latitude, targetWp.longitude,
                    nextTargetWp.latitude, nextTargetWp.longitude
                )

                // Smoothly blend heading into next leg during the final 20% of current leg
                val smoothBearing = if (fraction > 0.8) {
                    val turnBlend = ((fraction - 0.8) / 0.2).toFloat()
                    GeoUtils.interpolateBearing(currentLegBearing, nextLegBearing, turnBlend)
                } else {
                    currentLegBearing
                }

                // 3. Update Coordinates state seamlessly
                _coordinates.value = _coordinates.value.copy(
                    latitude = interpolatedLat,
                    longitude = interpolatedLng,
                    bearing = smoothBearing,
                    timestamp = System.currentTimeMillis()
                )

                // 4. Update overall continuous route progress
                val overallFraction = (globalDistanceTraveledMeters / totalPathDistanceMeters).toFloat()
                _routeProgress.value = overallFraction.coerceIn(0f, 1f)

                delay(subTickIntervalMs)
            }
        }
    }

    fun pauseRouteReplay() {
        _isRoutePlaying.value = false
        routeReplayJob?.cancel()
        routeReplayJob = null
        addLog(TelemetryLog(tag = "ROUTE_PAUSE", level = LogLevel.INFO, message = "Route playback paused."))
    }

    fun resumeRouteReplay() {
        val current = _activeRoute.value ?: return
        startRouteReplay(current)
    }

    fun stopRouteReplay() {
        _isRoutePlaying.value = false
        _activeRoute.value = null
        _routeProgress.value = 0f
        routeReplayJob?.cancel()
        routeReplayJob = null
    }

    fun setRouteSpeedMultiplier(multiplier: Float) {
        _routeSpeedMultiplier.value = multiplier
    }

    fun toggleLoopRoute() {
        _isLoopingRoute.value = !_isLoopingRoute.value
    }

    // --- Custom Route Creation ---

    fun startCreatingRoute() {
        _isCreatingRoute.value = true
        _newRouteWaypoints.value = listOf(
            Waypoint(
                latitude = _coordinates.value.latitude,
                longitude = _coordinates.value.longitude,
                name = "Start Point",
                index = 0
            )
        )
    }

    fun addWaypointToNewRoute(lat: Double, lng: Double) {
        val current = _newRouteWaypoints.value.toMutableList()
        val index = current.size
        current.add(
            Waypoint(
                latitude = lat,
                longitude = lng,
                name = "Waypoint ${index + 1}",
                index = index
            )
        )
        _newRouteWaypoints.value = current
    }

    fun removeWaypoint(index: Int) {
        val current = _newRouteWaypoints.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _newRouteWaypoints.value = current
        }
    }

    fun saveNewRoute(title: String, description: String) {
        val wps = _newRouteWaypoints.value
        if (wps.size < 2) return

        var totalDistMeters = 0.0
        for (i in 0 until wps.size - 1) {
            totalDistMeters += GeoUtils.calculateDistanceMeters(
                wps[i].latitude, wps[i].longitude,
                wps[i + 1].latitude, wps[i + 1].longitude
            )
        }
        val distKm = totalDistMeters / 1000.0

        viewModelScope.launch {
            repository.saveRoute(
                title = title.ifEmpty { "Custom Route #${System.currentTimeMillis() % 1000}" },
                description = description.ifEmpty { "${String.format(Locale.US, "%.1f", distKm)} km • ${wps.size} Waypoints" },
                distanceKm = distKm,
                waypoints = wps
            )
            _isCreatingRoute.value = false
            _newRouteWaypoints.value = emptyList()
            addLog(
                TelemetryLog(
                    tag = "ROUTE_SAVED",
                    level = LogLevel.SUCCESS,
                    message = "Saved new route '$title' with ${wps.size} waypoints."
                )
            )
        }
    }

    fun cancelRouteCreation() {
        _isCreatingRoute.value = false
        _newRouteWaypoints.value = emptyList()
    }

    fun deleteSavedRoute(id: String) {
        viewModelScope.launch {
            repository.deleteRoute(id)
        }
    }

    // --- Favorites ---

    fun saveCurrentAsFavorite(title: String, subtitle: String = "") {
        viewModelScope.launch {
            repository.saveFavorite(
                title = title.ifEmpty { "Pinned Place" },
                subtitle = subtitle.ifEmpty { String.format(Locale.US, "%.4f, %.4f", _coordinates.value.latitude, _coordinates.value.longitude) },
                lat = _coordinates.value.latitude,
                lng = _coordinates.value.longitude
            )
            addLog(
                TelemetryLog(
                    tag = "FAVORITE_SAVED",
                    level = LogLevel.SUCCESS,
                    message = "Added '$title' to favorite locations."
                )
            )
        }
    }

    fun deleteFavorite(id: String) {
        viewModelScope.launch {
            repository.deleteFavorite(id)
        }
    }

    // --- Telemetry Logs ---

    fun addLog(log: TelemetryLog) {
        val current = _inMemoryLogs.value.toMutableList()
        current.add(0, log)
        if (current.size > 200) {
            _inMemoryLogs.value = current.take(200)
        } else {
            _inMemoryLogs.value = current
        }
        viewModelScope.launch {
            repository.insertLog(log)
        }
    }

    fun clearLogs() {
        _inMemoryLogs.value = emptyList()
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun clearCrashReports() {
        viewModelScope.launch {
            repository.clearCrashReports()
            CrashTracker.clearReports()
        }
    }

    // --- Predefined Route Generators ---

    private fun generateJoggingPath(centerLat: Double, centerLng: Double): List<Waypoint> {
        val offsets = listOf(
            0.0 to 0.0,
            0.002 to 0.001,
            0.0035 to 0.003,
            0.003 to 0.006,
            0.0015 to 0.008,
            -0.0005 to 0.0075,
            -0.0025 to 0.0055,
            -0.0035 to 0.003,
            -0.003 to 0.0005,
            -0.0015 to -0.0015,
            -0.0005 to -0.001,
            0.0 to 0.0
        )
        return offsets.mapIndexed { idx, (dLat, dLng) ->
            Waypoint(
                latitude = centerLat + dLat,
                longitude = centerLng + dLng,
                name = "Jog Point ${idx + 1}",
                index = idx
            )
        }
    }

    private fun generateHighwayPath(startLat: Double, startLng: Double): List<Waypoint> {
        val points = mutableListOf<Waypoint>()
        for (i in 0..40) {
            val progress = i / 40.0
            val lat = startLat + (progress * 0.12) + (Math.sin(progress * Math.PI * 3) * 0.008)
            val lng = startLng - (progress * 0.09) + (Math.cos(progress * Math.PI * 2) * 0.005)
            points.add(Waypoint(lat, lng, "Highway Mile ${(progress * 14).toInt()}", i))
        }
        return points
    }

    private fun generateTokyoPath(): List<Waypoint> {
        val coords = listOf(
            35.6595 to 139.7005,
            35.6612 to 139.7020,
            35.6635 to 139.7040,
            35.6660 to 139.7035,
            35.6690 to 139.7015,
            35.6720 to 139.6990,
            35.6745 to 139.6970,
            35.6730 to 139.6940,
            35.6695 to 139.6960,
            35.6650 to 139.6980,
            35.6615 to 139.6995,
            35.6595 to 139.7005
        )
        return coords.mapIndexed { idx, (lat, lng) ->
            Waypoint(lat, lng, "Shibuya Step $idx", idx)
        }
    }

    private fun generateCentralParkPath(): List<Waypoint> {
        val coords = listOf(
            40.7645 to -73.9740, // Grand Army Plaza
            40.7680 to -73.9715,
            40.7740 to -73.9680,
            40.7810 to -73.9630, // Metropolitan Museum
            40.7890 to -73.9580, // Guggenheim
            40.7960 to -73.9530, // Harlem Meer
            40.8000 to -73.9570, // North Meadow
            40.7950 to -73.9630,
            40.7870 to -73.9690, // Great Lawn
            40.7800 to -73.9750, // Sheep Meadow
            40.7720 to -73.9790, // Columbus Circle
            40.7645 to -73.9740
        )
        return coords.mapIndexed { idx, (lat, lng) ->
            Waypoint(lat, lng, "Central Park WP $idx", idx)
        }
    }

    // Gemini AI Copilot Chat State
    private val _copilotMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val copilotMessages: StateFlow<List<ChatMessage>> = _copilotMessages.asStateFlow()

    private val _isCopilotGenerating = MutableStateFlow(false)
    val isCopilotGenerating: StateFlow<Boolean> = _isCopilotGenerating.asStateFlow()

    fun sendCopilotMessage(userText: String) {
        if (userText.isBlank() || _isCopilotGenerating.value) return

        val userMessage = ChatMessage(
            role = "user",
            content = userText.trim()
        )

        val updatedHistory = _copilotMessages.value + userMessage
        _copilotMessages.value = updatedHistory
        _isCopilotGenerating.value = true

        viewModelScope.launch {
            try {
                val currentLat = _coordinates.value.latitude
                val currentLng = _coordinates.value.longitude
                val assistantReply = GeminiCopilotService.sendMessage(
                    history = updatedHistory,
                    userMessage = userText.trim(),
                    currentLat = currentLat,
                    currentLng = currentLng
                )
                _copilotMessages.value = _copilotMessages.value + assistantReply

                // If assistant produced a LoadRoute action, auto-persist the route into the Room Database
                assistantReply.actions.filterIsInstance<CopilotAction.LoadRoute>().firstOrNull()?.let { routeAction ->
                    saveAiRouteSilently(routeAction.title, routeAction.waypoints)
                }
            } catch (e: Exception) {
                _copilotMessages.value = _copilotMessages.value + ChatMessage(
                    role = "model",
                    content = "Unable to reach AI service: ${e.localizedMessage ?: "Unknown error"}. Please check your connection and try again.",
                    isError = true
                )
            } finally {
                _isCopilotGenerating.value = false
            }
        }
    }

    fun clearCopilotMessages() {
        _copilotMessages.value = emptyList()
    }

    private suspend fun saveAiRouteSilently(title: String, waypoints: List<Waypoint>) {
        if (waypoints.isEmpty()) return
        var totalDistMeters = 0.0
        for (i in 0 until waypoints.size - 1) {
            totalDistMeters += GeoUtils.calculateDistanceMeters(
                waypoints[i].latitude, waypoints[i].longitude,
                waypoints[i + 1].latitude, waypoints[i + 1].longitude
            )
        }
        val distKm = totalDistMeters / 1000.0
        val routeTitle = title.ifEmpty { "AI Route" }
        repository.saveRoute(
            title = routeTitle,
            description = "AI Generated Route (${waypoints.size} waypoints • ${String.format(Locale.US, "%.1f", distKm)} km)",
            distanceKm = distKm,
            waypoints = waypoints
        )
    }

    fun loadAiGeneratedRoute(title: String, waypoints: List<Waypoint>) {
        if (waypoints.isEmpty()) return

        viewModelScope.launch {
            // If the route has sparse waypoints, snap it to the actual physical road network
            val resolvedWaypoints = if (waypoints.size in 2..5) {
                val origin = waypoints.first()
                val dest = waypoints.last()
                val roadRes = RoadRoutingRepository.calculateRoadNavigationRoute(
                    originLat = origin.latitude,
                    originLng = origin.longitude,
                    destLat = dest.latitude,
                    destLng = dest.longitude,
                    originName = origin.name.ifBlank { "Start" },
                    destName = dest.name.ifBlank { "Destination" }
                )
                if (roadRes.waypoints.size > waypoints.size) roadRes.waypoints else waypoints
            } else {
                waypoints
            }

            var totalDistMeters = 0.0
            for (i in 0 until resolvedWaypoints.size - 1) {
                totalDistMeters += GeoUtils.calculateDistanceMeters(
                    resolvedWaypoints[i].latitude, resolvedWaypoints[i].longitude,
                    resolvedWaypoints[i + 1].latitude, resolvedWaypoints[i + 1].longitude
                )
            }
            val distKm = totalDistMeters / 1000.0

            val route = RouteItem(
                id = UUID.randomUUID().toString(),
                title = title.ifEmpty { "Road Navigation Route" },
                description = "Road Navigation Route (${resolvedWaypoints.size} road points • ${String.format(Locale.US, "%.1f", distKm)} km)",
                distanceKm = distKm,
                waypoints = resolvedWaypoints,
                isCustom = true
            )

            repository.saveRoute(
                title = route.title,
                description = route.description,
                distanceKm = route.distanceKm,
                waypoints = route.waypoints
            )
            startRouteReplay(route)
            // Set current mock target to initial waypoint
            if (resolvedWaypoints.isNotEmpty()) {
                updateCoordinates(resolvedWaypoints.first().latitude, resolvedWaypoints.first().longitude, "ROAD_NAV_START")
            }
            setTab(AppTab.MAP)
            addLog(
                TelemetryLog(
                    tag = "ROAD_NAV_LOADED",
                    level = LogLevel.SUCCESS,
                    message = "Loaded Road Navigation route '${route.title}' (${resolvedWaypoints.size} road points, ${String.format(Locale.US, "%.1f", distKm)} km)."
                )
            )
        }
    }

    fun saveAiRouteAndOpenRoutesTab(title: String, waypoints: List<Waypoint>) {
        if (waypoints.isEmpty()) return

        viewModelScope.launch {
            val resolvedWaypoints = if (waypoints.size in 2..5) {
                val origin = waypoints.first()
                val dest = waypoints.last()
                val roadRes = RoadRoutingRepository.calculateRoadNavigationRoute(
                    originLat = origin.latitude,
                    originLng = origin.longitude,
                    destLat = dest.latitude,
                    destLng = dest.longitude,
                    originName = origin.name.ifBlank { "Start" },
                    destName = dest.name.ifBlank { "Destination" }
                )
                if (roadRes.waypoints.size > waypoints.size) roadRes.waypoints else waypoints
            } else {
                waypoints
            }

            var totalDistMeters = 0.0
            for (i in 0 until resolvedWaypoints.size - 1) {
                totalDistMeters += GeoUtils.calculateDistanceMeters(
                    resolvedWaypoints[i].latitude, resolvedWaypoints[i].longitude,
                    resolvedWaypoints[i + 1].latitude, resolvedWaypoints[i + 1].longitude
                )
            }
            val distKm = totalDistMeters / 1000.0

            val route = RouteItem(
                id = UUID.randomUUID().toString(),
                title = title.ifEmpty { "Road Navigation Route" },
                description = "Road Navigation Route (${resolvedWaypoints.size} road points • ${String.format(Locale.US, "%.1f", distKm)} km)",
                distanceKm = distKm,
                waypoints = resolvedWaypoints,
                isCustom = true
            )

            repository.saveRoute(
                title = route.title,
                description = route.description,
                distanceKm = route.distanceKm,
                waypoints = route.waypoints
            )
            _activeRoute.value = route
            setTab(AppTab.ROUTES)
            addLog(
                TelemetryLog(
                    tag = "ROAD_NAV_SAVED",
                    level = LogLevel.SUCCESS,
                    message = "Saved Road Navigation route '${route.title}' into Routes Tab."
                )
            )
        }
    }
}


