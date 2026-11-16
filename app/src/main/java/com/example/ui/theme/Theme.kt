package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val JustDoDarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = OnIndigoPrimaryDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = OnIndigoContainerDark,
    secondary = PurpleSecondaryDark,
    onSecondary = OnPurpleSecondaryDark,
    secondaryContainer = PurpleContainerDark,
    onSecondaryContainer = OnPurpleContainerDark,
    tertiary = VioletTertiaryDark,
    onTertiary = OnVioletTertiaryDark,
    tertiaryContainer = VioletContainerDark,
    onTertiaryContainer = OnVioletContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

private val JustDoLightColorScheme = lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = OnIndigoPrimaryLight,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = OnIndigoContainerLight,
    secondary = PurpleSecondaryLight,
    onSecondary = OnPurpleSecondaryLight,
    secondaryContainer = PurpleContainerLight,
    onSecondaryContainer = OnPurpleContainerLight,
    tertiary = VioletTertiaryLight,
    onTertiary = OnVioletTertiaryLight,
    tertiaryContainer = VioletContainerLight,
    onTertiaryContainer = OnVioletContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

val JustDoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Composable
fun JustDoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) JustDoDarkColorScheme else JustDoLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = JustDoTypography,
        shapes = JustDoShapes,
        content = content
    )
}
