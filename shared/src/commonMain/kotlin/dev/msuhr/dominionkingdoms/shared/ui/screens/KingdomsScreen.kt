package dev.msuhr.dominionkingdoms.shared.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import dev.msuhr.dominionkingdoms.shared.ui.KingdomUiState
import dev.msuhr.dominionkingdoms.shared.ui.KingdomViewModel
import dev.msuhr.dominionkingdoms.shared.ui.components.CardDetailPager
import dev.msuhr.dominionkingdoms.shared.ui.components.KingdomCardList
import dev.msuhr.dominionkingdoms.shared.ui.components.KingdomList
import dev.msuhr.dominionkingdoms.shared.ui.components.KingdomsTabToggle
import dev.msuhr.dominionkingdoms.shared.utils.Constants
import dev.msuhr.dominionkingdoms.shared.utils.calculatePadding
import kotlinx.coroutines.launch

@Composable
fun KingdomsScreen(
    onTitleChanged: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: KingdomViewModel,
    innerPadding: PaddingValues
) {
    val topBarTitle by viewModel.topBarTitle.collectAsState()
    LaunchedEffect(topBarTitle) { onTitleChanged(topBarTitle) }

    val kingdomListState = rememberLazyListState()
    val singleKingdomState = rememberLazyGridState()

    val uiState by viewModel.uiState.collectAsState()
    val kingdom by viewModel.kingdom.collectAsState()
    val playerCount by viewModel.playerCount.collectAsState()
    val isDismissEnabled by viewModel.isCardDismissalEnabled.collectAsState()
    val isLandscapeDismissEnabled by viewModel.isLandscapeDismissalEnabled.collectAsState()
    val selectedCard by viewModel.selectedCard.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isGridViewEnabled by viewModel.isGridViewEnabled.collectAsState()
    val pendingDelete by viewModel.pendingDelete.collectAsState()

    val allKingdoms by viewModel.allKingdoms.collectAsState()
    val hasOwnedExpansions by viewModel.hasOwnedExpansions.collectAsState()
    val kingdomsTab by viewModel.kingdomsTab.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    // Reset single kingdom scroll state when returning to the list
    LaunchedEffect(uiState) {
        if (uiState == KingdomUiState.KINGDOM_LIST) {
            singleKingdomState.scrollToItem(0)
        }
    }

    LaunchedEffect(kingdom.uuid) {
        singleKingdomState.scrollToItem(0)
    }

    // Clear snackbar and error when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearError()
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.scrollToTopEvent.collect {
            kingdomListState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                viewModel.clearError()
            }
        }
    }

    LaunchedEffect(pendingDelete) {
        pendingDelete?.let { deletedKingdom ->
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Deleted \"${deletedKingdom.name}\"",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoDelete()
                } else {
                    viewModel.confirmPendingDelete()
                }
            }
        }
    }

    when (uiState) {

        KingdomUiState.KINGDOM_LIST -> {
            val listPadding = PaddingValues(
                top = Constants.PADDING_SMALL,
                start = Constants.PADDING_SMALL,
                end = Constants.PADDING_SMALL,
                bottom = Constants.PADDING_SMALL + innerPadding.calculateBottomPadding()
            )

            Column(modifier = Modifier.fillMaxSize()) {
                KingdomsTabToggle(
                    selected = kingdomsTab,
                    onSelect = { viewModel.selectKingdomsTab(it) },
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 4.dp,
                        bottom = 4.dp
                    )
                )

                when (kingdomsTab) {
                    KingdomViewModel.KingdomsTab.MY -> KingdomList(
                        kingdomList = allKingdoms,
                        hasOwnedExpansions = hasOwnedExpansions,
                        onKingdomClicked = { viewModel.selectKingdom(it) },
                        onDeleteClick = { viewModel.deleteKingdom(it.uuid) },
                        onFavoriteClick = { viewModel.toggleFavorite(it) },
                        onKingdomNameChange = { uuid, newName -> viewModel.updateKingdomName(uuid, newName) },
                        listState = kingdomListState,
                        paddingValues = listPadding
                    )

                    KingdomViewModel.KingdomsTab.SHARED -> {
                        // The community tab is backed by the kingdom-sharing
                        // web service (Android-only for now).
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No kingdoms shared yet",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        KingdomUiState.LOADING -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // Show generated kingdom
        KingdomUiState.SINGLE_KINGDOM -> {
            KingdomCardList(
                kingdom = kingdom,
                onCardClick = { viewModel.selectCard(it) },
                selectedPlayers = playerCount,
                onPlayerCountChange = {
                    viewModel.userChangedPlayerCount(it)
                },
                listState = singleKingdomState,
                isCardDismissEnabled = isDismissEnabled,
                isLandscapeDismissEnabled = isLandscapeDismissEnabled,
                onCardDismissed = { viewModel.onCardDismissed(it) },
                paddingValues = calculatePadding(innerPadding),
                isGridViewEnabled = isGridViewEnabled
            )
        }

        KingdomUiState.CARD_DETAIL -> {
            CardDetailPager(
                cardList = kingdom.getAllCards(),
                initialCard = selectedCard ?: return,
                onClick = { viewModel.clearSelectedCard() },
                onPageChanged = { viewModel.selectCard(it) },
                paddingValues = calculatePadding(innerPadding),
                onFavorite = { viewModel.toggleCardFavorite(it) },
                onBan = { viewModel.toggleCardEnabled(it) }
            )
        }
    }
}
