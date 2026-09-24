package dev.msuhr.dominionkingdoms.ui.screens

import android.util.Log
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.msuhr.dominionkingdoms.MainActivity
import dev.msuhr.dominionkingdoms.ui.KingdomUiState
import dev.msuhr.dominionkingdoms.utils.Constants
import dev.msuhr.dominionkingdoms.utils.formatTimeShort
import dev.msuhr.dominionkingdoms.utils.calculatePadding
import dev.msuhr.dominionkingdoms.ui.KingdomViewModel
import dev.msuhr.dominionkingdoms.ui.components.CardDetailPager
import dev.msuhr.dominionkingdoms.ui.components.KingdomCardList
import dev.msuhr.dominionkingdoms.ui.components.KingdomsTabToggle
import dev.msuhr.dominionkingdoms.ui.components.SharedKingdomList
import dev.msuhr.dominionkingdoms.ui.components.KingdomList
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
    val uploadingKingdomUuid by viewModel.uploadingKingdomUuid.collectAsState()
    val kingdomsTab by viewModel.kingdomsTab.collectAsState()
    val sharedKingdomItems by viewModel.sharedKingdomItems.collectAsState()
    val sharedRatingLabels by viewModel.sharedRatingLabels.collectAsState()
    val isSharedLoading by viewModel.isSharedLoading.collectAsState()
    val sharedHasMore by viewModel.sharedHasMore.collectAsState()
    val uploadRequested by viewModel.uploadRequested.collectAsState()
    val uploadQuota by viewModel.uploadQuota.collectAsState()
    val sharedListState = rememberLazyListState()

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Deep links: open shared kingdoms pushed from MainActivity's VIEW intent
    val pendingSharedKingdomId by (context as? MainActivity)?.pendingSharedKingdomId
        ?.collectAsState() ?: remember { mutableStateOf(null) }
    LaunchedEffect(pendingSharedKingdomId) {
        val kingdomId = pendingSharedKingdomId ?: return@LaunchedEffect
        (context as? MainActivity)?.consumeSharedKingdomLink()
        viewModel.openSharedKingdom(kingdomId)
    }

    // Upload result: fire the system share sheet with the share URL + confirm via snackbar
    LaunchedEffect(Unit) {
        viewModel.shareUrlEvent.collect { url ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out this Dominion kingdom: $url")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share kingdom"))
            snackbarHostState.showSnackbar(message = "Kingdom uploaded!", duration = SnackbarDuration.Short)
        }
    }

    Log.i(
        "MainActivity",
        "Kingdom Screen Content. UI State: ${viewModel.uiState.collectAsState().value}"
    )

    // Reset single kingdom scroll state when returning to the list
    LaunchedEffect(uiState) {
        if (uiState == KingdomUiState.KINGDOM_LIST) {
            singleKingdomState.scrollToItem(0)
        }
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
                    // Snackbar dismissed - confirm the delete
                    viewModel.confirmPendingDelete()
                }
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

    // Upload confirmation: makes sure the user knows the kingdom becomes public
    // and informs about the daily upload limit.
    if (uploadRequested && !kingdom.isEmpty()) {
        val limitReached = (uploadQuota?.remaining ?: 1) <= 0
        val dailyLimit = uploadQuota?.limit ?: 5
        val remaining = uploadQuota?.remaining

        AlertDialog(
            onDismissRequest = { viewModel.consumeUploadRequest() },
            title = { Text(if (limitReached) "Daily share limit reached" else "Share this kingdom?") },
            text = {
                Column {
                    if (!limitReached) {
                        Text(
                            "'" + kingdom.name + "' will be uploaded to kingdoms.msuhr.dev and will be " +
                                "publicly visible. Anyone with the link can view it, rate it, and open it in their app."
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when {
                            limitReached ->
                                "You have used all " + dailyLimit + " of today's uploads. " +
                                    (uploadQuota?.resetAt?.let { "You can share again after " + formatTimeShort(it) + "." } ?: "")
                            remaining != null ->
                                "You have $remaining of $dailyLimit uploads left today."
                            else ->
                                "You can share up to $dailyLimit kingdoms per day."
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !limitReached && uploadingKingdomUuid == null,
                    onClick = {
                        viewModel.consumeUploadRequest()
                        viewModel.uploadKingdom(kingdom)
                    }
                ) { Text(if (limitReached) "Limit reached" else "Share") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.consumeUploadRequest() }) { Text("Close") }
            }
        )
    }

    when (uiState) {

        KingdomUiState.KINGDOM_LIST -> {

            // The toggle sits below the system bars; the lists then only need
            // the small content padding, not the full top inset again.
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

                    KingdomViewModel.KingdomsTab.SHARED -> SharedKingdomList(
                        kingdoms = sharedKingdomItems,
                        ratingLabels = sharedRatingLabels,
                        isLoading = isSharedLoading,
                        hasMore = sharedHasMore,
                        onLoadMore = { viewModel.loadSharedKingdoms(reset = false) },
                        onKingdomClick = { viewModel.openSharedKingdom(it.uuid) },
                        onFavoriteClick = { viewModel.toggleSharedKingdomFavorite(it.uuid) },
                        listState = sharedListState,
                        paddingValues = listPadding
                    )
                }
            }
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
                listState = singleKingdomState,
                isCardDismissEnabled = isDismissEnabled,
                isLandscapeDismissEnabled = isLandscapeDismissEnabled,
                onCardDismissed = { viewModel.onCardDismissed(it) },
                paddingValues = calculatePadding(innerPadding),
                isGridViewEnabled = isGridViewEnabled
            )
        }

        KingdomUiState.CARD_DETAIL -> {
            Log.i("MainView", "View card detail (${selectedCard?.name})")
            CardDetailPager(
                cardList = kingdom.getAllCards(),
                initialCard = selectedCard!!,
                onClick = { viewModel.clearSelectedCard() },
                onPageChanged = { viewModel.selectCard(it) },
                paddingValues = calculatePadding(innerPadding),
                onFavorite = { viewModel.toggleCardFavorite(it) },
                onBan = { viewModel.toggleCardEnabled(it) }
            )
        }
    }
}
