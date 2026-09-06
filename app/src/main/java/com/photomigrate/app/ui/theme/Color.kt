package com.photomigrate.app.ui.theme

import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF007AFF) // Apple Blue
val SecondaryBlue = Color(0xFF5AC8FA)
val AccentPurple = Color(0xFFBF5AF2)
val AccentTeal = Color(0xFF64D2FF)

val SuccessGreen = Color(0xFF34C759)
val WarningAmber = Color(0xFFFFCC00)
val ErrorRed = Color(0xFFFF3B30)

// CRED Money Neo-Futuristic Palette
val CredNeonPink = Color(0xFFFF2E93)
val CredNeonPinkGlow = Color(0xFFFF65B2)
val CredPinkGradientStart = Color(0xFFFF1493)
val CredPinkGradientEnd = Color(0xFFFF65B2)
val CredPurpleGlow = Color(0xFF7000FF)
val CredObsidianBg = Color(0xFF0A0B12)
val CredCardBg = Color(0xFF131422)
val CredCardBorder = Color(0xFF23253B)
val CredActiveBorder = Color(0xFFFF2E93)
val CredTextPrimary = Color(0xFFFFFFFF)
val CredTextSecondary = Color(0xFF9394A5)

val DarkBackground = Color(0xFF0A0B12)
val DarkSurface = Color(0xFF131422)
val DarkSurfaceVariant = Color(0xFF1C1E30)
val GlassCardBorder = Color(0xFF23253B)

val LightBackground = Color(0xFFF2F2F7)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE5E5EA)

// Mesh Gradient Colors for background
val MeshBlue = Color(0xFFD1E3FF)
val MeshPurple = Color(0xFFE9D5FF)
val MeshTeal = Color(0xFFCCFBF1)

// Theme Palettes
sealed class ThemeColors(
    val primary: Color,
    val mesh1: Color,
    val mesh2: Color,
    val mesh3: Color
) {
    object Azure : ThemeColors(Color(0xFF007AFF), Color(0xFFD1E3FF), Color(0xFFE9D5FF), Color(0xFFCCFBF1))
    object Sakura : ThemeColors(Color(0xFFFF2E93), Color(0xFF2A081D), Color(0xFF1A0A20), Color(0xFF0A0B12))
    object Emerald : ThemeColors(Color(0xFF34C759), Color(0xFFDCFCE7), Color(0xFFECFCCB), Color(0xFFF0FDF4))
    object Royal : ThemeColors(Color(0xFF5856D6), Color(0xFFE0E7FF), Color(0xFFF5F3FF), Color(0xFFFAE8FF))
    object Amber : ThemeColors(Color(0xFFFF9500), Color(0xFFFEF3C7), Color(0xFFFFF7ED), Color(0xFFFFFBEB))
}

// Theme Accent Palettes
data class GlassThemeColors(
    val primary: Color,
    val mesh1: Color,
    val mesh2: Color,
    val mesh3: Color
)

val ThemeBlue = GlassThemeColors(Color(0xFF007AFF), Color(0xFFD1E3FF), Color(0xFFE9D5FF), Color(0xFFCCFBF1))
val ThemeEmerald = GlassThemeColors(Color(0xFF34C759), Color(0xFFD1FAE5), Color(0xFFECFDF5), Color(0xFFF0FDF4))
val ThemeAmber = GlassThemeColors(Color(0xFFFF9500), Color(0xFFFEF3C7), Color(0xFFFFF7ED), Color(0xFFFFFBEB))
val ThemeRose = GlassThemeColors(Color(0xFFFF2E93), Color(0xFF2A081A), Color(0xFF1C0A1A), Color(0xFF0A0B12))
val ThemeViolet = GlassThemeColors(Color(0xFF5856D6), Color(0xFFEDE9FE), Color(0xFFF5F3FF), Color(0xFFFDF2F8))
val ThemeAmoled = GlassThemeColors(Color(0xFFFF2E93), Color(0xFF0A0B12), Color(0xFF141522), Color(0xFF1F0818))
val ThemeApple = GlassThemeColors(Color(0xFF007AFF), Color(0xFFF2F2F7), Color(0xFFFFFFFF), Color(0xFFE5E5EA))
val ThemeDracula = GlassThemeColors(Color(0xFFBD93F9), Color(0xFF282A36), Color(0xFF44475A), Color(0xFF6272A4))
val ThemeSynthwave = GlassThemeColors(Color(0xFFFF2E93), Color(0xFF241734), Color(0xFF2D1B4E), Color(0xFFB967FF))
