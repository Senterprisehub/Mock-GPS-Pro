package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.CityCatalog
import com.example.ui.components.InteractiveMapView
import com.example.ui.components.VirtualJoystick
import com.example.ui.theme.BlueDark
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.BlueSubtle
import com.example.ui.theme.CyanAccent
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
import com.example.ui.viewmodel.MockGpsViewModel
import java.util.Locale

@Composable
fun MapJoystickScreen(
    viewModel: MockGpsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val coordinates by viewModel.coordinates.collectAsState()
    val realCoordinates by viewModel.realCoordinates.collectAsState()
    val isMocking by viewModel.isMocking.collectAsState()
    val engineConfig by viewModel.engineConfig.collectAsState()
    val activeRoute by viewModel.activeRoute.collectAsState()
    val isRoutePlaying by viewModel.isRoutePlaying.collectAsState()
    val routeProgress by viewModel.routeProgress.collectAsState()
    val isCreatingRoute by viewModel.isCreatingRoute.collectAsState()
    val newRouteWaypoints by viewModel.newRouteWaypoints.collectAsState()
    val currentAreaName by viewModel.currentAreaName.collectAsState()
    val mapTouchPanSpeed by viewModel.mapTouchPanSpeed.collectAsState()

    var showFavoriteDialog by remember { mutableStateOf(false) }
    var favoriteTitleInput by remember { mutableStateOf("") }
    var showSpeedSliderPopup by remember { mutableStateOf(false) }
    var isJoystickVisible by remember { mutableStateOf(false) }

    // Route drawing save dialog
    var showSaveDrawnRouteDialog by remember { mutableStateOf(false) }
    var drawnRouteTitleInput by remember { mutableStateOf("") }
    var drawnRouteDescInput by remember { mutableStateOf("") }

    // Track active map viewport center for current area detection
    var mapCenterLat by remember { mutableDoubleStateOf(coordinates.latitude) }
    var mapCenterLng by remember { mutableDoubleStateOf(coordinates.longitude) }

    val currentCatalogArea = remember(mapCenterLat, mapCenterLng) {
        CityCatalog.getNearestArea(mapCenterLat, mapCenterLng)
    }

    BackHandler(enabled = showFavoriteDialog) {
        showFavoriteDialog = false
    }
    BackHandler(enabled = showSpeedSliderPopup) {
        showSpeedSliderPopup = false
    }
    BackHandler(enabled = showSaveDrawnRouteDialog) {
        showSaveDrawnRouteDialog = false
    }

    // Trigger reverse geocoding when map center moves
    LaunchedEffect(mapCenterLat, mapCenterLng) {
        viewModel.reverseGeocodeMapCenter(mapCenterLat, mapCenterLng)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .testTag("map_joystick_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Controls Bar (Location Badge, Speed Controller, Bookmark, In-Map Joystick Toggle)
            Surface(
                color = PureWhite,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200)
                    .zIndex(20f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Current Map Location Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BlueSubtle)
                                .border(1.dp, BluePrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable {
                                    Toast.makeText(context, "Location: $currentAreaName", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = "Current Area",
                                    tint = BluePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (currentAreaName != "Detecting area...") "📍 $currentAreaName" else "${currentCatalogArea.flag} ${currentCatalogArea.name}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = BlueDark,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        // Bookmark Place Quick Action Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate100)
                                .border(1.dp, Slate200, RoundedCornerShape(8.dp))
                                .clickable {
                                    favoriteTitleInput = if (currentAreaName != "Detecting area..." && currentAreaName.isNotBlank()) currentAreaName else "Bookmark Spot"
                                    showFavoriteDialog = true
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = "Bookmark",
                                    tint = BluePrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Bookmark",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate800,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Current Speed Pill (Clickable to expand speed slider popup)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (showSpeedSliderPopup) BluePrimary else Slate100)
                                .border(1.dp, if (showSpeedSliderPopup) BluePrimary else Slate200, RoundedCornerShape(8.dp))
                                .clickable { showSpeedSliderPopup = !showSpeedSliderPopup }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Speed",
                                    tint = if (showSpeedSliderPopup) PureWhite else BluePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${coordinates.speedKmh.toInt()} km/h",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (showSpeedSliderPopup) PureWhite else BluePrimary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // In-Map Joystick Checkbox Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate100)
                                .border(1.dp, Slate200, RoundedCornerShape(8.dp))
                                .padding(end = 8.dp)
                        ) {
                            Checkbox(
                                checked = isJoystickVisible,
                                onCheckedChange = { isJoystickVisible = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = BluePrimary,
                                    uncheckedColor = Slate400
                                ),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Joystick",
                                fontSize = 11.sp,
                                color = Slate700,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Expandable Speed Panel with Quick Options & Continuous Slider
                    AnimatedVisibility(visible = showSpeedSliderPopup) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate50)
                                .border(1.dp, Slate200, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Map Movement Speed:", fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${coordinates.speedKmh.toInt()} km/h",
                                    fontSize = 11.sp,
                                    color = BluePrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Preset Speed Mode Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val speedPresets = listOf(
                                    Pair("🚶 5 km/h", 5f),
                                    Pair("🏃 12 km/h", 12f),
                                    Pair("🚴 25 km/h", 25f),
                                    Pair("🛵 45 km/h", 45f),
                                    Pair("🚗 80 km/h", 80f),
                                    Pair("⚡ 120 km/h", 120f)
                                )
                                speedPresets.forEach { (label, speedVal) ->
                                    val isSelected = Math.abs(coordinates.speedKmh - speedVal) < 2f
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) BluePrimary else PureWhite)
                                            .border(1.dp, if (isSelected) BluePrimary else Slate300, RoundedCornerShape(6.dp))
                                            .clickable { viewModel.updateSpeed(speedVal) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            color = if (isSelected) PureWhite else Slate700,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Continuous Speed Slider
                            Slider(
                                value = coordinates.speedKmh,
                                onValueChange = { viewModel.updateSpeed(it) },
                                valueRange = 1f..150f,
                                colors = SliderDefaults.colors(
                                    thumbColor = BluePrimary,
                                    activeTrackColor = BluePrimary,
                                    inactiveTrackColor = Slate200
                                )
                            )
                        }
                    }
                }
            }

            // Map Area with Floating HUD & In-Map Joystick Overlays
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Interactive Real Tile Map
                val displayWaypoints = if (isCreatingRoute) newRouteWaypoints else (activeRoute?.waypoints ?: emptyList())

                InteractiveMapView(
                    coordinates = coordinates,
                    realCoordinates = realCoordinates,
                    engineConfig = engineConfig,
                    onCoordinatesChanged = { lat, lng, reason ->
                        viewModel.updateCoordinates(lat, lng, reason)
                    },
                    onRefreshRealLocation = {
                        viewModel.locateCurrentDevice(updatePinIfStopped = true) { lat, lng ->
                            mapCenterLat = lat
                            mapCenterLng = lng
                        }
                        Toast.makeText(context, "📍 Centering on Current Location", Toast.LENGTH_SHORT).show()
                    },
                    onMapCenterChanged = { lat, lng ->
                        mapCenterLat = lat
                        mapCenterLng = lng
                    },
                    routeWaypoints = displayWaypoints,
                    isCreatingRoute = isCreatingRoute,
                    onMapTapForWaypoint = { lat, lng ->
                        viewModel.addWaypointToNewRoute(lat, lng)
                    },
                    onResetBearing = {
                        viewModel.updateBearing(0f)
                        Toast.makeText(context, "🧭 Compass reset to North (0°)", Toast.LENGTH_SHORT).show()
                    },
                    touchPanSpeedMultiplier = mapTouchPanSpeed,
                    modifier = Modifier.fillMaxSize()
                )

                // Route Recording Active Floating Banner (Complete with Waypoint Count, Undo, Save, and Cancel)
                if (isCreatingRoute) {
                    Surface(
                        color = PureWhite,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 6.dp,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, BluePrimary),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "✏️ Drawing Mode: Tap map to add waypoints (${newRouteWaypoints.size} placed)",
                                fontSize = 11.sp,
                                color = BluePrimary,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Undo last point
                                if (newRouteWaypoints.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            viewModel.removeWaypoint(newRouteWaypoints.size - 1)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate800),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Undo", fontSize = 10.sp)
                                    }
                                }

                                // Save Route
                                Button(
                                    onClick = {
                                        if (newRouteWaypoints.size >= 2) {
                                            drawnRouteTitleInput = "Custom Route ${System.currentTimeMillis() % 1000}"
                                            drawnRouteDescInput = "${newRouteWaypoints.size} Waypoints"
                                            showSaveDrawnRouteDialog = true
                                        } else {
                                            Toast.makeText(context, "Please tap at least 2 points on the map!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (newRouteWaypoints.size >= 2) EmeraldSuccess else Slate300),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Save Route", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Route", fontSize = 10.sp)
                                }

                                // Cancel Drawing
                                Button(
                                    onClick = { viewModel.cancelRouteCreation() },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // Active Road Navigation HUD Banner (Google Maps Navigation Style)
                if (activeRoute != null && !isCreatingRoute) {
                    val currentNavRoute = activeRoute!!
                    Surface(
                        color = PureWhite,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, BluePrimary),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp, start = 12.dp, end = 12.dp)
                            .fillMaxWidth(0.94f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(BluePrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Navigation,
                                            contentDescription = "Navigating",
                                            tint = PureWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = currentNavRoute.title,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900,
                                                fontSize = 12.sp
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f km", currentNavRoute.distanceKm)} • ${currentNavRoute.waypoints.size} road points • ${if (isRoutePlaying) "Driving..." else "Paused"}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isRoutePlaying) EmeraldSuccess else Slate600,
                                                fontWeight = if (isRoutePlaying) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Play / Pause Replay
                                    IconButton(
                                        onClick = {
                                            if (isRoutePlaying) {
                                                viewModel.pauseRouteReplay()
                                            } else {
                                                viewModel.resumeRouteReplay()
                                            }
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = if (isRoutePlaying) Slate100 else BluePrimary,
                                            contentColor = if (isRoutePlaying) Slate800 else PureWhite
                                        ),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isRoutePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isRoutePlaying) "Pause" else "Resume",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Close / Clear Route
                                    IconButton(
                                        onClick = {
                                            viewModel.stopRouteReplay()
                                            Toast.makeText(context, "Navigation route cleared", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = Slate100,
                                            contentColor = Slate600
                                        ),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Stop Navigation",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Progress Bar along road navigation route
                            LinearProgressIndicator(
                                progress = { routeProgress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = BluePrimary,
                                trackColor = Slate200
                            )
                        }
                    }
                }

                // In-Map Virtual Joystick (Positioned directly inside the bottom-start corner of the map)
                if (isJoystickVisible) {
                    VirtualJoystick(
                        onDragStep = { dx, dy, maxR ->
                            viewModel.onJoystickDrag(dx, dy, maxR)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 14.dp, bottom = 44.dp),
                        sizeDp = 130
                    )
                }
            }
        }

        // Save Favorite / Bookmark Dialog
        if (showFavoriteDialog) {
            AlertDialog(
                onDismissRequest = { showFavoriteDialog = false },
                containerColor = PureWhite,
                title = {
                    Text("Bookmark Place", color = Slate900, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = String.format(Locale.US, "Coords: %.6f, %.6f", coordinates.latitude, coordinates.longitude),
                            fontSize = 12.sp,
                            color = Slate600,
                            fontFamily = FontFamily.Monospace
                        )
                        OutlinedTextField(
                            value = favoriteTitleInput,
                            onValueChange = { favoriteTitleInput = it },
                            placeholder = { Text("Name (e.g. My Office, Home, Park)", color = Slate400) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = Slate300
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveCurrentAsFavorite(favoriteTitleInput)
                            showFavoriteDialog = false
                            favoriteTitleInput = ""
                            Toast.makeText(context, "Saved to bookmarks list!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text("Save Bookmark")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFavoriteDialog = false }) {
                        Text("Cancel", color = Slate600)
                    }
                }
            )
        }

        // Save Drawn Route Dialog
        if (showSaveDrawnRouteDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDrawnRouteDialog = false },
                containerColor = PureWhite,
                title = {
                    Text("Save Drawn Route", color = Slate900, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${newRouteWaypoints.size} waypoints recorded on map.",
                            fontSize = 12.sp,
                            color = Slate600
                        )
                        OutlinedTextField(
                            value = drawnRouteTitleInput,
                            onValueChange = { drawnRouteTitleInput = it },
                            placeholder = { Text("Route Title (e.g. Riverwalk Route)", color = Slate400) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = Slate300
                            )
                        )
                        OutlinedTextField(
                            value = drawnRouteDescInput,
                            onValueChange = { drawnRouteDescInput = it },
                            placeholder = { Text("Description (Optional)", color = Slate400) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = Slate300
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveNewRoute(drawnRouteTitleInput, drawnRouteDescInput)
                            showSaveDrawnRouteDialog = false
                            drawnRouteTitleInput = ""
                            drawnRouteDescInput = ""
                            Toast.makeText(context, "Route saved to Routes tab!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                    ) {
                        Text("Save & Complete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDrawnRouteDialog = false }) {
                        Text("Keep Editing", color = Slate600)
                    }
                }
            )
        }
    }
}
