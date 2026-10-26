package com.example

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AppBottomNavBar
import com.example.ui.components.AppHeaderBar
import com.example.ui.screens.AiCopilotScreen
import com.example.ui.screens.MapJoystickScreen
import com.example.ui.screens.PermissionsSettingsScreen
import com.example.ui.screens.RoutesScreen
import com.example.ui.screens.TelemetryLogsScreen
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.MockGpsTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MockGpsViewModel

private const val LATEST_RELEASE_URL =
    "https://github.com/Senterprisehub/Mock-GPS-Pro/releases/latest/download/Mock.GPS.Pro.-.v1.0.apk"

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Handled reactively by ViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Global Crash & Abnormality Tracker
        com.example.data.CrashTracker.init(this)
        com.example.data.CrashTracker.updateState("App Started / Opened")

        // Stop any external overlay window so joystick stays cleanly in-app
        try {
            com.example.service.FloatingJoystickService.stop(this)
        } catch (e: Exception) {
            // Ignore
        }

        // Request baseline location and notification permissions only if not already granted
        val neededPermissions = mutableListOf<String>()
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted) {
            neededPermissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            neededPermissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notifGranted) {
                neededPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (neededPermissions.isNotEmpty()) {
            try {
                permissionLauncher.launch(neededPermissions.toTypedArray())
            } catch (e: Exception) {
                Log.w("MainActivity", "Permission launch failed: ${e.message}")
            }
        }

        setContent {
            MockGpsTheme(darkTheme = false) {
                MockGpsApp()
            }
        }
    }
}

@Composable
fun MockGpsApp(viewModel: MockGpsViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val currentTab by viewModel.currentTab.collectAsState()
    val isMocking by viewModel.isMocking.collectAsState()
    val permissionsState by viewModel.permissionsState.collectAsState()

    var showExitConfirmDialog by remember { mutableStateOf(false) }

    val openLatestRelease: () -> Unit = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LATEST_RELEASE_URL))
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No browser available to open the latest customer download.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Intercept back navigation: Close exit dialog if open, return to MAP tab if on other tabs, or show confirmation exit dialog
    BackHandler(enabled = true) {
        if (showExitConfirmDialog) {
            showExitConfirmDialog = false
        } else if (currentTab != AppTab.MAP) {
            viewModel.setTab(AppTab.MAP)
        } else {
            showExitConfirmDialog = true
        }
    }

    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Exit",
                        tint = Slate800,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Are you sure you want to exit?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Text(
                    text = if (isMocking) {
                        "GPS Simulation is currently ACTIVE. Would you like to minimize to the background to keep GPS active, or exit the application completely?"
                    } else {
                        "Do you want to exit Mock GPS Pro?"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = Slate600)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        activity?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger, contentColor = PureWhite)
                ) {
                    Text("Exit App", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showExitConfirmDialog = false }
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            showExitConfirmDialog = false
                            activity?.moveTaskToBack(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary, contentColor = PureWhite)
                    ) {
                        Text("Run in Background")
                    }
                }
            }
        )
    }

    val hasSettingsWarning = !permissionsState.mockLocationAppSelected || !permissionsState.fineLocationGranted

    Scaffold(
        topBar = {
            AppHeaderBar(
                isMocking = isMocking,
                onToggleMocking = { viewModel.toggleMocking() },
                onDownloadLatestRelease = openLatestRelease,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        },
        bottomBar = {
            AppBottomNavBar(
                selectedTab = currentTab,
                onTabSelected = { viewModel.setTab(it) },
                hasSettingsWarning = hasSettingsWarning
            )
        },
        containerColor = Slate50,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate50)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    AppTab.MAP -> MapJoystickScreen(viewModel = viewModel)
                    AppTab.SETTINGS -> PermissionsSettingsScreen(viewModel = viewModel)
                    AppTab.ROUTES -> RoutesScreen(viewModel = viewModel)
                    AppTab.LOGS -> TelemetryLogsScreen(viewModel = viewModel)
                    AppTab.AI_COPILOT -> AiCopilotScreen(viewModel = viewModel)
                }
            }
        }
    }
}

