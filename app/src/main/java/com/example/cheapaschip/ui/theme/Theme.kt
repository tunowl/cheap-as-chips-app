package com.example.cheapaschip.ui.theme

import android.app.Activity
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

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueMedium,
    secondary = BrandGreenMedium,
    tertiary = BrandYellowMedium,
    background = Color(0xFF0F172A), // Slate 900
    surface = Color(0xFF1E293B), // Slate 800
    onPrimary = SurfaceWhite,
    onSecondary = SurfaceWhite,
    onTertiary = SurfaceWhite,
    onBackground = SurfaceWhite,
    onSurface = SurfaceWhite,
    surfaceVariant = Color(0xFF334155), // Slate 700
    onSurfaceVariant = Color(0xFF94A3B8), // Slate 400
    outline = Color(0xFF475569) // Slate 600
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlueMedium,
    secondary = BrandGreenMedium,
    tertiary = BrandYellowMedium,
    background = BackgroundSlate,
    surface = SurfaceWhite,
    onPrimary = SurfaceWhite,
    onSecondary = SurfaceWhite,
    onTertiary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFF1F5F9), // Slate 100
    onSurfaceVariant = TextGray,
    outline = CardBorderSlate
)

@Composable
fun CheapAsChipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamicColor default to false so our professional custom brand colors are consistently used
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
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