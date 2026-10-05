package com.coffeetime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    background = LightBackground,
    surface = LightSurface,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    surfaceVariant = LightSurfaceContainerHigh,
    onBackground = LightOnSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainerHighest = LightSurfaceContainerHigh,
)

private val DarkColors = darkColorScheme(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    surfaceVariant = DarkSurfaceContainerHigh,
    onBackground = DarkOnSurface,
    surfaceContainerLow = DarkSurface,
    surfaceContainerHighest = DarkSurfaceContainerHigh,
)

/** Colores de marca que Material 3 no trae: advertencia (stock crítico, Por cobrar, Sobrante). */
@Immutable
data class CoffeeExtraColors(
    val warning: Color, val onWarning: Color,
    val warningContainer: Color, val onWarningContainer: Color,
)
private val LightExtra = CoffeeExtraColors(LightWarning, LightOnWarning, LightWarningContainer, LightOnWarningContainer)
private val DarkExtra = CoffeeExtraColors(DarkWarning, DarkOnWarning, DarkWarningContainer, DarkOnWarningContainer)
val LocalCoffeeExtraColors = staticCompositionLocalOf { LightExtra }

@Composable
fun CoffeeTimeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // Sin dynamic color: la marca manda sobre el fondo de pantalla del dispositivo.
    CompositionLocalProvider(LocalCoffeeExtraColors provides if (darkTheme) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = CoffeeTypography,
            shapes = CoffeeShapes,
            content = content,
        )
    }
}

/** Acceso: MaterialTheme.colorScheme.warningContainer, etc. */
val ColorScheme.warning @Composable get() = LocalCoffeeExtraColors.current.warning
val ColorScheme.onWarning @Composable get() = LocalCoffeeExtraColors.current.onWarning
val ColorScheme.warningContainer @Composable get() = LocalCoffeeExtraColors.current.warningContainer
val ColorScheme.onWarningContainer @Composable get() = LocalCoffeeExtraColors.current.onWarningContainer
