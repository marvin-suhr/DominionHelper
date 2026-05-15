package com.marvinsuhr.dominionhelper.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.marvinsuhr.dominionhelper.model.Card
import com.marvinsuhr.dominionhelper.model.Expansion
import com.marvinsuhr.dominionhelper.model.ExpansionWithEditions
import com.marvinsuhr.dominionhelper.ui.LibraryUiState
import com.marvinsuhr.dominionhelper.ui.LibraryViewModel
import com.marvinsuhr.dominionhelper.ui.components.BlacklistedCardsListItem
import com.marvinsuhr.dominionhelper.ui.components.CardDetailPager
import com.marvinsuhr.dominionhelper.ui.components.CardView
import com.marvinsuhr.dominionhelper.ui.components.ExpansionListItem
import com.marvinsuhr.dominionhelper.ui.components.FavoriteCardsListItem
import com.marvinsuhr.dominionhelper.ui.components.LibraryCardList
import com.marvinsuhr.dominionhelper.ui.components.SearchBar
import com.marvinsuhr.dominionhelper.utils.Constants
import com.marvinsuhr.dominionhelper.utils.calculatePadding
import kotlinx.coroutines.launch

/**
 * Sealed class representing items that can be displayed in the unified library list.
 *
 * This abstraction allows a single LazyColumn to dynamically display different content types
 * (expansions, search results, etc.) without recomposing the SearchBar, which preserves
 * focus and keyboard state when transitioning between search and non-search views.
 */
sealed class LibraryListItem {
    /**
     * Search bar item - always rendered first in the list
     */
    data class SearchItem(
        val searchText: String,
        val onSearchTextChange: (String) -> Unit
    ) : LibraryListItem()

    /**
     * Expansion header item (section header shown once before all expansions)
     */
    data object ExpansionHeaderItem : LibraryListItem()

    /**
     * Expansion item from the library
     */
    data class ExpansionItem(val expansion: ExpansionWithEditions) : LibraryListItem()

    /**
     * Individual card item (shown in search results)
     */
    data class CardItem(val card: Card) : LibraryListItem()

    /**
     * "X cards found" info header with sort button (shown in search results)
     */
    data class CardsFoundInfoItem(
        val count: Int,
        val sortType: LibraryViewModel.SortType,
        val onSortTypeSelected: (LibraryViewModel.SortType) -> Unit
    ) : LibraryListItem()

    /**
     * Manage header item (section header shown once before favorite and blacklisted cards)
     */
    data object ManageHeaderItem : LibraryListItem()

    /**
     * Section header for favorite cards (shown above blacklisted cards)
     */
    data class FavoriteCardsSectionItem(
        val favoriteCardCount: Int,
        val onClick: () -> Unit
    ) : LibraryListItem()

    /**
     * Section header for blacklisted cards (shown at bottom of expansion list)
     */
    data class BlacklistedSectionItem(
        val disabledCardCount: Int,
        val onClick: () -> Unit
    ) : LibraryListItem()
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
    val errorMessage by viewModel.errorMessage.collectAsState()

    // List states
    val libraryListState = rememberLazyListState()
    val cardListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Side effects
    LaunchedEffect(title) { onTitleChanged(title) }

