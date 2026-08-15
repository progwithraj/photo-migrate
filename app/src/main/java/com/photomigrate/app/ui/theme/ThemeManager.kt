package com.photomigrate.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppTheme {
    BLUE, EMERALD, AMBER, ROSE, VIOLET, AMOLED, APPLE, DRACULA, SYNTHWAVE
}

object ThemeManager {
    var currentTheme by mutableStateOf(AppTheme.BLUE)
    
    val themes = AppTheme.values()

    fun setTheme(theme: AppTheme) {
        currentTheme = theme
    }
    
    fun getThemeColors(theme: AppTheme): GlassThemeColors {
        return when (theme) {
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
    }
}
