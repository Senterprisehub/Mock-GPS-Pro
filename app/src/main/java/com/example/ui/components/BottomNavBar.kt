package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.BlueSubtle
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.viewmodel.AppTab

@Composable
fun AppBottomNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    hasSettingsWarning: Boolean,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = PureWhite,
        tonalElevation = 4.dp,
        modifier = modifier
            .border(1.dp, Slate200)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Tab 1: Map & Joystick
        NavigationBarItem(
            selected = selectedTab == AppTab.MAP,
            onClick = { onTabSelected(AppTab.MAP) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.MAP) Icons.Filled.Map else Icons.Outlined.Map,
                    contentDescription = "Map & Joystick"
                )
            },
            label = { Text("Map", fontSize = 11.sp) },
            colors = navigationItemColors(),
            modifier = Modifier.testTag("tab_map")
        )

        // Tab 2: Permissions & Settings
        NavigationBarItem(
            selected = selectedTab == AppTab.SETTINGS,
            onClick = { onTabSelected(AppTab.SETTINGS) },
            icon = {
                BadgedBox(
                    badge = {
                        if (hasSettingsWarning) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AmberWarning)
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (selectedTab == AppTab.SETTINGS) Icons.Filled.Shield else Icons.Outlined.Shield,
                        contentDescription = "Permissions & Settings"
                    )
                }
            },
            label = { Text("Permissions", fontSize = 11.sp) },
            colors = navigationItemColors(),
            modifier = Modifier.testTag("tab_settings")
        )

        // Tab 3: Routes & GPX
        NavigationBarItem(
            selected = selectedTab == AppTab.ROUTES,
            onClick = { onTabSelected(AppTab.ROUTES) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.ROUTES) Icons.Filled.AltRoute else Icons.Outlined.AltRoute,
                    contentDescription = "Routes & GPX"
                )
            },
            label = { Text("Routes", fontSize = 11.sp) },
            colors = navigationItemColors(),
            modifier = Modifier.testTag("tab_routes")
        )

        // Tab 4: Live Telemetry
        NavigationBarItem(
            selected = selectedTab == AppTab.LOGS,
            onClick = { onTabSelected(AppTab.LOGS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.LOGS) Icons.Filled.Terminal else Icons.Outlined.Terminal,
                    contentDescription = "Live Telemetry"
                )
            },
            label = { Text("Telemetry", fontSize = 11.sp) },
            colors = navigationItemColors(),
            modifier = Modifier.testTag("tab_logs")
        )

        // Tab 5: Gemini AI Copilot
        NavigationBarItem(
            selected = selectedTab == AppTab.AI_COPILOT,
            onClick = { onTabSelected(AppTab.AI_COPILOT) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.AI_COPILOT) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                    contentDescription = "Gemini AI Copilot",
                    tint = if (selectedTab == AppTab.AI_COPILOT) BluePrimary else Slate400
                )
            },
            label = { Text("AI Copilot", fontSize = 11.sp, fontWeight = if (selectedTab == AppTab.AI_COPILOT) FontWeight.Bold else FontWeight.Normal) },
            colors = navigationItemColors(),
            modifier = Modifier.testTag("tab_ai_copilot")
        )
    }
}


@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = BluePrimary,
    selectedTextColor = BluePrimary,
    unselectedIconColor = Slate400,
    unselectedTextColor = Slate600,
    indicatorColor = BlueSubtle
)
