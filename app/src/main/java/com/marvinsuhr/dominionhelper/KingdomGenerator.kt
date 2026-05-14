package com.marvinsuhr.dominionhelper

import android.util.Log
import com.marvinsuhr.dominionhelper.model.Card
import com.marvinsuhr.dominionhelper.data.CardDao
import com.marvinsuhr.dominionhelper.data.ExpansionDao
import com.marvinsuhr.dominionhelper.data.UserPrefsRepository
import com.marvinsuhr.dominionhelper.model.Expansion
import com.marvinsuhr.dominionhelper.model.Kingdom
import com.marvinsuhr.dominionhelper.model.Set
import com.marvinsuhr.dominionhelper.model.Type
import com.marvinsuhr.dominionhelper.ui.RandomMode
import com.marvinsuhr.dominionhelper.ui.VetoMode
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val predicateMapSettings: Map<String, (Card) -> Boolean> = mapOf(
    "Attack" to { it.types.contains(Type.ATTACK) },
    "Cost 2" to { it.cost == 2 }
)

val predicateMapLandscape: Map<String, (Card) -> Boolean> = mapOf(
    "Ally" to { it.types.contains(Type.ALLY) },
    "Prophecy" to { it.types.contains(Type.PROPHECY) }
)

@Singleton
class KingdomGenerator @Inject constructor(
    private val cardDao: CardDao,
    private val expansionDao: ExpansionDao,
    private val userPrefsRepository: UserPrefsRepository,
    private val cardDependencyResolver: CardDependencyResolver
) {

    suspend fun generateKingdom(): Kingdom {
        val randomMode = userPrefsRepository.randomMode.first()

        when (randomMode) {
            RandomMode.FULL_RANDOM -> {
                Log.i("Kingdom Generator", "Starting generation - Full random selected")
                return generateKingdomFullRandom()
            }

            RandomMode.EVEN_AMOUNTS -> {
                Log.i("Kingdom Generator", "Starting generation - Even amounts selected")
                return generateKingdomEvenAmounts()
            }
        }
    }

    private suspend fun generateKingdomFullRandom(): Kingdom {
        val cardList = mutableSetOf<Card>()
        val totalCardsToGenerate = userPrefsRepository.numberOfCardsToGenerate.first()

        val (cardPool, landscapePool) = getCandidatesFullRandom()
        Log.d("Kingdom Generator", "Portrait candidates: ${cardPool.size}")
        Log.d("Kingdom Generator", "Landscape candidates: ${landscapePool.size}")

        applyPortraitPredicates(cardPool, cardList, predicateMapSettings)

        // Fill remaining portrait slots
        val cardsLeft = totalCardsToGenerate - cardList.size
        if (cardsLeft > 0) { // Check if we need to add more
            val cardsToFill = cardPool.shuffled().take(cardsLeft)
            Log.d("Kingdom Generator", "Cards to fill: ${cardsToFill.joinToString { it.name }}")
            cardList.addAll(cardsToFill)
        }

        val landscapeList = generateLandscapeCards(landscapePool)

        // For FULL_RANDOM, we need to figure out which expansions the cards came from
        val expansionNames = cardList.mapNotNull { it.sets.firstOrNull()?.displayName }.distinct()
        val kingdomName = generateKingdomNameFromExpansionsList(expansionNames)
        Log.i("Kingdom Generator", "Generated kingdom name: $kingdomName from expansions: $expansionNames")

        // TODO: Check if it makes sense to just load ids instead of the whole card
        // Use cardDependencyResolver to build the full Kingdom with all dependent cards
        return cardDependencyResolver.addDependentCards(cardList, landscapeList, kingdomName)
    }

    private suspend fun generateKingdomEvenAmounts(): Kingdom {
        val cardList = mutableSetOf<Card>()
        val totalCardsToGenerate = userPrefsRepository.numberOfCardsToGenerate.first()

        val numberOfExpansions = userPrefsRepository.randomExpansionAmount.first()
        if (numberOfExpansions <= 0) {
            Log.w("Kingdom Generator", "Number of expansions was 0, may result in empty kingdom or fallback behavior.")
            // Consider returning an empty/error Kingdom or throwing an exception if this is invalid state
        }

        val randomExpansions = expansionDao.getFixedAmountOfOwnedExpansions(numberOfExpansions)
        if (randomExpansions.isEmpty() && numberOfExpansions > 0) {
            Log.w("Kingdom Generator", "Requested $numberOfExpansions expansions, but no owned expansions found/selected.")
            // Consider fallback or error
        }


        val (cardPool, landscapePoolCandidates) = getCandidatesEvenAmounts(randomExpansions)
        Log.d("Kingdom Generator", "Portrait candidates: ${cardPool.size}")
        Log.d("Kingdom Generator", "Landscape candidates: ${landscapePoolCandidates.size}")

        applyPortraitPredicates(cardPool, cardList, predicateMapSettings)

        // Fill remaining portrait slots according to even amounts logic
        fillPortraitsEvenly(
            totalCardsToGenerate,
            cardList,
            cardPool, // This should be the remaining candidates after predicates
            randomExpansions
        )

        val landscapeList = generateLandscapeCards(landscapePoolCandidates)

        // Generate kingdom name from selected expansions
        val kingdomName = generateKingdomNameFromExpansions(randomExpansions)
        Log.i("Kingdom Generator", "Generated kingdom name: $kingdomName from ${randomExpansions.size} expansions")

        // TODO: Check if it makes sense to just load ids instead of the whole card/
        // Use cardDependencyResolver to build the full Kingdom with all dependent cards
        return cardDependencyResolver.addDependentCards(cardList, landscapeList, kingdomName)
    }

    private fun generateKingdomNameFromExpansions(expansions: List<Expansion>): String {
        val expansionNames = expansions.map { it.name }
        return generateKingdomNameFromExpansionsList(expansionNames)
    }

    private fun generateKingdomNameFromExpansionsList(expansionNames: List<String>): String {
        return when {
            expansionNames.isEmpty() -> "Unnamed Kingdom" // TODO error?
            expansionNames.size == 1 -> expansionNames[0]
            expansionNames.size == 2 -> "${expansionNames[0]}, ${expansionNames[1]}"
            else -> "${expansionNames[0]}, ${expansionNames[1]}, +${expansionNames.size - 2}"
        }
    }

    private fun applyPortraitPredicates(
        cardPool: MutableSet<Card>,
        cardList: MutableSet<Card>,
        predicates: Map<String, (Card) -> Boolean>
    ) {
        // TODO: For each predicate, check if it is enabled
        for ((predicateName, predicate) in predicates) {
            val success = satisfyRequirement(predicateName, cardPool, cardList, predicate)
            if (!success) {
                Log.w("KingdomGenerator", "Failed to add required $predicateName card.")
            }
        }
    }

    private fun fillPortraitsEvenly(
        totalCardsToGenerate: Int,
        cardList: MutableSet<Card>, // cards already selected by predicates
        initialCardPool: MutableSet<Card>, // remaining candidates after predicates
        randomExpansions: List<Expansion>
    ) {
        val cardsStillToSelectGlobally = totalCardsToGenerate - cardList.size

        if (cardsStillToSelectGlobally <= 0) {
            Log.d("Kingdom Generator", "All portrait card slots already filled by predicates.")
            return
        }

        if (randomExpansions.isEmpty()) {
            Log.w("Kingdom Generator", "Even amounts: No expansions to draw from. Filling remaining $cardsStillToSelectGlobally slots randomly from available pool.")
            val cardsToFill = initialCardPool.shuffled().take(cardsStillToSelectGlobally)
            cardList.addAll(cardsToFill)
            if (cardList.size < totalCardsToGenerate){
                Log.w("Kingdom Generator", "Could not fill all $totalCardsToGenerate cards with random fallback. Only found ${cardList.size - (totalCardsToGenerate - cardsStillToSelectGlobally)} additional cards.")
            }
            return
        }

        val targetTotalCardsPerExpansion = (totalCardsToGenerate.toDouble() / randomExpansions.size).roundToInt()
        var actualCardsSelectedInThisPhase = 0
        val mutableCardPool = initialCardPool.toMutableSet() // Work with a copy

        Log.d("Kingdom Generator", "Targeting $targetTotalCardsPerExpansion total cards per expansion for the remaining $cardsStillToSelectGlobally slots.")

        for (expansion in randomExpansions) {
            if (actualCardsSelectedInThisPhase >= cardsStillToSelectGlobally) break

            val cardsAlreadySelectedFromThisExpansion = cardList.count { card ->
                card.sets.any { set -> set.name == expansion.id }
            }
            var cardsNeededFromThisExpansion = targetTotalCardsPerExpansion - cardsAlreadySelectedFromThisExpansion
            cardsNeededFromThisExpansion =
                max(0, cardsNeededFromThisExpansion)
            cardsNeededFromThisExpansion =
                min(cardsNeededFromThisExpansion, cardsStillToSelectGlobally - actualCardsSelectedInThisPhase)

            if (cardsNeededFromThisExpansion <= 0) {
                Log.d("Kingdom Generator", "Expansion ${expansion.name} already has enough cards (${cardsAlreadySelectedFromThisExpansion} / $targetTotalCardsPerExpansion) or no more cards needed globally for this phase.")
                continue
            }

            val candidateCardsFromThisExpansion = mutableCardPool
                .filter { card -> card.sets.any { set -> set.name == expansion.id } }
                .shuffled() // Shuffle before taking

            val takeAmount = min(candidateCardsFromThisExpansion.size, cardsNeededFromThisExpansion)

            if (takeAmount > 0) {
                val selectedFromExpansion = candidateCardsFromThisExpansion.take(takeAmount)
                Log.d("Kingdom Generator", "Selected an additional ${selectedFromExpansion.size} cards from ${expansion.name} (already had $cardsAlreadySelectedFromThisExpansion, needed $cardsNeededFromThisExpansion, took $takeAmount): ${selectedFromExpansion.joinToString { it.name }}")
                cardList.addAll(selectedFromExpansion)
                mutableCardPool.removeAll(selectedFromExpansion.toSet())
                actualCardsSelectedInThisPhase += selectedFromExpansion.size
            } else {
                Log.d("Kingdom Generator", "No additional cards to select from ${expansion.name} (needed $cardsNeededFromThisExpansion, found ${candidateCardsFromThisExpansion.size}).")
            }
        }

        val finalRemainingToDistribute = cardsStillToSelectGlobally - actualCardsSelectedInThisPhase
        if (finalRemainingToDistribute > 0 && mutableCardPool.isNotEmpty()) {
            Log.d("Kingdom Generator", "Even distribution phase completed. Filling remaining $finalRemainingToDistribute slots from the general pool of ${mutableCardPool.size} available cards.")
            val fillFromTheRest = mutableCardPool.shuffled().take(finalRemainingToDistribute)
            Log.d("Kingdom Generator", "Filling with: ${fillFromTheRest.joinToString { it.name }}")
            cardList.addAll(fillFromTheRest)
            actualCardsSelectedInThisPhase += fillFromTheRest.size
        }

        if (totalCardsToGenerate - cardList.size > 0) { // Check final count against original total
            val stillMissing = totalCardsToGenerate - cardList.size
            Log.w("Kingdom Generator", "Could not select all $totalCardsToGenerate cards. Still need $stillMissing. This might happen if expansions have too few cards or pool was exhausted.")
        }
    }

    private fun generateLandscapeCards(
        landscapePoolCandidates: MutableSet<Card>
    ): MutableSet<Card> {
        val landscapeList = mutableSetOf<Card>()
        val totalLandscapesToGenerate = 2 // Assuming this is fixed or from userPrefs

        // TODO: For each predicate in landscapePredicateMap, check if it is enabled
        for ((predicateName, predicate) in predicateMapLandscape) {
            val success = satisfyRequirement(predicateName, landscapePoolCandidates, landscapeList, predicate)
            if (!success) {
                Log.w("KingdomGenerator", "Failed to add required $predicateName landscape card.")
            }
        }

        val landscapesLeft = totalLandscapesToGenerate - landscapeList.size
        if (landscapesLeft > 0) { // Check if we need to add more
            val landscapesToFill = landscapePoolCandidates
                .filter { card -> card.types.none { type -> type == Type.ALLY || type == Type.PROPHECY } || landscapeList.any { selected -> selected.name == card.name } } // Allow re-selection if it was already chosen by predicate
                .shuffled()
                .distinctBy { it.name } // Ensure we don't add the same landscape twice in this step if it wasn't a predicate
                .take(landscapesLeft)

            if (landscapesToFill.isEmpty() && landscapesLeft > 0) {
                Log.w("KingdomGenerator", "Could not find $landscapesLeft unique, non-dependent landscapes to fill remaining slots. Pool size: ${landscapePoolCandidates.size}")
            } else {
                Log.d("Kingdom Generator", "Landscapes to fill: ${landscapesToFill.joinToString { it.name }}")
            }
            landscapeList.addAll(landscapesToFill)
        }
        // TODO: Generate 2 *different* landscapes if possible and desired. Current logic might pick same one if predicates forced one type.
        // The distinctBy helps but if predicate already picked 'Ally X', it could pick 'Ally X' again if it's the only one left.
        // This might require more complex logic if strictly two different named landscapes are required.
        // For now, it will ensure at most 'totalLandscapesToGenerate' are in the list.
        return landscapeList
    }

    private suspend fun getCandidatesFullRandom(): Pair<MutableSet<Card>, MutableSet<Card>> {

        val portraitCandidates = mutableSetOf<Card>()
        val landscapeCandidates = mutableSetOf<Card>()

        // Gets all owned cards
        portraitCandidates.addAll(cardDao.getEnabledOwnedCards())
        landscapeCandidates.addAll(cardDao.getEnabledOwnedLandscapes())
        return Pair(portraitCandidates, landscapeCandidates)
    }

    suspend fun getCandidatesEvenAmounts(
        randomExpansions: List<Expansion>
    ): Pair<MutableSet<Card>, MutableSet<Card>> {

        val portraitCandidates = mutableSetOf<Card>()
        val landscapeCandidates = mutableSetOf<Card>()

        if (randomExpansions.isEmpty()) {
            Log.w(
                "KingdomGenerator",
                "Even amounts selected, but no owned expansions found or selected."
            )
            // Error?
        }

        for (expansion in randomExpansions) {
            portraitCandidates.addAll(
                cardDao.getPortraitsByExpansion(expansion.id)
            )
            landscapeCandidates.addAll(
                cardDao.getLandscapesByExpansion(expansion.id)
            )
        }

        return Pair(portraitCandidates, landscapeCandidates)
    }

    // Helper function to satisfy a requirement
    // Returns true if a card was successfully added, false otherwise
    private fun satisfyRequirement(
        predicateName: String,
        cardPool: MutableSet<Card>,
        cardList: MutableSet<Card>,
        predicate: (Card) -> Boolean
    ): Boolean {
        val candidates = cardPool.filter(predicate).shuffled()
        Log.d(
            "Kingdom Generator",
            "$predicateName candidates: ${candidates.joinToString { it.name }}"
        )

        if (candidates.isEmpty()) {
            Log.e("Kingdom Generator", "No $predicateName cards found in card pool!")
            return false
        } else {
            val cardToAdd = candidates.random()
            Log.d("Kingdom Generator", "Selected $predicateName card: ${cardToAdd.name}")
            cardList.add(cardToAdd)
            cardPool.remove(cardToAdd)
            return true
        }
    }

    //---------

    // Pass List here?
    // TODO: Review this

    suspend fun replaceCardInKingdom(
        cardToRemove: Card,
        cardsToExclude: kotlin.collections.Set<Card>
    ): Card? {

        val isLandscape = cardToRemove.landscape
        val newCard: Card? = when (userPrefsRepository.vetoMode.first()) {

            // Reroll from the same expansion
            VetoMode.REROLL_SAME -> {
                Log.i("KingdomGenerator", "Rerolling from the same expansion.")
                generateSingleRandomCardFromExpansion(
                    cardToRemove.sets,
                    cardsToExclude,
                    isLandscape
                )
            }

            // Reroll from any owned expansions
            // TODO: This rerolls from any OWNED expansion, but we need to reroll from any SELECTED expansion probably
            VetoMode.REROLL_ANY -> {
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
        excludeCards: kotlin.collections.Set<Card> = emptySet(),
        isLandscape: Boolean
    ): Card? {
        val excludedCardIds = excludeCards.map { it.id }.toSet()

        Log.i("Kingdom Generator", "Generating random card from owned Expansions")
        return cardDao.getSingleCardFromOwnedExpansionsWithExceptions(
            excludedCardIds,
            isLandscape
        )
    }

    suspend fun generateSingleRandomCardFromExpansion(
        sets: List<Set>,
        excludeCards: kotlin.collections.Set<Card> = emptySet(),
        isLandscape: Boolean
    ): Card? {
        val excludedCardIds = excludeCards.map { it.id }.toSet()

        val setName1: String? = sets.getOrNull(0)?.name
        val setName2: String? = sets.getOrNull(1)?.name

        return if (setName1 != null) {
            val logMessage = "Generating random card from $setName1" +
                    (if (setName2 != null) " and $setName2" else "")
            Log.i("Kingdom Generator", logMessage)

            cardDao.getSingleCardFromExpansionWithExceptions(
                setName1,
                setName2,
                excludedCardIds,
                isLandscape
            )
        } else {
            // No sets provided (sets list was empty), or set names were null
            Log.w("Kingdom Generator", "Cannot generate card: No valid sets provided.")
            null
        }
    }
}