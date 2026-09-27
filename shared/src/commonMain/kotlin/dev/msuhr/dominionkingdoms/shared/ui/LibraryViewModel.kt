package dev.msuhr.dominionkingdoms.shared.ui

import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.model.Edition
import dev.msuhr.dominionkingdoms.model.ExpansionWithEditions
import dev.msuhr.dominionkingdoms.model.OwnedEdition
import dev.msuhr.dominionkingdoms.platform.logD
import dev.msuhr.dominionkingdoms.platform.logI
import dev.msuhr.dominionkingdoms.shared.data.AppCardSource
import dev.msuhr.dominionkingdoms.shared.data.AppExpansionSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryUiState {
    EXPANSIONS,
    EXPANSION_CARDS,
    SEARCH_RESULTS,
    CARD_DETAIL,
    BLACKLISTED_CARDS,
    FAVORITE_CARDS,
    PROMO_CARDS
}

class LibraryViewModel(
    private val cardDao: AppCardSource,
    private val expansionDao: AppExpansionSource
) : SharedViewModel(), ScreenViewModel {

    enum class SortType(val text: String) {
        TYPE("Sort by type"),
        ALPHABETICAL("Sort alphabetically"),
        COST("Sort by cost"),
        EXPANSION("Sort by expansion"),
        ENABLED("Sort by enabled");
    }

    // Interface stuff

    override fun handleBackNavigation(): Boolean {
        when (_uiState.value) {
            LibraryUiState.EXPANSIONS -> {
                return false
            }

            LibraryUiState.EXPANSION_CARDS -> {
                clearSelectedExpansion()
                switchUiStateTo(LibraryUiState.EXPANSIONS)
                return true
            }

            LibraryUiState.SEARCH_RESULTS -> {
                changeSearchText("")
                clearAllCards()
                switchUiStateTo(LibraryUiState.EXPANSIONS)
                return true
            }

            LibraryUiState.BLACKLISTED_CARDS -> {
                clearSelectedExpansion()
                switchUiStateTo(LibraryUiState.EXPANSIONS)
                return true
            }

            LibraryUiState.FAVORITE_CARDS -> {
                clearSelectedExpansion()
                switchUiStateTo(LibraryUiState.EXPANSIONS)
                return true
            }

            LibraryUiState.CARD_DETAIL -> {
                clearSelectedCard()
                switchUiStateTo(lastState)
                return true
            }

            LibraryUiState.PROMO_CARDS -> {
                clearSelectedExpansion()
                switchUiStateTo(LibraryUiState.EXPANSIONS)
                return true
            }
        }
    }

    override fun onSortTypeSelected(sortType: AppSortType) {
        (sortType as? AppSortType.Library)?.let { updateSortType(it) }
    }

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    override fun triggerScrollToTop() {
        if (_uiState.value == LibraryUiState.EXPANSIONS || _uiState.value == LibraryUiState.SEARCH_RESULTS) {
            _scrollToTopEvent.tryEmit(Unit)
        } else {
            clearSelectedExpansion()
            clearSelectedCard()
            switchUiStateTo(LibraryUiState.EXPANSIONS)
        }
    }

    private val _sortType = MutableStateFlow(SortType.TYPE)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    override val currentAppSortType: StateFlow<AppSortType?> =
        _sortType.map { AppSortType.Library(it) }
            .stateIn(scope, EAGER, null)

    private val _uiState = MutableStateFlow(LibraryUiState.EXPANSIONS)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    override val showBackButton: StateFlow<Boolean> =
        _uiState.map { it != LibraryUiState.EXPANSIONS }
            .stateIn(scope, EAGER, false)

    override val showTopAppBar: StateFlow<Boolean> =
        _uiState.map {
            it == LibraryUiState.EXPANSION_CARDS || it == LibraryUiState.FAVORITE_CARDS ||
                it == LibraryUiState.BLACKLISTED_CARDS || it == LibraryUiState.PROMO_CARDS
        }.stateIn(scope, EAGER, false)

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
            .stateIn(scope, EAGER, emptyList())

    val expansionCardCounts: StateFlow<Map<String, Pair<Int, Int>>> = combine(
        expansionsWithEditions,
        cardDao.getNonBasicCardsFlow()
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
                val landscapes = editionCards.count { it.landscape }

                counts[editionId] = Pair(portraits, landscapes)

                editionCards.forEach { card ->
                    if (card.landscape) landscapeSet.add(card.id)
                    else if (card.supply) portraitSet.add(card.id)
                }
            }

            counts[expansionWithEditions.id] = Pair(portraitSet.size, landscapeSet.size)
        }
        counts
    }.stateIn(scope, EAGER, emptyMap())

    private val _selectedExpansion = MutableStateFlow<ExpansionWithEditions?>(null)
    val selectedExpansion: StateFlow<ExpansionWithEditions?> = _selectedExpansion.asStateFlow()

    private val _selectedEdition = MutableStateFlow(OwnedEdition.NONE)
    val selectedEdition: StateFlow<OwnedEdition> = _selectedEdition.asStateFlow()

    private val _cardsToShow = MutableStateFlow<List<Card>>(emptyList())
    val cardsToShow: StateFlow<List<Card>> = _cardsToShow.asStateFlow()

    private val _selectedCard = MutableStateFlow<Card?>(null)
    val selectedCard: StateFlow<Card?> = _selectedCard.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val topBarTitle: StateFlow<String> = combine(
        _uiState,
        _selectedExpansion,
        _selectedCard,
        _cardsToShow
    ) { uiScreenState, selectedExpansion, selectedCard, cardsToShow ->
        when (uiScreenState) {
            LibraryUiState.EXPANSIONS -> "Library"
            LibraryUiState.EXPANSION_CARDS -> {
                selectedExpansion?.let { expansion ->
                    "${expansion.name} ${getEnabledCardAmount(cardsToShow)}"
                } ?: "Cards"
            }
            LibraryUiState.SEARCH_RESULTS -> "Search Results"
            LibraryUiState.BLACKLISTED_CARDS -> "Blacklisted Cards (${cardsToShow.size})"
            LibraryUiState.FAVORITE_CARDS -> "Favorite Cards (${cardsToShow.size})"
            LibraryUiState.PROMO_CARDS -> "Promo Cards"
            LibraryUiState.CARD_DETAIL -> selectedCard?.name ?: "Details"
        }
    }.stateIn(scope, EAGER, "Library")

    private fun switchUiStateTo(newState: LibraryUiState) {
        _uiState.value = newState
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
        launch {
            val edition = expansionsWithEditions.value.find { it.expansion.id == expansionId }
                ?.editions?.find { it.editionNumber == editionNumber }
            edition?.let {
                expansionDao.updateEditionOwnership(it.id, !it.isOwned)
            }
        }
    }

    /**
     * Cycle through ownership states for multi-edition expansions.
     * The cycle is: NONE -> FIRST -> SECOND -> BOTH -> NONE
     * For Cornucopia & Guilds 2nd edition, both expansions share ownership.
     */
    fun cycleMultiEditionOwnership(expansion: ExpansionWithEditions) {
        launch {
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
            expansion.secondEdition?.let { edition ->
                // The 2nd edition of Cornucopia / Guilds is the shared
                // CORNUCOPIA_GUILDS edition - skip it here, it is updated below.
                if (edition.expansionId != "CORNUCOPIA_GUILDS") {
                    expansionDao.updateEditionOwnership(edition.id, shouldOwnSecond)
                }
            }

            if (expansion.id == "CORNUCOPIA" || expansion.id == "GUILDS") {
                val cornGuild = expansionsWithEditions.value.find { it.id == "CORNUCOPIA_GUILDS" }
                cornGuild?.editions?.find { it.editionNumber == 2 }?.let {
                    expansionDao.updateEditionOwnership(it.id, shouldOwnSecond)
                }
            }

            logI("LibraryViewModel", "Cycled ownership for ${expansion.name}: $currentOwned -> $newOwned")
        }
    }

    /////////////////////////
    // Expansion functions //
    /////////////////////////

    fun selectExpansion(expansion: ExpansionWithEditions) {
        launch {
            val ownedEditions = whichEditionIsOwned(expansion)
            val cards = getCardsFromOwnedEditions(expansion, ownedEditions)
            _selectedExpansion.value = expansion
            _selectedEdition.value = ownedEditions
            _cardsToShow.value = sortCards(cards.toList())

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
    }

    fun selectEdition(
        expansion: ExpansionWithEditions,
        clickedEditionNumber: Int,
        currentOwnedEdition: OwnedEdition
    ) {
        launch {
            val newSelectedEdition = when (clickedEditionNumber) {
                1 -> toggleFirstEdition(currentOwnedEdition)
                2 -> toggleSecondEdition(currentOwnedEdition)
                else -> currentOwnedEdition
            }
            val cards = getCardsFromOwnedEditions(expansion, newSelectedEdition)
            _cardsToShow.value = sortCards(cards.toList())
            _selectedEdition.value = newSelectedEdition
        }
    }

    private fun toggleFirstEdition(current: OwnedEdition): OwnedEdition = when (current) {
        OwnedEdition.FIRST -> OwnedEdition.SECOND
        OwnedEdition.SECOND -> OwnedEdition.BOTH
        OwnedEdition.BOTH -> OwnedEdition.SECOND
        OwnedEdition.NONE -> OwnedEdition.FIRST
    }

    private fun toggleSecondEdition(current: OwnedEdition): OwnedEdition = when (current) {
        OwnedEdition.FIRST -> OwnedEdition.BOTH
        OwnedEdition.SECOND -> OwnedEdition.FIRST
        OwnedEdition.BOTH -> OwnedEdition.FIRST
        OwnedEdition.NONE -> OwnedEdition.SECOND
    }

    fun selectCard(card: Card) {
        _selectedCard.value = card
        if (_uiState.value != LibraryUiState.CARD_DETAIL) {
            lastState = _uiState.value
        }
        _uiState.value = LibraryUiState.CARD_DETAIL
    }

    fun clearSelectedCard() {
        _selectedCard.value = null
        switchUiStateTo(lastState)
    }

    fun clearAllCards() {
        _cardsToShow.value = emptyList()
    }

    private fun sortCards(cards: List<Card>): List<Card> {
        if (cards.isEmpty()) return cards
        val sortedCards = when (_sortType.value) {
            SortType.TYPE -> {
                val name = _selectedExpansion.value?.name
                cards.sortedWith(Card.CardTypeComparator(sortByCostAsTieBreaker = name == "Base" || name == "Empires"))
            }
            SortType.EXPANSION -> cards.sortedBy { it.sets.first().displayName }
            SortType.ALPHABETICAL -> cards.sortedBy { it.name }
            SortType.COST -> cards.sortedBy { it.cost }
            SortType.ENABLED -> cards.sortedBy { !it.isEnabled }
        }
        return sortedCards
    }

    fun updateSortType(newSortType: AppSortType.Library) {
        _sortType.value = newSortType.sortType
        _cardsToShow.value = sortCards(_cardsToShow.value)
    }

    fun changeSearchText(newText: String) {
        _searchText.value = newText
        launch {
            if (newText.isEmpty()) {
                _cardsToShow.value = emptyList()
                _uiState.value = stateBeforeSearch
            } else if (newText.length >= 2 || (newText.isNotEmpty() && newText.first().isDigit())) {
                if (_uiState.value != LibraryUiState.SEARCH_RESULTS) {
                    stateBeforeSearch = _uiState.value
                }
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
        launch {
            val disabledCards = cardDao.getDisabledCardsExceptPromo()
            _cardsToShow.value = sortCards(disabledCards)
            _uiState.value = LibraryUiState.BLACKLISTED_CARDS
        }
    }

    val blacklistedCardCount: StateFlow<Int> = cardDao.getDisabledCardCountExceptPromo()
        .stateIn(scope, EAGER, 0)

    fun showFavoriteCards() {
        launch {
            val favoriteCards = cardDao.getFavoriteCards()
            _cardsToShow.value = sortCards(favoriteCards)
            _uiState.value = LibraryUiState.FAVORITE_CARDS
        }
    }

    val favoriteCardCount: StateFlow<Int> = cardDao.getFavoriteCardCount()
        .stateIn(scope, EAGER, 0)

    fun showPromoCards() {
        launch {
            val promoCards = cardDao.getPromoCards()
            _cardsToShow.value = sortCards(promoCards)
            _uiState.value = LibraryUiState.PROMO_CARDS
        }
    }

    val promoCardCount: StateFlow<String> = cardDao.getPromoCardCountFlow()
        .map { cards ->
            val enabled = cards.count { it.isEnabled }
            "$enabled / ${cards.size} cards owned"
        }
        .stateIn(scope, EAGER, "0 / 0 cards owned")

    fun toggleCardFavorite(card: Card) {
        launch {
            val newIsFavoriteState = !card.isFavorite
            cardDao.toggleCardFavorite(card.id, newIsFavoriteState)
            _cardsToShow.value = _cardsToShow.value.map { c ->
                if (c.id == card.id) c.copy(isFavorite = newIsFavoriteState) else c
            }
            if (_selectedCard.value?.id == card.id) {
                _selectedCard.value = _cardsToShow.value.find { it.id == card.id }
            }
        }
    }

    fun toggleCardEnabled(card: Card) {
        launch {
            val newIsEnabledState = !card.isEnabled
            cardDao.toggleCardEnabled(card.id, newIsEnabledState)
            _cardsToShow.value = _cardsToShow.value.map { c ->
                if (c.id == card.id) c.copy(isEnabled = newIsEnabledState) else c
            }
            if (_selectedCard.value?.id == card.id) {
                _selectedCard.value = _cardsToShow.value.find { it.id == card.id }
            }
            if (sortType.value == SortType.ENABLED) {
                _cardsToShow.value = sortCards(_cardsToShow.value)
            }
        }
    }
}

private fun getEnabledCardAmount(cards: List<Card>): String {
    val enabledCount = cards.count { it.isEnabled }
    return "($enabledCount/${cards.size})"
}
