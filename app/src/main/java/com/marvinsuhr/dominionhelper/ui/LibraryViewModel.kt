package com.marvinsuhr.dominionhelper.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marvinsuhr.dominionhelper.model.Card
import com.marvinsuhr.dominionhelper.data.CardDao
import com.marvinsuhr.dominionhelper.model.Expansion
import com.marvinsuhr.dominionhelper.data.ExpansionDao
import com.marvinsuhr.dominionhelper.model.AppSortType
import com.marvinsuhr.dominionhelper.model.ExpansionWithEditions
import com.marvinsuhr.dominionhelper.model.OwnedEdition
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
import kotlin.text.first

enum class LibraryUiState {
    EXPANSIONS,
    EXPANSION_CARDS,
    SEARCH_RESULTS,
    CARD_DETAIL,
    BLACKLISTED_CARDS
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

            LibraryUiState.CARD_DETAIL -> {
                Log.i("BackHandler", "Deselect card -> Return to card list")
                clearSelectedCard()
                switchUiStateTo(lastState)
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
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(LibraryUiState.EXPANSIONS)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    override val showBackButton: StateFlow<Boolean> =
        uiState.map { uiState ->
            uiState != LibraryUiState.EXPANSIONS // && !isSearch?
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    override val showTopAppBar: StateFlow<Boolean> =
        uiState.map { uiState ->
            uiState != LibraryUiState.EXPANSIONS && uiState != LibraryUiState.SEARCH_RESULTS && uiState != LibraryUiState.CARD_DETAIL
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Fields

    private var lastState: LibraryUiState = LibraryUiState.EXPANSIONS
    private var stateBeforeSearch: LibraryUiState = LibraryUiState.EXPANSIONS

    // Expansion variables
    private val _expansionsWithEditions = MutableStateFlow<List<ExpansionWithEditions>>(emptyList())
    val expansionsWithEditions: StateFlow<List<ExpansionWithEditions>> =
        _expansionsWithEditions.asStateFlow()

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

            LibraryUiState.SEARCH_RESULTS -> "Search Results" // I think this isn't shown
            LibraryUiState.BLACKLISTED_CARDS -> "Blacklisted Cards"
            LibraryUiState.CARD_DETAIL -> selectedCard?.name ?: "Details"
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Kingdoms"
    )

    init {
        loadExpansionsWithEditions()
    }

    private fun switchUiStateTo(newState: LibraryUiState) {
        _uiState.value = newState
        Log.d("LibraryViewModel", "Switched UI state to $newState")
    }

    // Load all expansions and their editions, grouped by name
    // TODO the expansion and edition entity is ass
    private fun loadExpansionsWithEditions() {
        viewModelScope.launch {
            expansionDao.getAll().collect { allExpansions ->

                val currentExpandedState =
                    _expansionsWithEditions.value.associate { it.name to it.isExpanded }

                // Make a map of name -> list of editions
                val expansionsGrouped = allExpansions.groupBy { it.name }
                val cornGuild = allExpansions.find { it.name == "Cornucopia & Guilds" }

                // Assemble ExpansionWithEditions object for each entry
                val expansionsWithEditions = expansionsGrouped
                    .filter { (expansionName, _) -> expansionName != "Cornucopia & Guilds" }
                    .map { (expansionName, editions) ->

                        val firstEdition = editions.find { it.edition == 1 }
                        val secondEdition =
                            if (firstEdition?.name == "Cornucopia" || firstEdition?.name == "Guilds") {
                                cornGuild
                            } else {
                                editions.find { it.edition == 2 }
                            }

                        // Choose the correct image
                        val image = when {
                            firstEdition != null -> firstEdition.imageName
                            secondEdition != null -> secondEdition.imageName // Only for Cornucopia + Guilds 2nd edition
                            else -> throw java.lang.IllegalArgumentException("No edition found.")
                        }

                        val shouldBeExpanded = currentExpandedState[expansionName] == true

                        ExpansionWithEditions(
                            name = expansionName,
                            firstEdition = firstEdition,
                            secondEdition = secondEdition,
                            image = image,
                            isExpanded = shouldBeExpanded
                        )
                    }

                _expansionsWithEditions.value = expansionsWithEditions
                Log.i(
                    "LibraryViewModel",
                    "Loaded expansions [${expansionsWithEditions.size}] with editions [${allExpansions.size}]"
                )
            }
        }
    }

    fun toggleExpansion(expansionToToggle: ExpansionWithEditions) {
        Log.i(
            "LibraryViewModel",
            "Toggling expansion ${expansionToToggle.name}: ${expansionToToggle.isExpanded}"
        )
        viewModelScope.launch {
            _expansionsWithEditions.value = _expansionsWithEditions.value.map { expansion ->
                if (expansion.name == expansionToToggle.name) {
                    // Create a new ExpansionWithEditions object with the toggled isExpanded flag
                    expansion.copy(isExpanded = !expansion.isExpanded)
                } else {
                    // Keep other expansions as they are
                    expansion
                }
            }
            Log.i(
                "LibraryViewModel",
                "Toggled expansion ${expansionToToggle.name}: ${expansionToToggle.isExpanded}"
            )
        }
    }

    // Update ownership of an expansion
    fun updateExpansionOwnership(expansion: Expansion, newIsOwned: Boolean) {
        viewModelScope.launch {
            // Update the database
            when (expansion.edition) {
                1 -> expansionDao.updateFirstEditionOwned(
                    expansion.name,
                    newIsOwned
                )

                2 -> expansionDao.updateSecondEditionOwned(
                    expansion.name,
                    newIsOwned
                )

                else -> throw java.lang.IllegalArgumentException("Invalid edition.")
            }

            // Update the object
            _expansionsWithEditions.value = _expansionsWithEditions.value.map {
                if (it.name == expansion.name) {
                    when (expansion.edition) {
                        1 -> it.copy(firstEdition = it.firstEdition?.copy(isOwned = newIsOwned))
                        2 -> it.copy(secondEdition = it.secondEdition?.copy(isOwned = newIsOwned))
                        else -> throw java.lang.IllegalArgumentException("Invalid edition.")
                    }
                } else {
                    it
                }
            }
            Log.i(
                "LibraryViewModel",
                "UpdateExpansionOwnership(): Updated isOwned for ${expansion.name}[${expansion.edition}] to $newIsOwned"
            )
        }
    }

    fun getOwnershipText(expansion: ExpansionWithEditions): String {

        val isFirstOwned = expansion.firstEdition?.isOwned == true
        val isSecondOwned = expansion.secondEdition?.isOwned == true

        return when {
            isFirstOwned && isSecondOwned -> "Both Editions Owned"
            isFirstOwned ->
                if (expansion.secondEdition == null) {
                    "Owned"
                } else {
                    "First Edition Owned"
                }

            isSecondOwned -> "Second Edition Owned"
            else -> "Unowned"
        }
    }

    /**
     * Get the current OwnedEdition state for an expansion with multiple editions.
     * Returns the appropriate state based on which editions are owned.
     */
    fun getOwnedEdition(expansion: ExpansionWithEditions): OwnedEdition {
        val isFirstOwned = expansion.firstEdition?.isOwned == true
        val isSecondOwned = expansion.secondEdition?.isOwned == true

        return when {
            isFirstOwned && isSecondOwned -> OwnedEdition.BOTH
            isFirstOwned -> OwnedEdition.FIRST
            isSecondOwned -> OwnedEdition.SECOND
            else -> OwnedEdition.NONE
        }
    }

    /**
     * Cycle through ownership states for an expansion with multiple editions.
     * The cycle is: NONE → FIRST → SECOND → BOTH → NONE
     * Updates both the database and the in-memory state.
     */
    // TODO Review
    fun cycleMultiEditionOwnership(expansion: ExpansionWithEditions) {
        viewModelScope.launch {
            val currentOwned = getOwnedEdition(expansion)
            val newOwned = when (currentOwned) {
                OwnedEdition.NONE -> OwnedEdition.FIRST
                OwnedEdition.FIRST -> OwnedEdition.SECOND
                OwnedEdition.SECOND -> OwnedEdition.BOTH
                OwnedEdition.BOTH -> OwnedEdition.NONE
            }

            // Update first edition ownership
            expansion.firstEdition?.let { first ->
                val shouldOwnFirst = newOwned == OwnedEdition.FIRST || newOwned == OwnedEdition.BOTH
                expansionDao.updateFirstEditionOwned(expansion.name, shouldOwnFirst)
            }

            // Update second edition ownership
            expansion.secondEdition?.let { second ->
                val shouldOwnSecond = newOwned == OwnedEdition.SECOND || newOwned == OwnedEdition.BOTH
                expansionDao.updateSecondEditionOwned(expansion.name, shouldOwnSecond)
            }

            // Update the in-memory state
            _expansionsWithEditions.value = _expansionsWithEditions.value.map {
                if (it.name == expansion.name) {
                    it.copy(
                        firstEdition = it.firstEdition?.copy(
                            isOwned = newOwned == OwnedEdition.FIRST || newOwned == OwnedEdition.BOTH
                        ),
                        secondEdition = it.secondEdition?.copy(
                            isOwned = newOwned == OwnedEdition.SECOND || newOwned == OwnedEdition.BOTH
                        )
                    )
                } else {
                    it
                }
            }

            Log.i("LibraryViewModel", "Cycled ownership for ${expansion.name}: $currentOwned -> $newOwned")
        }
    }

    /////////////////////////
    // Expansion functions //
    /////////////////////////

    fun selectExpansion(expansion: ExpansionWithEditions) {
        viewModelScope.launch {

            val ownedEditions = whichEditionIsOwned(expansion)
            val set = getCardsFromOwnedEditions(expansion, ownedEditions)
            _selectedExpansion.value = expansion
            _selectedEdition.value = ownedEditions
            _cardsToShow.value = sortCards(set.toList())

            Log.d(
                "LibraryViewModel",
                "Loaded ${_cardsToShow.value.size} cards for expansion ${expansion.name}"
            )

            _uiState.value = LibraryUiState.EXPANSION_CARDS
            Log.d("LibraryViewModel", "Selected ${expansion.name}")
        }
    }

    private fun whichEditionIsOwned(expansion: ExpansionWithEditions): OwnedEdition {
        if (expansion.firstEdition?.isOwned == true) {
            if (expansion.secondEdition?.isOwned == true) {
                return OwnedEdition.BOTH
            }
            return OwnedEdition.FIRST
        } else if (expansion.secondEdition?.isOwned == true) {
            return OwnedEdition.SECOND
        } else {
            // Neither owned - default to available edition
            // If second edition exists, default to it, otherwise use first edition
            return if (expansion.secondEdition != null) {
                OwnedEdition.SECOND
            } else {
                OwnedEdition.FIRST
            }
        }
    }

    private suspend fun getCardsFromOwnedEditions(
        expansion: ExpansionWithEditions,
        ownedEdition: OwnedEdition
    ): Set<Card> {

        val set = mutableSetOf<Card>()

        when (ownedEdition) {
            OwnedEdition.FIRST -> {
                expansion.firstEdition?.let { firstEdition ->
                    set.addAll(cardDao.getCardsByExpansion(firstEdition.id))
                }
            }

            OwnedEdition.SECOND -> {
                expansion.secondEdition?.let { secondEdition ->
                    set.addAll(cardDao.getCardsByExpansion(secondEdition.id))
                }
            }

            else -> {
                if (expansion.firstEdition != null) {
                    set.addAll(cardDao.getCardsByExpansion(expansion.firstEdition.id))
                }
                if (expansion.secondEdition != null) {
                    set.addAll(cardDao.getCardsByExpansion(expansion.secondEdition.id))
                }
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

            val set = getCardsFromOwnedEditions(expansion, newSelectedEdition)
            _cardsToShow.value = sortCards(set.toList())
            _selectedEdition.value = newSelectedEdition
            Log.d(
                "LibraryViewModel",
                "Selected edition $clickedEditionNumber for ${expansion.name}: $currentOwnedEdition -> $newSelectedEdition"
            )
        }
    }

    private fun toggleFirstEdition(current: OwnedEdition): OwnedEdition {
        return when (current) {
            OwnedEdition.FIRST -> OwnedEdition.SECOND     // First only → Switch to Second
            OwnedEdition.SECOND -> OwnedEdition.BOTH       // Second only → Add First
            OwnedEdition.BOTH -> OwnedEdition.SECOND       // Both → Remove First, keep Second
            // NONE should never happen, but if it does, default to FIRST
            OwnedEdition.NONE -> OwnedEdition.FIRST
        }
    }

    private fun toggleSecondEdition(current: OwnedEdition): OwnedEdition {
        return when (current) {
            OwnedEdition.FIRST -> OwnedEdition.BOTH       // First only → Add Second
            OwnedEdition.SECOND -> OwnedEdition.FIRST      // Second only → Switch to First
            OwnedEdition.BOTH -> OwnedEdition.FIRST        // Both → Remove Second, keep First
            // NONE should never happen, but if it does, default to SECOND
            OwnedEdition.NONE -> OwnedEdition.SECOND
        }
    }

    fun selectEdition(expansion: Expansion) {
        viewModelScope.launch {
            _cardsToShow.value =
                sortCards(cardDao.getCardsByExpansion(expansion.id))
            // We need to set these, so we need ExpansionWithEditions here. But also an int for the clicked edition
            //_selectedExpansion.value = expansion
            //_selectedEdition.value = whichEditionIsOwned(expansion)
            //_uiScreenState.value = UiScreenState.EXPANSION_CARDS

            Log.d("LibraryViewModel", "Selected edition ${expansion.name}")
        }
    }

    fun expansionHasTwoEditions(expansion: ExpansionWithEditions): Boolean {
        return expansion.firstEdition != null && expansion.secondEdition != null
    }

    fun selectCard(card: Card) {
        _selectedCard.value = card
        if (uiState.value != LibraryUiState.CARD_DETAIL) {
            lastState =
                uiState.value // Saving whether we come from search results or expansion cards
        }
        _uiState.value = LibraryUiState.CARD_DETAIL
        Log.d("LibraryViewModel", "Selected card ${card.name}")
    }

    fun clearSelectedCard() {
        _selectedCard.value = null
        // TODO I'd rather not have this here
        switchUiStateTo(LibraryUiState.EXPANSION_CARDS)
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
                if (name == "Base" || name == "Empires") {
                    cards.sortedWith(Card.CardTypeComparator(sortByCostAsTieBreaker = true))
                } else {
                    cards.sortedWith(Card.CardTypeComparator())
                }
            }

            SortType.EXPANSION -> cards.sortedBy { it.sets.first() }
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
        Log.d("LibraryViewModel", "Updated search text to $newText")

        // Automatically trigger search when text changes
        viewModelScope.launch {
            if (newText.isEmpty()) {
                // Clear search results and go back to previous state
                _cardsToShow.value = emptyList()
                // Return to the state we were in before searching
                _uiState.value = stateBeforeSearch
            } else if (newText.length >= 2 || newText.first().isDigit()) {
                // Save current state before switching to search results
                if (_uiState.value != LibraryUiState.SEARCH_RESULTS) {
                    stateBeforeSearch = _uiState.value
                }
                // Perform search
                _cardsToShow.value = cardDao.getFilteredCards("%$newText%")
                _uiState.value = LibraryUiState.SEARCH_RESULTS
            }

            Log.d(
                "LibraryViewModel",
                "Searched for $newText, search results: ${_cardsToShow.value.size}"
            )
        }
    }

    // TODO Remove?
    fun searchCards(newText: String) {
        viewModelScope.launch {

            if (newText.isEmpty()) {
                _cardsToShow.value = emptyList()
            } else if (newText.length >= 2 || newText.first().isDigit()) {

                // TODO: Sort? Type sort is broken here
                _cardsToShow.value = cardDao.getFilteredCards("%$newText%")
            }

            _uiState.value = LibraryUiState.SEARCH_RESULTS

            Log.d(
                "LibraryViewModel",
                "Searched for $newText, search results: ${_cardsToShow.value.size}"
            )
        }
    }

    // Card functions
    /*fun loadAllCards() {
        Log.d("LibraryViewModel", "Loading all cards")
        viewModelScope.launch {
            _cards.value = cardDao.getAll()
            sortCards()
            Log.d("LibraryViewModel", "Loaded all ${_cards.value.size} cards")
        }
    }*/

    fun triggerError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun showBlacklistedCards() {
        viewModelScope.launch {
            val disabledCards = cardDao.getDisabledCards()
            _cardsToShow.value = sortCards(disabledCards)
            _uiState.value = LibraryUiState.BLACKLISTED_CARDS
            Log.d("LibraryViewModel", "Showing ${disabledCards.size} disabled cards")
        }
    }

    val disabledCardCount: StateFlow<Int> = cardDao.getDisabledCardCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun toggleCardFavorite(card: Card) {
        viewModelScope.launch {

            val newIsFavoriteState = !card.isFavorite

            // Update database
            cardDao.toggleCardFavorite(card.id, newIsFavoriteState)

            // Update object
            _cardsToShow.value = _cardsToShow.value.map { c ->
                if (c.id == card.id) {
                    c.copy(isFavorite = newIsFavoriteState)
                } else {
                    c
                }
            }

            Log.d("LibraryViewModel", "Toggled card ${card.name} to favorite $newIsFavoriteState")
        }
    }

    fun toggleCardEnabled(card: Card) {
        viewModelScope.launch {

            val newIsEnabledState = !card.isEnabled

            // Update database
            cardDao.toggleCardEnabled(card.id, newIsEnabledState)

            // Update object
            _cardsToShow.value = _cardsToShow.value.map { c ->
                if (c.id == card.id) {
                    c.copy(isEnabled = newIsEnabledState)
                } else {
                    c
                }
            }

            // TODO does this make sense? When SortType == ENABLED, changing cards makes them jump
            if (sortType.value == SortType.ENABLED) {
                _cardsToShow.value = sortCards(_cardsToShow.value)
            }

            Log.d("LibraryViewModel", "Toggled card ${card.name} to enabled $newIsEnabledState")
        }
    }
}

private fun getEnabledCardAmount(cards: List<Card>): String {
    val enabledCount = cards.count { it.isEnabled }
    return "(${enabledCount}/${cards.size})"
}