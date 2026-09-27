package dev.msuhr.dominionkingdoms.data

import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.model.CardJson
import dev.msuhr.dominionkingdoms.model.DarkAgesMode
import dev.msuhr.dominionkingdoms.model.Edition
import dev.msuhr.dominionkingdoms.model.ExpansionData
import dev.msuhr.dominionkingdoms.model.ExpansionWithEditions
import dev.msuhr.dominionkingdoms.model.Kingdom
import dev.msuhr.dominionkingdoms.model.PromoMode
import dev.msuhr.dominionkingdoms.model.ProsperityMode
import dev.msuhr.dominionkingdoms.model.RandomMode
import dev.msuhr.dominionkingdoms.model.RuleOption
import dev.msuhr.dominionkingdoms.model.Set as CardSet
import dev.msuhr.dominionkingdoms.model.Type
import dev.msuhr.dominionkingdoms.model.VetoMode
import dev.msuhr.dominionkingdoms.shared.data.AppCardSource
import dev.msuhr.dominionkingdoms.shared.data.AppExpansionSource
import dev.msuhr.dominionkingdoms.shared.data.AppPrefsSource
import dev.msuhr.dominionkingdoms.shared.data.KingdomStore
import dev.msuhr.dominionkingdoms.shared.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory implementations of the shared data sources, backing the iOS app
 * until persistence (Room KMP + DataStore) is ported. All expansions start
 * out owned, matching the Android app's defaults for generation.
 */
class InMemoryAppDataStore(cardsJson: String = BUNDLED_CARDS_JSON) {

    val cardSource: AppCardSource = InMemoryAppCardSource(cardsJson)
    val expansionSource: AppExpansionSource = InMemoryAppExpansionSource()
    val prefsSource: AppPrefsSource = InMemoryAppPrefs()
    val kingdomStore: KingdomStore = InMemoryKingdomStore()
}

class InMemoryAppCardSource(cardsJson: String) : AppCardSource {

    private val cardsById = MutableStateFlow(
        CardJson.parseCards(cardsJson).associateBy { it.id }
    )

    private val cards: List<Card> get() = cardsById.value.values.toList()

    private val portraits: List<Card>
        get() = cards.filter { it.supply && !it.landscape && !it.basic && it.isEnabled }
    private val supplyLandscapes: List<Card>
        get() = cards.filter { it.supply && it.landscape && !it.basic && it.isEnabled }

    // --- CardDataSource (generator contract) ---

    override suspend fun getEnabledOwnedCards(): List<Card> = portraits

    override suspend fun getEnabledOwnedSupplyLandscapes(): List<Card> = supplyLandscapes

    override suspend fun getPortraitsByExpansion(id: String): List<Card> =
        portraits.filter { card -> card.sets.any { it.name == id } }

    override suspend fun getSupplyLandscapesByExpansion(id: String): List<Card> =
        supplyLandscapes.filter { card -> card.sets.any { it.name == id } }

    override suspend fun getEnabledCardsByExpansion(id: String): List<Card> =
        cards.filter { card -> card.isEnabled && card.supply && card.sets.any { it.name == id } }

    override suspend fun getRandomEnabledProphecy(): Card? =
        supplyLandscapes.filter { it.types.contains(Type.PROPHECY) }.randomOrNull()

    override suspend fun getRandomEnabledAlly(): Card? =
        supplyLandscapes.filter { it.types.contains(Type.ALLY) }.randomOrNull()

    override suspend fun getSingleCardFromExpansionWithExceptions(
        set1: String,
        set2: String?,
        excludedCards: Set<Int>,
        isLandscape: Boolean
    ): Card? {
        val pool = if (isLandscape) supplyLandscapes else portraits
        return pool.filter { card ->
            card.id !in excludedCards &&
                (card.sets.any { it.name == set1 } || (set2 != null && card.sets.any { it.name == set2 }))
        }.randomOrNull()
    }

