package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.FavoritePlaceEntity
import com.example.data.RouteItem
import com.example.data.Waypoint
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.BlueSubtle
import com.example.ui.theme.EmeraldSubtle
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
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MockGpsViewModel
import java.util.Locale

@Composable
fun RoutesScreen(
    viewModel: MockGpsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coordinates by viewModel.coordinates.collectAsState()
    val activeRoute by viewModel.activeRoute.collectAsState()
    val isRoutePlaying by viewModel.isRoutePlaying.collectAsState()
    val routeProgress by viewModel.routeProgress.collectAsState()
    val speedMultiplier by viewModel.routeSpeedMultiplier.collectAsState()
    val isLooping by viewModel.isLoopingRoute.collectAsState()
    val savedRoutes by viewModel.savedRoutes.collectAsState()
    val favoritePlaces by viewModel.favoritePlaces.collectAsState()
    val mapTouchPanSpeed by viewModel.mapTouchPanSpeed.collectAsState()

    var showGpxImportDialog by remember { mutableStateOf(false) }
    var gpxXmlContent by remember { mutableStateOf("") }
    var gpxRouteName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("routes_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Routes & Saved Bookmarks",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                )
            }
            Text(
                text = "Manage saved places, adjust map movement speed, and draw custom simulation paths.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Slate600,
                    fontSize = 12.sp
                )
            )
        }

        // ==========================================
        // 1. MAP FINGER TOUCH MOVEMENT SPEED CARD (INCREASABLE TOUCH PAN SPEED)
        // ==========================================
        Card(
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1A: Finger Touch Map Movement Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BlueSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = "Map Drag Speed",
                                tint = BluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Map Touch Movement Speed",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            )
                            Text(
                                text = "How far & fast the map glides when dragged by finger",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Slate600,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Live Multiplier Badge
                    Surface(
                        color = BluePrimary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1fx", mapTouchPanSpeed),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Increaser & Decreaser Buttons for Map Touch Movement Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Decrease by -5x
                    Button(
                        onClick = {
                            viewModel.decreaseMapTouchPanSpeed(5f)
                            Toast.makeText(context, "Map drag speed: ${String.format(Locale.US, "%.1fx", (mapTouchPanSpeed - 5f).coerceAtLeast(1f))}", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate800),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("-5x", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Decrease by -1x
                    Button(
                        onClick = {
                            viewModel.decreaseMapTouchPanSpeed(1f)
                            Toast.makeText(context, "Map drag speed: ${String.format(Locale.US, "%.1fx", (mapTouchPanSpeed - 1f).coerceAtLeast(1f))}", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate800),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("-1x", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Increase by +1x
                    Button(
                        onClick = {
                            viewModel.increaseMapTouchPanSpeed(1f)
                            Toast.makeText(context, "Map drag speed: ${String.format(Locale.US, "%.1fx", (mapTouchPanSpeed + 1f).coerceAtMost(25f))}", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueSubtle, contentColor = BluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Text("1x", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Increase by +5x (Fast Increaser)
                    Button(
                        onClick = {
                            viewModel.increaseMapTouchPanSpeed(5f)
                            Toast.makeText(context, "Map drag speed: ${String.format(Locale.US, "%.1fx", (mapTouchPanSpeed + 5f).coerceAtMost(25f))}", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary, contentColor = PureWhite),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Text("5x (Fast)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Continuous Touch Pan Speed Slider
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Slider(
                        value = mapTouchPanSpeed,
                        onValueChange = { viewModel.updateMapTouchPanSpeed(it) },
                        valueRange = 1f..25f,
                        colors = SliderDefaults.colors(
                            thumbColor = BluePrimary,
                            activeTrackColor = BluePrimary,
                            inactiveTrackColor = Slate200
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1.0x (Standard)", style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 10.sp))
                        Text("12.5x (Fast Glide)", style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 10.sp))
                        Text("25.0x (Hyper Speed)", style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 10.sp))
                    }
                }

                // Quick Touch Pan Speed Preset Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val panPresets = listOf(
                        Pair("1x (Standard 1:1)", 1.0f),
                        Pair("2x (Smooth)", 2.0f),
                        Pair("4x (Fast Pan)", 4.0f),
                        Pair("8x (Turbo Glide)", 8.0f),
                        Pair("15x (Ultra Fast)", 15.0f),
                        Pair("25x (Max Speed)", 25.0f)
                    )
                    panPresets.forEach { (label, speedVal) ->
                        val isSelected = Math.abs(mapTouchPanSpeed - speedVal) < 0.5f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) BluePrimary else Slate100)
                                .border(1.dp, if (isSelected) BluePrimary else Slate200, RoundedCornerShape(6.dp))
                                .clickable {
                                    viewModel.updateMapTouchPanSpeed(speedVal)
                                    Toast.makeText(context, "Touch movement speed set to ${label}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) PureWhite else Slate700,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                androidx.compose.material3.HorizontalDivider(color = Slate100, thickness = 1.dp)

                // Section 1B: GPS Travel / Simulation Speed (km/h)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "GPS Simulation Speed",
                                tint = Slate700,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "GPS Joystick / Simulation Speed",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                            )
                            Text(
                                text = "Simulated travel velocity for joystick & route replay",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Slate600,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Current GPS speed digital badge
                    Text(
                        text = "${coordinates.speedKmh.toInt()} km/h",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    )
                }

                // GPS Speed Increaser & Decreaser Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // -10 km/h
                    Button(
                        onClick = {
                            val newSpeed = (coordinates.speedKmh - 10f).coerceAtLeast(1f)
                            viewModel.updateSpeed(newSpeed)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate800),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("-10", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // -2 km/h
                    Button(
                        onClick = {
                            val newSpeed = (coordinates.speedKmh - 2f).coerceAtLeast(1f)
                            viewModel.updateSpeed(newSpeed)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate800),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Text("-2", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // +2 km/h
                    Button(
                        onClick = {
                            val newSpeed = (coordinates.speedKmh + 2f).coerceAtMost(160f)
                            viewModel.updateSpeed(newSpeed)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueSubtle, contentColor = BluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Text("2", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // +10 km/h
                    Button(
                        onClick = {
                            val newSpeed = (coordinates.speedKmh + 10f).coerceAtMost(160f)
                            viewModel.updateSpeed(newSpeed)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = PureWhite),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Text("10", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Continuous Precision GPS Speed Slider
                Slider(
                    value = coordinates.speedKmh,
                    onValueChange = { viewModel.updateSpeed(it) },
                    valueRange = 1f..150f,
                    colors = SliderDefaults.colors(
                        thumbColor = Slate800,
                        activeTrackColor = Slate800,
                        inactiveTrackColor = Slate200
                    )
                )

                // Quick Preset Speed Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presets = listOf(
                        Pair("🚶 Walk (5)", 5f),
                        Pair("🏃 Jog (12)", 12f),
                        Pair("🚴 Cycle (25)", 25f),
                        Pair("🛵 Scooter (45)", 45f),
                        Pair("🚗 Car (80)", 80f),
                        Pair("⚡ High Speed (120)", 120f)
                    )
                    presets.forEach { (label, speedVal) ->
                        val isSelected = Math.abs(coordinates.speedKmh - speedVal) < 2f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Slate800 else Slate100)
                                .border(1.dp, if (isSelected) Slate800 else Slate200, RoundedCornerShape(6.dp))
                                .clickable { viewModel.updateSpeed(speedVal) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) PureWhite else Slate700,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. SAVED BOOKMARK LIST WITH PLACE NAMES
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Bookmarks",
                        tint = BluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "SAVED BOOKMARKS (${favoritePlaces.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Slate600,
                            letterSpacing = 0.8.sp
                        )
                    )
                }

                // Bookmark Current GPS Location button
                TextButton(
                    onClick = {
                        val title = String.format(Locale.US, "Spot #%d", favoritePlaces.size + 1)
                        viewModel.saveCurrentAsFavorite(title)
                        Toast.makeText(context, "Current location saved to bookmarks!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("+ Bookmark Here", fontSize = 11.sp, color = BluePrimary, fontWeight = FontWeight.Bold)
                }
            }

            if (favoritePlaces.isEmpty()) {
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No saved bookmarks yet",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        )
                        Text(
                            text = "Tap the 'Bookmark' button in the Map tab or '+ Bookmark Here' above to save favorite spots.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate600,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    favoritePlaces.forEach { place ->
                        BookmarkPlaceCard(
                            place = place,
                            onTeleport = {
                                viewModel.updateCoordinates(place.latitude, place.longitude, "BOOKMARK_TELEPORT")
                                viewModel.setTab(AppTab.MAP)
                                Toast.makeText(context, "📍 Set target to: ${place.title}", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                viewModel.deleteFavorite(place.id)
                                Toast.makeText(context, "Removed bookmark", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // ==========================================
        // 3. ACTION SHORTCUTS (DRAW ON MAP & IMPORT)
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Draw on Map Action
            Surface(
                color = PureWhite,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        viewModel.startCreatingRoute()
                        viewModel.setTab(AppTab.MAP)
                        Toast.makeText(context, "Drawing Mode: Tap anywhere on the map to add waypoints!", Toast.LENGTH_LONG).show()
                    }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocation,
                        contentDescription = "Draw on Map",
                        tint = BluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Draw on Map",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                    Text(
                        text = "Tap map to add waypoints",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Slate600,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // Import GPX / KML Button
            Surface(
                color = PureWhite,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier
                    .weight(1f)
                    .clickable { showGpxImportDialog = true }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "Import GPX",
                        tint = BluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Import GPX / KML",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                    Text(
                        text = "Load coordinates track",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Slate600,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        // ==========================================
        // 4. ACTIVE ROUTE CONTROLLER CARD
        // ==========================================
        activeRoute?.let { route ->
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NOW REPLAYING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = BluePrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = route.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Slate900,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        // Progress percentage
                        Text(
                            text = "${(routeProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        )
                    }

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { routeProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = BluePrimary,
                        trackColor = Slate200
                    )

                    // Speed Multiplier row & Loop toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Multipliers
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0.5f, 1f, 2f, 5f, 10f).forEach { mult ->
                                val isSelected = speedMultiplier == mult
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) BluePrimary else Slate100)
                                        .clickable { viewModel.setRouteSpeedMultiplier(mult) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${mult}x",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) PureWhite else Slate600,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Loop toggle button
                        IconButton(
                            onClick = { viewModel.toggleLoopRoute() },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isLooping) EmeraldSubtle else Slate100)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Loop,
                                contentDescription = "Loop Route",
                                tint = if (isLooping) EmeraldSuccess else Slate600,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Playback Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isRoutePlaying) viewModel.pauseRouteReplay()
                                else viewModel.startRouteReplay(route)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRoutePlaying) Slate700 else EmeraldSuccess
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isRoutePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isRoutePlaying) "Pause" else "Resume")
                        }

                        Button(
                            onClick = { viewModel.stopRouteReplay() },
                            colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. SAVED CUSTOM ROUTES
        // ==========================================
        if (savedRoutes.isNotEmpty()) {
            Text(
                text = "SAVED CUSTOM ROUTES (${savedRoutes.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Slate600,
                    letterSpacing = 0.8.sp
                )
            )

            savedRoutes.forEach { route ->
                RouteCard(
                    route = route,
                    isPlaying = activeRoute?.id == route.id && isRoutePlaying,
                    onPlay = {
                        viewModel.startRouteReplay(route)
                        viewModel.setTab(AppTab.MAP)
                    },
                    onDelete = {
                        viewModel.deleteSavedRoute(route.id)
                        Toast.makeText(context, "Deleted route", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // ==========================================
        // 6. GPX IMPORT DIALOG
        // ==========================================
        if (showGpxImportDialog) {
            AlertDialog(
                onDismissRequest = { showGpxImportDialog = false },
                containerColor = PureWhite,
                title = {
                    Text("Import Track Coordinates", color = Slate900, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Paste coordinate lines (e.g. 37.7749, -122.4194):",
                            fontSize = 12.sp,
                            color = Slate600
                        )
                        OutlinedTextField(
                            value = gpxRouteName,
                            onValueChange = { gpxRouteName = it },
                            placeholder = { Text("Route Title (e.g. Morning Jog)", color = Slate400) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = Slate300
                            )
                        )
                        OutlinedTextField(
                            value = gpxXmlContent,
                            onValueChange = { gpxXmlContent = it },
                            placeholder = {
                                Text(
                                    "37.7749, -122.4194\n37.7760, -122.4220\n37.7790, -122.4250",
                                    color = Slate400,
                                    fontSize = 11.sp
                                )
                            },
                            minLines = 4,
                            maxLines = 6,
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
                            try {
                                val lines = gpxXmlContent.split("\n")
                                val waypoints = mutableListOf<Waypoint>()
                                lines.forEachIndexed { idx, line ->
                                    val parts = line.split(",").map { it.trim() }
                                    if (parts.size >= 2) {
                                        val lat = parts[0].toDoubleOrNull()
                                        val lng = parts[1].toDoubleOrNull()
                                        if (lat != null && lng != null) {
                                            waypoints.add(Waypoint(lat, lng, "WP $idx", idx))
                                        }
                                    }
                                }
                                if (waypoints.size >= 2) {
                                    viewModel.startCreatingRoute()
                                    waypoints.forEach { wp ->
                                        viewModel.addWaypointToNewRoute(wp.latitude, wp.longitude)
                                    }
                                    viewModel.saveNewRoute(
                                        title = gpxRouteName.ifEmpty { "Imported Track" },
                                        description = "${waypoints.size} Imported Waypoints"
                                    )
                                    showGpxImportDialog = false
                                    gpxXmlContent = ""
                                    gpxRouteName = ""
                                    Toast.makeText(context, "Track imported successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please provide at least 2 valid coordinate lines.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to parse coordinates: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text("Import")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGpxImportDialog = false }) {
                        Text("Cancel", color = Slate600)
                    }
                }
            )
        }
    }
}

@Composable
private fun BookmarkPlaceCard(
    place: FavoritePlaceEntity,
    onTeleport: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BlueSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            fontSize = 13.sp
                        )
                    )
                    Text(
                        text = String.format(Locale.US, "%.5f, %.5f", place.latitude, place.longitude),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Slate600,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onTeleport,
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Go", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Bookmark",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteCard(
    route: RouteItem,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isPlaying) BluePrimary else Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                )
                Text(
                    text = route.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Slate600,
                        fontSize = 11.sp
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Route",
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) EmeraldSuccess else BluePrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPlaying) "Playing" else "Replay")
                }
            }
        }
    }
}
