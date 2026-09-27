package dev.msuhr.dominionkingdoms.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Port of the Android app's typography (app/ui/theme/Type.kt).
val SharedTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

// iOS port note: the Android app uses Material You dynamic colors when
// "Use system theme" is enabled (Android 12+ only). Dynamic color does not
// exist on iOS, so the shared app always uses the custom "Official" colors -
// the same palette the Android app falls back to when dynamic color is off.
private val DarkColorScheme = darkColorScheme(
    primary = AppColorScheme.darkCustomColors.primary,
    secondary = AppColorScheme.darkCustomColors.secondary,
    tertiary = AppColorScheme.darkCustomColors.tertiary
)

private val LightColorScheme = lightColorScheme(
    primary = AppColorScheme.lightCustomColors.primary,
    secondary = AppColorScheme.lightCustomColors.secondary,
    tertiary = AppColorScheme.lightCustomColors.tertiary
)

/**
 * App theme. The user preference [darkModePreference] works like on Android:
 * null = follow system, true = dark, false = light.
 */
@Composable
fun DominionKingdomsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val finalColorScheme = if (darkTheme) {
        AppColorScheme.darkCustomColors
    } else {
        AppColorScheme.lightCustomColors
    }

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = SharedTypography,
        content = content
    )
}