    override suspend fun getSingleCardFromOwnedExpansionsWithExceptions(
        excludedCards: Set<Int>,
        isLandscape: Boolean
    ): Card? {
        val pool = if (isLandscape) supplyLandscapes else portraits
        return pool.filter { it.id !in excludedCards }.randomOrNull()
    }

    override suspend fun getCardsByNameList(names: List<String>): List<Card> {
        val nameSet = names.toSet()
        return cards.filter { it.name in nameSet }
    }

    // --- AppCardSource (UI contract) ---

    override suspend fun getCardsByExpansion(id: String): List<Card> =
        cards.filter { card -> card.sets.any { it.name == id } }

    override suspend fun getCardsByIds(ids: List<Int>): List<Card> {
        val idSet = ids.toSet()
        return cards.filter { it.id in idSet }
    }

    override suspend fun getFavoriteCards(): List<Card> = cards.filter { it.isFavorite }

    override suspend fun getDisabledCardsExceptPromo(): List<Card> =
        cards.filter { !it.isEnabled && !it.hasPromoSet() }

    override suspend fun getPromoCards(): List<Card> = getCardsByExpansion("PROMO")

    override fun getFavoriteCardCount(): Flow<Int> = cardsFlow().map { list -> list.count { it.isFavorite } }

    override fun getDisabledCardCountExceptPromo(): Flow<Int> =
        cardsFlow().map { list -> list.count { !it.isEnabled && !it.hasPromoSet() } }

    override fun getPromoCardCountFlow(): Flow<List<Card>> =
        cardsFlow().map { list -> list.filter { it.hasPromoSet() } }

    override fun getNonBasicCardsFlow(): Flow<List<Card>> =
        cardsFlow().map { list -> list.filter { !it.basic } }

    override fun getCardsFlow(): Flow<List<Card>> = cardsFlow()

    override suspend fun toggleCardFavorite(cardId: Int, isFavorite: Boolean) =
        updateCard(cardId) { it.copy(isFavorite = isFavorite) }

    override suspend fun toggleCardEnabled(cardId: Int, isEnabled: Boolean) =
        updateCard(cardId) { it.copy(isEnabled = isEnabled) }

    override suspend fun getFilteredCards(filter: String): List<Card> {
        val f = filter
        val fUpper = f.replace(" ", "_").uppercase()
        val fLower = f.lowercase()
        return cards.filter { card ->
            val matches =
                card.name.contains(f, ignoreCase = true) ||
                    card.cost?.toString() == f ||
                    card.debt?.toString() == f ||
                    (fLower == "debt" && (card.debt ?: 0) > 0) ||
                    (fLower == "potion" && card.potion) ||
                    (fLower == "overpay" && card.overpay) ||
                    card.categories.any { it.name.uppercase() == fUpper } ||
                    card.types.any { it.name.uppercase() == fUpper }
            matches && !card.types.contains(Type.PILE) && !card.types.contains(Type.MAT)
        }
    }

    // --- helpers ---

    private fun cardsFlow(): Flow<List<Card>> = cardsById.map { it.values.toList() }

    private fun Card.hasPromoSet(): Boolean = sets.any { it.name == "PROMO" || it == CardSet.PROMO }

    private fun updateCard(cardId: Int, update: (Card) -> Card) {
        cardsById.value = cardsById.value.toMutableMap().apply {
            this[cardId]?.let { this[cardId] = update(it) }
        }
    }
}

class InMemoryAppExpansionSource : AppExpansionSource {

    private val expansionData: ExpansionData by lazy {
        CardJson.parseExpansions(BUNDLED_SETS_JSON)
    }

    // All editions owned initially - the iOS demo parity behaviour: generation
    // works immediately; ownership can be changed per expansion in the Library.
    private val ownedEditionIds = MutableStateFlow(
        expansionData.editions.map { it.id }.toSet()
    )

