package dev.msuhr.dominionkingdoms.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.model.ExpansionWithEditions
import dev.msuhr.dominionkingdoms.ui.LibraryUiState
import dev.msuhr.dominionkingdoms.ui.LibraryViewModel
import dev.msuhr.dominionkingdoms.ui.components.*
import dev.msuhr.dominionkingdoms.utils.Constants
import dev.msuhr.dominionkingdoms.utils.calculatePadding

/**
 * Sealed class representing items that can be displayed in the unified library list.
 *
 * This abstraction allows a single LazyColumn to dynamically display different content types
 * (expansions, search results, etc.) without recomposing the SearchBar, which preserves
 * focus and keyboard state when transitioning between search and non-search views.
 */
sealed class LibraryListItem {
    // Search bar item - always rendered first in the list
    data class SearchItem(val searchText: String, val onSearchTextChange: (String) -> Unit) : LibraryListItem()
    // Expansion header item (section header shown once before all expansions)
    data class ExpansionHeaderItem(val ownedCount: Int, val totalCount: Int) : LibraryListItem()
    // Expansion item from the library
    data class ExpansionItem(val expansion: ExpansionWithEditions) : LibraryListItem()
    // Individual card item (shown in search results)
    data class CardItem(val card: Card) : LibraryListItem()
    // "X cards found" info header with sort button (shown in search results)
    data class CardsFoundInfoItem(val count: Int, val sortType: LibraryViewModel.SortType, val onSortTypeSelected: (LibraryViewModel.SortType) -> Unit) : LibraryListItem()
    // Manage header item (section header shown once before favorite and blacklisted cards)
    data object ManageHeaderItem : LibraryListItem()
    // Section header for favorite cards (shown above blacklisted cards)
    data class PromoCardsSectionItem(val countText: String, val onClick: () -> Unit) : LibraryListItem()
    // Section header for favorite cards (shown above blacklisted cards)
    data class FavoriteCardsSectionItem(val favoriteCardCount: Int, val onClick: () -> Unit) : LibraryListItem()
    // Section header for blacklisted cards (shown at bottom of expansion list)
    data class BlacklistedSectionItem(val disabledCardCount: Int, val onClick: () -> Unit) : LibraryListItem()
}

/**
 * Main library screen providing access to expansions, cards, and search functionality.
 */
