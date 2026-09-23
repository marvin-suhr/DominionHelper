package dev.msuhr.dominionkingdoms.data

import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.model.DarkAgesMode
import dev.msuhr.dominionkingdoms.model.ExpansionWithEditions
import dev.msuhr.dominionkingdoms.model.PromoMode
import dev.msuhr.dominionkingdoms.model.ProsperityMode
import dev.msuhr.dominionkingdoms.model.RandomMode
import dev.msuhr.dominionkingdoms.model.RuleOption
import dev.msuhr.dominionkingdoms.model.VetoMode
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over the card storage used by the shared domain logic
 * (KingdomGenerator / CardDependencyResolver). The Android Room DAO
 * implements this; the iOS demo provides an in-memory implementation.
 */
interface CardDataSource {
    suspend fun getEnabledOwnedCards(): List<Card>
    suspend fun getEnabledOwnedSupplyLandscapes(): List<Card>
    suspend fun getPortraitsByExpansion(id: String): List<Card>
    suspend fun getSupplyLandscapesByExpansion(id: String): List<Card>
    suspend fun getEnabledCardsByExpansion(id: String): List<Card>
    suspend fun getRandomEnabledProphecy(): Card?
    suspend fun getRandomEnabledAlly(): Card?
    suspend fun getSingleCardFromExpansionWithExceptions(
        set1: String,
        set2: String?,
        excludedCards: Set<Int>,
        isLandscape: Boolean
    ): Card?
    suspend fun getSingleCardFromOwnedExpansionsWithExceptions(
        excludedCards: Set<Int>,
        isLandscape: Boolean
    ): Card?
    suspend fun getCardsByNameList(names: List<String>): List<Card>
}

interface ExpansionDataSource {
    suspend fun getOwnedExpansionsWithEditions(): List<ExpansionWithEditions>
}

/**
 * The user preferences the shared generation logic depends on.
 * The Android DataStore-backed repository implements this.
 */
interface UserPrefsSource {
    val randomMode: Flow<RandomMode>
    val randomExpansionAmount: Flow<Int>
    val vetoMode: Flow<VetoMode>
    val numberOfCardsToGenerate: Flow<Int>
    val landscapeCount: Flow<Int>
    val landscapeDifferentCategories: Flow<Boolean>
    val pickLandscapesFromAnyOwned: Flow<Boolean>
    val darkAgesStarterCardsMode: Flow<DarkAgesMode>
    val prosperityBasicCardsMode: Flow<ProsperityMode>
    val promoMode: Flow<PromoMode>
    val kingdomSortType: Flow<String>
    val activeRules: Flow<Map<String, RuleOption>>
    val landscapeRules: Flow<Map<String, Boolean>>
}
