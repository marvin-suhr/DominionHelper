package dev.msuhr.dominionkingdoms.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.data.CardDao
import dev.msuhr.dominionkingdoms.model.Edition
import dev.msuhr.dominionkingdoms.data.ExpansionDao
import dev.msuhr.dominionkingdoms.model.AppSortType
import dev.msuhr.dominionkingdoms.model.ExpansionWithEditions
import dev.msuhr.dominionkingdoms.model.OwnedEdition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryUiState {
    EXPANSIONS,
    EXPANSION_CARDS,
    SEARCH_RESULTS,
    CARD_DETAIL,
    BLACKLISTED_CARDS,
    FAVORITE_CARDS,
    PROMO_CARDS
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val cardDao: CardDao,
    private val expansionDao: ExpansionDao
) : ViewModel(), ScreenViewModel {

    enum class SortType(val text: String) {
        TYPE("Sort by type"),
        ALPHABETICAL("Sort alphabetically"),
        COST("Sort by cost"),
        EXPANSION("Sort by expansion"),
        ENABLED("Sort by enabled");
        // TODO Sort by edition for library
    }

    // Interface stuff

    override fun handleBackNavigation(): Boolean {
        when (_uiState.value) {
            LibraryUiState.EXPANSIONS -> {
                Log.i("BackHandler", "Leave expansion list -> Exit app")
                return false
            }

            LibraryUiState.EXPANSION_CARDS -> {
                Log.i("BackHandler", "Leave expansion list -> Return to expansion list")
                clearSelectedExpansion()
                switchUiStateTo(LibraryUiState.EXPANSIONS)
                return true
            }

            LibraryUiState.SEARCH_RESULTS -> {
                Log.i("BackHandler", "Deactivate search")
                changeSearchText("") // This handles clearing search and returning to previous state
                clearAllCards()
                switchUiStateTo(LibraryUiState.EXPANSIONS)
                return true
            }

            LibraryUiState.BLACKLISTED_CARDS -> {
                Log.i("BackHandler", "Leave blacklisted cards -> Return to expansion list")
                clearSelectedExpansion()
                switchUiStateTo(LibraryUiState.EXPANSIONS)
                return true
            }

            LibraryUiState.FAVORITE_CARDS -> {
                Log.i("BackHandler", "Leave favorite cards -> Return to expansion list")
                clearSelectedExpansion()
                switchUiStateTo(LibraryUiState.EXPANSIONS)
                return true
            }

            LibraryUiState.CARD_DETAIL -> {
                Log.i("BackHandler", "Deselect card -> Return to card list")
                clearSelectedCard()
                switchUiStateTo(lastState)
                return true
            }

            LibraryUiState.PROMO_CARDS -> {
                Log.i("BackHandler", "Leave promo cards -> Return to expansion list")
                clearSelectedExpansion()
                switchUiStateTo(LibraryUiState.EXPANSIONS)
                return true
            }
        }
    }

    override fun onSortTypeSelected(sortType: AppSortType) {
        Log.d("LibraryViewModel", "Selected sort type $sortType")
        updateSortType(sortType as AppSortType.Library)
    }

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToTopEvent = _scrollToTopEvent.asSharedFlow()

    override fun triggerScrollToTop() {
        _scrollToTopEvent.tryEmit(Unit)
    }

    private val _sortType = MutableStateFlow(SortType.TYPE)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    override val currentAppSortType: StateFlow<AppSortType?> =
        sortType.map { AppSortType.Library(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _uiState = MutableStateFlow(LibraryUiState.EXPANSIONS)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    override val showBackButton: StateFlow<Boolean> =
        uiState.map { uiState -> uiState != LibraryUiState.EXPANSIONS }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override val showTopAppBar: StateFlow<Boolean> =
        uiState.map { uiState ->
            uiState == LibraryUiState.EXPANSION_CARDS || uiState == LibraryUiState.FAVORITE_CARDS || uiState == LibraryUiState.BLACKLISTED_CARDS || uiState == LibraryUiState.PROMO_CARDS
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Fields

    private var lastState: LibraryUiState = LibraryUiState.EXPANSIONS
    private var stateBeforeSearch: LibraryUiState = LibraryUiState.EXPANSIONS

    val expansionsWithEditions: StateFlow<List<ExpansionWithEditions>> =
        expansionDao.getAllWithEditions()
            .map { all ->
                // Restore conceptual mapping for shared editions
                val cornucopiaGuilds = all.find { it.expansion.id == "CORNUCOPIA_GUILDS" }
                all.filter { it.expansion.id != "CORNUCOPIA_GUILDS" && it.expansion.id != "PROMO" }
                    .map { expWithEds ->
                        if (expWithEds.id == "CORNUCOPIA" || expWithEds.id == "GUILDS") {
                            val sharedEdition = cornucopiaGuilds?.editions?.find { it.editionNumber == 2 }
                            if (sharedEdition != null) {
                                expWithEds.copy(editions = expWithEds.editions + sharedEdition)
                            } else expWithEds
                        } else expWithEds
                    }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val expansionCardCounts: StateFlow<Map<String, Pair<Int, Int>>> = combine(
        expansionsWithEditions,
        cardDao.getCardsForCountingFlow()
    ) { expansions, allCards ->
        val counts = mutableMapOf<String, Pair<Int, Int>>()
        val enabledCards = allCards.filter { it.isEnabled }

        expansions.forEach { expansionWithEditions ->
            val portraitSet = mutableSetOf<Int>()
            val landscapeSet = mutableSetOf<Int>()

            expansionWithEditions.editions.forEach { edition ->
                val editionId = edition.id
                val editionCards = enabledCards.filter { card ->
                    card.sets.any { set -> set.name == editionId }
                }

                val portraits = editionCards.count { it.supply && !it.landscape }
                val landscapes = editionCards.count { /*it.supply &&*/ it.landscape }

                counts[editionId] = Pair(portraits, landscapes)

                editionCards.forEach { card ->
                    if (card.landscape) landscapeSet.add(card.id)
                    else if (card.supply) portraitSet.add(card.id)
                }
            }

            counts[expansionWithEditions.id] = Pair(portraitSet.size, landscapeSet.size)
        }
        counts
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _selectedExpansion = MutableStateFlow<ExpansionWithEditions?>(null)
    val selectedExpansion: StateFlow<ExpansionWithEditions?> = _selectedExpansion.asStateFlow()

    private val _selectedEdition = MutableStateFlow(OwnedEdition.NONE)
    val selectedEdition: StateFlow<OwnedEdition> = _selectedEdition.asStateFlow()

    // Card / Kingdom variables
    private val _cardsToShow = MutableStateFlow<List<Card>>(emptyList())
    val cardsToShow: StateFlow<List<Card>> = _cardsToShow.asStateFlow()

    private val _selectedCard = MutableStateFlow<Card?>(null)
    val selectedCard: StateFlow<Card?> = _selectedCard.asStateFlow()

    // Search related
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val topBarTitle: StateFlow<String> = combine(
        uiState,
        selectedExpansion,
        selectedCard,
        cardsToShow
    ) { uiScreenState, selectedExpansion, selectedCard, cardsToShow ->
        when (uiScreenState) {
            LibraryUiState.EXPANSIONS -> "Library"
            LibraryUiState.EXPANSION_CARDS -> {
                selectedExpansion?.let { expansion ->
                    "${expansion.name} ${getEnabledCardAmount(cardsToShow)}"
                } ?: "Cards"
            }
            LibraryUiState.SEARCH_RESULTS -> "Search Results" // This isn't shown
            LibraryUiState.BLACKLISTED_CARDS -> "Blacklisted Cards (${cardsToShow.size})"
            LibraryUiState.FAVORITE_CARDS -> "Favorite Cards (${cardsToShow.size})"
            LibraryUiState.PROMO_CARDS -> "Promo Cards"
            LibraryUiState.CARD_DETAIL -> selectedCard?.name ?: "Details"
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "Library")

    /*init {
        loadExpansionsWithEditions()
    } ??? */

    private fun switchUiStateTo(newState: LibraryUiState) {
        _uiState.value = newState
        Log.d("LibraryViewModel", "Switched UI state to $newState")
    }

    fun getOwnedEdition(expansion: ExpansionWithEditions): OwnedEdition {
        val editions = expansion.editions
        val firstOwned = editions.find { it.editionNumber == 1 }?.isOwned == true
        val secondOwned = editions.find { it.editionNumber == 2 }?.isOwned == true

        return when {
            firstOwned && secondOwned -> OwnedEdition.BOTH
            firstOwned -> OwnedEdition.FIRST
            secondOwned -> OwnedEdition.SECOND
            else -> OwnedEdition.NONE
        }
    }

    fun toggleSingleEditionOwnership(expansionId: String, editionNumber: Int) {
        viewModelScope.launch {
            val edition = expansionsWithEditions.value.find { it.expansion.id == expansionId }
                ?.editions?.find { it.editionNumber == editionNumber }
            edition?.let {
                expansionDao.updateEditionOwnership(it.id, !it.isOwned)
            }
        }
    }

    // TODO check this
    /**
     * Cycle through ownership states for multi-edition expansions.
     * The cycle is: NONE → FIRST → SECOND → BOTH → NONE
     * Updates database atomically and updates in-memory state.
     * For Cornucopia & Guilds 2nd edition, both expansions share ownership.
     */
    fun cycleMultiEditionOwnership(expansion: ExpansionWithEditions) {
        viewModelScope.launch {
            val currentOwned = getOwnedEdition(expansion)
            val newOwned = when (currentOwned) {
                OwnedEdition.NONE -> OwnedEdition.FIRST
                OwnedEdition.FIRST -> OwnedEdition.SECOND
                OwnedEdition.SECOND -> OwnedEdition.BOTH
                OwnedEdition.BOTH -> OwnedEdition.NONE
            }

            val shouldOwnFirst = newOwned == OwnedEdition.FIRST || newOwned == OwnedEdition.BOTH
            val shouldOwnSecond = newOwned == OwnedEdition.SECOND || newOwned == OwnedEdition.BOTH
            
            expansion.firstEdition?.let { expansionDao.updateEditionOwnership(it.id, shouldOwnFirst) }
            expansion.secondEdition?.let { expansionDao.updateEditionOwnership(it.id, shouldOwnSecond) }
            
            if (expansion.id == "CORNUCOPIA" || expansion.id == "GUILDS") {
                val cornGuild = expansionsWithEditions.value.find { it.id == "CORNUCOPIA_GUILDS" }
                cornGuild?.editions?.find { it.editionNumber == 2 }?.let {
                    expansionDao.updateEditionOwnership(it.id, shouldOwnSecond)
                }
            }

            Log.i("LibraryViewModel", "Cycled ownership for ${expansion.name}: $currentOwned -> $newOwned")
        }
    }

    /////////////////////////
    // Expansion functions //
    /////////////////////////

    // TODO Check these all. Might be outdated
    fun selectExpansion(expansion: ExpansionWithEditions) {
        viewModelScope.launch {
            val ownedEditions = whichEditionIsOwned(expansion)
            val cards = getCardsFromOwnedEditions(expansion, ownedEditions)
            _selectedExpansion.value = expansion
            _selectedEdition.value = ownedEditions
            _cardsToShow.value = sortCards(cards.toList())

            Log.d(
                "LibraryViewModel",
                "Loaded ${_cardsToShow.value.size} cards for expansion ${expansion.name}"
            )

            switchUiStateTo(LibraryUiState.EXPANSION_CARDS)
        }
    }

    private fun whichEditionIsOwned(expansion: ExpansionWithEditions): OwnedEdition {
        val firstOwned = expansion.firstEdition?.isOwned == true
        val secondOwned = expansion.secondEdition?.isOwned == true
        
        return when {
            firstOwned && secondOwned -> OwnedEdition.BOTH
            firstOwned -> OwnedEdition.FIRST
            secondOwned -> OwnedEdition.SECOND
            else -> if (expansion.secondEdition != null) OwnedEdition.SECOND else OwnedEdition.FIRST
        }
    }

    private suspend fun getCardsFromOwnedEditions(
        expansion: ExpansionWithEditions,
        ownedEdition: OwnedEdition
    ): Set<Card> {
        val set = mutableSetOf<Card>()
        when (ownedEdition) {
            OwnedEdition.FIRST -> expansion.firstEdition?.let { set.addAll(cardDao.getCardsByExpansion(it.id)) }
            OwnedEdition.SECOND -> expansion.secondEdition?.let { set.addAll(cardDao.getCardsByExpansion(it.id)) }
            else -> {
                expansion.firstEdition?.let { set.addAll(cardDao.getCardsByExpansion(it.id)) }
                expansion.secondEdition?.let { set.addAll(cardDao.getCardsByExpansion(it.id)) }
            }
        }
        return set
    }

    fun clearSelectedExpansion() {
        _selectedExpansion.value = null
        _cardsToShow.value = emptyList()
        Log.d("LibraryViewModel", "Cleared selected expansion")
    }

    // When edition selector in CardList is pressed
    // Clicking an edition toggles it, but always ensures at least one edition is selected
    fun selectEdition(
        expansion: ExpansionWithEditions,
        clickedEditionNumber: Int,
        currentOwnedEdition: OwnedEdition
    ) {
        viewModelScope.launch {
            val newSelectedEdition = when (clickedEditionNumber) {
                1 -> toggleFirstEdition(currentOwnedEdition)
                2 -> toggleSecondEdition(currentOwnedEdition)
                else -> currentOwnedEdition
            }
            val cards = getCardsFromOwnedEditions(expansion, newSelectedEdition)
            _cardsToShow.value = sortCards(cards.toList())
            _selectedEdition.value = newSelectedEdition
            Log.d(
                "LibraryViewModel",
                "Selected edition $clickedEditionNumber for ${expansion.name}: $currentOwnedEdition -> $newSelectedEdition"
            )
        }
    }

    private fun toggleFirstEdition(current: OwnedEdition): OwnedEdition = when (current) {
        OwnedEdition.FIRST -> OwnedEdition.SECOND
        OwnedEdition.SECOND -> OwnedEdition.BOTH
        OwnedEdition.BOTH -> OwnedEdition.SECOND
        OwnedEdition.NONE -> OwnedEdition.FIRST // // NONE should never happen, but if it does, default to FIRST
    }

    private fun toggleSecondEdition(current: OwnedEdition): OwnedEdition = when (current) {
        OwnedEdition.FIRST -> OwnedEdition.BOTH
        OwnedEdition.SECOND -> OwnedEdition.FIRST
        OwnedEdition.BOTH -> OwnedEdition.FIRST
        OwnedEdition.NONE -> OwnedEdition.SECOND // // NONE should never happen, but if it does, default to SECOND
    }

    fun selectCard(card: Card) {
        _selectedCard.value = card
        if (uiState.value != LibraryUiState.CARD_DETAIL) {
            lastState = uiState.value
        }
        _uiState.value = LibraryUiState.CARD_DETAIL
        Log.d("LibraryViewModel", "Selected card ${card.name}")
    }

    fun clearSelectedCard() {
        _selectedCard.value = null
        switchUiStateTo(lastState)
        Log.d("LibraryViewModel", "Cleared selected card")
    }

    fun clearAllCards() {
        _cardsToShow.value = emptyList()
        Log.d("LibraryViewModel", "Cleared all cards")
    }

    private fun sortCards(cards: List<Card>): List<Card> {
        if (cards.isEmpty()) return cards
        val sortedCards = when (_sortType.value) {
            SortType.TYPE -> {
                // String comparison sucks
                val name = _selectedExpansion.value?.name
                cards.sortedWith(Card.CardTypeComparator(sortByCostAsTieBreaker = name == "Base" || name == "Empires"))
            }
            SortType.EXPANSION -> cards.sortedBy { it.sets.first().displayName }
            SortType.ALPHABETICAL -> cards.sortedBy { it.name }
            SortType.COST -> cards.sortedBy { it.cost }
            SortType.ENABLED -> cards.sortedBy { !it.isEnabled }
        }
        Log.d("LibraryViewModel", "Sorted ${sortedCards.size} cards by ${_sortType.value}")
        return sortedCards
    }

    fun updateSortType(newSortType: AppSortType.Library) {
        _sortType.value = newSortType.sortType
        // Sort expansion list
        _cardsToShow.value = sortCards(_cardsToShow.value)
        Log.d("LibraryViewModel", "Updated sort type to ${_sortType.value}")
    }

    fun changeSearchText(newText: String) {
        _searchText.value = newText
        viewModelScope.launch {
            if (newText.isEmpty()) {
                // Clear search results and go back to previous state
                _cardsToShow.value = emptyList()
                // Return to the state we were in before searching
                // Isn't this always EXPANSION LIST?
                _uiState.value = stateBeforeSearch
            } else if (newText.length >= 2 || (newText.isNotEmpty() && newText.first().isDigit())) {
                // Save current state before switching to search results
                if (_uiState.value != LibraryUiState.SEARCH_RESULTS) {
                    stateBeforeSearch = _uiState.value
                }
                // Perform search
                _cardsToShow.value = cardDao.getFilteredCards(newText)
                _uiState.value = LibraryUiState.SEARCH_RESULTS
            }
        }
    }

    fun triggerError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun showBlacklistedCards() {
        viewModelScope.launch {
            val disabledCards = cardDao.getDisabledCardsExceptPromo()
            _cardsToShow.value = sortCards(disabledCards)
            _uiState.value = LibraryUiState.BLACKLISTED_CARDS
            Log.d("LibraryViewModel", "Showing ${disabledCards.size} disabled cards")
        }
    }

    val blacklistedCardCount: StateFlow<Int> = cardDao.getDisabledCardCountExceptPromo()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun showFavoriteCards() {
        viewModelScope.launch {
            val favoriteCards = cardDao.getFavoriteCards()
            _cardsToShow.value = sortCards(favoriteCards)
            _uiState.value = LibraryUiState.FAVORITE_CARDS
            Log.d("LibraryViewModel", "Showing ${favoriteCards.size} favorite cards")
        }
    }

    val favoriteCardCount: StateFlow<Int> = cardDao.getFavoriteCardCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun showPromoCards() {
        viewModelScope.launch {
            val promoCards = cardDao.getCardsByExpansion("PROMO")
            _cardsToShow.value = sortCards(promoCards)
            _uiState.value = LibraryUiState.PROMO_CARDS
        }
    }

    val promoCardCount: StateFlow<String> = cardDao.getCardsByExpansionFlow("PROMO")
        .map { cards ->
            val enabled = cards.count { it.isEnabled }
            "$enabled / ${cards.size} cards owned"
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "0 / 0 cards owned")

    fun toggleCardFavorite(card: Card) {
        viewModelScope.launch {
            val newIsFavoriteState = !card.isFavorite
            cardDao.toggleCardFavorite(card.id, newIsFavoriteState)
            // Update both cardsToShow and selectedCard
            _cardsToShow.value = _cardsToShow.value.map { c ->
                if (c.id == card.id) c.copy(isFavorite = newIsFavoriteState) else c
            }
            if (_selectedCard.value?.id == card.id) {
                _selectedCard.value = _cardsToShow.value.find { it.id == card.id }
            }
            Log.d("LibraryViewModel", "Toggled card ${card.name} to favorite $newIsFavoriteState")
        }
    }

    fun toggleCardEnabled(card: Card) {
        viewModelScope.launch {
            val newIsEnabledState = !card.isEnabled
            cardDao.toggleCardEnabled(card.id, newIsEnabledState)
            // Update both cardsToShow and selectedCard
            _cardsToShow.value = _cardsToShow.value.map { c ->
                if (c.id == card.id) c.copy(isEnabled = newIsEnabledState) else c
            }
            if (_selectedCard.value?.id == card.id) {
                _selectedCard.value = _cardsToShow.value.find { it.id == card.id }
            }
            // TODO does this make sense? When SortType == ENABLED, changing cards makes them jump
            if (sortType.value == SortType.ENABLED) {
                _cardsToShow.value = sortCards(_cardsToShow.value)
            }
            // TODO does this make sense? When SortType == ENABLED, changing cards makes them jump
        }
    }
}

private fun getEnabledCardAmount(cards: List<Card>): String {
    val enabledCount = cards.count { it.isEnabled }
    return "(${enabledCount}/${cards.size})"
}