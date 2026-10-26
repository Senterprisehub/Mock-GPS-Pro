package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberSubtle
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.BlueSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.MockGpsViewModel
import java.util.Locale

@Composable
fun PermissionsSettingsScreen(
    viewModel: MockGpsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissions by viewModel.permissionsState.collectAsState()
    val engineConfig by viewModel.engineConfig.collectAsState()

    // Activity result launcher for permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissions()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("permissions_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "System Permissions & Diagnostics",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                )
            }
            Text(
                text = "Android requires explicit authorization for mock location providers, background services, and telemetry.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Slate600,
                    fontSize = 12.sp
                )
            )
        }

        // Developer Options Banner
        val isMockSelected = permissions.mockLocationAppSelected
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isMockSelected) EmeraldSubtle else AmberSubtle
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isMockSelected) EmeraldSuccess.copy(alpha = 0.5f) else AmberWarning.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isMockSelected) EmeraldSuccess.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMockSelected) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isMockSelected) EmeraldSuccess else AmberWarning,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isMockSelected) "Mock Location App Active" else "Mock Location App Not Selected",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isMockSelected) EmeraldSuccess else AmberWarning
                            )
                        )
                        Text(
                            text = if (isMockSelected) {
                                "Mock GPS Pro is configured as the active mock provider in Android Developer Options."
                            } else {
                                "To spoof location, select 'Mock GPS Pro' under Developer Options -> Select mock location app."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate700,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open Developer Options. Please open Settings manually.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMockSelected) Slate900 else AmberWarning,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Developer Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Permission Cards List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "SYSTEM PERMISSION STATUS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Slate600,
                    letterSpacing = 0.8.sp
                )
            )

            // 1. Fine Location
            PermissionCard(
                title = "Precise Location Access",
                permissionCode = "ACCESS_FINE_LOCATION",
                icon = Icons.Default.MyLocation,
                isGranted = permissions.fineLocationGranted,
                onRequest = {
                    permissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )

            // 2. Background Location
            PermissionCard(
                title = "Background Location",
                permissionCode = "ACCESS_BACKGROUND_LOCATION",
                icon = Icons.Default.CompassCalibration,
                isGranted = permissions.bgLocationGranted,
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        permissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                    } else {
                        Toast.makeText(context, "Granted automatically on this Android version", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // 3. System Overlay
            PermissionCard(
                title = "Display Over Other Apps",
                permissionCode = "SYSTEM_ALERT_WINDOW",
                icon = Icons.Default.DisplaySettings,
                isGranted = permissions.overlayPermissionGranted,
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Please enable 'Display Over Other Apps' in Settings.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            // 4. Foreground Service
            PermissionCard(
                title = "Foreground Location Service",
                permissionCode = "FOREGROUND_SERVICE_LOCATION",
                icon = Icons.Default.Memory,
                isGranted = permissions.fgsPermissionGranted,
                onRequest = {
                    Toast.makeText(context, "Foreground service is enabled", Toast.LENGTH_SHORT).show()
                }
            )

            // 5. Battery Optimization
            PermissionCard(
                title = "Ignore Battery Optimization",
                permissionCode = "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
                icon = Icons.Default.BatteryChargingFull,
                isGranted = permissions.batteryOptimizationIgnored,
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Please configure battery optimization in system settings.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
        }

        // Untraceable & Mock Engine Fine-Tuning Card
        Card(
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Anti-Detection & Untraceable Engine",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                }

                // Jitter Randomization
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Satellite Drift / Signal Jitter",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "Injects Gaussian micro-fluctuations so apps cannot detect static fake coordinates",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                        Switch(
                            checked = engineConfig.jitterEnabled,
                            onCheckedChange = { isChecked ->
                                try {
                                    viewModel.updateConfig(engineConfig.copy(jitterEnabled = isChecked))
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = BluePrimary,
                                uncheckedTrackColor = Slate200
                            )
                        )
                    }

                    // Jitter Radius Slider - Always accessible
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Jitter Radius:", fontSize = 11.sp, color = Slate600)
                        Text(
                            text = String.format(Locale.US, "± %.1f meters %s", engineConfig.jitterRadiusMeters, if (engineConfig.jitterEnabled) "(ACTIVE)" else "(DISABLED)"),
                            fontSize = 11.sp,
                            color = if (engineConfig.jitterEnabled) BluePrimary else Slate600,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = engineConfig.jitterRadiusMeters,
                        onValueChange = { newRadius ->
                            try {
                                viewModel.updateConfig(engineConfig.copy(jitterEnabled = true, jitterRadiusMeters = newRadius))
                            } catch (e: Exception) {
                                // Ignore
                            }
                        },
                        valueRange = 0.5f..15.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = BluePrimary,
                            activeTrackColor = BluePrimary,
                            inactiveTrackColor = Slate200
                        )
                    )
                }

                // Update Interval
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Update Interval (Frequency)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "Rate at which location callbacks are sent to providers",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                        Text(
                            text = "${engineConfig.updateIntervalMs} ms",
                            fontSize = 12.sp,
                            color = BluePrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = engineConfig.updateIntervalMs.toFloat(),
                        onValueChange = { viewModel.updateConfig(engineConfig.copy(updateIntervalMs = it.toLong())) },
                        valueRange = 100f..2000f,
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = BluePrimary,
                            activeTrackColor = BluePrimary,
                            inactiveTrackColor = Slate200
                        )
                    )
                }

                // Simulated Satellite Count
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Simulated Satellites in View",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "${engineConfig.simulatedSatellitesCount} Sats (HDOP 0.9)",
                            fontSize = 12.sp,
                            color = EmeraldSuccess,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = engineConfig.simulatedSatellitesCount.toFloat(),
                        onValueChange = { viewModel.updateConfig(engineConfig.copy(simulatedSatellitesCount = it.toInt())) },
                        valueRange = 4f..28f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldSuccess,
                            activeTrackColor = EmeraldSuccess,
                            inactiveTrackColor = Slate200
                        )
                    )
                }

                // NMEA Telemetry Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NMEA 0183 Sentence Spoofing",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Generates authentic \$GPGGA & \$GPRMC sentences for hardware telemetry bypass",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                    Switch(
                        checked = engineConfig.generateNmea,
                        onCheckedChange = { viewModel.updateConfig(engineConfig.copy(generateNmea = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = BluePrimary,
                            uncheckedTrackColor = Slate200
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    permissionCode: String,
    icon: ImageVector,
    isGranted: Boolean,
    onRequest: () -> Unit
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BlueSubtle)
                        .border(1.dp, BluePrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                    Text(
                        text = permissionCode,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Slate600
                        )
                    )
                }
            }

            // Status Badge / Action Button
            if (isGranted) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(EmeraldSubtle)
                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "GRANTED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess,
                            fontSize = 10.sp
                        )
                    )
                }
            } else {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Grant", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
