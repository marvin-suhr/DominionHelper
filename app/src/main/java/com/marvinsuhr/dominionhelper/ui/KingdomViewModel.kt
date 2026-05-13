package com.marvinsuhr.dominionhelper.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marvinsuhr.dominionhelper.CardDependencyResolver
import com.marvinsuhr.dominionhelper.KingdomGenerator
import com.marvinsuhr.dominionhelper.data.ExpansionDao
import com.marvinsuhr.dominionhelper.data.UserPrefsRepository
import com.marvinsuhr.dominionhelper.model.AppSortType
import com.marvinsuhr.dominionhelper.model.Card
import com.marvinsuhr.dominionhelper.model.CardNames
import com.marvinsuhr.dominionhelper.model.Kingdom
import com.marvinsuhr.dominionhelper.data.repositories.KingdomRepository
import com.marvinsuhr.dominionhelper.utils.insertOrReplaceAtKeyPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class KingdomUiState {
    KINGDOM_LIST,
    LOADING,
    SINGLE_KINGDOM,
    CARD_DETAIL
}

@HiltViewModel
class KingdomViewModel @Inject constructor(
    private val kingdomRepository: KingdomRepository,
    private val expansionDao: ExpansionDao,
    private val kingdomGenerator: KingdomGenerator,
    private val cardDependencyResolver: CardDependencyResolver,
    private val userPrefsRepository: UserPrefsRepository
) : ViewModel(), ScreenViewModel {

    enum class SortType(val text: String) {
        EXPANSION("Sort by expansion"),
        ALPHABETICAL("Sort alphabetically"),
        COST("Sort by cost")
    }

    // Interface stuff

    override fun handleBackNavigation(): Boolean {
        when (_uiState.value) {

            KingdomUiState.KINGDOM_LIST -> {
                return false
            }

            KingdomUiState.LOADING -> {
                return false
            }

            KingdomUiState.SINGLE_KINGDOM -> {
                // Save kingdom if it's newly created
                saveKingdomIfNeeded()
                switchUiStateTo(KingdomUiState.KINGDOM_LIST)
                // Clear kingdom?
                return true
            }

            KingdomUiState.CARD_DETAIL -> {
                clearSelectedCard()
                switchUiStateTo(KingdomUiState.SINGLE_KINGDOM)
                return true
            }
        }
    }

    override fun onSortTypeSelected(sortType: AppSortType) {
        Log.d("LibraryViewModel", "Selected sort type $sortType")
        userChangedSortType(sortType as AppSortType.Kingdom)
    }

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToTopEvent = _scrollToTopEvent.asSharedFlow()

    override fun triggerScrollToTop() {
        _scrollToTopEvent.tryEmit(Unit)
    }

    private val _sortType = MutableStateFlow(SortType.EXPANSION)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    override val currentAppSortType: StateFlow<AppSortType?> =
        sortType.map { AppSortType.Kingdom(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(KingdomUiState.KINGDOM_LIST)
    val uiState: StateFlow<KingdomUiState> = _uiState.asStateFlow()

    private val _kingdom = MutableStateFlow(Kingdom())
    val kingdom: StateFlow<Kingdom> = _kingdom.asStateFlow()

    // Track if the current kingdom is newly created (not yet saved) or previously saved
    private val _isNewKingdom = MutableStateFlow(false)
    val isNewKingdom: StateFlow<Boolean> = _isNewKingdom.asStateFlow()

    private val _selectedCard = MutableStateFlow<Card?>(null)
    val selectedCard: StateFlow<Card?> = _selectedCard.asStateFlow()

    override val showBackButton: StateFlow<Boolean> =
        uiState.map { uiState ->
            uiState == KingdomUiState.SINGLE_KINGDOM || uiState == KingdomUiState.CARD_DETAIL
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    override val showTopAppBar: StateFlow<Boolean> =
        uiState.map { uiState ->
            uiState != KingdomUiState.KINGDOM_LIST && uiState != KingdomUiState.CARD_DETAIL
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Top bar title that shows kingdom name when viewing a kingdom
    val topBarTitle: StateFlow<String> =
        combine(uiState, kingdom, selectedCard) { uiState, kingdom, selectedCard ->
            when (uiState) {
                KingdomUiState.SINGLE_KINGDOM -> kingdom.name
                KingdomUiState.CARD_DETAIL -> selectedCard?.name?: "Card Detail"
                else -> "Kingdoms"
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Kingdoms")

    // Fields

    // Player count
    private val _playerCount = MutableStateFlow(2)
    val playerCount: StateFlow<Int> = _playerCount.asStateFlow()

    // Track if there are any owned expansions
    val hasOwnedExpansions: StateFlow<Boolean> = expansionDao.hasAnyOwnedExpansion()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val allKingdoms: StateFlow<List<Kingdom>> = kingdomRepository.getAllKingdoms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isCardDismissalEnabled: StateFlow<Boolean> = combine(
        userPrefsRepository.vetoMode,
        _kingdom,
        _isNewKingdom
    ) { currentVetoMode, currentKingdom, isNew ->
        // Only allow vetoing if:
        // 1. It's a newly created kingdom (not previously saved), AND
        // 2. Either veto mode is enabled OR we have more than 10 cards
        isNew && (currentVetoMode != VetoMode.NO_REROLL || currentKingdom.randomCards.size > 10)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    // TopBarTitle stuff

    private fun switchUiStateTo(newState: KingdomUiState) {
        _uiState.value = newState
        Log.d("KingdomViewModel", "Switched UI state to $newState")
    }

    fun selectCard(card: Card) {
        _selectedCard.value = card
        _uiState.value = KingdomUiState.CARD_DETAIL
        Log.d("LibraryViewModel", "Selected card ${card.name}")
    }

    fun clearSelectedCard() {
        _selectedCard.value = null
        // TODO I'd rather not have this here
        switchUiStateTo(KingdomUiState.SINGLE_KINGDOM)
        Log.d("LibraryViewModel", "Cleared selected card")
    }

    fun getRandomKingdom() {
        viewModelScope.launch {

            if (expansionDao.getOwnedOnce().count() < 1) {
                Log.d(
                    "LibraryViewModel",
                    "No kingdom generated, as the user does not own any expansion"
                )
                triggerError("You need at least one expansion to generate a kingdom.")
                return@launch
            }

            val generatedKingdom = kingdomGenerator.generateKingdom()

            // Save the kingdom to database immediately (with its initial state)
            kingdomRepository.saveKingdom(generatedKingdom)

            // The generator now returns a full Kingdom with all dependencies resolved
            _kingdom.value = generatedKingdom
            _isNewKingdom.value = true // Mark as new kingdom for UI purposes (vetoing)
            switchUiStateTo(KingdomUiState.SINGLE_KINGDOM)
        }
    }

    private fun applySortTypeToKingdom(
        kingdom: Kingdom,
        newSortType: SortType
    ): Kingdom {

        // Sort kingdom lists (only random makes sense right?)
        val sortedRandomCards = sortCards(kingdom.randomCards, newSortType)
        //val sortedDependentCards = sortCards(kingdom.dependentCards, newSortType)
        //val sortedBasicCards = sortCards(kingdom.basicCards, newSortType)
        //val sortedStartingCards = sortCards(kingdom.startingCards, newSortType)
        //val sortedLandscapeCards = sortCards(kingdom.landscapeCards, newSortType)

        return kingdom.copy(
            randomCards = sortedRandomCards,
            //dependentCards = sortedDependentCards,
            //basicCards = sortedBasicCards,
            //startingCards = sortedStartingCards,
            //landscapeCards = sortedLandscapeCards,
            // TODO think of sth else
            creationTimeStamp = kingdom.creationTimeStamp + 1
        )
    }

    private fun sortCards(
        cards: LinkedHashMap<Card, Int>,
        sortType: SortType
    ): LinkedHashMap<Card, Int> {
        if (cards.isEmpty()) return linkedMapOf()

        val sortedEntries = when (sortType) {
            SortType.EXPANSION -> cards.entries.sortedBy { it.key.sets.first().name }
            SortType.ALPHABETICAL -> cards.entries.sortedBy { it.key.name }
            SortType.COST -> cards.entries.sortedBy { it.key.cost }
        }

        val sortedCards = LinkedHashMap<Card, Int>()
        sortedEntries.forEach { sortedCards[it.key] = it.value }
        Log.d("LibraryViewModel", "Sorted ${sortedCards.size} cards by ${_sortType.value}")
        return sortedCards
    }

    private fun applyPlayerCountToKingdom(kingdom: Kingdom, count: Int): Kingdom {
        val updatedRandomCards = getCardAmounts(kingdom.randomCards, count)
        val updatedDependentCards = getCardAmounts(kingdom.dependentCards, count)
        val updatedBasicCards = getCardAmounts(kingdom.basicCards, count)

        return kingdom.copy(
            randomCards = updatedRandomCards,
            dependentCards = updatedDependentCards,
            basicCards = updatedBasicCards
        )
    }

    fun userChangedPlayerCount(newPlayerCount: Int) {
        Log.d("KingdomViewModel", "Selected player count $newPlayerCount")
        _playerCount.value = newPlayerCount
        _kingdom.update { currentGlobalKingdom ->
            applyPlayerCountToKingdom(currentGlobalKingdom, newPlayerCount)
        }
    }

    fun userChangedSortType(newSortType: AppSortType.Kingdom) {
        Log.d("KingdomViewModel", "Selected sort type $newSortType")
        _sortType.value = newSortType.sortType
        _kingdom.update { currentGlobalKingdom ->
            applySortTypeToKingdom(currentGlobalKingdom, newSortType.sortType)
        }
    }

    // TODO: Move elsewherre
    fun getCardAmounts(
        cards: LinkedHashMap<Card, Int>,
        playerCount: Int
    ): LinkedHashMap<Card, Int> {
        assert(playerCount in 2..4)

        val cardAmounts = linkedMapOf<Card, Int>()

        cards.forEach { card, amount ->
            val amount = when (card.name) {
                CardNames.COPPER -> when (playerCount) {
                    2 -> 46
                    3 -> 39
                    4 -> 32
                    else -> throw IllegalArgumentException("Invalid player count: $playerCount")
                }

                CardNames.SILVER -> 40
                CardNames.GOLD -> 30
                CardNames.CURSE -> when (playerCount) {
                    2 -> 10
                    3 -> 20
                    4 -> 30
                    else -> throw IllegalArgumentException("Invalid player count: $playerCount")
                }

                CardNames.ESTATE -> if (playerCount == 2) 8 else 12
                CardNames.DUCHY -> if (playerCount == 2) 8 else 12
                CardNames.PROVINCE -> if (playerCount == 2) 8 else 12

                CardNames.RUINS_PILE -> when (playerCount) {
                    2 -> 10
                    3 -> 20
                    4 -> 30
                    else -> throw IllegalArgumentException("Invalid player count: $playerCount")
                }

                CardNames.REWARD_PILE -> if (playerCount == 2) 6 else 12

                else -> 1
            }
            cardAmounts[card] = amount
        }
        return cardAmounts
    }

    fun selectKingdom(kingdom: Kingdom) {
        // At this point the kingdoms are fully loaded? But without dependencies!
        // Consider displaying KingdomEntities at this stage! TODO
        // Problem: KingdomEntity needs to know images of card ids. -> Manageable
        // Furthermore: Should Kingdom contain card IDs or whole cards?
        // KingdomListUiItem

        Log.i("KingdomViewModel", "Selected kingdom ${kingdom.name}")
        viewModelScope.launch {
            val fullKingdom = cardDependencyResolver.addDependentCards(
                kingdom.randomCards.keys,
                kingdom.landscapeCards.keys
            )
            // Preserve the original kingdom's metadata (name, uuid, favorites, etc.)
            val kingdomWithMetadata = fullKingdom.copy(
                uuid = kingdom.uuid,
                creationTimeStamp = kingdom.creationTimeStamp,
                isFavorite = kingdom.isFavorite,
                name = kingdom.name
            )
            // player count
            // sort
            _kingdom.value = kingdomWithMetadata
            _isNewKingdom.value = false // This is a previously saved kingdom
            switchUiStateTo(KingdomUiState.SINGLE_KINGDOM)
        }
    }

    fun clearKingdom() {
        _kingdom.value = Kingdom()
        switchUiStateTo(KingdomUiState.KINGDOM_LIST)
    }

    fun triggerError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Card dismissal / reroll

    fun onCardDismissed(dismissedCard: Card) {

        val currentKingdom = _kingdom.value

        // Check if the card to be dismissed is actually present
        // (only random and landscape cards are dismissable)
        if (!currentKingdom.randomCards.containsKey(dismissedCard)
            && !currentKingdom.landscapeCards.containsKey(dismissedCard)
        ) {
            Log.w(
                "LibraryViewModel",
                "Attempted to dismiss card '${dismissedCard.name}' not found in the current kingdom."
            )
            return
        }

        Log.i("LibraryViewModel", "Dismissing card '${dismissedCard.name}' from the kingdom.")

        viewModelScope.launch {
            if (userPrefsRepository.vetoMode.first() == VetoMode.NO_REROLL) {
                handleNoRerollDismissal(dismissedCard, dismissedCard.landscape)
            } else {
                handleRerollDismissal(dismissedCard, currentKingdom, dismissedCard.landscape)
            }
        }
    }

    private fun handleNoRerollDismissal(
        dismissedCard: Card,
        wasLandscape: Boolean
    ) {
        Log.i(
            "LibraryViewModel",
            "VetoMode is NO_REROLL. Removing '${dismissedCard.name}' without replacement."
        )
        _kingdom.update { currentKingdom ->
            val updatedKingdom = when {
                wasLandscape -> currentKingdom.copy(
                    landscapeCards = LinkedHashMap(
                        currentKingdom.landscapeCards.toMutableMap()
                            .apply { remove(dismissedCard) })
                )

                else -> currentKingdom.copy(
                    randomCards = LinkedHashMap(
                        currentKingdom.randomCards.toMutableMap()
                            .apply { remove(dismissedCard) })
                )
            }

            // Save the updated kingdom to database immediately
            viewModelScope.launch {
                kingdomRepository.saveKingdom(updatedKingdom)
            }

            updatedKingdom
        }
    }

    private suspend fun handleRerollDismissal(
        dismissedCard: Card,
        kingdomSnapshot: Kingdom,
        wasLandscape: Boolean
    ) {
        // Determine which list to use for exclusion and replacement target
        val originalCardsMap =
            if (wasLandscape) kingdomSnapshot.landscapeCards else kingdomSnapshot.randomCards
        val cardsToExclude = originalCardsMap.keys.toMutableSet()

        val newCard = kingdomGenerator.replaceCardInKingdom(dismissedCard, cardsToExclude)

        if (newCard == null) {
            Log.e(
                "LibraryViewModel",
                "Failed to generate a replacement card for '${dismissedCard.name}'."
            )
            triggerError("Could not find a replacement card.")
            return
        }

        Log.i("LibraryViewModel", "Replaced '${dismissedCard.name}' with '${newCard.name}'.")
        _kingdom.update { currentKingdom ->
            val updatedKingdom = if (newCard.landscape) {
                currentKingdom.copy(
                    landscapeCards = insertOrReplaceAtKeyPosition(
                        map = kingdomSnapshot.landscapeCards,
                        targetKey = dismissedCard,
                        newKey = newCard,
                        newValue = 1
                    )
                )
            } else {
                currentKingdom.copy(
                    randomCards = insertOrReplaceAtKeyPosition(
                        map = kingdomSnapshot.randomCards,
                        targetKey = dismissedCard,
                        newKey = newCard,
                        newValue = 1
                    )
                )
            }

            // Save the updated kingdom to database immediately
            viewModelScope.launch {
                kingdomRepository.saveKingdom(updatedKingdom)
            }

            updatedKingdom
        }
    }

    // In its own class?

    suspend fun fetchKingdomDetails(uuid: String): Kingdom? {
        return kingdomRepository.getKingdomById(uuid)
    }

    fun deleteKingdom(uuid: String) {
        viewModelScope.launch {
            kingdomRepository.deleteKingdomById(uuid)

            // If selected kingdom was deleted
            if (_kingdom.value.uuid == uuid) {
                _kingdom.value = Kingdom()
                switchUiStateTo(KingdomUiState.KINGDOM_LIST)
            }
        }
    }

    fun toggleFavorite(kingdom: Kingdom) {
        viewModelScope.launch {
            kingdomRepository.favoriteKingdomById(kingdom.uuid, !kingdom.isFavorite)
        }
    }

    fun updateKingdomName(uuid: String, newName: String) {
        viewModelScope.launch {
            kingdomRepository.changeKingdomName(uuid, newName)
        }
    }

    private fun saveKingdomIfNeeded() {
        // Reset the new kingdom flag when navigating back
        // The kingdom is already saved continuously after each veto
        _isNewKingdom.value = false
    }
}