    private fun buildWithEditions(owned: Set<String>): List<ExpansionWithEditions> {
        return expansionData.expansions.map { expansion ->
            ExpansionWithEditions(
                expansion = expansion,
                editions = expansionData.editions
                    .filter { it.expansionId == expansion.id }
                    .map { it.copy(isOwned = it.id in owned) }
            )
        }
    }

    override fun getAllWithEditions(): Flow<List<ExpansionWithEditions>> =
        ownedEditionIds.map { owned -> buildWithEditions(owned) }

    override suspend fun getOwnedExpansionsWithEditions(): List<ExpansionWithEditions> {
        val owned = ownedEditionIds.value
        return buildWithEditions(owned)
            .map { expWithEds ->
                expWithEds.copy(editions = expWithEds.editions.filter { it.isOwned })
            }
            .filter { it.editions.isNotEmpty() }
    }

    override suspend fun updateEditionOwnership(editionId: String, isOwned: Boolean) {
        ownedEditionIds.value = ownedEditionIds.value.toMutableSet().apply {
            if (isOwned) add(editionId) else remove(editionId)
        }
    }

    override fun hasAnyOwnedEdition(): Flow<Boolean> =
        ownedEditionIds.map { it.isNotEmpty() }
}

class InMemoryAppPrefs : AppPrefsSource {

    private val landscapeRulesDefault = mapOf(
        "landscape_event" to true,
        "landscape_landmark" to true,
        "landscape_project" to true,
        "landscape_trait" to true,
        "landscape_way" to true
    )

    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    private val _useSystemTheme = MutableStateFlow(true)
    private val _randomMode = MutableStateFlow(Constants.DEFAULT_RANDOM_MODE)
    private val _randomExpansionAmount = MutableStateFlow(Constants.DEFAULT_RANDOM_EXPANSION_AMOUNT)
    private val _vetoMode = MutableStateFlow(Constants.DEFAULT_VETO_MODE)
    private val _allowVetoing = MutableStateFlow(true)
    private val _numberOfCardsToGenerate = MutableStateFlow(Constants.DEFAULT_NUMBER_OF_CARDS_TO_GENERATE)
    private val _landscapeCount = MutableStateFlow(Constants.DEFAULT_LANDSCAPE_COUNT)
    private val _landscapeDifferentCategories = MutableStateFlow(Constants.DEFAULT_LANDSCAPE_DIFFERENT_CATEGORIES)
    private val _pickLandscapesFromAnyOwned = MutableStateFlow(true)
    private val _darkAgesStarterCardsMode = MutableStateFlow(Constants.DEFAULT_DARK_AGES_STARTER_CARDS)
    private val _prosperityBasicCardsMode = MutableStateFlow(Constants.DEFAULT_PROSPERITY_BASIC_CARDS)
    private val _promoMode = MutableStateFlow(Constants.DEFAULT_PROMO_MODE)
    private val _kingdomSortType = MutableStateFlow("EXPANSION")
    private val _activeRules = MutableStateFlow<Map<String, RuleOption>>(emptyMap())
    private val _landscapeRules = MutableStateFlow(landscapeRulesDefault)
    private val _kingdomGridView = MutableStateFlow(true)

    override val isDarkMode: Flow<Boolean?> = _isDarkMode
    override val useSystemTheme: Flow<Boolean> = _useSystemTheme
    override val allowVetoing: Flow<Boolean> = _allowVetoing
    override val kingdomGridView: Flow<Boolean> = _kingdomGridView