    LaunchedEffect(Unit) {
        viewModel.scrollToTopEvent.collect {
            when (uiState) {
                LibraryUiState.EXPANSION_CARDS -> cardListState.animateScrollToItem(0)
                LibraryUiState.SEARCH_RESULTS -> libraryListState.animateScrollToItem(0)
                LibraryUiState.EXPANSIONS -> libraryListState.animateScrollToItem(0)
                LibraryUiState.FAVORITE_CARDS -> cardListState.animateScrollToItem(0)
                LibraryUiState.BLACKLISTED_CARDS -> cardListState.animateScrollToItem(0)
                else -> {}
            }
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
            val listItems = remember(uiState, expansionsWithEditions, cardsToShow, searchText, disabledCardCount, favoriteCardCount) {
                buildListItems(
                    uiState = uiState,
                    expansionsWithEditions = expansionsWithEditions,
                    cardsToShow = cardsToShow,
                    searchText = searchText,
                    disabledCardCount = disabledCardCount,
                    favoriteCardCount = favoriteCardCount,
                    viewModel = viewModel
                )
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
                            is LibraryListItem.FavoriteCardsSectionItem -> "favorite_section"
                            is LibraryListItem.BlacklistedSectionItem -> "blacklisted_section"
                        }
                    },
                    // contentType helps Compose reuse item layouts more efficiently
                    contentType = { it::class.java.name }
                ) { item ->
                    when (item) {
                        is LibraryListItem.SearchItem -> {
                            SearchBar(
                                searchText = item.searchText,
                                onSearchTextChange = item.onSearchTextChange
                            )
                        }

                        is LibraryListItem.ExpansionHeaderItem -> {
                            HeaderItem("Expansions")
                        }

                        is LibraryListItem.ExpansionItem -> {
                            ExpansionListItemContent(
                                expansion = item.expansion,
                                viewModel = viewModel,
                                onExpansionClick = { viewModel.selectExpansion(item.expansion) },
                                onOwnershipToggle = { expansion, isOwned ->
                                    viewModel.updateExpansionOwnership(expansion, isOwned)
                                }
                            )
                        }

                        is LibraryListItem.CardItem -> {
                            CardView(
                                card = item.card,
                                onCardClick = { viewModel.selectCard(item.card) },
                                showIcon = true,
                                onToggleEnable = { viewModel.toggleCardEnabled(item.card) }
                            )
                        }

                        is LibraryListItem.CardsFoundInfoItem -> {
                            CardsFoundInfoRow(
                                count = item.count,
                                sortType = item.sortType,
                                onSortTypeSelected = item.onSortTypeSelected
                            )
                        }

                        is LibraryListItem.ManageHeaderItem -> {
                            HeaderItem("Manage")
                        }

                        is LibraryListItem.FavoriteCardsSectionItem -> {
                            FavoriteCardsListItem(
                                favoriteCardCount = item.favoriteCardCount,
                                onClick = item.onClick
                            )
                        }

                        is LibraryListItem.BlacklistedSectionItem -> {
                            BlacklistedCardsListItem(
                                disabledCardCount = item.disabledCardCount,
                                onClick = item.onClick
                            )
                        }
                    }
                }
            }
        }

        // Show cards within the selected expansion
        LibraryUiState.EXPANSION_CARDS -> {
            Log.i("LibraryScreen", "View expansion cards: ${selectedExpansion?.name}")
            LibraryCardList(
                cardList = cardsToShow,
                sortType = sortType,
                includeEditionSelection = viewModel.expansionHasTwoEditions(selectedExpansion!!),
                selectedEdition = selectedEdition,
                onEditionSelected = { editionClicked, ownedEdition ->
                    viewModel.selectEdition(selectedExpansion!!, editionClicked, ownedEdition)
                },
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
                selectedEdition = com.marvinsuhr.dominionhelper.model.OwnedEdition.NONE,
                onEditionSelected = { _, _ -> },
                onCardClick = { viewModel.selectCard(it) },
                onToggleEnable = { viewModel.toggleCardEnabled(it) },
                onFavorite = { viewModel.toggleCardFavorite(it) },
                onBan = { viewModel.toggleCardEnabled(it) },
                listState = cardListState,
                paddingValues = calculatePadding(innerPadding)
            )
        }

        // Show list of banned cards
        LibraryUiState.BLACKLISTED_CARDS -> {
            Log.i("LibraryScreen", "View blacklisted cards")
            LibraryCardList(
                cardList = cardsToShow,
                sortType = sortType,
                includeEditionSelection = false,
                selectedEdition = com.marvinsuhr.dominionhelper.model.OwnedEdition.NONE,
                onEditionSelected = { _, _ -> },
                onCardClick = { viewModel.selectCard(it) },
                onToggleEnable = { viewModel.toggleCardEnabled(it) },
                onFavorite = { viewModel.toggleCardFavorite(it) },
                onBan = { viewModel.toggleCardEnabled(it) },
                listState = cardListState,
                paddingValues = calculatePadding(innerPadding)
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
                paddingValues = calculatePadding(innerPadding)
            )
        }
    }
}

