package com.example.recipefinder.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Terracotta40,
    onPrimary = Cream99,
    primaryContainer = Terracotta90,
    onPrimaryContainer = Terracotta10,
    secondary = Sage40,
    onSecondary = Cream99,
    secondaryContainer = Sage90,
    onSecondaryContainer = Sage10,
    tertiary = Mustard40,
    onTertiary = Cream99,
    tertiaryContainer = Mustard90,
    onTertiaryContainer = Mustard10,
    background = Cream99,
    onBackground = Ink10,
    surface = Cream99,
    onSurface = Ink10,
    surfaceVariant = SandVariant,
    onSurfaceVariant = Ink30,
    surfaceContainerLowest = androidx.compose.ui.graphics.Color.White,
    surfaceContainerLow = Cream98,
    surfaceContainer = Cream96,
    surfaceContainerHigh = Cream94,
    surfaceContainerHighest = Cream92,
    outline = Outline50,
    outlineVariant = OutlineVariant80,
    error = Error40,
    onError = Cream99,
    errorContainer = Error90,
    onErrorContainer = Error10,
)

private val DarkColors = darkColorScheme(
    primary = Terracotta80,
    onPrimary = Terracotta20,
    primaryContainer = Terracotta30,
    onPrimaryContainer = Terracotta90,
    secondary = Sage80,
    onSecondary = Sage20,
    secondaryContainer = Sage30,
    onSecondaryContainer = Sage90,
    tertiary = Mustard80,
    onTertiary = Mustard20,
    tertiaryContainer = Mustard30,
    onTertiaryContainer = Mustard90,
    background = Espresso06,
    onBackground = Milk90,
    surface = Espresso06,
    onSurface = Milk90,
    surfaceVariant = Ink30,
    onSurfaceVariant = Sand80,
    surfaceContainerLowest = Espresso04,
    surfaceContainerLow = Espresso10,
    surfaceContainer = Espresso12,
    surfaceContainerHigh = Espresso17,
    surfaceContainerHighest = Espresso22,
    outline = Outline60,
    outlineVariant = Ink30,
    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,
)

private val RecipeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * App theme. Dynamic (wallpaper) colors are deliberately not used so the warm food palette is
 * consistent on every device; light and dark follow the system setting.
 */
@Composable
fun RecipeFinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = RecipeTypography,
        shapes = RecipeShapes,
        content = content,
    )
}
