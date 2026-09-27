package dev.msuhr.dominionkingdoms.shared.platform

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// The Android app does not use the shared UI (it has its own full UI); these
// actuals only make the shared UI compile for the Android target.

@Composable
actual fun rememberImagePainter(imageName: String): Painter {
    val context = LocalContext.current
    val id = context.resources.getIdentifier(imageName, "drawable", context.packageName)
    return if (id != 0) painterResource(id) else ColorPainter(Color(0xFFE1E3DD))
}

actual fun appVersionName(): String = "android"

actual fun formatTimeShort(epochMs: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))

@Composable
actual fun isSystemDarkTheme(): Boolean {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    return (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
