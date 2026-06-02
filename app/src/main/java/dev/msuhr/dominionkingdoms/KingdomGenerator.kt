package dev.msuhr.dominionkingdoms

import android.util.Log
import dev.msuhr.dominionkingdoms.model.*
import dev.msuhr.dominionkingdoms.model.Set as CardSet
import dev.msuhr.dominionkingdoms.data.CardDao
import dev.msuhr.dominionkingdoms.data.ExpansionDao
import dev.msuhr.dominionkingdoms.data.UserPrefsRepository
import dev.msuhr.dominionkingdoms.ui.RandomMode
import dev.msuhr.dominionkingdoms.ui.VetoMode
import dev.msuhr.dominionkingdoms.ui.KingdomViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
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

    /**
     * Holds the available cards to draw from during generation.
     */
    private data class CandidatePool(
        val portraitPool: MutableSet<Card>,
        val landscapePool: MutableSet<Card>
    ) {
        override fun toString(): String = "${portraitPool.size} portrait cards, ${landscapePool.size} landscape cards"
    }

    suspend fun generateKingdom(): Kingdom {
        val totalCardsToGenerate = userPrefsRepository.numberOfCardsToGenerate.first()
        val totalLandscapeCardsToGenerate = userPrefsRepository.landscapeCount.first()
        val useDifferentLandscapeCategories = userPrefsRepository.landscapeDifferentCategories.first()
        val pickLandscapesFromAnyOwned = userPrefsRepository.pickLandscapesFromAnyOwned.first()
        val randomMode = userPrefsRepository.randomMode.first()
        val activeRulesMap = userPrefsRepository.activeRules.first()
        val landscapeRulesMap = userPrefsRepository.landscapeRules.first()

        val activeRules = activeRulesMap.mapNotNull { (id, option) ->
            CardRules.getRuleById(id)?.copy(option = option)
        }

        // Filter landscape pool based on enabled landscape types
        val enabledLandscapeTypes = landscapeRulesMap
            .filter { (_, enabled) -> enabled }
            .keys
            .mapNotNull { ruleId ->
                // Get the rule and check its condition to find the Type
                CardRules.getRuleById(ruleId)?.condition
            }

        return when (randomMode) {

            // Take cards completely randomly from all owned expansions
            RandomMode.FULL_RANDOM -> {
                val pool = getCandidatesFullRandom()
                Log.i("Kingdom Generator", "Starting generation - Full random")
                finalizeKingdomGeneration(
                    totalCardsToGenerate,
                    totalLandscapeCardsToGenerate,
                    useDifferentLandscapeCategories,
                    activeRules,
                    pool,
                    enabledLandscapeTypes
                )
            }

            // Take equal amounts of cards from random owned expansions
            RandomMode.EVEN_AMOUNTS -> {
                val numberOfExpansionsToPick = userPrefsRepository.randomExpansionAmount.first()
                
                val ownedExpansions = expansionDao.getOwnedOnce() // TODO fail early? ownedExpansions - numberOfExpansionsToPick
                // Group editions by their name (e.g. "Base 1E" and "Base 2E" both have name "Base")
                val groupedExpansions = ownedExpansions.groupBy { it.name }
                
                // Randomly pick the specified number of UNIQUE expansions
                // TODO give user feedback if groupedExpansions < numberOfExpansionsToPick
                val pickedExpansionNames = groupedExpansions.keys.shuffled().take(min(numberOfExpansionsToPick, groupedExpansions.size))
                
                // Flatten to include all editions for the picked expansions (so cards from both 1E and 2E are available)
                val randomExpansions = pickedExpansionNames.flatMap { groupedExpansions[it] ?: emptyList() }

                val pool = preparePoolWithLandscapeOption(
                    landscapeFromAnyOwned = pickLandscapesFromAnyOwned,
                    modeSpecificPoolProvider = { getCandidatesEvenAmounts(randomExpansions) }
                )

                Log.i("Kingdom Generator", "Starting generation - Even amounts ($randomExpansions)")

                finalizeKingdomGeneration(
                    totalCardsToGenerate,
                    totalLandscapeCardsToGenerate,
                    useDifferentLandscapeCategories,
                    activeRules,
                    pool,
                    enabledLandscapeTypes,
                    fillPortraitsStrategy = { cardList, cardPool, rules ->
                        fillPortraitsEvenly(totalCardsToGenerate, cardList, cardPool, randomExpansions, rules)
                    },
                    expansionSource = randomExpansions
                )
            }

            // Take random cards from a random subset of owned expansions
            RandomMode.LIMITED_RANDOM -> {
                val numberOfExpansionsToPick = userPrefsRepository.randomExpansionAmount.first()

                val ownedExpansions = expansionDao.getOwnedOnce()
                val groupedExpansions = ownedExpansions.groupBy { it.name }
                val pickedExpansionNames = groupedExpansions.keys.shuffled().take(min(numberOfExpansionsToPick, groupedExpansions.size))
                val randomExpansions = pickedExpansionNames.flatMap { groupedExpansions[it] ?: emptyList() }

                val pool = preparePoolWithLandscapeOption(
                    landscapeFromAnyOwned = pickLandscapesFromAnyOwned,
                    modeSpecificPoolProvider = { getCandidatesEvenAmounts(randomExpansions) }
                )

                Log.i("Kingdom Generator", "Starting generation - Limited random ($randomExpansions)")

                finalizeKingdomGeneration(
                    totalCardsToGenerate,
                    totalLandscapeCardsToGenerate,
                    useDifferentLandscapeCategories,
                    activeRules,
                    pool,
                    enabledLandscapeTypes,
                    expansionSource = randomExpansions
                )
            }
        }
    }

    /**
     * Shared logic to finalize the kingdom generation after the pool has been prepared.
     */
    private suspend fun finalizeKingdomGeneration(
        totalCardsToGenerate: Int,
        totalLandscapeCardsToGenerate: Int,
        useDifferentLandscapeCategories: Boolean,
        rules: List<GenerationRule>,
        pool: CandidatePool,
        enabledLandscapeTypes: List<(Card) -> Boolean> = emptyList(),
        fillPortraitsStrategy: ((MutableSet<Card>, MutableSet<Card>, List<GenerationRule>) -> Unit)? = null,
        expansionSource: List<Expansion>? = null
    ): Kingdom {
        val cardList = mutableSetOf<Card>()
        val landscapeList = mutableSetOf<Card>()

        // We don't even theoretically have enough cards to generate the kingdom. Abort
        if (pool.portraitPool.size < totalCardsToGenerate) {
            throw GenerationException("Not enough portrait cards owned / selected to meet $totalCardsToGenerate requirement. Only ${pool.portraitPool.size} cards available.")
        }
        if (pool.landscapePool.size < totalLandscapeCardsToGenerate) {
            throw GenerationException("Not enough landscape cards owned / selected to meet $totalLandscapeCardsToGenerate requirement. Only ${pool.landscapePool.size} cards available.")
        }

        Log.d("Kingdom Generator", "${pool.portraitPool.size} portrait and ${pool.landscapePool.size} landscape candidates found (goal: $totalCardsToGenerate / $totalLandscapeCardsToGenerate)")

        // 0. Filter landscape pool based on enabled landscape types
        if (enabledLandscapeTypes.isNotEmpty()) {
            val beforeFilter = pool.landscapePool.size
            pool.landscapePool.retainAll { card ->
                enabledLandscapeTypes.any { condition -> condition(card) }
            }
            Log.d("Kingdom Generator", "Filtered landscapes: $beforeFilter -> ${pool.landscapePool.size} (enabled types: ${enabledLandscapeTypes.size})")
        }

        // 1. Filter rules to only portrait rules (landscapes are now handled as simple on/off switches)
        val portraitRules = rules.filter { it.target == RuleTarget.PORTRAIT }.toMutableList()

        // 2. Fill landscape pool with enabled types
        // TODO Shouldn't we check beforehand if [landscapes required by rules] > landscapesToInclude? -> YES, fail early
        val landscapesLeft = totalLandscapeCardsToGenerate - landscapeList.size
        // -> Check early. This means that the rules require more cards than we have set to generate. -> Exception (?)

        fillLandscapePool(landscapesLeft, pool.landscapePool, landscapeList, useDifferentLandscapeCategories)

        // 3. Apply portrait rules and fill
        applyRules(pool.portraitPool, cardList, portraitRules)

        if (fillPortraitsStrategy != null) {
            fillPortraitsStrategy(cardList, pool.portraitPool, portraitRules)
        } else { // TODO Can't we use fillPortraitsStrategy here too
            val cardsLeft = totalCardsToGenerate - cardList.size
            if (cardsLeft > 0) {
                repeat(cardsLeft) {
                    if (pool.portraitPool.isNotEmpty()) {
                        val selected = pool.portraitPool.shuffled().first()
                        cardList.add(selected)
                        pool.portraitPool.remove(selected)
                        prunePoolByLimitRules(pool.portraitPool, cardList, portraitRules)
                    }
                }
            }
        }

        // 4. Ensure mandatory landscape dependencies (Omen -> Prophecy, Liaison -> Ally)
        ensureRequiredLandscapes(cardList, landscapeList)

        if (cardList.size < totalCardsToGenerate) {
            throw GenerationException("Not enough cards in the pool to reach $totalCardsToGenerate cards with the current rules. Only ${cardList.size} cards available.")
        }

        val expansionNames = if (expansionSource != null) {
            expansionSource.map { it.name }.distinct()
        } else {
            cardList.mapNotNull { it.sets.firstOrNull()?.displayName }.distinct()
        }
        val kingdomName = generateKingdomNameFromExpansionsList(expansionNames)

        // TODO Would be cooler to add Prophecy / Ally here. But it sucks since we have to randomize
        // TODO: Check if it makes sense to just load ids instead of the whole card
        val generatedKingdom = cardDependencyResolver.addDependentCards(cardList, landscapeList, kingdomName)

        // Apply sorting before returning
        val sortTypeName = userPrefsRepository.kingdomSortType.first()
        val sortType = try {
            KingdomViewModel.SortType.valueOf(sortTypeName)
        } catch (e: Exception) {
            KingdomViewModel.SortType.EXPANSION // Maybe always do this
        }
        return sortKingdom(generatedKingdom, sortType)
    }

    private suspend fun preparePoolWithLandscapeOption(
        landscapeFromAnyOwned: Boolean,
        modeSpecificPoolProvider: suspend () -> CandidatePool
    ): CandidatePool {
        val pool = modeSpecificPoolProvider() // So far this is always getCandidatesEvenAmounts
        return if (landscapeFromAnyOwned) {
            val globalCandidates = getCandidatesFullRandom() // Kinda weird, why not just get landscapes
            pool.copy(
                landscapePool = globalCandidates.landscapePool
            )
        } else {
            pool
        }
    }

    /**
     * Filters the card pool based on EXCLUDE rules and attempts to satisfy AT_LEAST/EXACTLY rules.
     */
    private fun applyRules(
        cardPool: MutableSet<Card>,
        cardList: MutableSet<Card>,
        rules: List<GenerationRule>
    ) {
        // 1. Filter out EXCLUDED cards from the pool
        val excludedPredicates = rules.filter { it.option.isExclude() }.map { it.condition }
        if (excludedPredicates.isNotEmpty()) {
            val toRemove = cardPool.filter { card -> excludedPredicates.any { it(card) } }
            cardPool.removeAll(toRemove.toSet())
            Log.d("Kingdom Generator", "Excluded ${toRemove.size} cards based on rules.")
        }

        // 2. Identify rules that require certain counts (min > 0)
        val requirementRules = rules.filter { it.option.min > 0 }

        // 3. Identify rules that LIMIT counts (max < MAX_CARDS)
        val limitRules = rules.filter { it.option.max < RuleOption.MAX_CARDS }

        // 4. Satisfy requirements
        var attempts = 0
        val maxCardsFromRules = 20 // Safety limit

        while (attempts < maxCardsFromRules) {
        // TODO I don't like this. This would be clearer: while (unsatisfiedRules.isNotEmpty())

            // Check if any added card randomly satisfies another rule
            val unsatisfiedRules = requirementRules.filter { rule ->
                val currentCount = cardList.count { rule.condition(it) }
                currentCount < rule.option.min
            }

            if (unsatisfiedRules.isEmpty()) break // All rules are satisfied

            // Pick an unsatisfied rule to work on (shuffled to avoid bias)
            val ruleToSatisfy = unsatisfiedRules.shuffled().first()

            val candidates = cardPool.filter { card ->

                // Must satisfy the current rule
                if (!ruleToSatisfy.condition(card)) return@filter false

                // Must not violate any existing limits if added
                limitRules.all { limitRule ->
                    val currentCount = cardList.count { limitRule.condition(it) }
                    val maxAllowed = limitRule.option.max
                    val satisfiesLimit = limitRule.condition(card)
                    if (satisfiesLimit) currentCount < maxAllowed else true
                }
            }
            
            if (candidates.isEmpty()) {
                throw GenerationException("Not enough cards to satisfy rule: ${ruleToSatisfy.name} (min ${ruleToSatisfy.option.min}) without violating other constraints.")
            }

            // Draw a card RANDOMLY (no prioritization)
            val selected = candidates.shuffled().first()

            cardList.add(selected)
            cardPool.remove(selected)
            prunePoolByLimitRules(cardPool, cardList, rules)
            attempts++

            // Check if the chosen card randomly satisfies another rule
            val otherSatisfied = unsatisfiedRules.filter { it != ruleToSatisfy && it.condition(selected) }
            if (otherSatisfied.isNotEmpty()) {
                Log.d("Kingdom Generator", "Added ${selected.name} to satisfy ${ruleToSatisfy.name}. This also fulfills: ${otherSatisfied.joinToString { it.name }}")
            } else {
                Log.d("Kingdom Generator", "Added ${selected.name} to satisfy ${ruleToSatisfy.name}.")
            }
        }

        // 5. Final pass: Remove any cards from pool that would violate limits during the fill phase
        prunePoolByLimitRules(cardPool, cardList, rules)
    }

    // TODO Can we get around running this a lot?
    private fun prunePoolByLimitRules(
        cardPool: MutableSet<Card>,
        cardList: Set<Card>,
        rules: List<GenerationRule>
    ) {
        val limitRules = rules.filter { it.option.max < RuleOption.MAX_CARDS }

        for (rule in limitRules) {
            val currentCount = cardList.count { rule.condition(it) }
            val maxAllowed = rule.option.max
            
            if (currentCount >= maxAllowed) {
                val toRemove = cardPool.filter { rule.condition(it) }
                cardPool.removeAll(toRemove.toSet())
            }
        }
    }

    private fun fillLandscapePool(
        landscapesLeft: Int,
        landscapePool: MutableSet<Card>,
        landscapeList: MutableSet<Card>,
        useDifferentLandscapeCategories: Boolean
    ) {
        if (landscapesLeft > 0) {
            var currentLandscapesLeft = landscapesLeft

            while (currentLandscapesLeft > 0 && landscapePool.isNotEmpty()) {
                val currentCategories = landscapeList.flatMap { card ->
                    card.types.intersect(CardRules.LANDSCAPE_TYPES)
                }.toSet()

                // If useDifferentCategories, filter out any cards from used categories
                val candidates = if (useDifferentLandscapeCategories) {
                    landscapePool.filter { card ->
                        // TODO Just check card.landscape == true here?
                        val cardLandscapeTypes = card.types.intersect(CardRules.LANDSCAPE_TYPES)
                        cardLandscapeTypes.isNotEmpty() && cardLandscapeTypes.none { it in currentCategories }
                    }
                } else {
                    landscapePool.toList()
                }

                if (candidates.isEmpty()) break  // TODO there are no candidates but we still need some. Exception here? -> Yes
                val selected = candidates.shuffled().first() // Take random card
                Log.d("Kingdom Generator", "Adding landscape card ${selected.name}")
                landscapeList.add(selected)
                landscapePool.remove(selected)
                currentLandscapesLeft--
            }
        }
    }

    /**
     * Ensures that if certain portraits are present, their mandatory landscape counterparts are added.
     * This handles Omen -> Prophecy and Liaison -> Ally.
     */
    private suspend fun ensureRequiredLandscapes(
        portraitCards: Set<Card>,
        landscapeList: MutableSet<Card>
    ) {
        // If there are Omens, add one random Prophecy
        if (portraitCards.any { it.types.contains(Type.OMEN) }) {
            val selected = cardDao.getRandomEnabledProphecy()
            if (selected != null) {
                Log.d("Kingdom Generator", "Omen(s) detected. Adding Prophecy: ${selected.name}")
                landscapeList.add(selected)
            } else {
                Log.w("Kingdom Generator", "Omen(s) detected but no Prophecies available!")
            }
        }

        // If there are Liaisons, add one random Ally
        if (portraitCards.any { it.types.contains(Type.LIAISON) }) {
            val selected = cardDao.getRandomEnabledAlly()

            if (selected != null) {
                Log.d("Kingdom Generator", "Liaison(s) detected. Adding Ally: ${selected.name}")
                landscapeList.add(selected)
            } else {
                Log.w("Kingdom Generator", "Liaison(s) detected but no Allies available!")
            }
        }
    }

    private fun generateKingdomNameFromExpansionsList(expansionNames: List<String>): String {
        return when {
            expansionNames.isEmpty() -> "Unnamed Kingdom"
            expansionNames.size == 1 -> expansionNames[0]
            expansionNames.size == 2 -> "${expansionNames[0]}, ${expansionNames[1]}"
            else -> "${expansionNames[0]}, ${expansionNames[1]}, +${expansionNames.size - 2}"
        }
    }

    private fun fillPortraitsEvenly(
        totalCardsToGenerate: Int,
        cardList: MutableSet<Card>,
        initialCardPool: MutableSet<Card>,
        randomExpansions: List<Expansion>,
        rules: List<GenerationRule>
    ) {
        val cardsStillToSelectGlobally = totalCardsToGenerate - cardList.size
        if (cardsStillToSelectGlobally <= 0) return

        if (randomExpansions.isEmpty()) {
            Log.w("Kingdom Generator", "Even amounts: No expansions to draw from. Filling remaining $cardsStillToSelectGlobally slots randomly from available pool.")
            repeat(cardsStillToSelectGlobally) {
                if (initialCardPool.isNotEmpty()) {
                    val selected = initialCardPool.shuffled().first()
                    cardList.add(selected)
                    initialCardPool.remove(selected)
                    prunePoolByLimitRules(initialCardPool, cardList, rules)
                }
            }
            return
        }

        // Group the selected expansions by name to treat different editions as one for "even" distribution
        val expansionsGroupedByName = randomExpansions.groupBy { it.name }
        val targetTotalCardsPerExpansionGroup = (totalCardsToGenerate.toDouble() / expansionsGroupedByName.size).roundToInt()

        Log.d("Kingdom Generator", "Targeting $targetTotalCardsPerExpansionGroup total cards per expansion for the remaining $cardsStillToSelectGlobally slots.")

        for ((_, editions) in expansionsGroupedByName) {
            val currentGlobalRemaining = totalCardsToGenerate - cardList.size
            if (currentGlobalRemaining <= 0) break

            val editionIds = editions.map { it.id }.toSet()
            
            val cardsAlreadySelectedFromThisGroup = cardList.count { card ->
                card.sets.any { set -> set.name in editionIds }
            }
            
            var cardsNeededFromThisGroup = targetTotalCardsPerExpansionGroup - cardsAlreadySelectedFromThisGroup
            cardsNeededFromThisGroup = max(0, cardsNeededFromThisGroup)
            cardsNeededFromThisGroup = min(cardsNeededFromThisGroup, currentGlobalRemaining)

            if (cardsNeededFromThisGroup <= 0) continue

            repeat(cardsNeededFromThisGroup) {
                if (totalCardsToGenerate - cardList.size <= 0) return@repeat
                
                val candidateCardsFromThisGroup = initialCardPool
                    .filter { card -> card.sets.any { set -> set.name in editionIds } }
                    .shuffled()

                val selected = candidateCardsFromThisGroup.firstOrNull() ?: return@repeat
                cardList.add(selected)
                initialCardPool.remove(selected)
                prunePoolByLimitRules(initialCardPool, cardList, rules)
            }
        }
    }

    private suspend fun getCandidatesFullRandom(): CandidatePool {
        val portraitCandidates = cardDao.getEnabledOwnedCards().toMutableSet()
        val landscapeCandidates = cardDao.getEnabledOwnedSupplyLandscapes().toMutableSet()
        return CandidatePool(portraitCandidates, landscapeCandidates)
    }

    // Rename
    private suspend fun getCandidatesEvenAmounts(
        randomExpansions: List<Expansion>
    ): CandidatePool {
        val portraitCandidates = mutableSetOf<Card>()
        val landscapeCandidates = mutableSetOf<Card>()

        Log.d("Kingdom Generator", "Getting candidates from expansions $randomExpansions")

        for (expansion in randomExpansions) {
            portraitCandidates.addAll(cardDao.getPortraitsByExpansion(expansion.id))
            landscapeCandidates.addAll(cardDao.getSupplyLandscapesByExpansion(expansion.id))
        }
        return CandidatePool(portraitCandidates, landscapeCandidates)
    }

    private fun sortKingdom(kingdom: Kingdom, sortType: KingdomViewModel.SortType): Kingdom {
        val sortedRandomCards = sortCardMap(kingdom.randomCards, sortType)
        val sortedLandscapeCards = sortCardMap(kingdom.landscapeCards, sortType)

        return kingdom.copy(
            randomCards = sortedRandomCards,
            landscapeCards = sortedLandscapeCards
        )
    }

    private fun sortCardMap(
        cards: LinkedHashMap<Card, Int>,
        sortType: KingdomViewModel.SortType
    ): LinkedHashMap<Card, Int> {
        if (cards.isEmpty()) return LinkedHashMap()

        val sortedEntries = when (sortType) {
            KingdomViewModel.SortType.EXPANSION -> cards.entries.sortedBy { it.key.sets.first().displayName }
            KingdomViewModel.SortType.ALPHABETICAL -> cards.entries.sortedBy { it.key.name }
            KingdomViewModel.SortType.COST -> cards.entries.sortedBy { it.key.cost ?: Int.MAX_VALUE }
        }

        val sortedCards = LinkedHashMap<Card, Int>()
        sortedEntries.forEach { sortedCards[it.key] = it.value }
        return sortedCards
    }

    // TODO review this v
    suspend fun replaceCardInKingdom(
        cardToRemove: Card,
        cardsToExclude: Set<Card>
    ): Card? {
        val isLandscape = cardToRemove.landscape
        val newCard: Card? = when (userPrefsRepository.vetoMode.first()) {
            VetoMode.REROLL_SAME -> {
                Log.i("KingdomGenerator", "Rerolling from the same expansion.")
                generateSingleRandomCardFromExpansion(cardToRemove.sets, cardsToExclude, isLandscape)
            }
            VetoMode.REROLL_ANY -> {
                // TODO: This rerolls from any OWNED expansion, but we need to reroll from any SELECTED expansion probably
                Log.i("KingdomGenerator", "Rerolling from any expansions.")
                generateSingleRandomCard(cardsToExclude, isLandscape)
            }
            // TODO: Veto mode NO_REROLL is checked beforehand. This is kind of messy tho.
            // In this case, returning null is an error case. Throw Exception here?
            VetoMode.NO_REROLL -> {
                Log.i("KingdomGenerator", "Not rerolling.")
                null
            }
        }
        return newCard
    }

    suspend fun generateSingleRandomCard(
        excludeCards: Set<Card> = emptySet(),
        isLandscape: Boolean
    ): Card? {
        val excludedCardIds = excludeCards.map { it.id }.toSet()
        Log.i("Kingdom Generator", "Generating random card from owned Expansions")
        return cardDao.getSingleCardFromOwnedExpansionsWithExceptions(excludedCardIds, isLandscape)
    }

    suspend fun generateSingleRandomCardFromExpansion(
        sets: List<CardSet>,
        excludeCards: Set<Card> = emptySet(),
        isLandscape: Boolean
    ): Card? {
        val excludedCardIds = excludeCards.map { it.id }.toSet()
        val setName1: String? = sets.getOrNull(0)?.name
        val setName2: String? = sets.getOrNull(1)?.name

        return if (setName1 != null) {
            val logMessage = "Generating random card from $setName1" +
                    (if (setName2 != null) " and $setName2" else "")
            Log.i("Kingdom Generator", logMessage)
            cardDao.getSingleCardFromExpansionWithExceptions(setName1, setName2, excludedCardIds, isLandscape)
        } else {
            // No sets provided (sets list was empty), or set names were null
            Log.w("Kingdom Generator", "Cannot generate card: No valid sets provided.")
            null
        }
    }
}
