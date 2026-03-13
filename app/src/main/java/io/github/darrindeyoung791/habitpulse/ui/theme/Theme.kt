package io.github.darrindeyoung791.habitpulse.ui.theme

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
    primary = PurpleDarkPrimary,
    onPrimary = PurpleDarkOnPrimary,
    primaryContainer = PurpleDarkPrimaryContainer,
    onPrimaryContainer = PurpleDarkOnPrimaryContainer,
    inversePrimary = PurpleDarkInversePrimary,
    secondary = PurpleDarkSecondary,
    onSecondary = PurpleDarkOnSecondary,
    secondaryContainer = PurpleDarkSecondaryContainer,
    onSecondaryContainer = PurpleDarkOnSecondaryContainer,
    tertiary = PurpleDarkTertiary,
    onTertiary = PurpleDarkOnTertiary,
    tertiaryContainer = PurpleDarkTertiaryContainer,
    onTertiaryContainer = PurpleDarkOnTertiaryContainer,
    error = PurpleDarkError,
    onError = PurpleDarkOnError,
    errorContainer = PurpleDarkErrorContainer,
    onErrorContainer = PurpleDarkOnErrorContainer,
    background = PurpleDarkBackground,
    onBackground = PurpleDarkOnBackground,
    surface = PurpleDarkSurface,
    onSurface = PurpleDarkOnSurface,
    inverseSurface = PurpleDarkInverseSurface,
    inverseOnSurface = PurpleDarkInverseOnSurface,
    surfaceVariant = PurpleDarkSurfaceVariant,
    onSurfaceVariant = PurpleDarkOnSurfaceVariant,
    outline = PurpleDarkOutline,
    outlineVariant = PurpleDarkOutlineVariant,
    surfaceTint = PurpleDarkSurfaceTint
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = PurpleOnPrimary,
    primaryContainer = PurplePrimaryContainer,
    onPrimaryContainer = PurpleOnPrimaryContainer,
    inversePrimary = PurpleInversePrimary,
    secondary = PurpleSecondary,
    onSecondary = PurpleOnSecondary,
    secondaryContainer = PurpleSecondaryContainer,
    onSecondaryContainer = PurpleOnSecondaryContainer,
    tertiary = PurpleTertiary,
    onTertiary = PurpleOnTertiary,
    tertiaryContainer = PurpleTertiaryContainer,
    onTertiaryContainer = PurpleOnTertiaryContainer,
    error = PurpleError,
    onError = PurpleOnError,
    errorContainer = PurpleErrorContainer,
    onErrorContainer = PurpleOnErrorContainer,
    background = PurpleBackground,
    onBackground = PurpleOnBackground,
    surface = PurpleSurface,
    onSurface = PurpleOnSurface,
    inverseSurface = PurpleInverseSurface,
    inverseOnSurface = PurpleInverseOnSurface,
    surfaceVariant = PurpleSurfaceVariant,
    onSurfaceVariant = PurpleOnSurfaceVariant,
    outline = PurpleOutline,
    outlineVariant = PurpleOutlineVariant,
    surfaceTint = PurpleSurfaceTint
)

@Composable
fun HabitPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // When enabled, uses Android's Monet engine to extract colors from wallpaper
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}