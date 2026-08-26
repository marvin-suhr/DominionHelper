package dev.msuhr.dominionkingdoms

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.msuhr.dominionkingdoms.ui.KingdomUiState
import dev.msuhr.dominionkingdoms.ui.KingdomViewModel
import dev.msuhr.dominionkingdoms.ui.LibraryViewModel
import dev.msuhr.dominionkingdoms.ui.MainViewModel
import dev.msuhr.dominionkingdoms.ui.ScreenViewModel
import dev.msuhr.dominionkingdoms.ui.SettingsViewModel
import dev.msuhr.dominionkingdoms.ui.components.TopBar
import dev.msuhr.dominionkingdoms.ui.theme.DominionKingdomsTheme
import dev.msuhr.dominionkingdoms.ui.theme.ThemeColorProvider
import dev.msuhr.dominionkingdoms.utils.Constants

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // 1. Theme Configuration & Flow Collection
            val isSystemDarkMode = isSystemInDarkTheme()
            val darkModePreference by mainViewModel.isDarkMode.collectAsStateWithLifecycle()
            val useSystemTheme by mainViewModel.useSystemTheme.collectAsStateWithLifecycle()

            val darkTheme = darkModePreference ?: isSystemDarkMode
            val colorScheme = ThemeColorProvider.getColorScheme(
                useSystemTheme = useSystemTheme,
                isDarkMode = darkModePreference,
                activity = this
            )

            // Safely updating system bars inside a SideEffect instead of raw composition
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            DominionKingdomsTheme(darkTheme = darkTheme, colorScheme = colorScheme) {

                // 3. Navigation Setup
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val currentScreen = CurrentScreen.fromRoute(currentRoute)

                // 4. UI Layout Components
                val snackbarHostState = remember { SnackbarHostState() }
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

                var currentTopBarTitle by rememberSaveable { mutableStateOf("") }

                // Resolve target ViewModels based on current navigation scope
                val currentLibraryViewModel = resolveViewModel<LibraryViewModel>(currentScreen, CurrentScreen.Library, navBackStackEntry)
                val currentKingdomViewModel = resolveViewModel<KingdomViewModel>(currentScreen, CurrentScreen.Kingdoms, navBackStackEntry)
                val currentSettingsViewModel = resolveViewModel<SettingsViewModel>(currentScreen, CurrentScreen.Settings, navBackStackEntry)

                // Observe the new card update state
                val showUpdateDialog by mainViewModel.showCardUpdateDialog.collectAsStateWithLifecycle()
                if (showUpdateDialog) {
                    CardUpdateNotificationDialog(
                        onDismiss = {
                            mainViewModel.dismissCardUpdateDialog()
                            currentKingdomViewModel?.refresh()
                        }
                    )
                }

                val currentViewModel: ScreenViewModel? = when (currentScreen) {
                    CurrentScreen.Library -> currentLibraryViewModel
                    CurrentScreen.Kingdoms -> currentKingdomViewModel
                    CurrentScreen.Settings -> currentSettingsViewModel
                }

                // Top App Bar Controls
                val showTopAppBar by currentViewModel?.showTopAppBar?.collectAsStateWithLifecycle()
                    ?: remember { mutableStateOf(false) }

                val showBackButton by currentViewModel?.showBackButton?.collectAsStateWithLifecycle()
                    ?: remember { mutableStateOf(false) }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        if (showTopAppBar) {
                            TopBar(
                                title = currentTopBarTitle,
                                showBackButton = showBackButton,
                                onBackButtonClicked = {
                                    if (currentViewModel?.handleBackNavigation() != true) {
                                        if (navController.previousBackStackEntry != null) {
                                            navController.popBackStack()
                                        } else {
                                            finish()
                                        }
                                    }
                                },
                                currentScreen = currentScreen,
                                onSortTypeSelected = { currentViewModel?.onSortTypeSelected(it) },
                                selectedSortType = currentViewModel?.currentAppSortType?.collectAsStateWithLifecycle()?.value,
                                scrollBehavior = scrollBehavior,
                                showGridViewToggle = currentScreen == CurrentScreen.Kingdoms,
                                isGridViewEnabled = currentKingdomViewModel?.isGridViewEnabled?.collectAsStateWithLifecycle()?.value ?: false,
                                onGridViewToggle = { currentKingdomViewModel?.toggleGridView() }
                            )
                        }
                    },
                    floatingActionButton = {
                        if (currentScreen == CurrentScreen.Kingdoms) {

                            val kingdomUiState by currentKingdomViewModel?.uiState?.collectAsStateWithLifecycle()
                                ?: remember { mutableStateOf(null) }

                            if (kingdomUiState == KingdomUiState.KINGDOM_LIST) {
                                ExtendedFloatingActionButton(
                                    onClick = { currentKingdomViewModel?.getRandomKingdom() },
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
                                val isSelected = item.screenRoute == currentRoute

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        Log.i("NavigationBarItem", "Selected ${item.label} (Previous: $currentRoute)")
                                        if (currentRoute != item.screenRoute) {
                                            navController.navigate(item.screenRoute) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        } else {
                                            when (currentScreen) {
                                                CurrentScreen.Library -> currentLibraryViewModel?.triggerScrollToTop()
                                                CurrentScreen.Kingdoms -> currentKingdomViewModel?.triggerScrollToTop()
                                                CurrentScreen.Settings -> currentSettingsViewModel?.triggerScrollToTop()
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
                        navController = navController,
                        onTitleChanged = { currentTopBarTitle = it },
                        snackbarHostState = snackbarHostState,
                        innerPadding = innerPadding
                    )
                }
            }
        }
    }

    /**
     * Inline helper to cleanly isolate back-stack ViewModel resolution
     */
    @Composable
    private inline fun <reified T : androidx.lifecycle.ViewModel> resolveViewModel(
        currentScreen: CurrentScreen,
        targetScreen: CurrentScreen,
        navBackStackEntry: NavBackStackEntry?
    ): T? = if (currentScreen == targetScreen && navBackStackEntry != null) hiltViewModel(navBackStackEntry) else null

    @Composable
    private fun CardUpdateNotificationDialog(onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = { /* Force explicit interaction */ },
            title = {
                Text(
                    text = "Database structure updated",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Hi and thanks for testing!\n\nUnfortunately, I had to overthink some database decisions and therefore had to delete user data concerning cards (banned and favorite state).\n\nSorry for the inconvenience, this shouldn't happen again!"
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Alright",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}
