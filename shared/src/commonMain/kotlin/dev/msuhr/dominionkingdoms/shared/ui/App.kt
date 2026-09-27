package dev.msuhr.dominionkingdoms.shared.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WebStories
import androidx.compose.material.icons.outlined.Castle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WebStories
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.msuhr.dominionkingdoms.shared.platform.isSystemDarkTheme
import dev.msuhr.dominionkingdoms.shared.ui.components.TopBar
import dev.msuhr.dominionkingdoms.shared.ui.screens.KingdomsScreen
import dev.msuhr.dominionkingdoms.shared.ui.screens.LibraryScreen
import dev.msuhr.dominionkingdoms.shared.ui.screens.SettingsScreen
import dev.msuhr.dominionkingdoms.shared.ui.theme.DominionKingdomsTheme
import dev.msuhr.dominionkingdoms.shared.utils.Constants

/**
 * Top-level navigation destinations - the shared equivalent of the Android
 * app's Navigation.kt (Navigation NavHost is replaced by simple state since
 * all sub-navigation is state based inside the ViewModels anyway).
 */
enum class CurrentScreen(val route: String) {
    Library("library_route"),
    Kingdoms("kingdoms_route"),
    Settings("settings_route");

    companion object {
        val START_DESTINATION = Kingdoms

        fun fromRoute(route: String?): CurrentScreen {
            return entries.firstOrNull { it.route == route } ?: START_DESTINATION
        }
    }
}

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val screenRoute: CurrentScreen
)

// Items in the bottom navigation bar
val bottomNavItems = listOf(
    BottomNavItem(
        label = "Library",
        selectedIcon = Icons.Filled.WebStories,
        unselectedIcon = Icons.Outlined.WebStories,
        screenRoute = CurrentScreen.Library
    ),
    BottomNavItem(
        label = "Kingdoms",
        selectedIcon = Icons.Filled.Castle,
        unselectedIcon = Icons.Outlined.Castle,
        screenRoute = CurrentScreen.Kingdoms
    ),
    BottomNavItem(
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        screenRoute = CurrentScreen.Settings
    )
)

/**
 * The full Dominion Kingdoms app - a port of the Android app's
 * MainActivity + Navigation for iOS (and other Compose Multiplatform targets).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KingdomsApp(
    container: dev.msuhr.dominionkingdoms.shared.AppContainer
) {
    // 1. Theme configuration (isDarkMode: null = system default)
    val darkModePreference by container.mainViewModel.isDarkMode.collectAsState()
    val isSystemDarkMode = isSystemDarkTheme()
    val darkTheme = darkModePreference ?: isSystemDarkMode

    DominionKingdomsTheme(darkTheme = darkTheme) {

        // 2. Navigation setup (simple state; three top-level destinations)
        var currentScreen by rememberSaveable { mutableStateOf(CurrentScreen.START_DESTINATION) }

        val snackbarHostState = remember { SnackbarHostState() }
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

        var currentTopBarTitle by rememberSaveable { mutableStateOf("") }

        val currentLibraryViewModel = container.libraryViewModel
        val currentKingdomViewModel = container.kingdomViewModel
        val currentSettingsViewModel = container.settingsViewModel

        val currentViewModel: ScreenViewModel? = when (currentScreen) {
            CurrentScreen.Library -> currentLibraryViewModel
            CurrentScreen.Kingdoms -> currentKingdomViewModel
            CurrentScreen.Settings -> currentSettingsViewModel
        }

        // Top App Bar controls
        val showTopAppBar by currentViewModel?.showTopAppBar?.collectAsState()
            ?: remember { mutableStateOf(false) }

        val showBackButton by currentViewModel?.showBackButton?.collectAsState()
            ?: remember { mutableStateOf(false) }

        val kingdomUiState by currentKingdomViewModel.uiState.collectAsState()
        val kingdomsTab by currentKingdomViewModel.kingdomsTab.collectAsState()

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (showTopAppBar) {
                    TopBar(
                        title = currentTopBarTitle,
                        showBackButton = showBackButton,
                        onBackButtonClicked = {
                            // The ViewModels handle their sub-state back
                            // navigation (card detail -> kingdom -> list)
                            currentViewModel?.handleBackNavigation()
                        },
                        currentScreen = currentScreen,
                        onSortTypeSelected = { currentViewModel?.onSortTypeSelected(it) },
                        selectedSortType = currentViewModel?.currentAppSortType?.collectAsState()?.value,
                        scrollBehavior = scrollBehavior,
                        showGridViewToggle = currentScreen == CurrentScreen.Kingdoms &&
                            kingdomUiState == KingdomUiState.SINGLE_KINGDOM,
                        isGridViewEnabled = currentKingdomViewModel.isGridViewEnabled.collectAsState().value,
                        onGridViewToggle = { currentKingdomViewModel.toggleGridView() },
                        customActions = { }
                    )
                }
            },
            floatingActionButton = {
                if (currentScreen == CurrentScreen.Kingdoms) {
                    if (kingdomUiState == KingdomUiState.KINGDOM_LIST &&
                        kingdomsTab == KingdomViewModel.KingdomsTab.MY
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = { currentKingdomViewModel.getRandomKingdom() },
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "FAB to generate a new kingdom",
                                modifier = Modifier.padding(end = Constants.PADDING_SMALL)
                            )
                            Text("Generate Kingdom")
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = item.screenRoute == currentScreen

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentScreen != item.screenRoute) {
                                    currentScreen = item.screenRoute
                                } else {
                                    when (currentScreen) {
                                        CurrentScreen.Library -> currentLibraryViewModel.triggerScrollToTop()
                                        CurrentScreen.Kingdoms -> currentKingdomViewModel.triggerScrollToTop()
                                        CurrentScreen.Settings -> currentSettingsViewModel.triggerScrollToTop()
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            AppNavigation(
                currentScreen = currentScreen,
                onScreenChanged = { currentScreen = it },
                onTitleChanged = { currentTopBarTitle = it },
                snackbarHostState = snackbarHostState,
                innerPadding = innerPadding,
                container = container
            )
        }
    }
}

@Composable
fun AppNavigation(
    currentScreen: CurrentScreen,
    onScreenChanged: (CurrentScreen) -> Unit,
    onTitleChanged: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    innerPadding: PaddingValues,
    container: dev.msuhr.dominionkingdoms.shared.AppContainer
) {
    when (currentScreen) {
        CurrentScreen.Library -> LibraryScreen(
            snackbarHostState = snackbarHostState,
            onTitleChanged = onTitleChanged,
            viewModel = container.libraryViewModel,
            innerPadding = innerPadding
        )

        CurrentScreen.Kingdoms -> KingdomsScreen(
            onTitleChanged = onTitleChanged,
            snackbarHostState = snackbarHostState,
            viewModel = container.kingdomViewModel,
            innerPadding = innerPadding
        )

        CurrentScreen.Settings -> SettingsScreen(
            onTitleChanged = onTitleChanged,
            snackbarHostState = snackbarHostState,
            viewModel = container.settingsViewModel,
            innerPadding = innerPadding
        )
    }
}
