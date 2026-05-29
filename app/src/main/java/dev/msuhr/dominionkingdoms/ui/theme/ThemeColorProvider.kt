package dev.msuhr.dominionkingdoms.ui.theme

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme

/**
 * Provides color scheme based on user preference.
 * When useSystemTheme is true, uses Material 3 dynamic colors (Android 12+ system theme).
 * When false, uses the custom "Official" Dominion app colors.
 */
object ThemeColorProvider {

    /**
     * Check if dynamic color is supported (Android 12+)
     */
    fun isDynamicColorAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    /**
     * Get the appropriate color scheme based on dark mode and system theme preference.
     * This is meant to be used from non-composable contexts (like MainActivity).
     *
     * @param useSystemTheme Whether to use system theme colors (ignored if API < 31)
     * @param isDarkMode Whether to use dark mode (null = auto based on system)
     * @param activity The activity (for dynamic color access)
     */
    fun getColorScheme(
        useSystemTheme: Boolean,
        isDarkMode: Boolean?,
        activity: Activity
    ): ColorScheme {
        // Determine actual dark mode state (null means use system default)
        val actualDarkMode = isDarkMode ?: isSystemInDarkThemeImpl(activity)

        // Dynamic colors only available on Android 12+
        val canUseDynamicColors = isDynamicColorAvailable()

        return if (useSystemTheme && canUseDynamicColors) {
            // Use dynamic system colors (Material You)
            if (actualDarkMode) {
                dynamicDarkColorScheme(activity)
            } else {
                dynamicLightColorScheme(activity)
            }
        } else {
            // Use custom "Official" app colors
            if (actualDarkMode) {
                AppColorScheme.darkCustomColors
            } else {
                AppColorScheme.lightCustomColors
            }
        }
    }

    /**
     * Check if system is in dark mode from a non-composable context.
     * This uses UiModeManager to get the current system night mode setting.
     */
    private fun isSystemInDarkThemeImpl(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.nightMode == UiModeManager.MODE_NIGHT_YES
    }
}
