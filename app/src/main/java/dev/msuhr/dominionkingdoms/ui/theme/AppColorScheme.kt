package dev.msuhr.dominionkingdoms.ui.theme

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
        //surfaceContainerHighest = if (isDark) Color(0xFF36353A) else Color(0xFFFFFFFF), // WHITE BACKGROUND
        surfaceContainerHighest = if (isDark) Color(0xFF36353A) else Color(0xFFEBE8E0), // Card background
        surfaceContainerLow = if (isDark) Color(0xFF181819) else Color(0xFFE3E6E0), // not used
        surfaceContainerLowest = if (isDark) Color(0xFF101012) else Color(0xFFE1E4DC) // not used
    )

    // Light Mode Colors
    val lightCustomColors = createColorScheme(
        //primary = Color(0xFFD4901F), // ORANGE
        //primary = Color(0xFFE6952B), // 1
        primary = Color(0xFFDF9F37), // 3
        //primary = Color(0xFFE5C158), // Gold coin
        onPrimary = Color(0xFFFFFFFF), // On player selection buttons?
        //primaryContainer = Color(0xFFFFE187),
        //onPrimaryContainer = Color(0xFF2E1500),

        primaryContainer = Color(0xFFFCE7CB),
        onPrimaryContainer = Color(0xFF331B00),

        // Error: Soft red (less harsh)
        error = Color(0xFFFFB2B2),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFB2B2),
        onErrorContainer = Color(0xFF411F2A),

        // Background: Warm off-white (cream instead of cool gray)
        background = Color(0xFFF9F7F2),
        onBackground = Color(0xFF1C1B18),

        // Surface: Warm white
        surface = Color(0xFFF9F7F2),
        onSurface = Color(0xFF3D3631),

        // SurfaceVariant: Warm gray
        surfaceVariant = Color(0xFFE1E3DD),
        onSurfaceVariant = Color(0xFF454744),

        outline = Color(0xFF94969A),
        outlineVariant = Color(0xFFB4B6BB),

        inverseSurface = Color(0xFF2F3032),
        inverseOnSurface = Color(0xFFF1F1F6),

        inversePrimary = Color(0xFFFFB972),

        secondary = Color(0xFF454744), // Navbar selected text
        onSecondary = Color(0xFF00FF00), // ???
        secondaryContainer = Color(0xFFF5E6C8), // Navbar selection blob
        onSecondaryContainer = Color(0xFF2D1600), // Navbar selected icon

        // Unused
        tertiary = Color(0xFFFF00FF),
        onTertiary = Color(0xFFFF00FF),
        tertiaryContainer = Color(0xFFFF00FF),
        onTertiaryContainer = Color(0xFFFF00FF),

        isDark = false
    )

    // Dark Mode Colors
    val darkCustomColors = createColorScheme(
        primary = Color(0xFFF0B27A), // ORANGE
        //primary = Color(0xFFE5C158), // GOLD COIN
        onPrimary = Color(0xFF4F3014),
        //primaryContainer = Color(0xFF4D4421),
        //onPrimaryContainer = Color(0xFFFFE187),

        primaryContainer = Color(0xFF593E23),
        onPrimaryContainer = Color(0xFFFCDCBF),

        inversePrimary = Color(0xFF755B00),

        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),

        background = Color(0xFF1C1B1F),
        onBackground = Color(0xFFE5E1E5),
        surface = Color(0xFF1C1B1F),
        onSurface = Color(0xFFE5E1E5),
        surfaceVariant = Color(0xFF464349),
        onSurfaceVariant = Color(0xFFCAC6CF),

        outline = Color(0xFF6C6971),
        outlineVariant = Color(0xFF464349),

        inverseSurface = Color(0xFFE5E1E5),
        inverseOnSurface = Color(0xFF2F3033),

        secondary = Color(0xFFCAC6CF), // Navbar selected text
        onSecondary = Color(0xFF00FF00), // ???
        secondaryContainer = Color(0xFF3D2E20), // Navbar selection blob
        onSecondaryContainer = Color(0xFFF0B27A), // Navbar selected icon

        // Unused
        tertiary = Color(0xFFFF00FF),
        onTertiary = Color(0xFFFF00FF),
        tertiaryContainer = Color(0xFFFF00FF),
        onTertiaryContainer = Color(0xFFFF00FF),

        isDark = true
    )
}
