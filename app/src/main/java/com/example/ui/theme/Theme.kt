package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GpsCyanPrimary,
    onPrimary = GpsDarkBg,
    primaryContainer = GpsDarkSurfaceVariant,
    onPrimaryContainer = GpsCyanLight,
    secondary = GpsEmeraldSuccess,
    onSecondary = GpsDarkBg,
    tertiary = GpsAmberWarning,
    background = GpsDarkBg,
    onBackground = GpsTextPrimary,
    surface = GpsDarkSurface,
    onSurface = GpsTextPrimary,
    surfaceVariant = GpsDarkSurfaceVariant,
    onSurfaceVariant = GpsTextSecondary,
    error = GpsRedError
)

private val LightColorScheme = lightColorScheme(
    primary = GpsCyanPrimaryDark,
    onPrimary = GpsLightSurface,
    primaryContainer = GpsLightSurfaceVariant,
    onPrimaryContainer = GpsDarkBg,
    secondary = GpsEmeraldSuccess,
    onSecondary = GpsLightSurface,
    tertiary = GpsAmberWarning,
    background = GpsLightBg,
    onBackground = GpsDarkBg,
    surface = GpsLightSurface,
    onSurface = GpsDarkBg,
    surfaceVariant = GpsLightSurfaceVariant,
    onSurfaceVariant = GpsTextMuted,
    error = GpsRedError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic color to enforce distinctive HUD theme
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

