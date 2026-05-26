package dev.msuhr.dominionkingdoms

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WebStories
import androidx.compose.material.icons.outlined.Castle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WebStories
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.msuhr.dominionkingdoms.ui.KingdomViewModel
import dev.msuhr.dominionkingdoms.ui.LibraryViewModel
import dev.msuhr.dominionkingdoms.ui.SettingsViewModel
import dev.msuhr.dominionkingdoms.ui.screens.KingdomsScreen
import dev.msuhr.dominionkingdoms.ui.screens.LibraryScreen
import dev.msuhr.dominionkingdoms.ui.screens.SettingsScreen
import dev.msuhr.dominionkingdoms.utils.Constants

sealed class CurrentScreen(val route: String) {
    object Library : CurrentScreen("library_route")
    object Kingdoms : CurrentScreen("kingdoms_route")
    object Settings : CurrentScreen("settings_route")

    companion object {
        fun fromRoute(route: String?): CurrentScreen {
            return when (route) {
                Library.route -> Library
                Kingdoms.route -> Kingdoms
                Settings.route -> Settings
                // Not possible. When starting up, currentRoute is null.
                //else -> throw IllegalArgumentException("Route $route is not recognized.")
                else -> Constants.START_DESTINATION
            }
        }
    }
}

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val screenRoute: String
)

// Items in the bottom navigation bar
val bottomNavItems = listOf(
    BottomNavItem(
        label = "Library",
        // TODO Decide on icon
        selectedIcon = Icons.Filled.WebStories,//Icons.Filled.Style,//Icons.Filled.LibraryBooks,
        unselectedIcon = Icons.Outlined.WebStories,//Icons.Outlined.Collections,//Icons.Outlined.LibraryBooks,
        screenRoute = CurrentScreen.Library.route
    ),
    BottomNavItem(
        label = "Kingdoms",
        selectedIcon = Icons.Filled.Castle,
        unselectedIcon = Icons.Outlined.Castle,
        screenRoute = CurrentScreen.Kingdoms.route
    ),
    BottomNavItem(
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        screenRoute = CurrentScreen.Settings.route
    )
)

@Composable
fun AppNavigation(
    navController: NavHostController,
    onTitleChanged: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Constants.START_DESTINATION.route,
        enterTransition = { fadeIn(animationSpec = tween(100)) },
        exitTransition = { fadeOut(animationSpec = tween(100)) }
    ) {

        // TODO: Find a way to not pass innerPadding to each screen (rough)
        // Library
        composable(CurrentScreen.Library.route) {
            val viewModel: LibraryViewModel = hiltViewModel()
            LibraryScreen(
                snackbarHostState = snackbarHostState,
                onTitleChanged = onTitleChanged,
                viewModel = viewModel,
                navController = navController,
                innerPadding = innerPadding
            )
        }

        // Kingdoms
        composable(CurrentScreen.Kingdoms.route) {
            val viewModel: KingdomViewModel = hiltViewModel()
            KingdomsScreen(
                onTitleChanged = onTitleChanged,
                snackbarHostState = snackbarHostState,
                viewModel = viewModel,
                navController = navController,
                innerPadding = innerPadding
            )
        }

        // Settings
        composable(CurrentScreen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                onTitleChanged = onTitleChanged,
                snackbarHostState = snackbarHostState,
                viewModel = viewModel,
                navController = navController,
                innerPadding = innerPadding
            )
        }

        // Card Detail Route (Example with argument)
        /*composable(
            route = AppDestinations.CARD_DETAIL_ROUTE,
            arguments = listOf(navArgument(AppDestinations.CARD_DETAIL_ARG_ID) { type =
                NavType.StringType; nullable = true })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString(AppDestinations.CARD_DETAIL_ARG_ID)
            CardDetailScreen(
                navController = navController,
                cardId = cardId,
                onTitleChanged = onTitleChanged
            )
        }*/
    }

    // --- Handle complex sub-navigation that's not just a new screen ---
    // This is where you translate your previous libraryUiState logic into navigation if needed.
    // This is a bit advanced and might be better handled *within* LibraryScreen itself
    // or by making CARD_DETAIL a distinct navigable route.

    // Example: If CARD_DETAIL in Library should navigate to the common CardDetailScreen:
    /*LaunchedEffect(libraryUiState, libraryViewModel.selectedCard.collectAsStateWithLifecycle().value) {
        if (libraryUiState == LibraryUiState.CARD_DETAIL) {
            val card = libraryViewModel.selectedCard.value
            if (card != null) {
                // Check current route to avoid navigating if already there or in a loop
                if (navController.currentDestination?.route != AppDestinations.CARD_DETAIL_ROUTE.replace("{${AppDestinations.CARD_DETAIL_ARG_ID}}", card.name /* or card.id */)) {
                    navController.navigate("${AppDestinations.CARD_DETAIL_ROUTE_PREFIX}/${card.name /* or card.id */}")
                }
            }
        }
    }

    // Similar logic for KingdomViewModel if kingdomUiState.CARD_DETAIL should go to CardDetailScreen
    LaunchedEffect(kingdomUiState, kingdomViewModel.selectedCard.collectAsStateWithLifecycle().value) { // Assuming kingdomVM has selectedCard
        if (kingdomUiState == KingdomUiState.CARD_DETAIL) {
            val card = kingdomViewModel.selectedCard.value // Adjust if card comes from elsewhere
            if (card != null) {
                if (navController.currentDestination?.route != AppDestinations.CARD_DETAIL_ROUTE.replace("{${AppDestinations.CARD_DETAIL_ARG_ID}}", card.name)) {
                    navController.navigate("${AppDestinations.CARD_DETAIL_ROUTE_PREFIX}/${card.name}")
                }
            }
        }
    }*/
}