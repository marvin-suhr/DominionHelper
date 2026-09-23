package dev.msuhr.dominionkingdoms.demo

import dev.msuhr.dominionkingdoms.data.CardDataSource
import dev.msuhr.dominionkingdoms.data.ExpansionDataSource
import dev.msuhr.dominionkingdoms.data.UserPrefsSource
import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.model.DarkAgesMode
import dev.msuhr.dominionkingdoms.model.Edition
import dev.msuhr.dominionkingdoms.model.Expansion
import dev.msuhr.dominionkingdoms.model.ExpansionSize
import dev.msuhr.dominionkingdoms.model.ExpansionWithEditions
import dev.msuhr.dominionkingdoms.model.PromoMode
import dev.msuhr.dominionkingdoms.model.ProsperityMode
import dev.msuhr.dominionkingdoms.model.RandomMode
import dev.msuhr.dominionkingdoms.model.RuleOption
import dev.msuhr.dominionkingdoms.model.Set as CardSet
import dev.msuhr.dominionkingdoms.model.VetoMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory implementations of the shared data sources, used by the iOS demo
 * app. They mimic the SQL filters of the Android Room DAOs closely enough for
 * kingdom generation. Everything is treated as owned and enabled.
 */
class InMemoryCardDataSource(private val cards: List<Card>) : CardDataSource {

    private val portraits = cards.filter { it.supply && !it.landscape && !it.basic && it.isEnabled }
    private val supplyLandscapes = cards.filter { it.supply && it.landscape && !it.basic && it.isEnabled }

    override suspend fun getEnabledOwnedCards(): List<Card> = portraits

    override suspend fun getEnabledOwnedSupplyLandscapes(): List<Card> = supplyLandscapes

    override suspend fun getPortraitsByExpansion(id: String): List<Card> =
        portraits.filter { card -> card.sets.any { it.name == id } }

    override suspend fun getSupplyLandscapesByExpansion(id: String): List<Card> =
        supplyLandscapes.filter { card -> card.sets.any { it.name == id } }

    override suspend fun getEnabledCardsByExpansion(id: String): List<Card> =
        cards.filter { card -> card.isEnabled && card.supply && card.sets.any { it.name == id } }

    override suspend fun getRandomEnabledProphecy(): Card? =
        supplyLandscapes.filter { it.types.contains(dev.msuhr.dominionkingdoms.model.Type.PROPHECY) }.randomOrNull()

    override suspend fun getRandomEnabledAlly(): Card? =
        supplyLandscapes.filter { it.types.contains(dev.msuhr.dominionkingdoms.model.Type.ALLY) }.randomOrNull()

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
}

/**
 * One expansion per card Set, all editions marked as owned. Edition numbers
 * are parsed from the Set name suffix (e.g. BASE_2E -> 2).
 */
class InMemoryExpansionDataSource(private val cards: List<Card>) : ExpansionDataSource {

    private val expansions: List<ExpansionWithEditions> by lazy {
        val usedSets: kotlin.collections.Set<CardSet> = cards.flatMap { it.sets }.toSet()
        usedSets.map { set ->
            val editionNumber = if (set.name.endsWith("_2E")) 2 else 1
            val expansionId = set.name.removeSuffix("_1E").removeSuffix("_2E")
            ExpansionWithEditions(
                expansion = Expansion(id = expansionId, name = set.displayName, imageName = set.imageName),
                editions = listOf(
                    Edition(
                        id = set.name,
                        expansionId = expansionId,
                        editionNumber = editionNumber,
                        isOwned = true,
                        year = 0,
                        size = ExpansionSize.MEDIUM,
                        imageName = set.imageName,
                        cards = 0,
                        landscapes = 0
                    )
                )
            )
        }
    }

    override suspend fun getOwnedExpansionsWithEditions(): List<ExpansionWithEditions> = expansions
}

/**
 * Demo preferences matching the Android app defaults.
 */
class InMemoryUserPrefs : UserPrefsSource {
    private val landscapeRulesDefault = mapOf(
        "landscape_event" to true,
        "landscape_landmark" to true,
        "landscape_project" to true,
        "landscape_trait" to true,
        "landscape_way" to true
    )

    override val randomMode: Flow<RandomMode> = MutableStateFlow(RandomMode.EVEN_AMOUNTS)
    override val randomExpansionAmount: Flow<Int> = MutableStateFlow(2)
    override val vetoMode: Flow<VetoMode> = MutableStateFlow(VetoMode.REROLL_SAME)
    override val numberOfCardsToGenerate: Flow<Int> = MutableStateFlow(10)
    override val landscapeCount: Flow<Int> = MutableStateFlow(2)
    override val landscapeDifferentCategories: Flow<Boolean> = MutableStateFlow(true)
    override val pickLandscapesFromAnyOwned: Flow<Boolean> = MutableStateFlow(true)
    override val darkAgesStarterCardsMode: Flow<DarkAgesMode> = MutableStateFlow(DarkAgesMode.TEN_PERCENT_PER_CARD)
    override val prosperityBasicCardsMode: Flow<ProsperityMode> = MutableStateFlow(ProsperityMode.TEN_PERCENT_PER_CARD)
    override val promoMode: Flow<PromoMode> = MutableStateFlow(PromoMode.POOL)
    override val kingdomSortType: Flow<String> = MutableStateFlow("EXPANSION")
    override val activeRules: Flow<Map<String, RuleOption>> = MutableStateFlow(emptyMap())
    override val landscapeRules: Flow<Map<String, Boolean>> = MutableStateFlow(landscapeRulesDefault)
}
