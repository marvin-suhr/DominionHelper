package dev.msuhr.dominionkingdoms.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import dev.msuhr.dominionkingdoms.shared.res.Res
import dev.msuhr.dominionkingdoms.shared.res.allDrawableResources
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIScreen

@Composable
actual fun rememberImagePainter(imageName: String): Painter {
    val resource: DrawableResource? = Res.allDrawableResources[imageName]
    return if (resource != null) painterResource(resource) else ColorPainter(Color(0xFFE1E3DD))
}

actual fun appVersionName(): String {
    val version = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
    return version ?: "1.0"
}

actual fun formatTimeShort(epochMs: Long): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = "HH:mm"
    }
    return formatter.stringFromDate(NSDate(epochMs / 1000.0))
}

@Composable
actual fun isSystemDarkTheme(): Boolean {
    return UIScreen.mainScreen.traitCollection.userInterfaceStyle ==
        UIUserInterfaceStyle.UIUserInterfaceStyleDark
}
