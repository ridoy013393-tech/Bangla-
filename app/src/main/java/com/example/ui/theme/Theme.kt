package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.ThemePreference

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE8FF),
    onPrimaryContainer = Color(0xFF001C55),
    secondary = BrandPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE4FF),
    onSecondaryContainer = Color(0xFF2B0070),
    tertiary = Color(0xFF0EA5E9),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFF),
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = Color(0xFFE7ECFC),
    outline = LightOutline,
    outlineVariant = Color(0xFFE2E8F8),
    error = DangerRed,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueLight,
    onPrimary = Color(0xFF001A44),
    primaryContainer = Color(0xFF003B99),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = BrandPurpleLight,
    onSecondary = Color(0xFF260066),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFE9D8FD),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF002438),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLowest = Color(0xFF070B14),
    surfaceContainerLow = Color(0xFF101726),
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = Color(0xFF202B42),
    outline = DarkOutline,
    outlineVariant = Color(0xFF25314B),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

val BanglaChatShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun BanglaChatTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BanglaChatTypography,
        shapes = BanglaChatShapes,
        content = content
    )
}