@Composable
fun LibraryScreen(
    onTitleChanged: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: LibraryViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    // State collection
    val uiState by viewModel.uiState.collectAsState()
    val title by viewModel.topBarTitle.collectAsState()
    val expansionsWithEditions by viewModel.expansionsWithEditions.collectAsState()
    val selectedExpansion by viewModel.selectedExpansion.collectAsState()
    val selectedEdition by viewModel.selectedEdition.collectAsState()
    val cardsToShow by viewModel.cardsToShow.collectAsState()
    val selectedCard by viewModel.selectedCard.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val disabledCardCount by viewModel.blacklistedCardCount.collectAsState()
    val favoriteCardCount by viewModel.favoriteCardCount.collectAsState()
    val promoCardCount by viewModel.promoCardCount.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val expansionCardCounts by viewModel.expansionCardCounts.collectAsState()

    val libraryListState = rememberLazyListState()
    val cardListState = rememberLazyListState()

    // Reset card list scroll state whenever we return to the expansion list or search results
    LaunchedEffect(uiState) {
        if (uiState == LibraryUiState.EXPANSIONS || uiState == LibraryUiState.SEARCH_RESULTS) {
            cardListState.scrollToItem(0)
        }
    }

    LaunchedEffect(title) { onTitleChanged(title) }

    // Clear snackbar and error when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearError()
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.scrollToTopEvent.collect {
            when (uiState) {
                LibraryUiState.EXPANSION_CARDS -> cardListState.animateScrollToItem(0)
                LibraryUiState.SEARCH_RESULTS -> libraryListState.animateScrollToItem(0)
                LibraryUiState.EXPANSIONS -> libraryListState.animateScrollToItem(0)
                LibraryUiState.FAVORITE_CARDS -> cardListState.animateScrollToItem(0)
                LibraryUiState.BLACKLISTED_CARDS -> cardListState.animateScrollToItem(0)
                LibraryUiState.PROMO_CARDS -> cardListState.animateScrollToItem(0)
                else -> {}
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    BackHandler {
        // First, let the ViewModel handle back navigation (e.g., from card list to expansion list)
        if (!viewModel.handleBackNavigation()) {
            // If ViewModel didn't handle it, navigate at the app level
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            }
        }
    }

    // TODO: REVIEW / REFACTOR
    // Unified list view for EXPANSIONS and SEARCH_RESULTS
    when (uiState) {
        LibraryUiState.EXPANSIONS, LibraryUiState.SEARCH_RESULTS -> {
            val listItems = remember(uiState, expansionsWithEditions, cardsToShow, searchText, disabledCardCount, favoriteCardCount, promoCardCount) {
                buildListItems(uiState, expansionsWithEditions, cardsToShow, searchText, disabledCardCount, favoriteCardCount, promoCardCount, viewModel)
            }

            LazyColumn(
                state = libraryListState,
                verticalArrangement = Arrangement.spacedBy(Constants.PADDING_SMALL),
                contentPadding = calculatePadding(innerPadding)
            ) {
                items(
                    items = listItems,
                    key = { item ->
                        when (item) {
                            is LibraryListItem.SearchItem -> "search_bar"
                            is LibraryListItem.ExpansionHeaderItem -> "expansion_header"
                            is LibraryListItem.ExpansionItem -> "expansion_${item.expansion.name}"
                            is LibraryListItem.CardItem -> "card_${item.card.id}"
                            is LibraryListItem.CardsFoundInfoItem -> "cards_found_info"
                            is LibraryListItem.ManageHeaderItem -> "manage_header"
                            is LibraryListItem.PromoCardsSectionItem -> "promo_section"
                            is LibraryListItem.FavoriteCardsSectionItem -> "favorite_section"
                            is LibraryListItem.BlacklistedSectionItem -> "blacklisted_section"
                        }
                    },
                    // contentType helps Compose reuse item layouts more efficiently
                    contentType = { it::class.java.name }
                ) { item ->
                    when (item) {
                        is LibraryListItem.SearchItem -> SearchBar(item.searchText, item.onSearchTextChange)
                        is LibraryListItem.ExpansionHeaderItem -> HeaderItem("Expansions (${item.ownedCount} / ${item.totalCount})")
                        is LibraryListItem.ExpansionItem -> {
                            val expansion = item.expansion
                            val ownedEdition = viewModel.getOwnedEdition(expansion)
                            
                            val counts = if (ownedEdition == dev.msuhr.dominionkingdoms.model.OwnedEdition.BOTH) {
                                expansionCardCounts[expansion.id]
                            } else {
                                expansion.activeEdition?.let { expansionCardCounts[it.id] }
                            } ?: Pair(0, 0)

                            ExpansionListItem(
                                expansion = expansion,
                                portraitCount = counts.first,
                                landscapeCount = counts.second,
                                onClick = { viewModel.selectExpansion(expansion) },
                                onOwnershipToggle = {
                                    if (expansion.hasMultipleEditions) {
                                        viewModel.cycleMultiEditionOwnership(expansion)
                                    } else {
                                        expansion.editions.firstOrNull()?.let {
                                            viewModel.toggleSingleEditionOwnership(expansion.expansion.id, it.editionNumber)
                                        }
                                    }
                                }
                            )
                        }
                        is LibraryListItem.CardItem -> CardView(
                            card = item.card,
                            onCardClick = { viewModel.selectCard(item.card) },
                            enabled = item.card.isEnabled,
                            showIcon = true,
                            onToggleEnable = { viewModel.toggleCardEnabled(item.card) },
                            onFavorite = { viewModel.toggleCardFavorite(item.card) },
                            onBan = { viewModel.toggleCardEnabled(item.card) }
                        )
                        is LibraryListItem.CardsFoundInfoItem -> CardsFoundInfoRow(item.count, item.sortType, item.onSortTypeSelected)
                        is LibraryListItem.ManageHeaderItem -> HeaderItem("Manage")
                        is LibraryListItem.PromoCardsSectionItem -> PromoCardsListItem(item.countText, item.onClick)
                        is LibraryListItem.FavoriteCardsSectionItem -> FavoriteCardsListItem(item.favoriteCardCount, item.onClick)
                        is LibraryListItem.BlacklistedSectionItem -> BlacklistedCardsListItem(item.disabledCardCount, item.onClick)
                    }
                }
            }
        }

        // Show cards within the selected expansion
        LibraryUiState.EXPANSION_CARDS -> {
            Log.i("LibraryScreen", "View expansion cards: ${selectedExpansion?.name} (${cardsToShow.size})")
            LibraryCardList(
                cardList = cardsToShow,
                sortType = sortType,
                includeEditionSelection = selectedExpansion!!.hasMultipleEditions,
                selectedEdition = selectedEdition,
                onEditionSelected = { ed, owned -> viewModel.selectEdition(selectedExpansion!!, ed, owned) },
                onCardClick = { viewModel.selectCard(it) },
                onToggleEnable = { viewModel.toggleCardEnabled(it) },
                onFavorite = { viewModel.toggleCardFavorite(it) },
                onBan = { viewModel.toggleCardEnabled(it) },
                listState = cardListState,
                paddingValues = calculatePadding(innerPadding)
            )
        }

        // Show list of favorite cards
        LibraryUiState.FAVORITE_CARDS -> {
            Log.i("LibraryScreen", "View favorite cards")
            LibraryCardList(
                cardList = cardsToShow,
                sortType = sortType,
                includeEditionSelection = false,
                selectedEdition = dev.msuhr.dominionkingdoms.model.OwnedEdition.NONE,
                onEditionSelected = { _, _ -> },
                onCardClick = { viewModel.selectCard(it) },
                onToggleEnable = { viewModel.toggleCardEnabled(it) },
                onFavorite = { viewModel.toggleCardFavorite(it) },
                onBan = { viewModel.toggleCardEnabled(it) },
                listState = cardListState,
                paddingValues = calculatePadding(innerPadding),
                onSortTypeSelected = { viewModel.updateSortType(dev.msuhr.dominionkingdoms.model.AppSortType.Library(it)) }
            )
        }

        // Show list of banned cards
        LibraryUiState.BLACKLISTED_CARDS -> {
            Log.i("LibraryScreen", "View blacklisted cards")
            LibraryCardList(
                cardList = cardsToShow,
                sortType = sortType,
                includeEditionSelection = false,
                selectedEdition = dev.msuhr.dominionkingdoms.model.OwnedEdition.NONE,
                onEditionSelected = { _, _ -> },
                onCardClick = { viewModel.selectCard(it) },
                onToggleEnable = { viewModel.toggleCardEnabled(it) },
                onFavorite = { viewModel.toggleCardFavorite(it) },
                onBan = { viewModel.toggleCardEnabled(it) },
                listState = cardListState,
                paddingValues = calculatePadding(innerPadding),
                onSortTypeSelected = { viewModel.updateSortType(dev.msuhr.dominionkingdoms.model.AppSortType.Library(it)) }
            )
        }

        LibraryUiState.PROMO_CARDS -> {
            LibraryCardList(
                cardList = cardsToShow,
                sortType = sortType,
                includeEditionSelection = false,
                selectedEdition = dev.msuhr.dominionkingdoms.model.OwnedEdition.NONE,
                onEditionSelected = { _, _ -> },
                onCardClick = { viewModel.selectCard(it) },
                onToggleEnable = { viewModel.toggleCardEnabled(it) },
                onFavorite = { viewModel.toggleCardFavorite(it) },
                onBan = { viewModel.toggleCardEnabled(it) },
                listState = cardListState,
                paddingValues = calculatePadding(innerPadding),
                onSortTypeSelected = { viewModel.updateSortType(dev.msuhr.dominionkingdoms.model.AppSortType.Library(it)) }
            )
        }

        // Show detail view of a single card
        LibraryUiState.CARD_DETAIL -> {
            Log.i("LibraryScreen", "View card detail: ${selectedCard?.name}")
            CardDetailPager(
                cardList = cardsToShow,
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

// TODO REVIEW / REFACTOR
/**
 * Builds the list of items for the unified library view.
 */
private fun buildListItems(uiState: LibraryUiState, expansionsWithEditions: List<ExpansionWithEditions>, cardsToShow: List<Card>, searchText: String, disabledCardCount: Int, favoriteCardCount: Int, promoCardCountText: String, viewModel: LibraryViewModel): List<LibraryListItem> {
    return when (uiState) {
        LibraryUiState.EXPANSIONS -> buildList {
            add(LibraryListItem.SearchItem(searchText) { viewModel.changeSearchText(it) })
            
            val ownedCount = expansionsWithEditions.count { it.isAnyOwned() }
            add(LibraryListItem.ExpansionHeaderItem(ownedCount, expansionsWithEditions.size))

            expansionsWithEditions.forEach { add(LibraryListItem.ExpansionItem(it)) }
            add(LibraryListItem.ManageHeaderItem)
            add(LibraryListItem.PromoCardsSectionItem(promoCardCountText) { viewModel.showPromoCards() })
            add(LibraryListItem.FavoriteCardsSectionItem(favoriteCardCount) { viewModel.showFavoriteCards() })
            add(LibraryListItem.BlacklistedSectionItem(disabledCardCount) { viewModel.showBlacklistedCards() })
        }
        LibraryUiState.SEARCH_RESULTS -> buildList {
            add(LibraryListItem.SearchItem(searchText) { viewModel.changeSearchText(it) })
            add(LibraryListItem.CardsFoundInfoItem(cardsToShow.size, viewModel.sortType.value) { viewModel.updateSortType(dev.msuhr.dominionkingdoms.model.AppSortType.Library(it)) })
            cardsToShow.forEach { add(LibraryListItem.CardItem(card = it)) }
        }
        else -> emptyList()
    }
}

@Composable
private fun HeaderItem(text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = text, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium, color = LocalContentColor.current.copy(alpha = 0.6f))
    }
}

@Composable
private fun CardsFoundInfoRow(count: Int, sortType: LibraryViewModel.SortType, onSortTypeSelected: (LibraryViewModel.SortType) -> Unit) {
    var showSortDialog by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = "$count cards found", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = { showSortDialog = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Sort results", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (showSortDialog) SortTypeDialog(sortType, { onSortTypeSelected(it); showSortDialog = false }, { showSortDialog = false })
}

@Composable
private fun SortTypeDialog(sortType: LibraryViewModel.SortType, onSortTypeSelected: (LibraryViewModel.SortType) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true, usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp).widthIn(max = 400.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, tonalElevation = 5.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "Sort by", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
                LibraryViewModel.SortType.entries.forEach { sortOption ->
                    val isSelected = sortOption == sortType
                    Row(modifier = Modifier.fillMaxWidth().clickable { onSortTypeSelected(sortOption) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(isSelected, { onSortTypeSelected(sortOption) })
                        Text(text = sortOption.text, modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioButton(selected: Boolean, onClick: () -> Unit) {
    Icon(
        imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(8.dp).clip(CircleShape).clickable(onClick = onClick))
}
