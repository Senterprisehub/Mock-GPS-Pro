package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MockCoordinates
import com.example.data.MockEngineConfig
import com.example.ui.theme.BlueDark
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.BlueSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.util.Locale

@Composable
fun CoordinatesHud(
    coordinates: MockCoordinates,
    engineConfig: MockEngineConfig? = null,
    zoomLevel: Float? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) } // Default to closed/compact view per user requirement

    val copyCoordinates = {
        val text = String.format(Locale.US, "%.6f, %.6f", coordinates.latitude, coordinates.longitude)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Mock Coordinates", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
    }

    Surface(
        color = PureWhite.copy(alpha = 0.96f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        shadowElevation = 4.dp,
        modifier = modifier
            .animateContentSize()
            .testTag("coordinates_hud")
    ) {
        if (!isExpanded) {
            // COLLAPSED / COMPACT HUD MODE (Clean coordinates and expand trigger, jitter only in expanded mode)
            Row(
                modifier = Modifier
                    .clickable { isExpanded = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
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
                    text = String.format(Locale.US, "%.4f, %.4f", coordinates.latitude, coordinates.longitude),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueDark
                    )
                )
                IconButton(
                    onClick = { isExpanded = true },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand Telemetry HUD",
                        tint = BluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            // EXPANDED FULL HUD MODE
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .width(180.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header with live indicator, copy button, and collapse/close toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Text(
                            text = "TELEMETRY HUD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                letterSpacing = 0.6.sp
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Copy Coordinates Button
                        IconButton(
                            onClick = { copyCoordinates() },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Coordinates",
                                tint = Slate600,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        // Close / Collapse Button
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExpandLess,
                                contentDescription = "Collapse HUD",
                                tint = BluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Lat & Lng & Jitter rows
                HudRow(label = "MOCK LAT:", value = String.format(Locale.US, "%.6f", coordinates.latitude), valueColor = BluePrimary)
                HudRow(label = "MOCK LNG:", value = String.format(Locale.US, "%.6f", coordinates.longitude), valueColor = BluePrimary)
                val jitterVal = if (engineConfig?.jitterEnabled == true) String.format(Locale.US, "± %.1f m", engineConfig.jitterRadiusMeters) else "OFF"
                HudRow(label = "JITTER:", value = jitterVal, valueColor = CyanAccent)
                HudRow(label = "ACCURACY:", value = String.format(Locale.US, "± %.1f m", coordinates.accuracy), valueColor = CyanAccent)
                if (zoomLevel != null) {
                    HudRow(label = "MAP ZOOM:", value = String.format(Locale.US, "%.1f x", zoomLevel), valueColor = Slate700)
                }

                // Speed & Bearing row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(Locale.US, "SPD: %.0f km/h", coordinates.speedKmh),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Slate700
                        )
                    )
                    Text(
                        text = String.format(Locale.US, "HDG: %.0f°", coordinates.bearing),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Slate700
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun HudRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Slate600
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        )
    }
}
