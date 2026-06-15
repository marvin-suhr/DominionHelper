package dev.msuhr.dominionkingdoms.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.msuhr.dominionkingdoms.CardDependencyResolver
import dev.msuhr.dominionkingdoms.KingdomGenerator
import dev.msuhr.dominionkingdoms.data.ExpansionDao
import dev.msuhr.dominionkingdoms.data.UserPrefsRepository
import dev.msuhr.dominionkingdoms.model.AppSortType
import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.model.CardNames
import dev.msuhr.dominionkingdoms.model.Kingdom
import dev.msuhr.dominionkingdoms.data.repositories.KingdomRepository
import dev.msuhr.dominionkingdoms.utils.insertOrReplaceAtKeyPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.msuhr.dominionkingdoms.model.Type
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
import java.util.LinkedHashMap

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
    private val userPrefsRepository: UserPrefsRepository,
    private val cardDao: dev.msuhr.dominionkingdoms.data.CardDao
) : ViewModel(), ScreenViewModel {

    enum class SortType(val text: String) {
        EXPANSION("Sort by expansion"),
        ALPHABETICAL("Sort alphabetically"),
        COST("Sort by cost")
    }

    // Interface stuff

    override fun handleBackNavigation(): Boolean {
        when (_uiState.value) {
            KingdomUiState.KINGDOM_LIST -> return false
            KingdomUiState.LOADING -> return false
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

    // Grid view toggle for kingdom cards
    val isGridViewEnabled: StateFlow<Boolean> = userPrefsRepository.kingdomGridView
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true) // Default to true

    // Pending delete state (for undo functionality)
    private val _pendingDelete = MutableStateFlow<Kingdom?>(null)
    val pendingDelete: StateFlow<Kingdom?> = _pendingDelete.asStateFlow()

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
    val hasOwnedExpansions: StateFlow<Boolean> = expansionDao.hasAnyOwnedEdition()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val allKingdoms: StateFlow<List<Kingdom>> = kingdomRepository.getAllKingdoms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isCardDismissalEnabled: StateFlow<Boolean> = combine(
        userPrefsRepository.allowVetoing,
        userPrefsRepository.vetoMode,
        _kingdom,
        _isNewKingdom
    ) { allowVetoing, currentVetoMode, currentKingdom, isNew ->
        // Only allow vetoing if:
        // 1. It's a newly created kingdom (not previously saved), AND
        // 2. Vetoing is enabled, AND
        // 3. A veto mode with rerolling is enabled OR we have more than 10 cards
        isNew && allowVetoing && (currentVetoMode != VetoMode.NO_REROLL || currentKingdom.randomCards.size > 10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isLandscapeDismissalEnabled: StateFlow<Boolean> = combine(
        userPrefsRepository.allowVetoing,
        userPrefsRepository.vetoMode,
        _isNewKingdom
    ) { allowVetoing, currentVetoMode, isNew ->
        // Only allow vetoing if:
        // 1. It's a newly created kingdom (not previously saved), AND
        // 2. Vetoing is enabled, AND
        // 3. A veto mode with rerolling is enabled OR we have more than 10 cards
        isNew && allowVetoing && currentVetoMode != VetoMode.NO_REROLL
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

    fun toggleGridView() {
        viewModelScope.launch {
            val newValue = !isGridViewEnabled.value
            userPrefsRepository.setKingdomGridView(newValue)
            Log.d("KingdomViewModel", "Grid view toggled: $newValue")
        }
    }

    fun getRandomKingdom() {
        viewModelScope.launch {
            if (expansionDao.getOwnedExpansionsWithEditions().isEmpty()) {
                triggerError("You need at least one expansion to generate a kingdom.")
                return@launch
            }

            try {
                var generatedKingdom = kingdomGenerator.generateKingdom()
                generatedKingdom = applyPlayerCountToKingdom(generatedKingdom, _playerCount.value)
                generatedKingdom = applySortTypeToKingdom(generatedKingdom, _sortType.value)

                // Save the kingdom to database immediately (with its initial state)
                kingdomRepository.saveKingdom(generatedKingdom)

                // The generator now returns a full Kingdom with all dependencies resolved
                _kingdom.value = generatedKingdom
                _isNewKingdom.value = true // Mark as new kingdom for UI purposes (vetoing)
                switchUiStateTo(KingdomUiState.SINGLE_KINGDOM)

                // Show warning message if present
                generatedKingdom.warningMessage?.let { warning ->
                    triggerError(warning)
                }

            } catch (e: KingdomGenerator.GenerationException) {
                Log.e("KingdomViewModel", "Generation failed", e)
                triggerError(e.message ?: "Could not generate kingdom.")
            } catch (e: Exception) {
                Log.e("KingdomViewModel", "Unexpected error during generation", e)
                triggerError("An unexpected error occurred.")
            }
        }
    }

    private fun applySortTypeToKingdom(kingdom: Kingdom, newSortType: SortType): Kingdom {
        val sortedRandomCards = sortCards(kingdom.randomCards, newSortType)
        // Is it okay to sort just random cards here?
        return kingdom.copy(
            randomCards = sortedRandomCards,
            creationTimeStamp = kingdom.creationTimeStamp + 1 // trigger recompose. This sucks
        )
    }

    private fun sortCards(cards: LinkedHashMap<Card, Int>, sortType: SortType): LinkedHashMap<Card, Int> {
        if (cards.isEmpty()) return linkedMapOf()
        val sortedEntries = when (sortType) {
            SortType.EXPANSION -> cards.entries.sortedBy { it.key.sets.first().displayName }
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
        _kingdom.update { currentGlobalKingdom -> applyPlayerCountToKingdom(currentGlobalKingdom, newPlayerCount) }
    }

    fun userChangedSortType(newSortType: AppSortType.Kingdom) {
        Log.d("KingdomViewModel", "Selected sort type $newSortType")
        _sortType.value = newSortType.sortType
        _kingdom.update { currentGlobalKingdom -> applySortTypeToKingdom(currentGlobalKingdom, newSortType.sortType) }
    }

    // TODO Move elsewhere
    fun getCardAmounts(cards: LinkedHashMap<Card, Int>, playerCount: Int): LinkedHashMap<Card, Int> {
        require(playerCount in 2..6) { "Invalid player count: $playerCount" }
        val cardAmounts = linkedMapOf<Card, Int>()
        cards.forEach { (card, _) ->
            val amount = if (card.types.contains(Type.VICTORY)) {
                if (card.name == CardNames.PROVINCE) {
                    when (playerCount) {
                        2 -> 8
                        3 -> 12
                        4 -> 12
                        5 -> 15
                        6 -> 18
                        else -> error("Invalid player count")
                    }
                } else when (playerCount) {
                    2 -> 8
                    else -> 12
                }
            } else {
                when (card.name) {
                    CardNames.COPPER -> when (playerCount) {
                        2 -> 46
                        3 -> 39
                        4 -> 32
                        5 -> 85
                        6 -> 78
                        else -> error("Invalid player count")
                    }
                    CardNames.SILVER -> when (playerCount) {
                        in 2..4 -> 40
                        in 5..6 -> 80
                        else -> error("Invalid player count")
                    }
                    CardNames.GOLD -> when (playerCount) {
                        in 2..4 -> 40
                        in 5..6 -> 60
                        else -> error("Invalid player count")
                    }
                    CardNames.PLATINUM -> 12
                    CardNames.CURSE -> (playerCount - 1) * 10
                    CardNames.RUINS_PILE -> (playerCount - 1) * 10
                    CardNames.SUN_TOKENS -> when (playerCount) {
                        2 -> 5
                        3 -> 8
                        4 -> 10
                        5 -> 12
                        6 -> 13
                        else -> error("Invalid player count")
                    }
                    CardNames.REWARD_PILE -> if (playerCount == 2) 6 else 12
                    CardNames.CASTLES -> if (playerCount == 2) 6 else 12
                    CardNames.SPOILS -> 15
                    else -> 1
                }
            }
            cardAmounts[card] = amount
        }
        return cardAmounts
    }

    fun getCardAmount(card:  Card, playerCount: Int): Int {
        require(playerCount in 2..6) { "Invalid player count: $playerCount" }
        return if (card.types.contains(Type.VICTORY)) {
            if (card.name == CardNames.PROVINCE) {
                when (playerCount) {
                    2 -> 8
                    3 -> 12
                    4 -> 12
                    5 -> 15
                    6 -> 18
                    else -> error("Invalid player count")
                }
            }
            else when (playerCount) {
                2 -> 8
                else -> 12
            }
        } else {
            when (card.name) {
                CardNames.COPPER -> when (playerCount) {
                    2 -> 46
                    3 -> 39
                    4 -> 32
                    5 -> 85
                    6 -> 78
                    else -> error("Invalid player count")
                }
                CardNames.SILVER -> when (playerCount) {
                    in 2..4 -> 40
                    in 5..6 -> 80
                    else -> error("Invalid player count")
                }
                CardNames.GOLD -> when (playerCount) {
                    in 2..4 -> 40
                    in 5..6 -> 60
                    else -> error("Invalid player count")
                }
                CardNames.PLATINUM -> 12
                CardNames.CURSE -> (playerCount - 1) * 10
                CardNames.RUINS_PILE -> (playerCount - 1) * 10
                CardNames.SUN_TOKENS -> when (playerCount) {
                    2 -> 5
                    3 -> 8
                    4 -> 10
                    5 -> 12
                    6 -> 13
                    else -> error("Invalid player count")
                }
                CardNames.REWARD_PILE -> if (playerCount == 2) 6 else 12
                CardNames.CASTLES -> if (playerCount == 2) 6 else 12
                CardNames.SPOILS -> 15
                else -> 1
            }
        }
    }

    fun selectKingdom(kingdom: Kingdom) {
        // At this point the kingdoms are fully loaded? But without dependencies!
        // Consider displaying KingdomEntities at this stage! TODO
        // Problem: KingdomEntity needs to know images of card ids. -> Manageable
        // Furthermore: Should Kingdom contain card IDs or whole cards?
        // KingdomListUiItem

        Log.i("KingdomViewModel", "Selected kingdom ${kingdom.name}")
        viewModelScope.launch {
            val fullKingdom = cardDependencyResolver.addDependentCards(kingdom.randomCards.keys, kingdom.landscapeCards.keys)
            // Preserve the original kingdom's metadata (name, uuid, favorites, etc.)
            val kingdomWithMetadata = fullKingdom.copy(uuid = kingdom.uuid, creationTimeStamp = kingdom.creationTimeStamp, isFavorite = kingdom.isFavorite, name = kingdom.name)
            // TODO player count
            // TODO sort
            _kingdom.value = kingdomWithMetadata
            _isNewKingdom.value = false
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
        if (!currentKingdom.randomCards.containsKey(dismissedCard) && !currentKingdom.landscapeCards.containsKey(dismissedCard)) {
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

    private fun handleNoRerollDismissal(dismissedCard: Card, wasLandscape: Boolean) {
        Log.i(
            "LibraryViewModel",
            "VetoMode is NO_REROLL. Removing '${dismissedCard.name}' without replacement."
        )
        _kingdom.update { currentKingdom ->
            val updatedKingdom = if (wasLandscape) {
                currentKingdom.copy(landscapeCards = LinkedHashMap(currentKingdom.landscapeCards.toMutableMap().apply { remove(dismissedCard) }))
            } else {
                currentKingdom.copy(randomCards = LinkedHashMap(currentKingdom.randomCards.toMutableMap().apply { remove(dismissedCard) }))
            }
            // Save the updated kingdom to database immediately
            viewModelScope.launch { kingdomRepository.saveKingdom(updatedKingdom) }
            updatedKingdom
        }
    }

    private suspend fun handleRerollDismissal(dismissedCard: Card, kingdomSnapshot: Kingdom, wasLandscape: Boolean) {
        // Determine which list to use for exclusion and replacement target
        val originalCardsMap = if (wasLandscape) kingdomSnapshot.landscapeCards else kingdomSnapshot.randomCards
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
                currentKingdom.copy(landscapeCards = insertOrReplaceAtKeyPosition(kingdomSnapshot.landscapeCards, dismissedCard, newCard, 1))
            } else {
                val cardAmount = getCardAmount(newCard, _playerCount.value)
                currentKingdom.copy(randomCards = insertOrReplaceAtKeyPosition(kingdomSnapshot.randomCards, dismissedCard, newCard, cardAmount))
            }
            viewModelScope.launch { kingdomRepository.saveKingdom(updatedKingdom) }
            updatedKingdom
        }
    }

    suspend fun fetchKingdomDetails(uuid: String): Kingdom? = kingdomRepository.getKingdomById(uuid)

    fun deleteKingdom(uuid: String) {
        viewModelScope.launch {
            // Find the kingdom to delete
            val kingdomToDelete = allKingdoms.value.find { it.uuid == uuid }
            if (kingdomToDelete != null) {
                // Store for undo
                _pendingDelete.value = kingdomToDelete
                // Remove from database temporarily
                kingdomRepository.deleteKingdomById(uuid)

                // If selected kingdom was deleted
                if (_kingdom.value.uuid == uuid) {
                    _kingdom.value = Kingdom()
                    switchUiStateTo(KingdomUiState.KINGDOM_LIST)
                }
            }
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            val pending = _pendingDelete.value
            if (pending != null) {
                // Restore the kingdom
                kingdomRepository.saveKingdom(pending)
                _pendingDelete.value = null
            }
        }
    }

    fun confirmPendingDelete() {
        viewModelScope.launch {
            // Clear pending delete (snackbar dismissed without undo)
            _pendingDelete.value = null
        }
    }

    fun toggleFavorite(kingdom: Kingdom) {
        viewModelScope.launch { kingdomRepository.favoriteKingdomById(kingdom.uuid, !kingdom.isFavorite) }
    }

    fun updateKingdomName(uuid: String, newName: String) {
        viewModelScope.launch { kingdomRepository.changeKingdomName(uuid, newName) }
    }

    private fun saveKingdomIfNeeded() {
        // Reset the new kingdom flag when navigating back
        // The kingdom is already saved continuously after each veto
        _isNewKingdom.value = false
    }

    fun toggleCardFavorite(card: Card) {
        viewModelScope.launch {
            val newIsFavoriteState = !card.isFavorite

            // Update database
            cardDao.toggleCardFavorite(card.id, newIsFavoriteState)

            // TODO Dunno how I feel about this
            // Update all card maps in the kingdom
            _kingdom.value = _kingdom.value.copy(
                randomCards = updateCardMap(_kingdom.value.randomCards, card.id) { it.copy(isFavorite = newIsFavoriteState) },
                basicCards = updateCardMap(_kingdom.value.basicCards, card.id) { it.copy(isFavorite = newIsFavoriteState) },
                dependentCards = updateCardMap(_kingdom.value.dependentCards, card.id) { it.copy(isFavorite = newIsFavoriteState) },
                startingCards = updateCardMap(_kingdom.value.startingCards, card.id) { it.copy(isFavorite = newIsFavoriteState) },
                landscapeCards = updateCardMap(_kingdom.value.landscapeCards, card.id) { it.copy(isFavorite = newIsFavoriteState) }
            )

            // Update selectedCard to maintain reference equality
            if (_selectedCard.value?.id == card.id) {
                _selectedCard.value = _kingdom.value.getAllCards().find { it.id == card.id }
            }

            Log.d("KingdomViewModel", "Toggled card ${card.name} to favorite $newIsFavoriteState")
        }
    }

    fun toggleCardEnabled(card: Card) {
        viewModelScope.launch {
            val newIsEnabledState = !card.isEnabled

            // Update database
            cardDao.toggleCardEnabled(card.id, newIsEnabledState)

            // Update all card maps in the kingdom
            _kingdom.value = _kingdom.value.copy(
                randomCards = updateCardMap(_kingdom.value.randomCards, card.id) { it.copy(isEnabled = newIsEnabledState) },
                basicCards = updateCardMap(_kingdom.value.basicCards, card.id) { it.copy(isEnabled = newIsEnabledState) },
                dependentCards = updateCardMap(_kingdom.value.dependentCards, card.id) { it.copy(isEnabled = newIsEnabledState) },
                startingCards = updateCardMap(_kingdom.value.startingCards, card.id) { it.copy(isEnabled = newIsEnabledState) },
                landscapeCards = updateCardMap(_kingdom.value.landscapeCards, card.id) { it.copy(isEnabled = newIsEnabledState) }
            )

            // Update selectedCard to maintain reference equality
            if (_selectedCard.value?.id == card.id) {
                _selectedCard.value = _kingdom.value.getAllCards().find { it.id == card.id }
            }

            Log.d("KingdomViewModel", "Toggled card ${card.name} to enabled $newIsEnabledState")
        }
    }

    // Helper function to update a card in a LinkedHashMap
    private fun updateCardMap(map: LinkedHashMap<Card, Int>, cardId: Int, update: (Card) -> Card): LinkedHashMap<Card, Int> {
        val newMap = linkedMapOf<Card, Int>()
        map.forEach { (card, amount) -> if (card.id == cardId) newMap[update(card)] = amount else newMap[card] = amount }
        return newMap
    }
}
