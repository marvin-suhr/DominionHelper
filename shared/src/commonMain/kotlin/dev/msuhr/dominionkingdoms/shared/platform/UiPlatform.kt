package dev.msuhr.dominionkingdoms.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

/**
 * UI platform abstractions for the shared Compose UI.
 * The Android app keeps its own implementations (R.drawable / Coil);
 * these actuals only exist so the shared UI can compile for both targets.
 */

/**
 * Resolves a card / expansion / icon image by its asset base name
 * (e.g. "village", "set_dark_ages", "cat_cantrip") to a painter.
 * Returns a neutral placeholder when the asset is missing.
 */
@Composable
expect fun rememberImagePainter(imageName: String): Painter

/** Human readable app version, shown at the bottom of the Settings screen. */
expect fun appVersionName(): String

/** Formats an epoch-ms timestamp as a short local time, e.g. "18:32". */
expect fun formatTimeShort(epochMs: Long): String

/** Current system dark mode (equivalent of isSystemInDarkTheme). */
@Composable
expect fun isSystemDarkTheme(): Boolean
