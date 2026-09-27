package dev.msuhr.dominionkingdoms.shared

import dev.msuhr.dominionkingdoms.CardDependencyResolver
import dev.msuhr.dominionkingdoms.KingdomGenerator
import dev.msuhr.dominionkingdoms.shared.data.AppCardSource
import dev.msuhr.dominionkingdoms.shared.data.AppExpansionSource
import dev.msuhr.dominionkingdoms.shared.data.AppPrefsSource
import dev.msuhr.dominionkingdoms.shared.data.KingdomStore
import dev.msuhr.dominionkingdoms.shared.ui.KingdomViewModel
import dev.msuhr.dominionkingdoms.shared.ui.LibraryViewModel
import dev.msuhr.dominionkingdoms.shared.ui.MainViewModel
import dev.msuhr.dominionkingdoms.shared.ui.SettingsViewModel

/**
 * Composition root for the shared app - the equivalent of the Android app's
 * Hilt di/AppModule.kt. The platform entry point (iOS MainViewController)
 * constructs this with its data-source implementations.
 */
class AppContainer(
    cardSource: AppCardSource,
    expansionSource: AppExpansionSource,
    prefsSource: AppPrefsSource,
    kingdomStore: KingdomStore
) {
    val mainViewModel: MainViewModel = MainViewModel(prefsSource)

    private val cardDependencyResolver: CardDependencyResolver =
        CardDependencyResolver(cardSource, prefsSource)

    private val kingdomGenerator: KingdomGenerator = KingdomGenerator(
        cardDao = cardSource,
        expansionDao = expansionSource,
        userPrefsRepository = prefsSource,
        cardDependencyResolver = cardDependencyResolver
    )

    val kingdomViewModel: KingdomViewModel = KingdomViewModel(
        kingdomRepository = kingdomStore,
        expansionDao = expansionSource,
        kingdomGenerator = kingdomGenerator,
        cardDependencyResolver = cardDependencyResolver,
        userPrefsRepository = prefsSource,
        cardDao = cardSource
    )

    val libraryViewModel: LibraryViewModel = LibraryViewModel(
        cardDao = cardSource,
        expansionDao = expansionSource
    )

    val settingsViewModel: SettingsViewModel = SettingsViewModel(
        userPrefsRepository = prefsSource
    )
}
