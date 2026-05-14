package com.marvinsuhr.dominionhelper.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

// Custom "Official" App Color Scheme
// This is a Dominion-inspired theme with warm, royal colors

object AppColorScheme {

    // Helper function to create a complete ColorScheme
    private fun createColorScheme(
        primary: Color,
        onPrimary: Color,
        primaryContainer: Color,
        onPrimaryContainer: Color,
        inversePrimary: Color,
        secondary: Color,
        onSecondary: Color,
        secondaryContainer: Color,
        onSecondaryContainer: Color,
        tertiary: Color,
        onTertiary: Color,
        tertiaryContainer: Color,
        onTertiaryContainer: Color,
        error: Color,
        onError: Color,
        errorContainer: Color,
        onErrorContainer: Color,
        background: Color,
        onBackground: Color,
        surface: Color,
        onSurface: Color,
        surfaceVariant: Color,
        onSurfaceVariant: Color,
        outline: Color,
        outlineVariant: Color,
        inverseSurface: Color,
        inverseOnSurface: Color,
        isDark: Boolean
    ) = ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        // Auto-generated colors based on tonal palette
        surfaceTint = primary.copy(alpha = 0.08f),
        scrim = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF),
        surfaceBright = if (isDark) Color(0xFF2C2B2F) else Color(0xFFECE0E4),
        surfaceDim = if (isDark) Color(0xFF1B1B1F) else Color(0xFFDADADD),
        surfaceContainer = if (isDark) Color(0xFF2B2A2E) else Color(0xFFE0DCD6), // Nav bar
        surfaceContainerHigh = if (isDark) Color(0xFF2B2A2E) else Color(0xFFE8EAF0), // not used
        surfaceContainerHighest = if (isDark) Color(0xFF36353A) else Color(0xFFEBE8E0), // Card background
        surfaceContainerLow = if (isDark) Color(0xFF181819) else Color(0xFFE3E6E0), // not used
        surfaceContainerLowest = if (isDark) Color(0xFF101012) else Color(0xFFE1E4DC) // not used
    )

    // Light Mode Colors
    val lightCustomColors = createColorScheme(
        // Primary: Warm amber/gold (Dominion card backs)
        primary = Color(0xFFD4901F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDD87),
        onPrimaryContainer = Color(0xFF2E1500),

        // Secondary: Warm copper/accent
        secondary = Color(0xFF8B5E3C),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFB8CB8D),
        onSecondaryContainer = Color(0xFF252F20),

        // Tertiary: Olive green (earth tone)
        tertiary = Color(0xFF5C6B3F),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFD0E8B8),
        onTertiaryContainer = Color(0xFF192220),

        // Error: Soft red (less harsh)
        error = Color(0xFFDC6A6A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD4),
        onErrorContainer = Color(0xFF411F2A),

        // Background: Warm off-white (cream instead of cool gray)
        background = Color(0xFFFAF8F3),
        onBackground = Color(0xFF1C1B18),

        // Surface: Warm white
        surface = Color(0xFFFFFBF9),
        onSurface = Color(0xFF1C1B18),

        // SurfaceVariant: Warm gray
        surfaceVariant = Color(0xFFE1E3DD),
        onSurfaceVariant = Color(0xFF454744),

        outline = Color(0xFF747773),
        outlineVariant = Color(0xFFB4B6BB),

        inverseSurface = Color(0xFF2F3032),
        inverseOnSurface = Color(0xFFF1F1F6),

        inversePrimary = Color(0xFFFFB972),

        isDark = false
    )

    // Dark Mode Colors
    val darkCustomColors = createColorScheme(
        /*primary = Color(0xFF9CBAFF),
        onPrimary = Color(0xFF002F66),
        primaryContainer = Color(0xFF00468E),
        onPrimaryContainer = Color(0xFFD3E4FF),
        inversePrimary = Color(0xFF2563EB),*/
        primary = Color(0xAAE5C158),
        onPrimary = Color(0xFF3A2D00),
        primaryContainer = Color(0xFF554300),
        onPrimaryContainer = Color(0xFFFFE187),
        inversePrimary = Color(0xFF755B00),

        secondary = Color(0xFFFFB972),
        onSecondary = Color(0xFF4B2800),
        secondaryContainer = Color(0xFF6D3B00),
        onSecondaryContainer = Color(0xFFFFDDB3),

        tertiary = Color(0xFF9CD49B),
        onTertiary = Color(0xFF003913),
        tertiaryContainer = Color(0xFF0F511F),
        onTertiaryContainer = Color(0xFFB8F1BB),

        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),

        background = Color(0xFF1C1B1F),
        onBackground = Color(0xFFE5E1E5),
        surface = Color(0xFF1C1B1F),
        onSurface = Color(0xFFE5E1E5),
        surfaceVariant = Color(0xFF464349),
        onSurfaceVariant = Color(0xFFCAC6CF),

        outline = Color(0xFF928F99),
        outlineVariant = Color(0xFF464349),

        inverseSurface = Color(0xFFE5E1E5),
        inverseOnSurface = Color(0xFF2F3033),

        isDark = true
    )
}
