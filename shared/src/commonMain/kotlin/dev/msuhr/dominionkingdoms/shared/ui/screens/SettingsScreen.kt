package dev.msuhr.dominionkingdoms.shared.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.msuhr.dominionkingdoms.shared.ui.SettingsSubScreen
import dev.msuhr.dominionkingdoms.shared.ui.SettingsViewModel
import dev.msuhr.dominionkingdoms.shared.ui.components.SettingsList

@Composable
fun SettingsScreen(
    onTitleChanged: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel,
    innerPadding: PaddingValues
) {
    LaunchedEffect(Unit) { onTitleChanged("Settings") }

    // Clear snackbar when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    // Always maintain separate states - main screen keeps its persistent state
    val mainScreenScrollState = rememberLazyListState()

    val settingsListState = if (uiState.currentSubScreen == SettingsSubScreen.MAIN) {
        mainScreenScrollState
    } else {
        key(uiState.currentSubScreen) {
            rememberLazyListState()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.scrollToTopEvent.collect {
            settingsListState.animateScrollToItem(0)
        }
    }

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

    if (uiState.showResetRulesDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowResetRulesDialog(false) },
            title = { Text("Reset Generation Rules") },
            text = { Text("Are you sure you want to reset all card types, categories, and landscape type rules to their default values?") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetGenerationRules() }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowResetRulesDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}
