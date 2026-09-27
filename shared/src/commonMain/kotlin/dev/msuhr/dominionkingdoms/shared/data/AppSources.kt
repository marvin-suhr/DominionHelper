package dev.msuhr.dominionkingdoms.shared.data

import dev.msuhr.dominionkingdoms.data.CardDataSource
import dev.msuhr.dominionkingdoms.data.ExpansionDataSource
import dev.msuhr.dominionkingdoms.data.UserPrefsSource
import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.model.ExpansionWithEditions
import dev.msuhr.dominionkingdoms.model.Kingdom
import kotlinx.coroutines.flow.Flow

/**
 * Extended data-source contracts for the shared UI. They deliberately EXTEND
 * the interfaces from data/Sources.kt (implemented by the Android Room DAOs)
 * instead of modifying them - the Android app compiles unchanged.
 *
 * The iOS app provides in-memory implementations of everything here
 * (see iosMain .../data/InMemoryAppDataStore.kt).
 */
interface AppCardSource : CardDataSource {
    /** All cards of one edition/expansion id, regardless of enabled state. */
    suspend fun getCardsByExpansion(id: String): List<Card>
    suspend fun getCardsByIds(ids: List<Int>): List<Card>
    suspend fun getFavoriteCards(): List<Card>
    suspend fun getDisabledCardsExceptPromo(): List<Card>
    suspend fun getPromoCards(): List<Card>
    fun getFavoriteCardCount(): Flow<Int>
    fun getDisabledCardCountExceptPromo(): Flow<Int>
    fun getPromoCardCountFlow(): Flow<List<Card>>
    fun getNonBasicCardsFlow(): Flow<List<Card>>
    fun getCardsFlow(): Flow<List<Card>>
    suspend fun toggleCardFavorite(cardId: Int, isFavorite: Boolean)
    suspend fun toggleCardEnabled(cardId: Int, isEnabled: Boolean)

    /**
     * Port of the Android Room search query: matches name substrings, exact
     * cost/debt numbers, "debt"/"potion"/"overpay" keywords and exact
     * category/type element names (spaces become underscores).
     */
    suspend fun getFilteredCards(filter: String): List<Card>
}

interface AppExpansionSource : ExpansionDataSource {
    fun getAllWithEditions(): Flow<List<ExpansionWithEditions>>
    suspend fun updateEditionOwnership(editionId: String, isOwned: Boolean)
    fun hasAnyOwnedEdition(): Flow<Boolean>
}

/**
 * The full user-preferences contract of the Android UserPrefsRepository,
 * extended beyond [UserPrefsSource] with the theme / veto / grid settings.
 */
interface AppPrefsSource : UserPrefsSource {
    val isDarkMode: Flow<Boolean?>
    val useSystemTheme: Flow<Boolean>
    val allowVetoing: Flow<Boolean>
    val kingdomGridView: Flow<Boolean>

    suspend fun setDarkMode(value: Boolean?)
    suspend fun setUseSystemTheme(value: Boolean)
    suspend fun setRandomMode(value: dev.msuhr.dominionkingdoms.model.RandomMode)
    suspend fun setRandomExpansionAmount(value: Int)
    suspend fun setVetoMode(value: dev.msuhr.dominionkingdoms.model.VetoMode)
    suspend fun setAllowVetoing(value: Boolean)
    suspend fun setNumberOfCardsToGenerate(value: Int)
    suspend fun setLandscapeCount(value: Int)
    suspend fun setLandscapeDifferentCategories(value: Boolean)
    suspend fun setPickLandscapesFromAnyOwned(value: Boolean)
    suspend fun setDarkAgesStarterCardsMode(value: dev.msuhr.dominionkingdoms.model.DarkAgesMode)
    suspend fun setProsperityBasicCardsMode(value: dev.msuhr.dominionkingdoms.model.ProsperityMode)
    suspend fun setPromoMode(value: dev.msuhr.dominionkingdoms.model.PromoMode)
    suspend fun setKingdomSortType(value: String)
    suspend fun setKingdomGridView(value: Boolean)
    suspend fun setRuleOption(ruleId: String, option: dev.msuhr.dominionkingdoms.model.RuleOption)
    suspend fun setLandscapeRule(ruleId: String, enabled: Boolean)
    suspend fun resetGenerationRules()
}

/** Saved kingdoms (in-memory on iOS; the Android Room repository implements its own). */
interface KingdomStore {
    fun getAllKingdoms(): Flow<List<Kingdom>>
    suspend fun saveKingdom(kingdom: Kingdom)
    suspend fun deleteKingdomById(uuid: String)
    suspend fun favoriteKingdomById(uuid: String, isFavorite: Boolean)
    suspend fun changeKingdomName(uuid: String, newName: String)
    suspend fun getKingdomById(uuid: String): Kingdom?
}
