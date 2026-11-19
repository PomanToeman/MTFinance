/**
 * @file Theme.kt
 * @scope Centralized Compose Material 3 Theme definitions and color palette.
 * @purpose Provides light and dark color schemes, global theme state management, and status colors for MTFinance.
 */
package com.example.mtfinance.screens.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Global theme mode selection options.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Visual status state for form save progress and validation states.
 */
enum class SaveStatusColor {
    GRAY,
    ORANGE,
    GREEN,
    RED
}

object ThemeState {
    var currentThemeMode by mutableStateOf(ThemeMode.SYSTEM)

    fun toggleTheme(isSystemDark: Boolean) {
        currentThemeMode = when (currentThemeMode) {
            ThemeMode.SYSTEM -> if (isSystemDark) ThemeMode.LIGHT else ThemeMode.DARK
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
    }
}

/**
 * Returns a standardized theme status color corresponding to [SaveStatusColor].
 */
@Composable
fun getSaveStatusThemeColor(status: SaveStatusColor): Color {
    return when (status) {
        SaveStatusColor.ORANGE -> Color(0xFFFF9800)
        SaveStatusColor.GREEN -> Color(0xFF4CAF50)
        SaveStatusColor.RED -> MaterialTheme.colorScheme.error
        SaveStatusColor.GRAY -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF002147),
    primaryContainer = Color(0xFF003D75),
    onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = Color(0xFF81D4FA),
    onSecondary = Color(0xFF002738),
    background = Color(0xFF121212),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2F33),
    onSurfaceVariant = Color(0xFFF0F4F9),
    outline = Color(0xFF8A929B),
    error = Color(0xFFFF8A80)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color.White,
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF2D3139),
    outline = Color(0xFF1A1C1E),
    error = Color(0xFFBA1A1A)
)

@Composable
fun MTFinanceTheme(
    themeMode: ThemeMode = ThemeState.currentThemeMode,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
