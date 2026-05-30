package dev.msuhr.dominionkingdoms

import android.util.Log
import dev.msuhr.dominionkingdoms.model.*
import dev.msuhr.dominionkingdoms.model.Set as CardSet
import dev.msuhr.dominionkingdoms.data.CardDao
import dev.msuhr.dominionkingdoms.data.ExpansionDao
import dev.msuhr.dominionkingdoms.data.UserPrefsRepository
import dev.msuhr.dominionkingdoms.ui.RandomMode
import dev.msuhr.dominionkingdoms.ui.VetoMode
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.first
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// TODO change at least / exactly / at most to a range slider instead -> YES
@Singleton
class KingdomGenerator @Inject constructor(
    private val cardDao: CardDao,
    private val expansionDao: ExpansionDao,
    private val userPrefsRepository: UserPrefsRepository,
    private val cardDependencyResolver: CardDependencyResolver
) {

    class GenerationException(message: String) : Exception(message)

    suspend fun generateKingdom(): Kingdom {
        val totalCardsToGenerate = userPrefsRepository.numberOfCardsToGenerate.first()
        val totalLandscapeCardsToGenerate = userPrefsRepository.landscapeCount.first()
        val useDifferentLandscapeCategories = userPrefsRepository.landscapeDifferentCategories.first()
        val pickLandscapesFromAnyOwned = userPrefsRepository.pickLandscapesFromAnyOwned.first()
        val randomMode = userPrefsRepository.randomMode.first()
        val activeRulesMap = userPrefsRepository.activeRules.first()

        val activeRules = activeRulesMap.mapNotNull { (id, option) ->
            CardRules.getRuleById(id)?.copy(option = option)
        }

        return when (randomMode) {

            // Take cards completely randomly from all owned expansions
            RandomMode.FULL_RANDOM -> {
                Log.i("Kingdom Generator", "Starting generation - Full random selected")
                generateKingdomFullRandom(
                    totalCardsToGenerate,
                    totalLandscapeCardsToGenerate,
                    useDifferentLandscapeCategories,
                    activeRules
                )
            }

            // Take equal amounts of cards from random owned expansions
            RandomMode.EVEN_AMOUNTS -> {
                val numberOfExpansionsToPick = userPrefsRepository.randomExpansionAmount.first()
                Log.i("Kingdom Generator", "Starting generation - Even amounts selected")
                generateKingdomEvenAmounts(
                    totalCardsToGenerate,
                    totalLandscapeCardsToGenerate,
                    useDifferentLandscapeCategories,
                    pickLandscapesFromAnyOwned,
                    numberOfExpansionsToPick,
                    activeRules
                )
            }

            // Take random cards from a random subset of owned expansions
            RandomMode.LIMITED_RANDOM -> {
                val numberOfExpansionsToPick = userPrefsRepository.randomExpansionAmount.first()
                Log.i("Kingdom Generator", "Starting generation - Limited random selected")
                generateKingdomLimitedRandom(
                    totalCardsToGenerate,
                    totalLandscapeCardsToGenerate,
                    useDifferentLandscapeCategories,
                    pickLandscapesFromAnyOwned,
                    numberOfExpansionsToPick,
                    activeRules
                )
            }
        }
    }

    // TODO: Make option to have this choose x random expansions and full random from there
    private suspend fun generateKingdomFullRandom(
        totalCardsToGenerate: Int,
        totalLandscapeCardsToGenerate: Int,
        useDifferentLandscapeCategories: Boolean,
        rules: List<GenerationRule>
    ): Kingdom {
        // TODO idk if I like this nonSupplyLandscapePool (Especially because it's not needed most of the time)
        val (cardPool, landscapePool, nonSupplyLandscapePool) = getCandidatesFullRandom()
        return finalizeKingdomGeneration(
            totalCardsToGenerate,
            totalLandscapeCardsToGenerate,
            useDifferentLandscapeCategories,
            rules,
            cardPool,
            landscapePool,
            nonSupplyLandscapePool
        )
    }

    private suspend fun generateKingdomLimitedRandom(
        totalCardsToGenerate: Int,
        totalLandscapeCardsToGenerate: Int,
        useDifferentLandscapeCategories: Boolean,
        pickLandscapesFromAnyOwned: Boolean,
        numberOfExpansionsToPick: Int,
        rules: List<GenerationRule>
    ): Kingdom {
        val ownedExpansions = expansionDao.getOwnedOnce()
        val groupedExpansions = ownedExpansions.groupBy { it.name }
        val pickedExpansionNames = groupedExpansions.keys.shuffled().take(min(numberOfExpansionsToPick, groupedExpansions.size))
        val randomExpansions = pickedExpansionNames.flatMap { groupedExpansions[it] ?: emptyList() }

        var (cardPool, landscapePool, nonSupplyLandscapePool) = getCandidatesEvenAmounts(randomExpansions)

        if (pickLandscapesFromAnyOwned) {
            val (_, globalLandscapes, globalNonSupplyLandscapes) = getCandidatesFullRandom()
            landscapePool = globalLandscapes
            nonSupplyLandscapePool = globalNonSupplyLandscapes
        }

        return finalizeKingdomGeneration(
            totalCardsToGenerate,
            totalLandscapeCardsToGenerate,
            useDifferentLandscapeCategories,
            rules,
            cardPool,
            landscapePool,
            nonSupplyLandscapePool
        )
    }

    private suspend fun finalizeKingdomGeneration(
        totalCardsToGenerate: Int,
        totalLandscapeCardsToGenerate: Int,
        useDifferentLandscapeCategories: Boolean,
        rules: List<GenerationRule>,
        cardPool: MutableSet<Card>,
        landscapePool: MutableSet<Card>,
        nonSupplyLandscapePool: MutableSet<Card>
    ): Kingdom {
        val cardList = mutableSetOf<Card>()

        // We don't even theoretically have enough cards to generate the kingdom. Abort
        if (cardPool.size < totalCardsToGenerate) {
            throw GenerationException("Not enough portrait cards owned / selected to meet $totalCardsToGenerate requirement. Only ${cardPool.size} cards available.")
        }
        if (landscapePool.size < totalLandscapeCardsToGenerate) {
            throw GenerationException("Not enough landscape cards owned / selected to meet $totalLandscapeCardsToGenerate requirement. Only ${landscapePool.size} cards available.")
        }

        Log.d("Kingdom Generator", "${cardPool.size} portrait and ${landscapePool.size} landscape candidates found")

        // TODO does it make sense to add portrait / landscape as an attribute on GenerationRule?
        /*Highly recommended. Currently, you use a heuristic (cardPool.any { rule.condition(it) })
        to guess if a rule applies to portraits or landscapes. This is clever but potentially slow
        and could lead to edge cases. Explicitly tagging rules (e.g., RuleTarget.PORTRAIT) would be
        cleaner and more robust.*/
        // 1. Separate rules by card target (Portrait vs Landscape)
        val portraitRules = rules.filter { rule ->
            // A rule targets Portraits if any Portrait card can satisfy it
            cardPool.any { rule.condition(it) }
        }
        val landscapeRules = rules.filter { rule ->
            // A rule targets Landscapes if any Landscape card can satisfy it
            landscapePool.any { rule.condition(it) }
        }

        // 2. Generate landscapes FIRST
        val landscapeList = mutableSetOf<Card>()
        applyRules(landscapePool, landscapeList, landscapeRules)

        // TODO Shouldn't we check beforehand if [landscapes required by rules] > landscapesToInclude? -> YES, fail early
        //val totalLandscapeCardsToGenerate = userPrefsRepository.landscapeCount.first()
        val landscapesLeft = totalLandscapeCardsToGenerate - landscapeList.size // TODO what if this is negative?
        // -> Check early. This means that the rules require more cards than we have set to generate. -> Exception (?)

        // TODO check this again + what to pass here. Maybe use some more lines from ^
        fillLandscapePool(landscapesLeft, landscapePool, landscapeList, useDifferentLandscapeCategories, landscapeRules)

        // 3. Apply portrait rules (including those triggered by landscapes)
        applyRules(cardPool, cardList, portraitRules)

        // 4. Fill remaining portrait slots
        val cardsLeft = totalCardsToGenerate - cardList.size
        if (cardsLeft > 0) {
            repeat(cardsLeft) {
                if (cardPool.isNotEmpty()) {
                    val selected = cardPool.shuffled().first()
                    cardList.add(selected)
                    cardPool.remove(selected)
                    prunePoolByLimitRules(cardPool, cardList, portraitRules)
                }
            }
        }

        // 5. Ensure mandatory landscape dependencies (Omen -> Prophecy, Liaison -> Ally)
        ensureRequiredLandscapes(cardList, landscapeList, nonSupplyLandscapePool)

        if (cardList.size < totalCardsToGenerate) {
            throw GenerationException("Not enough cards in the pool to reach $totalCardsToGenerate cards with the current rules. Only ${cardList.size} cards available.")
        }

        val expansionNames = cardList.mapNotNull { it.sets.firstOrNull()?.displayName }.distinct()
        val kingdomName = generateKingdomNameFromExpansionsList(expansionNames)

        // Dirty return
        // TODO: Check if it makes sense to just load ids instead of the whole card
        // Use cardDependencyResolver to build the full Kingdom with all dependent cards
        return cardDependencyResolver.addDependentCards(cardList, landscapeList, kingdomName)
    }

    private suspend fun generateKingdomEvenAmounts(
        totalCardsToGenerate: Int,
        totalLandscapeCardsToGenerate: Int,
        useDifferentLandscapeCategories: Boolean,
        pickLandscapesFromAnyOwned: Boolean,
        numberOfExpansionsToPick: Int,
        rules: List<GenerationRule>): Kingdom
    {
        val cardList = mutableSetOf<Card>()
        
        val ownedExpansions = expansionDao.getOwnedOnce() // TODO fail early? ownedExpansions - numberOfExpansionsToPick
        // Group editions by their name (e.g. "Base 1E" and "Base 2E" both have name "Base")
        val groupedExpansions = ownedExpansions.groupBy { it.name } // I THINK this respectes (un)owned editions
        
        // Randomly pick the specified number of UNIQUE expansions
        // TODO give user feedback if groupedExpansions < numberOfExpansionsToPick
        val pickedExpansionNames = groupedExpansions.keys.shuffled().take(min(numberOfExpansionsToPick, groupedExpansions.size))
        
        // Flatten to include all editions for the picked expansions (so cards from both 1E and 2E are available)
        // Unsure about this rn but sure
        val randomExpansions = pickedExpansionNames.flatMap { groupedExpansions[it] ?: emptyList() }

        var (cardPool, landscapePool, nonSupplyLandscapePool) = getCandidatesEvenAmounts(randomExpansions)

        // TODO hmm what is this
        if (pickLandscapesFromAnyOwned) {
            val (_, globalLandscapes, globalNonSupplyLandscapes) = getCandidatesFullRandom()
            landscapePool = globalLandscapes
            nonSupplyLandscapePool = globalNonSupplyLandscapes
        }

        // We don't even theoretically have enough cards to generate the kingdom. Abort
        if (cardPool.size < totalCardsToGenerate) {
            throw GenerationException("Not enough portrait cards owned / selected to meet $totalCardsToGenerate requirement. Only ${cardPool.size} cards available.")
        }
        if (landscapePool.size < totalLandscapeCardsToGenerate) {
            throw GenerationException("Not enough landscape cards owned / selected to meet $totalLandscapeCardsToGenerate requirement. Only ${landscapePool.size} cards available.")
        }

        Log.d("Kingdom Generator", "${cardPool.size} portrait and ${landscapePool.size} landscape candidates found")

        // TODO add this information to the Rule object
        // 1. Separate rules by card target
        val portraitRules = rules.filter { rule ->
            cardPool.any { rule.condition(it) }
        }
        val landscapeRules = rules.filter { rule ->
            landscapePool.any { rule.condition(it) }
        }

        // 2. Generate landscapes FIRST
        val landscapeList = mutableSetOf<Card>()
        applyRules(landscapePool, landscapeList, landscapeRules)

        val landscapesLeft = totalLandscapeCardsToGenerate - landscapeList.size

        fillLandscapePool(landscapesLeft, landscapePool, landscapeList, useDifferentLandscapeCategories, landscapeRules)

        // 3. Apply portrait rules (including those triggered by landscapes)
        applyRules(cardPool, cardList, portraitRules)

        // 4. Fill remaining portrait slots according to even amounts logic
        fillPortraitsEvenly(
            totalCardsToGenerate,
            cardList,
            cardPool,
            randomExpansions,
            portraitRules
        )

        // 5. Ensure we have enough cards if even distribution failed to reach the total
        val finalCardsLeft = totalCardsToGenerate - cardList.size
        if (finalCardsLeft > 0) {
            repeat(finalCardsLeft) {
                if (cardPool.isNotEmpty()) {
                    val selected = cardPool.shuffled().first()
                    cardList.add(selected)
                    cardPool.remove(selected)
                    prunePoolByLimitRules(cardPool, cardList, portraitRules)
                }
            }
        }

        // 6. Ensure mandatory landscape dependencies (Omen -> Prophecy, Liaison -> Ally)
        ensureRequiredLandscapes(cardList, landscapeList, nonSupplyLandscapePool)

        if (cardList.size < totalCardsToGenerate) {
            throw GenerationException("Not enough cards in the pool to reach $totalCardsToGenerate cards with the current rules. Only ${cardList.size} cards available.")
        }

        val kingdomName = generateKingdomNameFromExpansions(randomExpansions)

        // TODO: Check if it makes sense to just load ids instead of the whole card/
        // Use cardDependencyResolver to build the full Kingdom with all dependent cards
        // Dirty return
        return cardDependencyResolver.addDependentCards(cardList, landscapeList, kingdomName)
    }

    /**
     * Filters the card pool based on EXCLUDE rules and attempts to satisfy AT_LEAST/EXACTLY rules.
     */
    private fun applyRules(
        cardPool: MutableSet<Card>,
        cardList: MutableSet<Card>,
        rules: List<GenerationRule>
    ) {
        // TODO do we consider "always pick two different landscapes" here?
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

        // TODO can't we do while (unsatisfiedRules != empty)? -> Yes, but set a limit anyway to prevent infinite loops
        while (attempts < maxCardsFromRules) {
            // For every rule, check if any added card already satisfies it
            // (cardList is not empty after the first loop has been satisfied)
            // Here we check if any rule-fulfilling card randomly satisfies a second rule
            val unsatisfiedRules = requirementRules.filter { rule ->
                val currentCount = cardList.count { rule.condition(it) }
                currentCount < rule.option.min
            }

            if (unsatisfiedRules.isEmpty()) break // All rules are satisfied

            // Pick an unsatisfied rule to work on (shuffled to avoid bias)
            val ruleToSatisfy = unsatisfiedRules.shuffled().first()

            // Filter pool for candidates that satisfy this rule AND don't violate any limit rules
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
        useDifferentLandscapeCategories: Boolean,
        rules: List<GenerationRule>
    ) {
        if (landscapesLeft > 0) {
            var currentLandscapesLeft = landscapesLeft
            
            // Respect limit rules for landscapes
            prunePoolByLimitRules(landscapePool, landscapeList, rules)

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
                prunePoolByLimitRules(landscapePool, landscapeList, rules)  // Just remove from normal landscapePool?
                currentLandscapesLeft--
            }
        }
    }

    /**
     * Ensures that if certain portraits are present, their mandatory landscape counterparts are added.
     * This handles Omen -> Prophecy and Liaison -> Ally.
     */
    private fun ensureRequiredLandscapes(
        portraitCards: Set<Card>,
        landscapeList: MutableSet<Card>,
        nonSupplyLandscapePool: Set<Card>
    ) {
        // If there are Omens, add one random Prophecy from the non-supply pool
        if (portraitCards.any { it.types.contains(Type.OMEN) }) {
            val prophecies = nonSupplyLandscapePool.filter { it.types.contains(Type.PROPHECY) }
            if (prophecies.isNotEmpty()) {
                val selected = prophecies.shuffled().first()
                Log.d("Kingdom Generator", "Omen(s) detected. Adding Prophecy: ${selected.name}")
                landscapeList.add(selected)
            } else {
                Log.w("Kingdom Generator", "Omen(s) detected but no Prophecies available in non-supply pool!")
            }
        }

        // If there are Liaisons, add one random Ally from the non-supply pool
        if (portraitCards.any { it.types.contains(Type.LIAISON) }) {
            val allies = nonSupplyLandscapePool.filter { it.types.contains(Type.ALLY) }
            if (allies.isNotEmpty()) {
                val selected = allies.shuffled().first()
                Log.d("Kingdom Generator", "Liaison(s) detected. Adding Ally: ${selected.name}")
                landscapeList.add(selected)
            } else {
                Log.w("Kingdom Generator", "Liaison(s) detected but no Allies available in non-supply pool!")
            }
        }
    }

    private fun generateKingdomNameFromExpansions(expansions: List<Expansion>): String {
        val expansionNames = expansions.map { it.name }
        return generateKingdomNameFromExpansionsList(expansionNames)
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

    private suspend fun getCandidatesFullRandom(): Triple<MutableSet<Card>, MutableSet<Card>, MutableSet<Card>> {
        val portraitCandidates = cardDao.getEnabledOwnedCards().toMutableSet()
        val landscapeCandidates = cardDao.getEnabledOwnedSupplyLandscapes().toMutableSet()
        val nonSupplyLandscapeCandidates = cardDao.getEnabledOwnedSpecialLandscapes().toMutableSet()
        return Triple(portraitCandidates, landscapeCandidates, nonSupplyLandscapeCandidates)
    }

    suspend fun getCandidatesEvenAmounts(
        randomExpansions: List<Expansion>
    ): Triple<MutableSet<Card>, MutableSet<Card>, MutableSet<Card>> {
        val portraitCandidates = mutableSetOf<Card>()
        val landscapeCandidates = mutableSetOf<Card>()
        val nonSupplyLandscapeCandidates = mutableSetOf<Card>()

        Log.d("Kingdom Generator", "Getting candidates from expansions $randomExpansions")

        for (expansion in randomExpansions) {
            portraitCandidates.addAll(cardDao.getPortraitsByExpansion(expansion.id))
            landscapeCandidates.addAll(cardDao.getSupplyLandscapesByExpansion(expansion.id))
            nonSupplyLandscapeCandidates.addAll(cardDao.getSpecialLandscapesByExpansion(expansion.id))
        }
        return Triple(portraitCandidates, landscapeCandidates, nonSupplyLandscapeCandidates)
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
