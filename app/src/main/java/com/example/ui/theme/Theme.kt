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

private val CosmicDarkColorScheme = darkColorScheme(
    primary = CosmicPurple,
    secondary = CosmicBlue,
    tertiary = CosmicPink,
    background = CosmicDarkBg,
    surface = CosmicDarkSurface,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = CosmicPurple,
    secondary = CosmicBlue,
    tertiary = CosmicPink,
    background = TextPrimary,
    surface = TextPrimary,
    onPrimary = CosmicDarkBg,
    onSecondary = CosmicDarkBg,
    onTertiary = CosmicDarkBg,
    onBackground = CosmicDarkBg,
    onSurface = CosmicDarkBg
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamic color on for supportive systems
    dynamicColor: Boolean = false, // Set false to prioritize our cosmic branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> CosmicDarkColorScheme
        else -> CosmicDarkColorScheme // Defaulting to our beautiful dark theme for immersive AI vibe
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
