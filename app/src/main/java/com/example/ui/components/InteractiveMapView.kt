package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.MockCoordinates
import com.example.data.MockEngineConfig
import com.example.data.Waypoint
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BlueDark
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.BlueSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.util.Locale
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tan

@Composable
fun InteractiveMapView(
    coordinates: MockCoordinates,
    realCoordinates: MockCoordinates? = null,
    engineConfig: MockEngineConfig? = null,
    onCoordinatesChanged: (Double, Double, String) -> Unit,
    onRefreshRealLocation: (() -> Unit)? = null,
    onMapCenterChanged: ((Double, Double) -> Unit)? = null,
    routeWaypoints: List<Waypoint> = emptyList(),
    isCreatingRoute: Boolean = false,
    onMapTapForWaypoint: ((Double, Double) -> Unit)? = null,
    onResetBearing: (() -> Unit)? = null,
    touchPanSpeedMultiplier: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val tileImageLoader = remember(context) { MapTileLoader.getImageLoader(context) }

    // Always updated state references to prevent stale closures in pointerInput
    val currentCoordinates by rememberUpdatedState(coordinates)
    val currentRealCoordinates by rememberUpdatedState(realCoordinates)
    val currentOnCoordinatesChanged by rememberUpdatedState(onCoordinatesChanged)
    val currentOnMapTapForWaypoint by rememberUpdatedState(onMapTapForWaypoint)
    val currentOnMapCenterChanged by rememberUpdatedState(onMapCenterChanged)
    val currentIsCreatingRoute by rememberUpdatedState(isCreatingRoute)

    // Viewport center - initialized to starting coordinates
    var centerLat by remember { mutableDoubleStateOf(coordinates.latitude) }
    var centerLng by remember { mutableDoubleStateOf(coordinates.longitude) }
    var zoomLevel by remember { mutableFloatStateOf(16f) } // 2.0 .. 22.0

    var lastKnownCoordLat by remember { mutableDoubleStateOf(coordinates.latitude) }
    var lastKnownCoordLng by remember { mutableDoubleStateOf(coordinates.longitude) }

    // Track tap events for double-tap and tap gestures
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }

    var selectedProvider by remember { mutableStateOf(MapTileProvider.GOOGLE_STREETS) }
    var showLayersMenu by remember { mutableStateOf(false) }

    // Re-center map immediately whenever coordinates change from teleport, search, presets, routes, or copilot
    LaunchedEffect(coordinates.latitude, coordinates.longitude) {
        val dLat = Math.abs(coordinates.latitude - lastKnownCoordLat)
        val dLng = Math.abs(coordinates.longitude - lastKnownCoordLng)
        // Whenever target coordinates are updated (teleport, search, preset, AI prompt, tap), re-center camera
        if (dLat > 0.00005 || dLng > 0.00005) {
            centerLat = coordinates.latitude
            centerLng = coordinates.longitude
            currentOnMapCenterChanged?.invoke(centerLat, centerLng)
        }
        lastKnownCoordLat = coordinates.latitude
        lastKnownCoordLng = coordinates.longitude
    }

    // Notify parent of initial center
    LaunchedEffect(Unit) {
        currentOnMapCenterChanged?.invoke(centerLat, centerLng)
    }

    // Radar pulse animations
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )
    val mockPulseRadius by infiniteTransition.animateFloat(
        initialValue = 7f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "MockPulseRadius"
    )
    val mockPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "MockPulseAlpha"
    )

    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE8ECEF))
            .clipToBounds()
            .testTag("interactive_map_view")
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(100f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(100f)

        // Web Mercator calculations (Supporting ultra-high zoom up to level 22)
        val intZoom = zoomLevel.toInt().coerceIn(2, 19)
        val subZoom = zoomLevel - intZoom
        val scale = 2.0.pow(subZoom.toDouble()).toFloat()
        val numTiles = 1 shl intZoom

        // Dynamic coordinate transformation helpers that always use the live center and zoom
        fun latLngToScreen(lat: Double, lng: Double): Offset {
            if (lat.isNaN() || lng.isNaN()) return Offset(widthPx / 2f, heightPx / 2f)
            val totalWorldPx = 256.0 * 2.0.pow(zoomLevel.toDouble())
            if (totalWorldPx <= 0.0) return Offset(widthPx / 2f, heightPx / 2f)

            val safeLat = lat.coerceIn(-85.0511, 85.0511)
            val dLng = ((lng - centerLng + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
            val dWorldX = (dLng / 360.0) * totalWorldPx

            val sinL = sin(Math.toRadians(safeLat)).coerceIn(-0.9999, 0.9999)
            val wy = (0.5 - ln((1.0 + sinL) / (1.0 - sinL)) / (4.0 * Math.PI)) * totalWorldPx

            val safeCLat = centerLat.coerceIn(-85.0511, 85.0511)
            val sinCLat = sin(Math.toRadians(safeCLat)).coerceIn(-0.9999, 0.9999)
            val cWorldY = (0.5 - ln((1.0 + sinCLat) / (1.0 - sinCLat)) / (4.0 * Math.PI)) * totalWorldPx

            val dWorldY = wy - cWorldY
            val sx = widthPx / 2f + dWorldX.toFloat()
            val sy = heightPx / 2f + dWorldY.toFloat()

            val safeSx = if (sx.isNaN() || sx.isInfinite()) widthPx / 2f else sx.coerceIn(-50000f, 50000f)
            val safeSy = if (sy.isNaN() || sy.isInfinite()) heightPx / 2f else sy.coerceIn(-50000f, 50000f)
            return Offset(safeSx, safeSy)
        }

        fun screenToLatLng(sx: Float, sy: Float): Pair<Double, Double> {
            val totalWorldPx = 256.0 * 2.0.pow(zoomLevel.toDouble())
            if (totalWorldPx <= 0.0 || widthPx <= 0f || heightPx <= 0f) return Pair(centerLat, centerLng)

            val dWorldX = (sx - widthPx / 2f).toDouble()
            val dWorldY = (sy - heightPx / 2f).toDouble()

            val dLng = (dWorldX / totalWorldPx) * 360.0
            val resLng = ((centerLng + dLng + 180.0) % 360.0 + 360.0) % 360.0 - 180.0

            val safeCLat = centerLat.coerceIn(-85.0511, 85.0511)
            val sinCLat = sin(Math.toRadians(safeCLat)).coerceIn(-0.9999, 0.9999)
            val cWorldY = (0.5 - ln((1.0 + sinCLat) / (1.0 - sinCLat)) / (4.0 * Math.PI)) * totalWorldPx

            val wy = (cWorldY + dWorldY).coerceIn(0.0, totalWorldPx - 1.0)
            val v = wy / totalWorldPx
            val n = Math.PI - 2.0 * Math.PI * v
            val lat = Math.toDegrees(atan(sinh(n)))
            val resLat = if (lat.isNaN()) 0.0 else lat.coerceIn(-85.0511, 85.0511)

            return Pair(resLat, resLng)
        }

        // Live Dynamic Panning Helper
        fun panMap(deltaX: Float, deltaY: Float) {
            if (deltaX == 0f && deltaY == 0f) return
            val totalWorldPx = 256.0 * 2.0.pow(zoomLevel.toDouble())
            if (totalWorldPx <= 0.0) return

            val curLng = centerLng
            val curLat = centerLat.coerceIn(-85.0511, 85.0511)

            val u = ((curLng + 180.0) % 360.0 + 360.0) % 360.0 / 360.0
            val sinL = sin(Math.toRadians(curLat)).coerceIn(-0.9999, 0.9999)
            val v = (0.5 - ln((1.0 + sinL) / (1.0 - sinL)) / (4.0 * Math.PI)).coerceIn(0.0001, 0.9999)

            val du = -deltaX.toDouble() / totalWorldPx
            val dv = -deltaY.toDouble() / totalWorldPx

            val newU = ((u + du) % 1.0 + 1.0) % 1.0
            val newV = (v + dv).coerceIn(0.0001, 0.9999)

            val newLng = newU * 360.0 - 180.0
            val n = Math.PI - 2.0 * Math.PI * newV
            val newLat = Math.toDegrees(atan(sinh(n))).coerceIn(-85.0511, 85.0511)

            centerLat = newLat
            centerLng = newLng
            currentOnMapCenterChanged?.invoke(newLat, newLng)
        }

        // Live Dynamic Pinch-Zoom Helper
        fun zoomMap(zoomChangeFactor: Float) {
            if (zoomChangeFactor <= 0f || zoomChangeFactor.isNaN() || zoomChangeFactor == 1.0f) return
            val zoomDelta = (ln(zoomChangeFactor.toDouble()) / ln(2.0)).toFloat()
            val oldZoom = zoomLevel
            val newZoom = (oldZoom + zoomDelta).coerceIn(2.0f, 22.0f)
            if (newZoom != oldZoom) {
                zoomLevel = newZoom
            }
        }

        // World coordinates for tile calculation
        val safeCenterLat = if (centerLat.isNaN()) 0.0 else centerLat.coerceIn(-85.0511, 85.0511)
        val safeCenterLng = if (centerLng.isNaN()) 0.0 else ((centerLng + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
        val centerWorldX = ((safeCenterLng + 180.0) / 360.0) * (256.0 * numTiles)
        val sinCenterLat = sin(Math.toRadians(safeCenterLat)).coerceIn(-0.9999, 0.9999)
        val centerWorldY = (0.5 - ln((1.0 + sinCenterLat) / (1.0 - sinCenterLat)) / (4.0 * Math.PI)) * (256.0 * numTiles)

        // Calculate visible tile ranges with bounded window
        val halfW = (widthPx / 2.0) / scale
        val halfH = (heightPx / 2.0) / scale
        val centerTileX = floor(centerWorldX / 256.0).toInt()
        val centerTileY = floor(centerWorldY / 256.0).toInt()

        val rawMinTileX = floor((centerWorldX - halfW) / 256.0).toInt() - 1
        val rawMaxTileX = ceil((centerWorldX + halfW) / 256.0).toInt() + 1
        val rawMinTileY = floor((centerWorldY - halfH) / 256.0).toInt() - 1
        val rawMaxTileY = ceil((centerWorldY + halfH) / 256.0).toInt() + 1

        val minTileX = rawMinTileX.coerceIn(centerTileX - 3, centerTileX + 3)
        val maxTileX = rawMaxTileX.coerceIn(minTileX, centerTileX + 3)
        val minTileY = rawMinTileY.coerceIn(0, (numTiles - 1).coerceAtLeast(0)).coerceIn((centerTileY - 3).coerceAtLeast(0), (centerTileY + 3).coerceAtMost((numTiles - 1).coerceAtLeast(0)))
        val maxTileY = rawMaxTileY.coerceIn(0, (numTiles - 1).coerceAtLeast(0)).coerceIn(minTileY, (centerTileY + 3).coerceAtMost((numTiles - 1).coerceAtLeast(0)))

        val tileSizePx = (256.0 * scale).toFloat()
        val tileSizeDp = with(density) { tileSizePx.toDp() }

        // Background Pre-fetching for immediate seamless panning
        LaunchedEffect(minTileX, maxTileX, minTileY, maxTileY, intZoom, selectedProvider) {
            val prefetchMinX = minTileX - 1
            val prefetchMaxX = maxTileX + 1
            val prefetchMinY = (minTileY - 1).coerceAtLeast(0)
            val prefetchMaxY = (maxTileY + 1).coerceAtMost(numTiles - 1)

            for (py in prefetchMinY..prefetchMaxY) {
                for (px in prefetchMinX..prefetchMaxX) {
                    val isAlreadyVisible = px in minTileX..maxTileX && py in minTileY..maxTileY
                    if (!isAlreadyVisible) {
                        val pUrl = MapTileLoader.buildTileUrl(selectedProvider, intZoom, px, py)
                        val pReq = ImageRequest.Builder(context)
                            .data(pUrl)
                            .size(coil.size.Size.ORIGINAL)
                            .memoryCacheKey("$intZoom-$px-$py-${selectedProvider.name}")
                            .diskCacheKey("$intZoom-$px-$py-${selectedProvider.name}")
                            .crossfade(false)
                            .build()
                        tileImageLoader.enqueue(pReq)
                    }
                }
            }
        }

        // Unified Gestures handling (Instant 1-tap pin placement, pin drag, ultra-smooth 1:1 finger pan, pinch-zoom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        val downPos = down.position
                        var isTransforming = false
                        var totalMoveDist = 0f
                        val slop = viewConfiguration.touchSlop
                        var previousTouchPos = downPos

                        // Check if down position is directly grabbing the Mock Target Pin (within 44dp hit area)
                        val currentMockScreenPos = latLngToScreen(currentCoordinates.latitude, currentCoordinates.longitude)
                        val isGrabbingMockPin = (downPos - currentMockScreenPos).getDistance() <= 44.dp.toPx()

                        do {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }
                            val pointerCount = activePointers.size

                            if (pointerCount == 0) {
                                break
                            }

                            if (isGrabbingMockPin && pointerCount == 1) {
                                // User is directly dragging the Mock Target Pin!
                                val change = activePointers[0]
                                val currentPos = change.position
                                val (draggedLat, draggedLng) = screenToLatLng(currentPos.x, currentPos.y)
                                currentOnCoordinatesChanged(draggedLat, draggedLng, "MAP_PIN_DRAG")
                                isTransforming = true
                                change.consume()
                                previousTouchPos = currentPos
                            } else if (pointerCount == 1) {
                                val change = activePointers[0]
                                val currentPos = change.position
                                val delta = currentPos - previousTouchPos
                                val moveDist = (currentPos - downPos).getDistance()
                                totalMoveDist = moveDist

                                if (moveDist > slop || isTransforming) {
                                    isTransforming = true
                                    change.consume()
                                    if (delta.x != 0f || delta.y != 0f) {
                                        panMap(delta.x, delta.y)
                                    }
                                }
                                previousTouchPos = currentPos
                            } else if (pointerCount >= 2) {
                                isTransforming = true
                                val p0 = activePointers[0]
                                val p1 = activePointers[1]

                                val prevDist = (p0.previousPosition - p1.previousPosition).getDistance()
                                val curDist = (p0.position - p1.position).getDistance()
                                val panDelta = ((p0.position - p0.previousPosition) + (p1.position - p1.previousPosition)) / 2f
                                val centroid = (p0.position + p1.position) / 2f

                                if (prevDist > 1f && curDist > 1f) {
                                    val zoomFactor = (curDist / prevDist).coerceIn(0.5f, 2.0f)
                                    zoomMap(zoomFactor)
                                }

                                if (panDelta.x != 0f || panDelta.y != 0f) {
                                    panMap(panDelta.x, panDelta.y)
                                }

                                previousTouchPos = centroid
                                activePointers.forEach { it.consume() }
                            }
                        } while (activePointers.isNotEmpty())

                        val elapsed = System.currentTimeMillis() - downTime
                        if (!isTransforming && totalMoveDist < slop) {
                            val now = System.currentTimeMillis()
                            val isDoubleTap = (now - lastTapTime < 350) && (downPos - lastTapPosition).getDistance() < slop * 3

                            if (isDoubleTap) {
                                val (tapLat, tapLng) = screenToLatLng(downPos.x, downPos.y)
                                centerLat = (centerLat + tapLat) / 2.0
                                centerLng = (centerLng + tapLng) / 2.0
                                zoomLevel = (zoomLevel + 1.5f).coerceAtMost(22.0f)
                                currentOnMapCenterChanged?.invoke(centerLat, centerLng)
                                lastTapTime = 0L
                            } else if (elapsed > 550) {
                                val (tapLat, tapLng) = screenToLatLng(downPos.x, downPos.y)
                                currentOnCoordinatesChanged(tapLat, tapLng, "MAP_LONG_PRESS")
                                lastTapTime = 0L
                            } else {
                                // Single tap on map -> Instantly moves Mock Target Pin to tapped location!
                                val (tapLat, tapLng) = screenToLatLng(downPos.x, downPos.y)
                                if (currentIsCreatingRoute) {
                                    currentOnMapTapForWaypoint?.invoke(tapLat, tapLng)
                                } else {
                                    currentOnCoordinatesChanged(tapLat, tapLng, "MAP_TAP")
                                }
                                lastTapTime = now
                                lastTapPosition = downPos
                            }
                        }
                    }
                }
        ) {
            // LAYER 0: Cartographic Base Grid (Always ensures crisp visual feedback while streaming tiles)
            Canvas(modifier = Modifier.fillMaxSize()) {
                try {
                    val gridSpacingPx = (64.dp.toPx() * scale).coerceAtLeast(16f)
                    val numCols = (widthPx / gridSpacingPx).toInt() + 2
                    val numRows = (heightPx / gridSpacingPx).toInt() + 2
                    
                    // Draw warm geographic background
                    val isDarkTile = selectedProvider == MapTileProvider.GOOGLE_SATELLITE || selectedProvider == MapTileProvider.GOOGLE_HYBRID
                    drawRect(color = if (isDarkTile) Color(0xFF191A1A) else Color(0xFFF2F4F7))

                    // Draw latitude/longitude grid lines
                    for (i in -1..numCols) {
                        val x = (i * gridSpacingPx) % widthPx
                        if (x.isFinite()) {
                            drawLine(
                                color = if (isDarkTile) Color(0xFF2A2D34) else Color(0xFFE2E8F0),
                                start = Offset(x, 0f),
                                end = Offset(x, heightPx),
                                strokeWidth = 1f
                            )
                        }
                    }
                    for (j in -1..numRows) {
                        val y = (j * gridSpacingPx) % heightPx
                        if (y.isFinite()) {
                            drawLine(
                                color = if (isDarkTile) Color(0xFF2A2D34) else Color(0xFFE2E8F0),
                                start = Offset(0f, y),
                                end = Offset(widthPx, y),
                                strokeWidth = 1f
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Safe draw
                }
            }

            // LAYER 1: Real Map Raster Tiles (OpenStreetMap / CartoDB / Esri World Satellite)
            Box(modifier = Modifier.fillMaxSize()) {
                if (minTileX <= maxTileX && minTileY <= maxTileY) {
                    for (ty in minTileY..maxTileY) {
                        for (tx in minTileX..maxTileX) {
                            val wrappedX = ((tx % numTiles) + numTiles) % numTiles
                            val tileScreenX = (widthPx / 2f + ((tx * 256.0 - centerWorldX) * scale)).toFloat()
                            val tileScreenY = (heightPx / 2f + ((ty * 256.0 - centerWorldY) * scale)).toFloat()

                            val tileUrl = MapTileLoader.buildTileUrl(selectedProvider, intZoom, tx, ty)

                            val tileOffsetX = with(density) { tileScreenX.toDp() }
                            val tileOffsetY = with(density) { tileScreenY.toDp() }

                            androidx.compose.runtime.key("$intZoom-$wrappedX-$ty-${selectedProvider.name}") {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(tileUrl)
                                        .size(coil.size.Size.ORIGINAL)
                                        .memoryCacheKey("$intZoom-$wrappedX-$ty-${selectedProvider.name}")
                                        .diskCacheKey("$intZoom-$wrappedX-$ty-${selectedProvider.name}")
                                        .crossfade(false)
                                        .build(),
                                    imageLoader = tileImageLoader,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier
                                        .offset(x = tileOffsetX, y = tileOffsetY)
                                        .size(tileSizeDp)
                                )
                            }
                        }
                    }
                }
            }

            // LAYER 2: Overlay Canvas (Real GPS Beacon, Mock Pin, Waypoints, Heading Vector, Scale Bar)
            Canvas(modifier = Modifier.fillMaxSize()) {
                try {
                    val realPos = realCoordinates?.let { latLngToScreen(it.latitude, it.longitude) }
                    val mockPos = latLngToScreen(coordinates.latitude, coordinates.longitude)

                    // 1. Draw Connecting Distance Line if both exist
                    if (realPos != null && realPos.x.isFinite() && realPos.y.isFinite() && mockPos.x.isFinite() && mockPos.y.isFinite()) {
                        val distKm = calculateDistanceKm(
                            realCoordinates.latitude, realCoordinates.longitude,
                            coordinates.latitude, coordinates.longitude
                        )
                        drawLine(
                            color = BluePrimary.copy(alpha = 0.6f),
                            start = realPos,
                            end = mockPos,
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )

                        // Draw distance badge between real & mock
                        val midPoint = Offset((realPos.x + mockPos.x) / 2f, (realPos.y + mockPos.y) / 2f)
                        val distLabel = if (distKm < 1.0) {
                            String.format(Locale.US, "Δ %.0f m", distKm * 1000.0)
                        } else {
                            String.format(Locale.US, "Δ %.2f km", distKm)
                        }

                        val badgeLayout = textMeasurer.measure(
                            text = distLabel,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Slate900
                            )
                        )
                        drawRoundRect(
                            color = PureWhite.copy(alpha = 0.9f),
                            topLeft = Offset(midPoint.x - badgeLayout.size.width / 2f - 6f, midPoint.y - badgeLayout.size.height / 2f - 4f),
                            size = androidx.compose.ui.geometry.Size(
                                badgeLayout.size.width + 12f,
                                badgeLayout.size.height + 8f
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Fill
                        )
                        drawRoundRect(
                            color = Slate300,
                            topLeft = Offset(midPoint.x - badgeLayout.size.width / 2f - 6f, midPoint.y - badgeLayout.size.height / 2f - 4f),
                            size = androidx.compose.ui.geometry.Size(
                                badgeLayout.size.width + 12f,
                                badgeLayout.size.height + 8f
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            text = distLabel,
                            topLeft = Offset(midPoint.x - badgeLayout.size.width / 2f, midPoint.y - badgeLayout.size.height / 2f),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Slate900
                            )
                        )
                    }

                    // 2. Draw Google Maps Driving Navigation Route Polyline & Markers
                    if (routeWaypoints.isNotEmpty()) {
                        if (routeWaypoints.size >= 2) {
                            val path = Path()
                            val validScreenPoints = mutableListOf<Offset>()
                            var firstPointSet = false

                            routeWaypoints.forEach { wp ->
                                val pt = latLngToScreen(wp.latitude, wp.longitude)
                                if (pt.x.isFinite() && pt.y.isFinite()) {
                                    validScreenPoints.add(pt)
                                    if (!firstPointSet) {
                                        path.moveTo(pt.x, pt.y)
                                        firstPointSet = true
                                    } else {
                                        path.lineTo(pt.x, pt.y)
                                    }
                                }
                            }

                            if (firstPointSet) {
                                // Layer A: White / Shadow halo casing
                                drawPath(
                                    path = path,
                                    color = PureWhite,
                                    style = Stroke(
                                        width = 8.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                                // Layer B: Google Maps Navigation Blue Casing
                                drawPath(
                                    path = path,
                                    color = Color(0xFF1557B0),
                                    style = Stroke(
                                        width = 6.5.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                                // Layer C: Primary Navigation Blue Polyline
                                drawPath(
                                    path = path,
                                    color = Color(0xFF1A73E8),
                                    style = Stroke(
                                        width = 5.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                                // Layer D: Center Inner Highlight Ribbon
                                drawPath(
                                    path = path,
                                    color = Color(0xFF8AB4F8),
                                    style = Stroke(
                                        width = 1.8.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )

                                // Layer E: Draw Direction Chevrons / Arrows along road segments
                                for (i in 0 until validScreenPoints.size - 1) {
                                    val p1 = validScreenPoints[i]
                                    val p2 = validScreenPoints[i + 1]
                                    val segDx = p2.x - p1.x
                                    val segDy = p2.y - p1.y
                                    val segDist = Math.hypot(segDx.toDouble(), segDy.toDouble()).toFloat()

                                    if (segDist > 30f) {
                                        val midX = (p1.x + p2.x) / 2f
                                        val midY = (p1.y + p2.y) / 2f
                                        val angleRad = Math.atan2(segDy.toDouble(), segDx.toDouble()).toFloat()

                                        val arrowLen = 5.dp.toPx()
                                        val leftAngle = angleRad + Math.toRadians(150.0).toFloat()
                                        val rightAngle = angleRad - Math.toRadians(150.0).toFloat()

                                        val leftPt = Offset(
                                            midX + (arrowLen * Math.cos(leftAngle.toDouble())).toFloat(),
                                            midY + (arrowLen * Math.sin(leftAngle.toDouble())).toFloat()
                                        )
                                        val rightPt = Offset(
                                            midX + (arrowLen * Math.cos(rightAngle.toDouble())).toFloat(),
                                            midY + (arrowLen * Math.sin(rightAngle.toDouble())).toFloat()
                                        )

                                        drawLine(
                                            color = PureWhite,
                                            start = Offset(midX, midY),
                                            end = leftPt,
                                            strokeWidth = 2.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                        drawLine(
                                            color = PureWhite,
                                            start = Offset(midX, midY),
                                            end = rightPt,
                                            strokeWidth = 2.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                            }
                        }

                        val isDenseRoadRoute = routeWaypoints.size > 15

                        // Draw Markers: Start (Green), Intermediate Turns, and Destination (Red)
                        routeWaypoints.forEachIndexed { index, wp ->
                            val pt = latLngToScreen(wp.latitude, wp.longitude)
                            if (pt.x.isFinite() && pt.y.isFinite()) {
                                when (index) {
                                    0 -> {
                                        // 🟢 START PIN
                                        drawCircle(color = PureWhite, radius = 12.dp.toPx(), center = pt)
                                        drawCircle(color = Color(0xFF1E8E3E), radius = 9.dp.toPx(), center = pt)
                                        drawCircle(color = PureWhite, radius = 4.dp.toPx(), center = pt)

                                        val startLabel = "🟢 START"
                                        val startLayout = textMeasurer.measure(
                                            text = startLabel,
                                            style = TextStyle(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        )
                                        drawRoundRect(
                                            color = PureWhite.copy(alpha = 0.92f),
                                            topLeft = Offset(pt.x - startLayout.size.width / 2f - 4f, pt.y - 22.dp.toPx() - 2f),
                                            size = androidx.compose.ui.geometry.Size(
                                                startLayout.size.width + 8f,
                                                startLayout.size.height + 4f
                                            ),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                        )
                                        drawText(
                                            textMeasurer = textMeasurer,
                                            text = startLabel,
                                            topLeft = Offset(pt.x - startLayout.size.width / 2f, pt.y - 22.dp.toPx()),
                                            style = TextStyle(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E8E3E)
                                            )
                                        )
                                    }
                                    routeWaypoints.size - 1 -> {
                                        // 🔴 DESTINATION PIN (Google Maps Red Pin)
                                        drawCircle(color = PureWhite, radius = 14.dp.toPx(), center = pt)
                                        drawCircle(color = Color(0xFFEA4335), radius = 10.dp.toPx(), center = pt)
                                        drawCircle(color = PureWhite, radius = 4.dp.toPx(), center = pt)

                                        val destLabel = "🏁 DESTINATION"
                                        val destLayout = textMeasurer.measure(
                                            text = destLabel,
                                            style = TextStyle(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEA4335),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        )
                                        drawRoundRect(
                                            color = PureWhite.copy(alpha = 0.92f),
                                            topLeft = Offset(pt.x - destLayout.size.width / 2f - 4f, pt.y - 24.dp.toPx() - 2f),
                                            size = androidx.compose.ui.geometry.Size(
                                                destLayout.size.width + 8f,
                                                destLayout.size.height + 4f
                                            ),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                        )
                                        drawText(
                                            textMeasurer = textMeasurer,
                                            text = destLabel,
                                            topLeft = Offset(pt.x - destLayout.size.width / 2f, pt.y - 24.dp.toPx()),
                                            style = TextStyle(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEA4335)
                                            )
                                        )
                                    }
                                    else -> {
                                        // Intermediate road turn or waypoint
                                        if (isDenseRoadRoute) {
                                            // Subtle turn maneuver dots for dense road-following routes
                                            if (index % 5 == 0) {
                                                drawCircle(color = PureWhite, radius = 4.dp.toPx(), center = pt)
                                                drawCircle(color = Color(0xFF1A73E8), radius = 2.5.dp.toPx(), center = pt)
                                            }
                                        } else {
                                            // Numbered badges for manual waypoints
                                            drawCircle(color = PureWhite, radius = 8.dp.toPx(), center = pt)
                                            drawCircle(color = BluePrimary, radius = 6.dp.toPx(), center = pt)
                                            drawCircle(color = PureWhite, radius = 2.5.dp.toPx(), center = pt)

                                            val wpText = "${index + 1}"
                                            val wpLayout = textMeasurer.measure(
                                                text = wpText,
                                                style = TextStyle(
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate900,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            )
                                            drawText(
                                                textMeasurer = textMeasurer,
                                                text = wpText,
                                                topLeft = Offset(pt.x - wpLayout.size.width / 2f, pt.y - 16.dp.toPx()),
                                                style = TextStyle(
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate900
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Draw Real Device GPS Marker (Emerald pulsing radar)
                    if (realPos != null && realPos.x.isFinite() && realPos.y.isFinite()) {
                        // Pulsing radar wave
                        drawCircle(
                            color = EmeraldSuccess.copy(alpha = pulseAlpha),
                            radius = pulseRadius * density.density,
                            center = realPos,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawCircle(
                            color = EmeraldGlow.copy(alpha = 0.20f),
                            radius = 10.dp.toPx(),
                            center = realPos
                        )
                        drawCircle(
                            color = PureWhite,
                            radius = 6.5.dp.toPx(),
                            center = realPos
                        )
                        drawCircle(
                            color = EmeraldSuccess,
                            radius = 4.5.dp.toPx(),
                            center = realPos
                        )

                        // Real device label badge
                        val realText = "REAL GPS"
                        val realLayout = textMeasurer.measure(
                            text = realText,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 7.5.sp,
                                color = EmeraldSuccess
                            )
                        )
                        drawRoundRect(
                            color = PureWhite.copy(alpha = 0.92f),
                            topLeft = Offset(realPos.x - realLayout.size.width / 2f - 4f, realPos.y + 8.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(
                                realLayout.size.width + 8f,
                                realLayout.size.height + 3f
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            text = realText,
                            topLeft = Offset(realPos.x - realLayout.size.width / 2f, realPos.y + 9.5.dp.toPx()),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 7.5.sp,
                                color = EmeraldSuccess
                            )
                        )
                    }

                    // 4. Draw Mock Target GPS Pin (Compact, High-Precision Royal Blue Pin with Heading Indicator & Clean Badge)
                    if (mockPos.x.isFinite() && mockPos.y.isFinite()) {
                        val safeBearing = if (coordinates.bearing.isNaN()) 0f else coordinates.bearing
                        val bearingRad = Math.toRadians(safeBearing.toDouble() - 90.0)
                        val coneLength = 26.dp.toPx()
                        val coneAngle = Math.toRadians(30.0)

                        val coneLeftX = mockPos.x + (cos(bearingRad - coneAngle) * coneLength).toFloat()
                        val coneLeftY = mockPos.y + (sin(bearingRad - coneAngle) * coneLength).toFloat()
                        val coneRightX = mockPos.x + (cos(bearingRad + coneAngle) * coneLength).toFloat()
                        val coneRightY = mockPos.y + (sin(bearingRad + coneAngle) * coneLength).toFloat()

                        if (coneLeftX.isFinite() && coneLeftY.isFinite() && coneRightX.isFinite() && coneRightY.isFinite()) {
                            // Heading beam / Field of view cone
                            val conePath = Path().apply {
                                moveTo(mockPos.x, mockPos.y)
                                lineTo(coneLeftX, coneLeftY)
                                arcTo(
                                    rect = androidx.compose.ui.geometry.Rect(
                                        mockPos.x - coneLength, mockPos.y - coneLength,
                                        mockPos.x + coneLength, mockPos.y + coneLength
                                    ),
                                    startAngleDegrees = (safeBearing - 90f - 30f),
                                    sweepAngleDegrees = 60f,
                                    forceMoveTo = false
                                )
                                close()
                            }
                            drawPath(
                                path = conePath,
                                brush = Brush.radialGradient(
                                    colors = listOf(BluePrimary.copy(alpha = 0.40f), BluePrimary.copy(alpha = 0.0f)),
                                    center = mockPos,
                                    radius = coneLength
                                )
                            )
                        }

                        // Animated pulsing radar beacon ring
                        drawCircle(
                            color = CyanAccent.copy(alpha = mockPulseAlpha),
                            radius = mockPulseRadius * density.density,
                            center = mockPos,
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Mock Pin Multi-Layer High-Visibility Target Beacon (Compact, crisp 15dp profile)
                        drawCircle(
                            color = BluePrimary.copy(alpha = 0.22f),
                            radius = 12.dp.toPx(),
                            center = mockPos
                        )
                        drawCircle(
                            color = PureWhite,
                            radius = 7.5.dp.toPx(),
                            center = mockPos
                        )
                        drawCircle(
                            color = BluePrimary,
                            radius = 5.dp.toPx(),
                            center = mockPos
                        )
                        drawCircle(
                            color = PureWhite,
                            radius = 2.dp.toPx(),
                            center = mockPos
                        )

                        // Heading directional pointer tick
                        val tickTipX = mockPos.x + (cos(bearingRad) * 11.dp.toPx()).toFloat()
                        val tickTipY = mockPos.y + (sin(bearingRad) * 11.dp.toPx()).toFloat()
                        if (tickTipX.isFinite() && tickTipY.isFinite()) {
                            drawLine(
                                color = BlueDark,
                                start = mockPos,
                                end = Offset(tickTipX, tickTipY),
                                strokeWidth = 2.2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        // Compact High-Contrast Mock Target Pin Badge
                        val mockText = "📍 MOCK TARGET"
                        val mockLayout = textMeasurer.measure(
                            text = mockText,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 7.5.sp,
                                color = BluePrimary
                            )
                        )
                        drawRoundRect(
                            color = PureWhite.copy(alpha = 0.94f),
                            topLeft = Offset(mockPos.x - mockLayout.size.width / 2f - 4f, mockPos.y - 18.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(
                                mockLayout.size.width + 8f,
                                mockLayout.size.height + 3f
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Fill
                        )
                        drawRoundRect(
                            color = BluePrimary.copy(alpha = 0.45f),
                            topLeft = Offset(mockPos.x - mockLayout.size.width / 2f - 4f, mockPos.y - 18.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(
                                mockLayout.size.width + 8f,
                                mockLayout.size.height + 3f
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            text = mockText,
                            topLeft = Offset(mockPos.x - mockLayout.size.width / 2f, mockPos.y - 16.5.dp.toPx()),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 7.5.sp,
                                color = BluePrimary
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Prevent any unexpected draw-time math exception from crashing the app
                }
            }
        }

        // OFF-SCREEN TARGET PIN RADAR INDICATOR (Ensures target pin is NEVER lost when panning or zooming)
        val mockScreenPos = latLngToScreen(coordinates.latitude, coordinates.longitude)
        val isMockOffScreen = mockScreenPos.x < 15f || mockScreenPos.x > (widthPx - 15f) ||
                mockScreenPos.y < 15f || mockScreenPos.y > (heightPx - 15f)

        if (isMockOffScreen) {
            val distOffScreenKm = calculateDistanceKm(centerLat, centerLng, coordinates.latitude, coordinates.longitude)
            val dLng = ((coordinates.longitude - centerLng + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
            val dLat = coordinates.latitude - centerLat
            val angleDeg = Math.toDegrees(kotlin.math.atan2(dLng, dLat)).toFloat()

            Surface(
                color = PureWhite.copy(alpha = 0.96f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, BluePrimary),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 58.dp)
                    .clickable {
                        centerLat = coordinates.latitude
                        centerLng = coordinates.longitude
                        currentOnMapCenterChanged?.invoke(centerLat, centerLng)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(BluePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = "Target Direction",
                            tint = PureWhite,
                            modifier = Modifier
                                .size(13.dp)
                                .rotate(angleDeg - 45f)
                        )
                    }
                    Text(
                        text = if (distOffScreenKm < 1.0) {
                            String.format(Locale.US, "📍 Target Pin: %.0fm • Tap to Center", distOffScreenKm * 1000)
                        } else {
                            String.format(Locale.US, "📍 Target Pin: %.1fkm • Tap to Center", distOffScreenKm)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BlueDark,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // TOP-LEFT: Compass Heading & Reset North Button
        Surface(
            color = PureWhite.copy(alpha = 0.95f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            shadowElevation = 2.dp,
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
                .clickable {
                    onResetBearing?.invoke()
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Compass Reset North",
                    tint = BluePrimary,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(coordinates.bearing)
                )
                Text(
                    text = "${coordinates.bearing.toInt()}° North (Reset)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // TOP-RIGHT: Telemetry HUD (Coordinates, Accuracy, Speed)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            CoordinatesHud(coordinates = coordinates, engineConfig = engineConfig, zoomLevel = zoomLevel)
        }

        // BOTTOM-RIGHT: Map Action Buttons (Zoom In/Out, Center Mock, Center Real, Layers)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Map Tile Layers Switcher Button
            Box {
                IconButton(
                    onClick = { showLayersMenu = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = PureWhite,
                        contentColor = Slate700
                    ),
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(3.dp, CircleShape)
                        .border(1.dp, Slate200, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Map Layers",
                        tint = BluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showLayersMenu,
                    onDismissRequest = { showLayersMenu = false },
                    modifier = Modifier.background(PureWhite)
                ) {
                    MapTileProvider.values().forEach { provider ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = provider.displayName,
                                    color = if (selectedProvider == provider) BluePrimary else Slate900,
                                    fontWeight = if (selectedProvider == provider) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                selectedProvider = provider
                                showLayersMenu = false
                            }
                        )
                    }
                }
            }

            // Zoom In (+)
            IconButton(
                onClick = {
                    val newZoom = (zoomLevel + 1f).coerceAtMost(22f)
                    // If target pin is near center or on screen, gently anchor toward the mock pin so zooming in focuses it
                    val dLat = Math.abs(coordinates.latitude - centerLat)
                    val dLng = Math.abs(coordinates.longitude - centerLng)
                    if (dLat < 0.05 && dLng < 0.05) {
                        centerLat = centerLat * 0.3 + coordinates.latitude * 0.7
                        centerLng = centerLng * 0.3 + coordinates.longitude * 0.7
                        currentOnMapCenterChanged?.invoke(centerLat, centerLng)
                    }
                    zoomLevel = newZoom
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = PureWhite,
                    contentColor = Slate800
                ),
                modifier = Modifier
                    .size(42.dp)
                    .shadow(3.dp, CircleShape)
                    .border(1.dp, Slate200, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }

            // Live Zoom Level Indicator Badge
            Surface(
                color = Slate900.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(vertical = 1.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.0fx", zoomLevel),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Zoom Out (-)
            IconButton(
                onClick = { zoomLevel = (zoomLevel - 1f).coerceAtLeast(2f) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = PureWhite,
                    contentColor = Slate800
                ),
                modifier = Modifier
                    .size(42.dp)
                    .shadow(3.dp, CircleShape)
                    .border(1.dp, Slate200, CircleShape)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }

            // Center on Real GPS
            IconButton(
                onClick = {
                    onRefreshRealLocation?.invoke()
                    realCoordinates?.let {
                        centerLat = it.latitude
                        centerLng = it.longitude
                        currentOnMapCenterChanged?.invoke(centerLat, centerLng)
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = PureWhite,
                    contentColor = EmeraldSuccess
                ),
                modifier = Modifier
                    .size(42.dp)
                    .shadow(3.dp, CircleShape)
                    .border(1.dp, Slate200, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Center on Real Location",
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Center on Mock Target Pin
            IconButton(
                onClick = {
                    centerLat = coordinates.latitude
                    centerLng = coordinates.longitude
                    currentOnMapCenterChanged?.invoke(centerLat, centerLng)
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = PureWhite,
                    contentColor = BluePrimary
                ),
                modifier = Modifier
                    .size(42.dp)
                    .shadow(3.dp, CircleShape)
                    .border(1.dp, Slate200, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Center on Mock Target",
                    tint = BluePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Set Mock Pin to Current Map Center
            IconButton(
                onClick = {
                    currentOnCoordinatesChanged(centerLat, centerLng, "MAP_CENTER_SET")
                    android.widget.Toast.makeText(context, "📍 Mock Target Pin set to Map Center", android.widget.Toast.LENGTH_SHORT).show()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = BluePrimary,
                    contentColor = PureWhite
                ),
                modifier = Modifier
                    .size(42.dp)
                    .shadow(4.dp, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PinDrop,
                    contentDescription = "Set Mock Pin to Map Center",
                    tint = PureWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Snap Mock Pin to Real GPS
            if (realCoordinates != null) {
                IconButton(
                    onClick = {
                        onCoordinatesChanged(realCoordinates.latitude, realCoordinates.longitude, "SNAP_TO_REAL_GPS")
                        centerLat = realCoordinates.latitude
                        centerLng = realCoordinates.longitude
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = PureWhite,
                        contentColor = BluePrimary
                    ),
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(3.dp, CircleShape)
                        .border(1.dp, Slate200, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Snap Mock to Real GPS",
                        tint = BluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // BOTTOM BAR: Live Status Strip (Device GPS vs Mock GPS)
        Surface(
            color = PureWhite.copy(alpha = 0.95f),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess)
                    )
                    Text(
                        text = if (realCoordinates != null) {
                            String.format(Locale.US, "GPS: %.4f, %.4f", realCoordinates.latitude, realCoordinates.longitude)
                        } else {
                            "Locating Device GPS..."
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Slate700
                        )
                    )
                }

                Text(
                    text = String.format(Locale.US, "CENTER: %.4f, %.4f", centerLat, centerLng),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Slate600
                    )
                )
            }
        }
    }
}

private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    if (lat1.isNaN() || lon1.isNaN() || lat2.isNaN() || lon2.isNaN()) return 0.0
    val r = 6371.0 // Radius of earth in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = (sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)).coerceIn(0.0, 1.0)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1.0 - a))
    val res = r * c
    return if (res.isNaN()) 0.0 else res
}
