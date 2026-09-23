package dev.msuhr.dominionkingdoms

import androidx.compose.ui.window.ComposeUIViewController
import dev.msuhr.dominionkingdoms.data.BUNDLED_CARDS_JSON
import dev.msuhr.dominionkingdoms.demo.DemoKingdomApp

/**
 * Entry point called from the Xcode project (iosApp/ContentView.swift).
 * The card data is embedded in the framework (BundledCards.kt) so the iOS
 * demo works without any additional resource setup.
 */
fun MainViewController() = ComposeUIViewController {
    DemoKingdomApp(cardsJson = BUNDLED_CARDS_JSON)
}
