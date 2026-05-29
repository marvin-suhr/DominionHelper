package dev.msuhr.dominionkingdoms.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.msuhr.dominionkingdoms.ui.SettingsSubScreen
import dev.msuhr.dominionkingdoms.ui.SettingsViewModel
import dev.msuhr.dominionkingdoms.ui.components.SettingsList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onTitleChanged: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues
) {

    Log.i("MainActivity", "Settings Screen Content. UI State: not implemented")

    LaunchedEffect(Unit) { onTitleChanged("Settings") }

    val settingsListState = rememberLazyListState()

    // Clear snackbar when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    /*LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            applicationScope.launch {
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                viewModel.clearError()
            }
        }
    }*/

    LaunchedEffect(Unit) {
        viewModel.scrollToTopEvent.collect {
            settingsListState.animateScrollToItem(0)
        }
    }

    BackHandler {
        // First, let the ViewModel handle back navigation
        if (!viewModel.handleBackNavigation()) {
            // If ViewModel didn't handle it, navigate at the app level
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    val title = when (uiState.currentSubScreen) {
        SettingsSubScreen.MAIN -> "Settings"
        SettingsSubScreen.CARD_TYPES -> "Card Types"
        SettingsSubScreen.CARD_CATEGORIES -> "Card Categories"
        SettingsSubScreen.CARD_COSTS -> "Card Costs"
        SettingsSubScreen.LANDSCAPES -> "Landscape Types"
    }
    LaunchedEffect(title) { onTitleChanged(title) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = innerPadding.calculateBottomPadding())
    ) {
        SettingsList(
            uiState.settings,
            listState = settingsListState,
            paddingValues = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                start = 8.dp,
                end = 8.dp,
                bottom = 8.dp
            ),
            showVersionInfo = uiState.currentSubScreen == SettingsSubScreen.MAIN
        )
    }
}
