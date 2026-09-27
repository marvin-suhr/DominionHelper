package dev.msuhr.dominionkingdoms

import androidx.compose.ui.window.ComposeUIViewController
import dev.msuhr.dominionkingdoms.data.InMemoryAppDataStore
import dev.msuhr.dominionkingdoms.shared.AppContainer
import dev.msuhr.dominionkingdoms.shared.ui.KingdomsApp

/**
 * Entry point called from the Xcode project (iosApp/ContentView.swift).
 * Runs the full shared Dominion Kingdoms UI (a port of the Android app) on
 * top of in-memory data sources over the bundled card / set databases.
 */
fun MainViewController() = ComposeUIViewController {
    val store = InMemoryAppDataStore()
    val container = AppContainer(
        cardSource = store.cardSource,
        expansionSource = store.expansionSource,
        prefsSource = store.prefsSource,
        kingdomStore = store.kingdomStore
    )
    KingdomsApp(container = container)
}
