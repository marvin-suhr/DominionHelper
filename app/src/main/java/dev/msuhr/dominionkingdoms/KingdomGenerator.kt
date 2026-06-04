package dev.msuhr.dominionkingdoms

import android.util.Log
import dev.msuhr.dominionkingdoms.model.*
import dev.msuhr.dominionkingdoms.data.CardDao
import dev.msuhr.dominionkingdoms.data.ExpansionDao
import dev.msuhr.dominionkingdoms.data.UserPrefsRepository
import dev.msuhr.dominionkingdoms.ui.RandomMode
import dev.msuhr.dominionkingdoms.ui.VetoMode
import dev.msuhr.dominionkingdoms.ui.KingdomViewModel
import dev.msuhr.dominionkingdoms.ui.PromoMode
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.roundToInt

@Singleton
class KingdomGenerator @Inject constructor(
    private val cardDao: CardDao,
    private val expansionDao: ExpansionDao,
    private val userPrefsRepository: UserPrefsRepository,
    private val cardDependencyResolver: CardDependencyResolver
) {

    class GenerationException(message: String) : Exception(message)

    // Internal representation of a set of cards and their source expansions
    // TODO Why use data class instead of class fields
    private data class GenerationContext(
        val portraitPool: MutableSet<Card>,
        val landscapePool: MutableSet<Card>,
        val expansionSource: List<ExpansionWithEditions>? = null,
        val config: GenerationConfig,
        var warning: String? = null
    )

    private data class GenerationConfig(
        val totalPortraits: Int,
        val totalLandscapes: Int,
        val useDifferentLandscapes: Boolean,
        val randomMode: RandomMode
    )

    suspend fun generateKingdom(): Kingdom {
        val config = GenerationConfig(
            totalPortraits = userPrefsRepository.numberOfCardsToGenerate.first(),
            totalLandscapes = userPrefsRepository.landscapeCount.first(),
            useDifferentLandscapes = userPrefsRepository.landscapeDifferentCategories.first(),
            randomMode = userPrefsRepository.randomMode.first()
        )
        
        val activeRulesMap = userPrefsRepository.activeRules.first()
        val landscapeRulesMap = userPrefsRepository.landscapeRules.first()
        val promoMode = userPrefsRepository.promoMode.first()

        val activeRules = activeRulesMap.mapNotNull { (id, option) ->
            CardRules.getRuleById(id)?.copy(option = option)
        }

        // Pre-calculate allowed landscape types
        val allowedLandscapeTypes = CardRules.LANDSCAPE_RULE_TO_TYPE
            .filter { (id, _) -> landscapeRulesMap[id] ?: true }
            .values
            .toSet()

        val enabledLandscapeFilter: (Card) -> Boolean = { card ->
            card.types.any { it in allowedLandscapeTypes }
        }

        val context = prepareContext(config, enabledLandscapeFilter)
        Log.i("Kingdom Generator", "Starting generation - Mode: ${config.randomMode} - Pool: ${context.portraitPool.size} portraits, ${context.landscapePool.size} landscapes")

        val generatedKingdom = runGenerationPipeline(context, activeRules, promoMode) // TODO add promoMode to context.config

        // Apply sorting before returning
        val sortTypeName = userPrefsRepository.kingdomSortType.first()

        // TODO I don't like that we sort here
        val sortType = try {
            KingdomViewModel.SortType.valueOf(sortTypeName)
        } catch (e: Exception) {
            KingdomViewModel.SortType.EXPANSION
        }
        return sortKingdom(generatedKingdom, sortType).copy(warningMessage = context.warning)
    }

    private suspend fun prepareContext(
        config: GenerationConfig,
        landscapeFilter: (Card) -> Boolean
    ): GenerationContext {
        val pickLandscapesFromAny = userPrefsRepository.pickLandscapesFromAnyOwned.first()
        var warning: String? = null

        return when (config.randomMode) {
            RandomMode.FULL_RANDOM -> {
                val portraits = cardDao.getEnabledOwnedCards().toMutableSet()
                val landscapes = cardDao.getEnabledOwnedSupplyLandscapes().filter(landscapeFilter).toMutableSet()
                GenerationContext(portraits, landscapes, null, config, null)
            }
            else -> {
                val requestedExpAmount = userPrefsRepository.randomExpansionAmount.first()
                val ownedExpansions = expansionDao.getOwnedExpansionsWithEditions()

                // Not enough expansions to satisfy config. Warn and continue
                if (ownedExpansions.size < requestedExpAmount) {
                    warning = "You only own ${ownedExpansions.size} expansions, but $requestedExpAmount were requested. Using all owned expansions."
                }
                
                val selectedExpansions = ownedExpansions.shuffled().take(requestedExpAmount)
                
                val editionIds = selectedExpansions.flatMap { it.editions }.filter { it.isOwned }.map { it.id }.toSet()
                val portraits = mutableSetOf<Card>()
                val landscapes = mutableSetOf<Card>()
                
                for (id in editionIds) {
                    portraits.addAll(cardDao.getPortraitsByExpansion(id))
                    if (!pickLandscapesFromAny) {
                        landscapes.addAll(cardDao.getSupplyLandscapesByExpansion(id).filter(landscapeFilter))
                    }
                }

                if (pickLandscapesFromAny) {
                    landscapes.addAll(cardDao.getEnabledOwnedSupplyLandscapes().filter(landscapeFilter))
                }

                GenerationContext(portraits, landscapes, selectedExpansions, config, /*editionIds,*/ warning)
            }
        }
    }

    // Shared logic to finalize the kingdom generation after the pool has been prepared
    private suspend fun runGenerationPipeline(
        context: GenerationContext,
        rules: List<GenerationRule>,
        promoMode: PromoMode
    ): Kingdom {
        val portraitList = mutableSetOf<Card>()
        val landscapeList = mutableSetOf<Card>()

        // 1. Validation
        // Error and cancel if not enough portrait cards
        if (context.portraitPool.size < context.config.totalPortraits) {
            throw GenerationException("Not enough portrait cards available (${context.portraitPool.size}) to meet requirement (${context.config.totalPortraits}).")
        }

        // Warning and continue if not enough landscape cards
        if (context.landscapePool.size < context.config.totalLandscapes) {
            context.warning = "Not enough landscape cards available (${context.landscapePool.size}) to meet requirements (${context.config.totalLandscapes})."
        }

        // 2. Landscape Phase
        fillRemainingLandscapes(
            targetCount = context.config.totalLandscapes,
            currentList = landscapeList,
            pool = context.landscapePool,
            useDifferentCategories = context.config.useDifferentLandscapes
        )

        // This should only happen due to useDifferentLandscapes
        if (landscapeList.size < context.config.totalLandscapes && context.warning == null) {
            context.warning = "Could only find ${landscapeList.size} of ${context.config.totalLandscapes} requested landscapes due to category constraints."
        }

        // 3. Promo Phase
        if (promoMode != PromoMode.NEVER) {
            val promoCards = cardDao.getEnabledCardsByExpansion("PROMO") // TODO how to circumvent this, it sucks
            if (promoCards.isNotEmpty()) {
                if (promoMode == PromoMode.ALWAYS_ONE) {
                    val selected = promoCards.shuffled().first()
                    portraitList.add(selected)
                    // Promo cards should NEVER be in the pool at this point
                    context.portraitPool.remove(selected)
                } else if (promoMode == PromoMode.POOL) {
                    context.portraitPool.addAll(promoCards)
                }
            }
        }

        // 4. Portrait Phase
        val portraitRules = rules.filter { it.target == RuleTarget.PORTRAIT }.toMutableList()
        applyRules(context.portraitPool, portraitList, portraitRules)
        
        if (context.config.randomMode == RandomMode.EVEN_AMOUNTS && context.expansionSource != null) { // TODO doesn't EVEN_AMOUNTS require expansionSource != null?
            fillPortraitsEvenly(context.config.totalPortraits, portraitList, context.portraitPool, context.expansionSource, portraitRules, context)
        } else {
            fillRemainingPortraits(context.config.totalPortraits, portraitList, context.portraitPool, portraitRules)
        }

        // 5. Final Material Check (Omen -> Prophecy, Liaison -> Ally)
        ensureRequiredLandscapes(portraitList, landscapeList)

        // 6. Naming
        val expansionNames = context.expansionSource?.map { it.name }?.distinct() 
            ?: portraitList.mapNotNull { it.sets.firstOrNull()?.displayName }.distinct()
        val kingdomName = generateKingdomName(expansionNames)

        return cardDependencyResolver.addDependentCards(portraitList, landscapeList, kingdomName)
    }

    private fun applyRules(
        pool: MutableSet<Card>,
        targetList: MutableSet<Card>,
        rules: List<GenerationRule>
    ) {
        // Exclude rules first
        val excludedPredicates = rules.filter { it.option.isExclude() }.map { it.condition }
        if (excludedPredicates.isNotEmpty()) {
            pool.removeAll { card -> excludedPredicates.any { it(card) } }
        }

        val requirementRules = rules.filter { it.option.min > 0 }
        var attempts = 0
        
        while (attempts < 20) { // TODO can't we loop over unsatisfiedRules.notEmpty()? We throw an Exception if we have no candidates left anyway

            // Check if any previously satisfied rules randomly satisfied other rules
            val unsatisfied = requirementRules.filter { rule -> targetList.count { rule.condition(it) } < rule.option.min }
            if (unsatisfied.isEmpty()) break

            // Work on a random rule to avoid bias
            val rule = unsatisfied.shuffled().first()
            val candidates = pool.filter { card -> 
                rule.condition(card) && rules.all { r -> // TODO Does it make sense to check against ALL rules here?
                    val count = targetList.count { r.condition(it) }
                    if (r.condition(card)) count < r.option.max else true // Kinda convuluted
                }
            }

            if (candidates.isEmpty()) {
                throw GenerationException("Cannot satisfy rule: ${rule.name} without violating other constraints.")
            }

            val selected = candidates.shuffled().first()
            targetList.add(selected)
            pool.remove(selected)
            attempts++
        }
        
        // Post-rule pool pruning: remove all cards from maxed categories
        pool.removeAll { card -> 
            rules.any { r -> r.option.max < RuleOption.MAX_CARDS && targetList.count { r.condition(it) } >= r.option.max && r.condition(card) }
        }
    }

    private fun fillRemainingLandscapes(
        targetCount: Int,
        currentList: MutableSet<Card>,
        pool: MutableSet<Card>,
        useDifferentCategories: Boolean
    ) {
        while (currentList.size < targetCount && pool.isNotEmpty()) {
            val candidates = if (useDifferentCategories) {
                val activeCats = currentList.flatMap { it.types.intersect(CardRules.LANDSCAPE_TYPES) }.toSet()
                pool.filter { card -> card.types.intersect(CardRules.LANDSCAPE_TYPES).none { it in activeCats } }
            } else pool.toList()

            if (candidates.isEmpty()) break

            val selected = candidates.shuffled().first()
            currentList.add(selected)
            pool.remove(selected)
        }
    }

    // Fill evenly from selected expansions
    private fun fillPortraitsEvenly(
        targetCount: Int,
        cardList: MutableSet<Card>,
        pool: MutableSet<Card>,
        expansions: List<ExpansionWithEditions>,
        rules: List<GenerationRule>,
        context: GenerationContext
    ) {
        val exhaustedExpansions = mutableSetOf<String>()

        // Group pool by expansion once
        val cardsByExpansion = pool.groupBy { card -> card.sets.first().name }
        val targetPerExpansion = (targetCount.toDouble() / expansions.size).roundToInt()

        for (expansion in expansions) {
            if (cardList.size >= targetCount) break

            // Check how many cards from that expansion are selected already
            val editionIds = expansion.editions.filter { it.isOwned }.map { it.id }
            val currentInExp = cardList.count { card -> card.sets.any { it.name in editionIds } }
            // Take the minimum of <expansion cards left> and <total cards left>
            val needed = min(targetPerExpansion - currentInExp, targetCount - cardList.size)

            repeat(needed) {
                // Get cards of expansions from the map we made earlier, check if card still in pool
                val candidates = editionIds.flatMap { id -> cardsByExpansion[id] ?: emptyList() }.filter { it in pool }
                val selected = candidates.shuffled().firstOrNull() ?: run {
                    // Another card from this expansion was requested, but not available
                    exhaustedExpansions.add(expansion.name)
                    return@repeat
                }
                cardList.add(selected)
                pool.remove(selected)
                // Reactive pruning - remove all cards from maxed categories
                pool.removeAll { c -> rules.any { r -> r.option.max < RuleOption.MAX_CARDS && cardList.count { r.condition(it) } >= r.option.max && r.condition(c) } }
            }
        }

        // TODO unsure about this. Can't we set warning any other way? I don't like passing the full context
        if (exhaustedExpansions.isNotEmpty()) {
            val names = exhaustedExpansions.joinToString(", ")
            val msg = "Not enough cards to satisfy even distribution for: $names."
            context.warning = context.warning?.let { "$it\n$msg" } ?: msg
        }

        // Final fallback fill
        fillRemainingPortraits(targetCount, cardList, pool, rules)
    }

    // Fill with random cards
    private fun fillRemainingPortraits(
        targetCount: Int,
        currentList: MutableSet<Card>,
        pool: MutableSet<Card>,
        rules: List<GenerationRule> = emptyList()
    ) {
        while (currentList.size < targetCount && pool.isNotEmpty()) {
            val selected = pool.shuffled().first()
            currentList.add(selected)
            pool.remove(selected)

            // Reactive pruning - remove all cards from maxed categories
            if (rules.isNotEmpty()) {
                pool.removeAll { card ->
                    rules.any { r -> r.option.max < RuleOption.MAX_CARDS && currentList.count { r.condition(it) } >= r.option.max && r.condition(card) }
                }
            }
        }

        // TODO: error here? When pool is empty
    }

    // Omen -> Prophecy, Liaison -> Ally
    private suspend fun ensureRequiredLandscapes(portraits: Set<Card>, landscapes: MutableSet<Card>) {
        if (portraits.any { it.types.contains(Type.OMEN) }) {
            val omen = cardDao.getRandomEnabledProphecy()
            omen?.let { landscapes.add(it) }
            // TODO Add warning if no prophecy was enabled
        }

        if (portraits.any { it.types.contains(Type.LIAISON) }) {
            val ally = cardDao.getRandomEnabledAlly()
            ally?.let { landscapes.add(it) }
            // TODO Add warning if no ally was enabled
        }
    }

    private fun generateKingdomName(names: List<String>): String = when {
        names.isEmpty() -> "Unnamed Kingdom"
        names.size <= 2 -> names.joinToString(", ")
        else -> "${names[0]}, ${names[1]}, +${names.size - 2}"
    }

    private fun sortKingdom(kingdom: Kingdom, sortType: KingdomViewModel.SortType): Kingdom {
        return kingdom.copy(
            randomCards = sortCardMap(kingdom.randomCards, sortType),
            landscapeCards = sortCardMap(kingdom.landscapeCards, sortType)
        )
    }

    private fun sortCardMap(cards: LinkedHashMap<Card, Int>, sortType: KingdomViewModel.SortType): LinkedHashMap<Card, Int> {
        if (cards.isEmpty()) return LinkedHashMap()
        val sorted = when (sortType) {
            KingdomViewModel.SortType.EXPANSION -> cards.entries.sortedBy { it.key.sets.first().displayName }
            KingdomViewModel.SortType.ALPHABETICAL -> cards.entries.sortedBy { it.key.name }
            KingdomViewModel.SortType.COST -> cards.entries.sortedBy { it.key.cost ?: Int.MAX_VALUE }
        }
        return LinkedHashMap<Card, Int>().apply { sorted.forEach { put(it.key, it.value) } }
    }

    suspend fun replaceCardInKingdom(card: Card, exclude: Set<Card>): Card? {
        val mode = userPrefsRepository.vetoMode.first()
        if (mode == VetoMode.NO_REROLL) return null
        return if (mode == VetoMode.REROLL_SAME) {
            cardDao.getSingleCardFromExpansionWithExceptions(card.sets[0].name, card.sets.getOrNull(1)?.name, exclude.map { it.id }.toSet(), card.landscape)
        } else {
            cardDao.getSingleCardFromOwnedExpansionsWithExceptions(exclude.map { it.id }.toSet(), card.landscape)
        }
    }
}
