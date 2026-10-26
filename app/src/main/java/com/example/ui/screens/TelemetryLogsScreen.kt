package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LogLevel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.BlueSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PureWhite
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.MockGpsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelemetryLogsScreen(
    viewModel: MockGpsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logs by viewModel.inMemoryLogs.collectAsState()
    val coordinates by viewModel.coordinates.collectAsState()
    val engineConfig by viewModel.engineConfig.collectAsState()
    val isMocking by viewModel.isMocking.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            "PROVIDER" -> logs.filter { it.tag.contains("PROVIDER", ignoreCase = true) }
            "NMEA" -> logs.filter { it.level == LogLevel.NMEA || it.tag.contains("NMEA", ignoreCase = true) }
            "JOYSTICK" -> logs.filter { it.tag.contains("JOYSTICK", ignoreCase = true) || it.tag.contains("PIN", ignoreCase = true) }
            "ROUTES" -> logs.filter { it.tag.contains("ROUTE", ignoreCase = true) }
            "SYSTEM" -> logs.filter { it.tag.contains("SYSTEM", ignoreCase = true) || it.tag.contains("SERVICE", ignoreCase = true) || it.tag.contains("CONFIG", ignoreCase = true) }
            else -> logs
        }
    }

    val copyAllLogs = {
        val text = logs.joinToString("\n") { log ->
            val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(log.timestamp))
            "[$time] [${log.tag}] ${log.message}"
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Mock GPS Logs", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied ${logs.size} log entries", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(12.dp)
            .testTag("telemetry_logs_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Telemetry Stats Row
        Surface(
            color = PureWhite,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryStatItem(label = "STATUS", value = if (isMocking) "ACTIVE" else "IDLE", color = if (isMocking) EmeraldSuccess else Slate600)
                TelemetryStatItem(label = "TOTAL LOGS", value = "${logs.size}", color = BluePrimary)
                TelemetryStatItem(label = "INTERVAL", value = "${engineConfig.updateIntervalMs}ms", color = BluePrimary)
                TelemetryStatItem(label = "JITTER", value = if (engineConfig.jitterEnabled) "±${engineConfig.jitterRadiusMeters}m" else "OFF", color = Slate600)
            }
        }

        // Terminal Top Bar
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
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "SYSTEM TELEMETRY STREAM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        fontSize = 11.sp
                    )
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Copy button
                IconButton(
                    onClick = copyAllLogs,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy All Logs",
                        tint = Slate600,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Clear button
                IconButton(
                    onClick = {
                        viewModel.clearLogs()
                        Toast.makeText(context, "Terminal cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Logs",
                        tint = Slate600,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "PROVIDER", "NMEA", "JOYSTICK", "ROUTES", "SYSTEM").forEach { tag ->
                val isSelected = selectedFilter == tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) BluePrimary else Slate100)
                        .border(1.dp, if (isSelected) BluePrimary else Slate200, RoundedCornerShape(6.dp))
                        .clickable { selectedFilter = tag }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) PureWhite else Slate700,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        // Monospace Terminal Feed
        Surface(
            color = PureWhite,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No logs in this category. Start mocking to capture telemetry.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate600,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(log.timestamp))
                        val tagColor = when (log.level) {
                            LogLevel.SUCCESS -> EmeraldSuccess
                            LogLevel.WARNING -> AmberWarning
                            LogLevel.ERROR -> RoseDanger
                            LogLevel.NMEA -> PurpleAccent
                            LogLevel.INFO -> BluePrimary
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "[$timeStr]",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Slate400
                                )
                            )
                            Text(
                                text = "[${log.tag}]",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tagColor
                                )
                            )
                            Text(
                                text = log.message,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = if (log.level == LogLevel.NMEA) PurpleAccent else Slate900
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Slate600
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = color
            )
        )
    }
}
