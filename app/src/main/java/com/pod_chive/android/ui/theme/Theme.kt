package com.pod_chive.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

//private val DarkColorScheme = darkColorScheme(
//    primary = Red,
//    secondary = PurpleGrey80,
//    tertiary = Blueish,
//    onPrimary = Color.White,
//)
//
//private val LightColorScheme = lightColorScheme(
//    primary = Red,
//    secondary = PurpleGrey40,
//    tertiary = Blueish
//
//    /* Other default colors to override
//    background = Color(0xFFFFFBFE),
//    surface = Color(0xFFFFFBFE),
//    onPrimary = Color.White,
//    onSecondary = Color.White,
//    onTertiary = Color.White,
//    onBackground = Color(0xFF1C1B1F),
//    onSurface = Color(0xFF1C1B1F),
//    */
//)

private val DarkColorScheme = darkColorScheme(
    primary = ChivePrimaryDark,
    onPrimary = ChiveOnPrimaryDark,
    primaryContainer = Color(0xFF205225),
    onPrimaryContainer = ChivePrimaryContainer,
    secondary = Color(0xFFBACCB3),
    onSecondary = Color(0xFF253423),
    secondaryContainer = Color(0xFF3C4B38),
    onSecondaryContainer = SageSecondaryContainer,
    tertiary = Color(0xFFA0CFD0),
    onTertiary = Color(0xFF003738),
    background = ChiveSurfaceDark,
    onBackground = ChiveOnSurfaceDark,
    surface = ChiveSurfaceDark,
    onSurface = ChiveOnSurfaceDark,
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD),
    outline = Color(0xFF8C9389),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = ChivePrimary,
    onPrimary = ChiveOnPrimary,
    primaryContainer = ChivePrimaryContainer,
    onPrimaryContainer = ChiveOnPrimaryContainer,
    secondary = SageSecondary,
    onSecondary = SageOnSecondary,
    secondaryContainer = SageSecondaryContainer,
    onSecondaryContainer = SageOnSecondaryContainer,
    tertiary = EarthTertiary,
    onTertiary = EarthOnTertiary,
    tertiaryContainer = EarthTertiaryContainer,
    background = ChiveSurface,
    onBackground = ChiveOnSurface,
    surface = ChiveSurface,
    onSurface = ChiveOnSurface,
    surfaceVariant = Color(0xFFDEE5D9),
    onSurfaceVariant = Color(0xFF424940),
    outline = ChiveOutline,
    error = Color(0xFFBA1A1A), // Keep errors red for clarity
    onError = Color(0xFFFFFFFF)
)

@Composable
fun PodchiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Apply this theme at the app/root level (for example in MainActivity setContent).
    // Avoid nesting PodchiveTheme in regular screen composables.
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
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