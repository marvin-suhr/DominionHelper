package dev.msuhr.dominionkingdoms.shared.ui

import dev.msuhr.dominionkingdoms.CardDependencyResolver
import dev.msuhr.dominionkingdoms.KingdomGenerator
import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.model.CardNames
import dev.msuhr.dominionkingdoms.model.Kingdom
import dev.msuhr.dominionkingdoms.model.KingdomSortType
import dev.msuhr.dominionkingdoms.model.Type
import dev.msuhr.dominionkingdoms.model.VetoMode
import dev.msuhr.dominionkingdoms.platform.logD
import dev.msuhr.dominionkingdoms.platform.logE
import dev.msuhr.dominionkingdoms.platform.logI
import dev.msuhr.dominionkingdoms.shared.data.AppCardSource
import dev.msuhr.dominionkingdoms.shared.data.AppExpansionSource
import dev.msuhr.dominionkingdoms.shared.data.AppPrefsSource
import dev.msuhr.dominionkingdoms.shared.data.KingdomStore
import dev.msuhr.dominionkingdoms.shared.utils.insertOrReplaceAtKeyPosition
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class KingdomUiState {
    KINGDOM_LIST,
    LOADING,
    SINGLE_KINGDOM,
    CARD_DETAIL
}

/** Shared sort type for kingdoms (mirrors Android's typealias to KingdomSortType). */
typealias KingdomSort = KingdomSortType

