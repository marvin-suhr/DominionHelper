package dev.msuhr.dominionkingdoms

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.msuhr.dominionkingdoms.data.UserPrefsRepository
import dev.msuhr.dominionkingdoms.ui.theme.DominionKingdomsTheme
import dev.msuhr.dominionkingdoms.ui.theme.ThemeColorProvider
import dev.msuhr.dominionkingdoms.ui.KingdomViewModel
import dev.msuhr.dominionkingdoms.ui.LibraryViewModel
import dev.msuhr.dominionkingdoms.ui.SettingsViewModel
import dev.msuhr.dominionkingdoms.ui.ScreenViewModel
import dev.msuhr.dominionkingdoms.ui.KingdomUiState
import dev.msuhr.dominionkingdoms.ui.components.TopBar
import dagger.hilt.android.AndroidEntryPoint
import dev.msuhr.dominionkingdoms.utils.Constants
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPrefsRepository: UserPrefsRepository

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isSystemDarkMode = isSystemInDarkTheme()
            val darkModePreference by userPrefsRepository.isDarkMode.collectAsState(
                initial = null
            )
            val darkTheme = darkModePreference ?: isSystemDarkMode

            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }

            val useSystemTheme by userPrefsRepository.useSystemTheme.collectAsState(initial = true)

            // Get the appropriate color scheme
            // Pass the actual resolved dark mode (not null) so ThemeColorProvider
            // can correctly select between dark/light colors
            val colorScheme = ThemeColorProvider.getColorScheme(
                useSystemTheme = useSystemTheme,
                isDarkMode = darkModePreference,
                activity = this
            )

            DominionKingdomsTheme(
                darkTheme = darkTheme,
                colorScheme = colorScheme
            ) {
                val coroutineScope = rememberCoroutineScope()

                // Database reset dialog
                val showResetDialog by userPrefsRepository.showDatabaseResetDialog.collectAsState(initial = false)
                if (showResetDialog) {
                    AlertDialog(
                        onDismissRequest = { /* Don't allow dismiss by clicking outside */ },
                        title = { Text("Database Updated") },
                        text = { Text("The app's database has been refreshed to support new features. Your local settings have been preserved, but the kingdom library has been reset.") },
                        confirmButton = {
                            TextButton(onClick = {
                                coroutineScope.launch {
                                    userPrefsRepository.setShowDatabaseResetDialog(false)
                                }
                            }) {
                                Text("OK")
                            }
                        }
                    )
                }

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val currentScreen = CurrentScreen.fromRoute(currentRoute)

                val snackbarHostState = remember { SnackbarHostState() }
                val topAppBarState = rememberTopAppBarState()
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

                var currentTopBarTitle by rememberSaveable { mutableStateOf("") }
                val onTitleChangedLambda = { newTitle: String ->
                    currentTopBarTitle = newTitle
                }

                // Get ViewModels for the current screen using the navBackStackEntry
                // This ensures we get the same instances as in the navigation composables
                val currentLibraryViewModel: LibraryViewModel? = navBackStackEntry?.let {
                    when (currentScreen) {
                        CurrentScreen.Library -> hiltViewModel(it)
                        else -> null
                    }
                }
                val currentKingdomViewModel: KingdomViewModel? = navBackStackEntry?.let {
                    when (currentScreen) {
                        CurrentScreen.Kingdoms -> hiltViewModel(it)
                        else -> null
                    }
                }
                val currentSettingsViewModel: SettingsViewModel? = navBackStackEntry?.let {
                    when (currentScreen) {
                        CurrentScreen.Settings -> hiltViewModel(it)
                        else -> null
                    }
                }

                // Get the current ViewModel to determine top bar visibility
                val currentViewModel: ScreenViewModel? = when (currentScreen) {
                    CurrentScreen.Library -> currentLibraryViewModel
                    CurrentScreen.Kingdoms -> currentKingdomViewModel
                    CurrentScreen.Settings -> currentSettingsViewModel
                }
                val showTopAppBar by currentViewModel?.showTopAppBar?.collectAsState() ?: remember { mutableStateOf(false) }
                val showBackButton by currentViewModel?.showBackButton?.collectAsState() ?: remember { mutableStateOf(false) }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        if (showTopAppBar) {
                            TopBar(
                                title = currentTopBarTitle,
                                showBackButton = showBackButton,
                                onBackButtonClicked = {
                                    // Let the current ViewModel handle back navigation first
                                    if (currentViewModel?.handleBackNavigation() != true) {
                                        // If ViewModel didn't handle it, navigate at app level
                                        if (navController.previousBackStackEntry != null) {
                                            navController.popBackStack()
                                        } else {
                                            finish()
                                        }
                                    }
                                },
                                currentScreen = currentScreen,
                                onSortTypeSelected = { currentViewModel?.onSortTypeSelected(it) },
                                selectedSortType = currentViewModel?.currentAppSortType?.collectAsState()?.value,
                                scrollBehavior = scrollBehavior,
                                showGridViewToggle = currentScreen == CurrentScreen.Kingdoms,
                                isGridViewEnabled = currentKingdomViewModel?.isGridViewEnabled?.collectAsState()?.value ?: false,
                                onGridViewToggle = { currentKingdomViewModel?.toggleGridView() }
                            )
                        }
                    },
                    floatingActionButton = {
                        // Only show FAB when on Kingdoms screen AND in KINGDOM_LIST state (not viewing a specific kingdom)
                        if (currentScreen == CurrentScreen.Kingdoms) {
                            val kingdomUiState by currentKingdomViewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(null) }
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
                                    },
                                    // Explicitly define the colors to ensure perfect contrast inside the active pill
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primary, // The gold pill background
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary, // The dark icon inside the gold pill
                                        selectedTextColor = MaterialTheme.colorScheme.primary, // The gold text under the pill
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, // Muted grey for inactive icons
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant // Muted grey for inactive text
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->

                    AppNavigation(
                        navController = navController,
                        onTitleChanged = onTitleChangedLambda,
                        snackbarHostState = snackbarHostState,
                        innerPadding = innerPadding
                    )
                }
            }
        }
    }
}
