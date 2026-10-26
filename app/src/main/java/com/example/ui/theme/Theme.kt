package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = PureWhite,
    primaryContainer = BlueSubtle,
    onPrimaryContainer = BlueDark,
    secondary = CyanAccent,
    onSecondary = PureWhite,
    secondaryContainer = CyanSubtle,
    onSecondaryContainer = CyanAccent,
    tertiary = EmeraldSuccess,
    onTertiary = PureWhite,
    tertiaryContainer = EmeraldSubtle,
    onTertiaryContainer = EmeraldSuccess,
    background = Slate50,
    onBackground = Slate900,
    surface = PureWhite,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate200,
    outlineVariant = Slate300,
    error = RoseDanger,
    onError = PureWhite,
    errorContainer = RoseSubtle,
    onErrorContainer = RoseDanger
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueLight,
    onPrimary = PureWhite,
    primaryContainer = Slate800,
    onPrimaryContainer = CyanGlow,
    secondary = CyanGlow,
    onSecondary = Slate950,
    secondaryContainer = Slate850,
    onSecondaryContainer = Slate200,
    tertiary = EmeraldGlow,
    onTertiary = PureWhite,
    tertiaryContainer = Slate850,
    onTertiaryContainer = EmeraldGlow,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate300,
    outline = Slate700,
    outlineVariant = Slate800,
    error = RoseDanger,
    onError = PureWhite
)

@Composable
fun MockGpsTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