class KingdomViewModel(
    private val kingdomRepository: KingdomStore,
    private val expansionDao: AppExpansionSource,
    private val kingdomGenerator: KingdomGenerator,
    private val cardDependencyResolver: CardDependencyResolver,
    private val userPrefsRepository: AppPrefsSource,
    private val cardDao: AppCardSource
) : SharedViewModel(), ScreenViewModel {

    enum class SortType(val text: String) {
        // kept for parity with the Android app; kingdoms use KingdomSortType
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
                saveKingdomIfNeeded()
                switchUiStateTo(KingdomUiState.KINGDOM_LIST)
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
        (sortType as? AppSortType.Kingdom)?.let { userChangedSortType(it) }
    }

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    override fun triggerScrollToTop() {
        if (_uiState.value == KingdomUiState.KINGDOM_LIST) {
            _scrollToTopEvent.tryEmit(Unit)
        } else {
            saveKingdomIfNeeded()
            clearSelectedCard()
            switchUiStateTo(KingdomUiState.KINGDOM_LIST)
        }
    }

    private val _sortType = MutableStateFlow(KingdomSortType.EXPANSION)
    val sortType: StateFlow<KingdomSortType> = _sortType.asStateFlow()

    override val currentAppSortType: StateFlow<AppSortType?> =
        _sortType.map { AppSortType.Kingdom(it) }
            .stateIn(scope, SUBSCRIBED, null)

    private val _uiState = MutableStateFlow(KingdomUiState.KINGDOM_LIST)
    val uiState: StateFlow<KingdomUiState> = _uiState.asStateFlow()

    private val _kingdom = MutableStateFlow(Kingdom())
    val kingdom: StateFlow<Kingdom> = _kingdom.asStateFlow()

    private val _isNewKingdom = MutableStateFlow(false)
    val isNewKingdom: StateFlow<Boolean> = _isNewKingdom.asStateFlow()

    // Grid view toggle for kingdom cards
    val isGridViewEnabled: StateFlow<Boolean> = userPrefsRepository.kingdomGridView
        .stateIn(scope, SUBSCRIBED, true)

    // Pending delete state (for undo functionality)
    private val _pendingDelete = MutableStateFlow<Kingdom?>(null)
    val pendingDelete: StateFlow<Kingdom?> = _pendingDelete.asStateFlow()

    private val _selectedCard = MutableStateFlow<Card?>(null)
    val selectedCard: StateFlow<Card?> = _selectedCard.asStateFlow()

    override val showBackButton: StateFlow<Boolean> =
        _uiState.map { it == KingdomUiState.SINGLE_KINGDOM || it == KingdomUiState.CARD_DETAIL }
            .stateIn(scope, SUBSCRIBED, false)

    override val showTopAppBar: StateFlow<Boolean> =
        _uiState.map { it != KingdomUiState.KINGDOM_LIST && it != KingdomUiState.CARD_DETAIL }
            .stateIn(scope, SUBSCRIBED, false)

    val topBarTitle: StateFlow<String> =
        combine(_uiState, _kingdom, _selectedCard) { uiState, kingdom, selectedCard ->
            when (uiState) {
                KingdomUiState.SINGLE_KINGDOM -> kingdom.name
                KingdomUiState.CARD_DETAIL -> selectedCard?.name ?: "Card Detail"
                else -> "Kingdoms"
            }
        }.stateIn(scope, SUBSCRIBED, "Kingdoms")

    // Fields

    private val _playerCount = MutableStateFlow(2)
    val playerCount: StateFlow<Int> = _playerCount.asStateFlow()

    val hasOwnedExpansions: StateFlow<Boolean> = expansionDao.hasAnyOwnedEdition()
        .stateIn(scope, SUBSCRIBED, false)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val allKingdoms: StateFlow<List<Kingdom>> = kingdomRepository.getAllKingdoms()
        .stateIn(scope, SUBSCRIBED, emptyList())

    val isCardDismissalEnabled: StateFlow<Boolean> = combine(
        userPrefsRepository.allowVetoing,
        userPrefsRepository.vetoMode,
        _kingdom,
        _isNewKingdom
    ) { allowVetoing, currentVetoMode, currentKingdom, isNew ->
        isNew && allowVetoing && (currentVetoMode != VetoMode.NO_REROLL || currentKingdom.randomCards.size > 10)
    }.stateIn(scope, SUBSCRIBED, false)

    val isLandscapeDismissalEnabled: StateFlow<Boolean> = combine(
        userPrefsRepository.allowVetoing,
        userPrefsRepository.vetoMode,
        _isNewKingdom
    ) { allowVetoing, currentVetoMode, isNew ->
        isNew && allowVetoing && currentVetoMode != VetoMode.NO_REROLL
    }.stateIn(scope, SUBSCRIBED, false)

    private fun switchUiStateTo(newState: KingdomUiState) {
        _uiState.value = newState
    }

    fun selectCard(card: Card) {
        _selectedCard.value = card
        _uiState.value = KingdomUiState.CARD_DETAIL
    }

    fun clearSelectedCard() {
        _selectedCard.value = null
        switchUiStateTo(KingdomUiState.SINGLE_KINGDOM)
    }

    fun toggleGridView() {
        launch {
            userPrefsRepository.setKingdomGridView(!isGridViewEnabled.value)
        }
    }

    fun getRandomKingdom() {
        launch {
            if (expansionDao.getOwnedExpansionsWithEditions().isEmpty()) {
                triggerError("You need at least one expansion to generate a kingdom.")
                return@launch
            }

            try {
                var generatedKingdom = kingdomGenerator.generateKingdom()
                generatedKingdom = applyPlayerCountToKingdom(generatedKingdom, _playerCount.value)
                generatedKingdom = applySortTypeToKingdom(generatedKingdom, _sortType.value)

                kingdomRepository.saveKingdom(generatedKingdom)

                _kingdom.value = generatedKingdom
                _isNewKingdom.value = true
                switchUiStateTo(KingdomUiState.SINGLE_KINGDOM)

                generatedKingdom.warningMessage?.let { warning ->
                    triggerError(warning)
                }
            } catch (e: KingdomGenerator.GenerationException) {
                logE("KingdomViewModel", "Generation failed", e)
                triggerError(e.message ?: "Could not generate kingdom.")
            } catch (e: Exception) {
                logE("KingdomViewModel", "Unexpected error during generation", e)
                triggerError("An unexpected error occurred.")
            }
        }
    }

    private fun applySortTypeToKingdom(kingdom: Kingdom, newSortType: KingdomSortType): Kingdom {
        val sortedRandomCards = sortCards(kingdom.randomCards, newSortType)
        return kingdom.copy(
            randomCards = sortedRandomCards,
            creationTimeStamp = kingdom.creationTimeStamp + 1 // trigger recompose
        )
    }

    private fun sortCards(cards: LinkedHashMap<Card, Int>, sortType: KingdomSortType): LinkedHashMap<Card, Int> {
        if (cards.isEmpty()) return linkedMapOf()
        val sortedEntries = when (sortType) {
            KingdomSortType.EXPANSION -> cards.entries.sortedBy { it.key.sets.first().displayName }
            KingdomSortType.ALPHABETICAL -> cards.entries.sortedBy { it.key.name }
            KingdomSortType.COST -> cards.entries.sortedBy { it.key.cost }
        }
        val sortedCards = LinkedHashMap<Card, Int>()
        sortedEntries.forEach { sortedCards[it.key] = it.value }
        return sortedCards
    }

    private fun applyPlayerCountToKingdom(kingdom: Kingdom, count: Int): Kingdom {
        return kingdom.copy(
            randomCards = getCardAmounts(kingdom.randomCards, count),
            dependentCards = getCardAmounts(kingdom.dependentCards, count),
            basicCards = getCardAmounts(kingdom.basicCards, count)
        )
    }

    fun userChangedPlayerCount(newPlayerCount: Int) {
        _playerCount.value = newPlayerCount
        _kingdom.update { current -> applyPlayerCountToKingdom(current, newPlayerCount) }
    }

    fun userChangedSortType(newSortType: AppSortType.Kingdom) {
        _sortType.value = newSortType.sortType
        _kingdom.update { current -> applySortTypeToKingdom(current, newSortType.sortType) }
    }

    fun getCardAmounts(cards: LinkedHashMap<Card, Int>, playerCount: Int): LinkedHashMap<Card, Int> {
        val cardAmounts = linkedMapOf<Card, Int>()
        cards.forEach { (card, _) ->
            cardAmounts[card] = getCardAmount(card, playerCount)
        }
        return cardAmounts
    }

    fun getCardAmount(card: Card, playerCount: Int): Int {
        return if (card.types.contains(Type.VICTORY)) {
            if (card.name == CardNames.PROVINCE) {
                when (playerCount) {
                    2 -> 8
                    3 -> 12
                    4 -> 12
                    5 -> 15
                    6 -> 18
                    else -> 8
                }
            } else {
                if (playerCount == 2) 8 else 12
            }
        } else {
            when (card.name) {
                CardNames.COPPER -> when (playerCount) {
                    2 -> 46
                    3 -> 39
                    4 -> 32
                    5 -> 85
                    6 -> 78
                    else -> 46
                }
                CardNames.SILVER -> if (playerCount in 2..4) 40 else 80
                CardNames.GOLD -> if (playerCount in 2..4) 40 else 60
                CardNames.PLATINUM -> 12
                CardNames.CURSE -> (playerCount - 1) * 10
                CardNames.RUINS_PILE -> (playerCount - 1) * 10
                CardNames.SUN_TOKENS -> when (playerCount) {
                    2 -> 5
                    3 -> 8
                    4 -> 10
                    5 -> 12
                    6 -> 13
                    else -> 5
                }
                CardNames.REWARD_PILE -> if (playerCount == 2) 6 else 12
                CardNames.CASTLES -> if (playerCount == 2) 6 else 12
                CardNames.SPOILS -> 15
                else -> 1
            }
        }
    }

    fun selectKingdom(kingdom: Kingdom) {
        logI("KingdomViewModel", "Selected kingdom ${kingdom.name}")
        launch {
            val fullKingdom = cardDependencyResolver.addDependentCards(kingdom.randomCards.keys, kingdom.landscapeCards.keys)
            val kingdomWithMetadata = fullKingdom.copy(
                uuid = kingdom.uuid,
                creationTimeStamp = kingdom.creationTimeStamp,
                isFavorite = kingdom.isFavorite,
                name = kingdom.name
            )
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

    // Kingdoms tabs. The Community tab of the Android app is backed by the
    // kingdom-sharing web service (Android-only for now); on iOS it shows the
    // empty state.
    enum class KingdomsTab(val label: String) {
        MY("My kingdoms"),
        SHARED("Community"),
    }

    private val _kingdomsTab = MutableStateFlow(KingdomsTab.MY)
    val kingdomsTab: StateFlow<KingdomsTab> = _kingdomsTab.asStateFlow()

    fun selectKingdomsTab(tab: KingdomsTab) {
        _kingdomsTab.value = tab
    }

    // Card dismissal / reroll
    fun onCardDismissed(dismissedCard: Card) {
        val currentKingdom = _kingdom.value
        if (!currentKingdom.randomCards.containsKey(dismissedCard) && !currentKingdom.landscapeCards.containsKey(dismissedCard)) {
            logI("KingdomViewModel", "Attempted to dismiss card '${dismissedCard.name}' not in kingdom.")
            return
        }

        logI("KingdomViewModel", "Dismissing card '${dismissedCard.name}' from the kingdom.")

        launch {
            if (userPrefsRepository.vetoMode.first() == VetoMode.NO_REROLL) {
                handleNoRerollDismissal(dismissedCard, dismissedCard.landscape)
            } else {
                handleRerollDismissal(dismissedCard, currentKingdom, dismissedCard.landscape)
            }
        }
    }

    private fun handleNoRerollDismissal(dismissedCard: Card, wasLandscape: Boolean) {
        _kingdom.update { currentKingdom ->
            val updatedKingdom = if (wasLandscape) {
                currentKingdom.copy(landscapeCards = LinkedHashMap(currentKingdom.landscapeCards.toMutableMap().apply { remove(dismissedCard) }))
            } else {
                currentKingdom.copy(randomCards = LinkedHashMap(currentKingdom.randomCards.toMutableMap().apply { remove(dismissedCard) }))
            }
            launch { kingdomRepository.saveKingdom(updatedKingdom) }
            updatedKingdom
        }
    }

    private suspend fun handleRerollDismissal(dismissedCard: Card, kingdomSnapshot: Kingdom, wasLandscape: Boolean) {
        val originalCardsMap = if (wasLandscape) kingdomSnapshot.landscapeCards else kingdomSnapshot.randomCards
        val cardsToExclude = originalCardsMap.keys.toMutableSet()
        val newCard = kingdomGenerator.replaceCardInKingdom(dismissedCard, cardsToExclude)
        if (newCard == null) {
            triggerError("Could not find a replacement card.")
            return
        }

        logI("KingdomViewModel", "Replaced '${dismissedCard.name}' with '${newCard.name}'.")
        _kingdom.update { currentKingdom ->
            val updatedKingdom = if (newCard.landscape) {
                currentKingdom.copy(landscapeCards = insertOrReplaceAtKeyPosition(kingdomSnapshot.landscapeCards, dismissedCard, newCard, 1))
            } else {
                val cardAmount = getCardAmount(newCard, _playerCount.value)
                currentKingdom.copy(randomCards = insertOrReplaceAtKeyPosition(kingdomSnapshot.randomCards, dismissedCard, newCard, cardAmount))
            }
            launch { kingdomRepository.saveKingdom(updatedKingdom) }
            updatedKingdom
        }
    }

    fun deleteKingdom(uuid: String) {
        launch {
            val kingdomToDelete = allKingdoms.value.find { it.uuid == uuid }
            if (kingdomToDelete != null) {
                _pendingDelete.value = kingdomToDelete
                kingdomRepository.deleteKingdomById(uuid)

                if (_kingdom.value.uuid == uuid) {
                    _kingdom.value = Kingdom()
                    switchUiStateTo(KingdomUiState.KINGDOM_LIST)
                }
            }
        }
    }

    fun undoDelete() {
        launch {
            _pendingDelete.value?.let { pending ->
                kingdomRepository.saveKingdom(pending)
                _pendingDelete.value = null
            }
        }
    }

    fun confirmPendingDelete() {
        launch {
            _pendingDelete.value = null
        }
    }

    fun toggleFavorite(kingdom: Kingdom) {
        launch { kingdomRepository.favoriteKingdomById(kingdom.uuid, !kingdom.isFavorite) }
    }

    fun updateKingdomName(uuid: String, newName: String) {
        launch { kingdomRepository.changeKingdomName(uuid, newName) }
    }

    private fun saveKingdomIfNeeded() {
        _isNewKingdom.value = false
    }

    fun toggleCardFavorite(card: Card) {
        launch {
            val newIsFavoriteState = !card.isFavorite
            cardDao.toggleCardFavorite(card.id, newIsFavoriteState)

            _kingdom.value = _kingdom.value.copy(
                randomCards = updateCardMap(_kingdom.value.randomCards, card.id) { it.copy(isFavorite = newIsFavoriteState) },
                basicCards = updateCardMap(_kingdom.value.basicCards, card.id) { it.copy(isFavorite = newIsFavoriteState) },
                dependentCards = updateCardMap(_kingdom.value.dependentCards, card.id) { it.copy(isFavorite = newIsFavoriteState) },
                startingCards = updateCardMap(_kingdom.value.startingCards, card.id) { it.copy(isFavorite = newIsFavoriteState) },
                landscapeCards = updateCardMap(_kingdom.value.landscapeCards, card.id) { it.copy(isFavorite = newIsFavoriteState) }
            )

            if (_selectedCard.value?.id == card.id) {
                _selectedCard.value = _kingdom.value.getAllCards().find { it.id == card.id }
            }
        }
    }

    fun toggleCardEnabled(card: Card) {
        launch {
            val newIsEnabledState = !card.isEnabled
            cardDao.toggleCardEnabled(card.id, newIsEnabledState)

            _kingdom.value = _kingdom.value.copy(
                randomCards = updateCardMap(_kingdom.value.randomCards, card.id) { it.copy(isEnabled = newIsEnabledState) },
                basicCards = updateCardMap(_kingdom.value.basicCards, card.id) { it.copy(isEnabled = newIsEnabledState) },
                dependentCards = updateCardMap(_kingdom.value.dependentCards, card.id) { it.copy(isEnabled = newIsEnabledState) },
                startingCards = updateCardMap(_kingdom.value.startingCards, card.id) { it.copy(isEnabled = newIsEnabledState) },
                landscapeCards = updateCardMap(_kingdom.value.landscapeCards, card.id) { it.copy(isEnabled = newIsEnabledState) }
            )

            if (_selectedCard.value?.id == card.id) {
                _selectedCard.value = _kingdom.value.getAllCards().find { it.id == card.id }
            }
        }
    }

    companion object {
        // Helper to update a card in a LinkedHashMap by id
        fun updateCardMap(map: LinkedHashMap<Card, Int>, cardId: Int, update: (Card) -> Card): LinkedHashMap<Card, Int> {
            val newMap = linkedMapOf<Card, Int>()
            map.forEach { (card, amount) -> if (card.id == cardId) newMap[update(card)] = amount else newMap[card] = amount }
            return newMap
        }
    }
}

/** Sealed sort types for the app top bar (shared equivalent of Android's model/AppSortType.kt). */
sealed class AppSortType(val text: String) {
    data class Kingdom(val sortType: KingdomSortType) : AppSortType(sortType.text)
    data class Library(val sortType: LibraryViewModel.SortType) : AppSortType(sortType.text)
}