    override val randomMode: Flow<RandomMode> = _randomMode
    override val randomExpansionAmount: Flow<Int> = _randomExpansionAmount
    override val vetoMode: Flow<VetoMode> = _vetoMode
    override val numberOfCardsToGenerate: Flow<Int> = _numberOfCardsToGenerate
    override val landscapeCount: Flow<Int> = _landscapeCount
    override val landscapeDifferentCategories: Flow<Boolean> = _landscapeDifferentCategories
    override val pickLandscapesFromAnyOwned: Flow<Boolean> = _pickLandscapesFromAnyOwned
    override val darkAgesStarterCardsMode: Flow<DarkAgesMode> = _darkAgesStarterCardsMode
    override val prosperityBasicCardsMode: Flow<ProsperityMode> = _prosperityBasicCardsMode
    override val promoMode: Flow<PromoMode> = _promoMode
    override val kingdomSortType: Flow<String> = _kingdomSortType
    override val activeRules: Flow<Map<String, RuleOption>> = _activeRules
    override val landscapeRules: Flow<Map<String, Boolean>> = _landscapeRules

    override suspend fun setDarkMode(value: Boolean?) { _isDarkMode.value = value }
    override suspend fun setUseSystemTheme(value: Boolean) { _useSystemTheme.value = value }
    override suspend fun setRandomMode(value: RandomMode) { _randomMode.value = value }
    override suspend fun setRandomExpansionAmount(value: Int) { _randomExpansionAmount.value = value }
    override suspend fun setVetoMode(value: VetoMode) { _vetoMode.value = value }
    override suspend fun setAllowVetoing(value: Boolean) { _allowVetoing.value = value }
    override suspend fun setNumberOfCardsToGenerate(value: Int) { _numberOfCardsToGenerate.value = value }
    override suspend fun setLandscapeCount(value: Int) { _landscapeCount.value = value }
    override suspend fun setLandscapeDifferentCategories(value: Boolean) { _landscapeDifferentCategories.value = value }
    override suspend fun setPickLandscapesFromAnyOwned(value: Boolean) { _pickLandscapesFromAnyOwned.value = value }
    override suspend fun setDarkAgesStarterCardsMode(value: DarkAgesMode) { _darkAgesStarterCardsMode.value = value }
    override suspend fun setProsperityBasicCardsMode(value: ProsperityMode) { _prosperityBasicCardsMode.value = value }
    override suspend fun setPromoMode(value: PromoMode) { _promoMode.value = value }
    override suspend fun setKingdomSortType(value: String) { _kingdomSortType.value = value }
    override suspend fun setKingdomGridView(value: Boolean) { _kingdomGridView.value = value }

    override suspend fun setRuleOption(ruleId: String, option: RuleOption) {
        _activeRules.value = _activeRules.value + (ruleId to option)
    }

    override suspend fun setLandscapeRule(ruleId: String, enabled: Boolean) {
        _landscapeRules.value = _landscapeRules.value + (ruleId to enabled)
    }

    override suspend fun resetGenerationRules() {
        _activeRules.value = emptyMap()
        _landscapeRules.value = landscapeRulesDefault
    }
}

class InMemoryKingdomStore : KingdomStore {

    private val kingdoms = MutableStateFlow<List<Kingdom>>(emptyList())

    override fun getAllKingdoms(): Flow<List<Kingdom>> = kingdoms

    override suspend fun saveKingdom(kingdom: Kingdom) {
        kingdoms.value = kingdoms.value.toMutableList().apply {
            val index = indexOfFirst { it.uuid == kingdom.uuid }
            if (index >= 0) this[index] = kingdom else add(0, kingdom)
        }
    }

    override suspend fun deleteKingdomById(uuid: String) {
        kingdoms.value = kingdoms.value.filterNot { it.uuid == uuid }
    }

    override suspend fun favoriteKingdomById(uuid: String, isFavorite: Boolean) {
        kingdoms.value = kingdoms.value.map {
            if (it.uuid == uuid) it.copy(isFavorite = isFavorite) else it
        }
    }

    override suspend fun changeKingdomName(uuid: String, newName: String) {
        kingdoms.value = kingdoms.value.map {
            if (it.uuid == uuid) it.copy(name = newName) else it
        }
    }

    override suspend fun getKingdomById(uuid: String): Kingdom? =
        kingdoms.value.find { it.uuid == uuid }
}