// TODO REVIEW / REFACTOR
/**
 * Builds the list of items for the unified library view.
 */
private fun buildListItems(
    uiState: LibraryUiState,
    expansionsWithEditions: List<ExpansionWithEditions>,
    cardsToShow: List<Card>,
    searchText: String,
    disabledCardCount: Int,
    favoriteCardCount: Int,
    viewModel: LibraryViewModel
): List<LibraryListItem> {
    return when (uiState) {
        LibraryUiState.EXPANSIONS -> buildList {
            add(LibraryListItem.SearchItem(searchText) { viewModel.changeSearchText(it) })
            add(LibraryListItem.ExpansionHeaderItem)
            expansionsWithEditions.forEach { expansion ->
                add(LibraryListItem.ExpansionItem(expansion))
            }
            add(LibraryListItem.ManageHeaderItem)
            add(LibraryListItem.FavoriteCardsSectionItem(favoriteCardCount) { viewModel.showFavoriteCards() })
            add(LibraryListItem.BlacklistedSectionItem(disabledCardCount) { viewModel.showBlacklistedCards() })
        }

        LibraryUiState.SEARCH_RESULTS -> buildList {
            add(LibraryListItem.SearchItem(searchText) { viewModel.changeSearchText(it) })
            add(LibraryListItem.CardsFoundInfoItem(cardsToShow.size, viewModel.sortType.value) {
                viewModel.updateSortType(com.marvinsuhr.dominionhelper.model.AppSortType.Library(it))
            })
            cardsToShow.forEach { card ->
                add(LibraryListItem.CardItem(card))
            }
        }

        else -> emptyList()
    }
}

/**
 * Displays a header item in the list
 */
@Composable
private fun HeaderItem(text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = LocalContentColor.current.copy(alpha = 0.6f)
        )
    }
}

/**
 * Content renderer for ExpansionItem including the expansion item and nested edition items.
 */
@Composable
private fun ExpansionListItemContent(
    expansion: ExpansionWithEditions,
    viewModel: LibraryViewModel,
    onExpansionClick: () -> Unit,
    onOwnershipToggle: (Expansion, Boolean) -> Unit
) {
    val hasMultipleEditions = expansion.firstEdition != null && expansion.secondEdition != null

    ExpansionListItem(
        expansion = expansion,
        onClick = onExpansionClick,
        onOwnershipToggle = {
            if (hasMultipleEditions) {
                // Multi-edition: cycle through ownership states (NONE → FIRST → SECOND → BOTH → NONE)
                viewModel.cycleMultiEditionOwnership(expansion)
            } else {
                // Single edition: simple toggle
                val editionToToggle = expansion.firstEdition ?: expansion.secondEdition
                editionToToggle?.let { edition ->
                    onOwnershipToggle(edition, !edition.isOwned)
                }
            }
        },
        hasMultipleEditions = hasMultipleEditions,
        onToggleExpansion = { viewModel.toggleExpansion(expansion) }
    )
}

/**
 * Displays the "X cards found" info row with sort button.
 */
@Composable
private fun CardsFoundInfoRow(
    count: Int,
    sortType: LibraryViewModel.SortType,
    onSortTypeSelected: (LibraryViewModel.SortType) -> Unit
) {
    var showSortDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$count cards found",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        IconButton(onClick = { showSortDialog = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "Sort results",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showSortDialog) {
        SortTypeDialog(
            sortType = sortType,
            onSortTypeSelected = {
                onSortTypeSelected(it)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }
}

/**
 * Sort type selection dialog.
 */
@Composable
private fun SortTypeDialog(
    sortType: LibraryViewModel.SortType,
    onSortTypeSelected: (LibraryViewModel.SortType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .widthIn(max = 400.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 5.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Sort by",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 8.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )

                LibraryViewModel.SortType.entries.forEach { sortOption ->
                    val isSelected = sortOption == sortType
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortTypeSelected(sortOption) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSortTypeSelected(sortOption) }
                        )
                        Text(
                            text = sortOption.text,
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

/**
 * Radio button icon for sort dialog.
 */
@Composable
private fun RadioButton(
    selected: Boolean,
    onClick: () -> Unit
) {
    Icon(
        imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    )
}
