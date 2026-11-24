package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioCoral,
    onPrimary = StudioOnCoral,
    primaryContainer = StudioCoralContainer,
    onPrimaryContainer = StudioOnCoralContainer,
    secondary = StudioCyan,
    onSecondary = StudioObsidian,
    secondaryContainer = StudioCyanContainer,
    onSecondaryContainer = StudioCyan,
    tertiary = StudioAmber,
    onTertiary = StudioObsidian,
    background = StudioObsidian,
    onBackground = StudioTextPrimary,
    surface = StudioSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = StudioTextSecondary,
    surfaceContainerLowest = StudioObsidian,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceHighest,
    outline = StudioBorder,
    outlineVariant = StudioBorderStrong,
    error = StudioError,
    onError = StudioObsidian,
    errorContainer = StudioErrorContainer,
    onErrorContainer = StudioError,
)

private val StudioLightColorScheme = lightColorScheme(
    primary = StudioCoralPressed,
    onPrimary = StudioLightSurface,
    secondary = StudioCyan,
    background = StudioLightBackground,
    onBackground = StudioLightText,
    surface = StudioLightSurface,
    onSurface = StudioLightText,
    surfaceVariant = StudioLightSurfaceVariant,
    onSurfaceVariant = StudioLightTextSecondary,
)

val MotionStudioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun MotionStudioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MotionStudioTypography,
        shapes = MotionStudioShapes,
        content = content,
    )
}
