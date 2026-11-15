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
import com.example.model.ThemeMode

private val HephaistosLightColorScheme = lightColorScheme(
    primary = SoftSlatePrimary,
    onPrimary = Color.White,
    primaryContainer = SoftSlateContainer,
    onPrimaryContainer = SoftSlateOnContainer,
    secondary = SoftSageSecondary,
    onSecondary = Color.White,
    secondaryContainer = SoftSageContainer,
    onSecondaryContainer = SoftSageOnContainer,
    tertiary = SoftTaupeTertiary,
    onTertiary = Color.White,
    tertiaryContainer = SoftTaupeContainer,
    onTertiaryContainer = SoftTaupeOnContainer,
    background = SoftAlabasterBg,
    onBackground = SoftInkPrimary,
    surface = SoftPureCard,
    onSurface = SoftInkPrimary,
    surfaceVariant = SoftLinenSurface,
    onSurfaceVariant = SoftInkSecondary,
    surfaceContainerLowest = SoftPureCard,
    surfaceContainerLow = SoftSurfaceLow,
    surfaceContainer = SoftLinenSurface,
    surfaceContainerHigh = SoftSurfaceHigh,
    surfaceContainerHighest = SoftOutline,
    outline = SoftOutline,
    outlineVariant = SoftOutlineVariant,
    error = SoftError,
    onError = Color.White,
    errorContainer = SoftErrorContainer,
    onErrorContainer = SoftOnErrorContainer
)

private val HephaistosDarkColorScheme = darkColorScheme(
    primary = VelvetMistPrimary,
    onPrimary = VelvetOnPrimary,
    primaryContainer = VelvetPrimaryContainer,
    onPrimaryContainer = VelvetOnPrimaryContainer,
    secondary = VelvetSageSecondary,
    onSecondary = VelvetOnSecondary,
    secondaryContainer = VelvetSecondaryContainer,
    onSecondaryContainer = VelvetOnSecondaryContainer,
    tertiary = VelvetTaupeTertiary,
    onTertiary = VelvetOnTertiary,
    tertiaryContainer = VelvetTertiaryContainer,
    onTertiaryContainer = VelvetOnTertiaryContainer,
    background = VelvetObsidianBg,
    onBackground = VelvetInkPrimary,
    surface = VelvetCard,
    onSurface = VelvetInkPrimary,
    surfaceVariant = VelvetSurfaceVariant,
    onSurfaceVariant = VelvetInkSecondary,
    surfaceContainerLowest = VelvetObsidianBg,
    surfaceContainerLow = VelvetSurfaceLow,
    surfaceContainer = VelvetCard,
    surfaceContainerHigh = VelvetSurfaceHigh,
    surfaceContainerHighest = VelvetOutline,
    outline = VelvetOutline,
    outlineVariant = VelvetOutlineVariant,
    error = VelvetError,
    onError = VelvetOnError,
    errorContainer = VelvetErrorContainer,
    onErrorContainer = VelvetOnErrorContainer
)

val HephaistosShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun HephaistosTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDarkTheme) HephaistosDarkColorScheme else HephaistosLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HephaistosTypography,
        shapes = HephaistosShapes,
        content = content
    )
}
