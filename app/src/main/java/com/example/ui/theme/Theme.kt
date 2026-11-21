package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TournaXDarkClayColorScheme = darkColorScheme(
    primary = NeonPurpleStart,
    onPrimary = Color.White,
    primaryContainer = ObsidianElevated,
    onPrimaryContainer = MetallicSilver,
    secondary = NeonPurpleEnd,
    onSecondary = Color.White,
    secondaryContainer = ObsidianElevated,
    onSecondaryContainer = NeonPurpleSoft,
    tertiary = EmeraldVerified,
    onTertiary = Color.White,
    background = ObsidianBackground,
    onBackground = MetallicSilver,
    surface = ObsidianCard,
    onSurface = MetallicSilver,
    surfaceVariant = ObsidianInput,
    onSurfaceVariant = MetallicSilverDim,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorderBright,
    error = CrimsonAlert,
    onError = Color.White,
)

private val TournaXShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun TournaXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TournaXDarkClayColorScheme,
        typography = TournaXTypography,
        shapes = TournaXShapes,
        content = content,
    )
}
