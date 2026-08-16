package com.photomigrate.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    secondary = AccentPurple,
    tertiary = AccentTeal,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    secondary = AccentPurple,
    tertiary = AccentTeal,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant
)

val LocalThemeColors = staticCompositionLocalOf { ThemeBlue }

@Composable
fun PhotoMigrateTheme(
    appTheme: AppTheme = AppTheme.BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val themeColors = when (appTheme) {
        AppTheme.BLUE -> ThemeBlue
        AppTheme.EMERALD -> ThemeEmerald
        AppTheme.AMBER -> ThemeAmber
        AppTheme.ROSE -> ThemeRose
        AppTheme.VIOLET -> ThemeViolet
        AppTheme.AMOLED -> ThemeAmoled
        AppTheme.APPLE -> ThemeApple
        AppTheme.DRACULA -> ThemeDracula
        AppTheme.SYNTHWAVE -> ThemeSynthwave
    }

    // Force dark mode for inherently dark themes
    val effectiveDarkTheme = when (appTheme) {
        AppTheme.AMOLED, AppTheme.DRACULA, AppTheme.SYNTHWAVE -> true
        else -> darkTheme
    }

    // Determine if we need light or dark text on the primary color
    val onPrimaryColor = when (appTheme) {
        AppTheme.AMOLED, AppTheme.AMBER -> Color.Black
        else -> Color.White
    }

    var colorScheme = if (effectiveDarkTheme) {
        DarkColorScheme.copy(primary = themeColors.primary, onPrimary = onPrimaryColor)
    } else {
        LightColorScheme.copy(primary = themeColors.primary, onPrimary = onPrimaryColor)
    }

    // Custom background overrides for specific themes
    colorScheme = when (appTheme) {
        AppTheme.AMOLED -> colorScheme.copy(
            primary = Color.White,
            onPrimary = Color.Black,
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF1C1C1E),
            onSurface = Color.White,
            onSurfaceVariant = Color.LightGray,
            onBackground = Color.White
        )
        AppTheme.DRACULA -> colorScheme.copy(
            onPrimary = Color.Black,
            background = Color(0xFF282A36),
            surface = Color(0xFF282A36),
            surfaceVariant = Color(0xFF44475A),
            onSurface = Color.White,
            onSurfaceVariant = Color.LightGray,
            onBackground = Color.White
        )
        AppTheme.SYNTHWAVE -> colorScheme.copy(
            onPrimary = Color.Black,
            background = Color(0xFF241734),
            surface = Color(0xFF241734),
            surfaceVariant = Color(0xFF2D1B4E),
            onSurface = Color.White,
            onSurfaceVariant = Color.LightGray,
            onBackground = Color.White
        )
        else -> colorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !effectiveDarkTheme
        }
    }

    CompositionLocalProvider(
        LocalThemeColors provides themeColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
