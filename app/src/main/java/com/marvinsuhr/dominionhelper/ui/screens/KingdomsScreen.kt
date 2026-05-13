package com.marvinsuhr.dominionhelper.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import com.marvinsuhr.dominionhelper.ui.KingdomUiState
import com.marvinsuhr.dominionhelper.utils.calculatePadding
import com.marvinsuhr.dominionhelper.ui.KingdomViewModel
import com.marvinsuhr.dominionhelper.ui.components.CardDetailPager
import com.marvinsuhr.dominionhelper.ui.components.KingdomCardList
import com.marvinsuhr.dominionhelper.ui.components.KingdomList
import kotlinx.coroutines.launch

@Composable
fun KingdomsScreen(
    onTitleChanged: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: KingdomViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    val topBarTitle by viewModel.topBarTitle.collectAsState()
    LaunchedEffect(topBarTitle) { onTitleChanged(topBarTitle) }

    val kingdomListState = rememberLazyListState()

    val uiState by viewModel.uiState.collectAsState()
    val kingdom by viewModel.kingdom.collectAsState()
    val playerCount by viewModel.playerCount.collectAsState()
    val isDismissEnabled by viewModel.isCardDismissalEnabled.collectAsState()
    val selectedCard by viewModel.selectedCard.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val allKingdoms by viewModel.allKingdoms.collectAsState()
    val hasOwnedExpansions by viewModel.hasOwnedExpansions.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    Log.i(
        "MainActivity",
        "Kingdom Screen Content. UI State: ${viewModel.uiState.collectAsState().value}"
    )

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

    BackHandler {
        // First, let the ViewModel handle back navigation (e.g., from card detail to kingdom list)
        if (!viewModel.handleBackNavigation()) {
            // If ViewModel didn't handle it, navigate at the app level
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            }
        }
    }

    when (uiState) {

        KingdomUiState.KINGDOM_LIST -> {

            KingdomList(
                kingdomList = allKingdoms,
                hasOwnedExpansions = hasOwnedExpansions,
                onGenerateKingdom = { viewModel.getRandomKingdom() },
                onKingdomClicked = { viewModel.selectKingdom(it) },
                onDeleteClick = { viewModel.deleteKingdom(it.uuid) },
                onFavoriteClick = { viewModel.toggleFavorite(it) },
                onKingdomNameChange = { uuid, newName -> viewModel.updateKingdomName(uuid, newName) },
                listState = kingdomListState,
                paddingValues = calculatePadding(innerPadding)
            )
        }

        KingdomUiState.LOADING -> {
            //KingdomListSkeleton()#
        }

        // Show generated kingdom
        KingdomUiState.SINGLE_KINGDOM -> {
            Log.i(
                "MainView",
                "View card list (Random: ${kingdom.randomCards.size}, Dependent: ${kingdom.dependentCards.size}, Basic: ${kingdom.basicCards.size} cards, Landscape: ${kingdom.landscapeCards.size})"
            )
            KingdomCardList(
                kingdom = kingdom,
                onCardClick = { viewModel.selectCard(it) },
                selectedPlayers = playerCount,
                onPlayerCountChange = {
                    viewModel.userChangedPlayerCount(it)
                },
                listState = kingdomListState,
                isDismissEnabled = isDismissEnabled,
                onCardDismissed = { viewModel.onCardDismissed(it) },
                paddingValues = calculatePadding(innerPadding)
            )
        }

        KingdomUiState.CARD_DETAIL -> {
            Log.i("MainView", "View card detail (${selectedCard?.name})")
            CardDetailPager(
                cardList = kingdom.getAllCards(),
                initialCard = selectedCard!!,
                onClick = { viewModel.clearSelectedCard() },
                onPageChanged = { viewModel.selectCard(it) },
                paddingValues = calculatePadding(innerPadding)
            )
        }
    }
}
