package dev.msuhr.dominionkingdoms

import android.util.Log
import com.google.common.primitives.Doubles.min
import dev.msuhr.dominionkingdoms.data.CardDao
import dev.msuhr.dominionkingdoms.data.UserPrefsRepository
import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.model.CardNames
import dev.msuhr.dominionkingdoms.model.Category
import dev.msuhr.dominionkingdoms.model.Kingdom
import dev.msuhr.dominionkingdoms.model.Type
import dev.msuhr.dominionkingdoms.ui.DarkAgesMode
import dev.msuhr.dominionkingdoms.ui.ProsperityMode
import dev.msuhr.dominionkingdoms.utils.isPercentChance
import dev.msuhr.dominionkingdoms.utils.listToMap
import dev.msuhr.dominionkingdoms.model.Set as CardSet
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardDependencyResolver @Inject constructor(
    private val cardDao: CardDao,
    private val userPrefsRepository: UserPrefsRepository
) {

    suspend fun addDependentCards(
        randomCardsSet: kotlin.collections.Set<Card>,
        landscapeCardsSet: kotlin.collections.Set<Card>,
        kingdomName: String? = null
    ): Kingdom {

        val basicCards = loadCards(CardNames.BASIC_CARDS.associateWith { 1 })

        val dependentCardsToLoad = getDependentCards(randomCardsSet)
        val dependentCards = loadCards(dependentCardsToLoad)

        val startingCardsToLoad = getStartingCards(randomCardsSet)
        val startingCards = loadCards(startingCardsToLoad)

        val randomCardsMap = listToMap(randomCardsSet.toList())
        val landscapeCardsMap = listToMap(landscapeCardsSet.toList())

        Log.i(
            "Card Dependency Resolver",
            "Generated ${randomCardsMap.size} random cards, ${basicCards.size} basic cards, ${dependentCards.size} dependent cards, ${startingCards.size} starting cards, and ${landscapeCardsMap.size} landscape cards."
        )
        return Kingdom(
            randomCardsMap,
            basicCards,
            dependentCards,
            startingCards,
            landscapeCardsMap,
            name = kingdomName ?: "Unnamed Kingdom"
        )
    }

    private suspend fun getDependentCards(cards: kotlin.collections.Set<Card>): LinkedHashMap<String, Int> {

        // Kingdom Signature - these are all the attributes we check for later
        val namesInKingdom = cards.map { it.name }.toSet()
        val typesInKingdom = cards.flatMap { it.types }.toSet()
        val categoriesInKingdom = cards.flatMap { it.categories }.toSet()
        val setsInKingdom = cards.flatMap { it.sets }.toSet()

        val dependentCardNames = mutableSetOf<String>()

        // 1. Process Type-based triggers
        CardDependencies.typeTriggers.forEach { (type, deps) ->
            if (typesInKingdom.contains(type)) dependentCardNames.addAll(deps)
        }

        // 2. Process Category-based triggers
        CardDependencies.categoryTriggers.forEach { (category, deps) ->
            if (categoriesInKingdom.contains(category)) dependentCardNames.addAll(deps)
        }

        // 3. Process Name-based triggers
        CardDependencies.nameTriggers.forEach { (name, deps) ->
            if (namesInKingdom.contains(name)) dependentCardNames.addAll(deps)
        }

        // 4. Potion
        if (cards.any { it.potion }) {
            dependentCardNames.add(CardNames.POTION)
        }

        // 5. Process List-based triggers (Loot, Spoils, etc.)
        // TODO why not make nameTriggers and namesInKingdom?
        if (namesInKingdom.any { it in CardNames.lootProviders }) {
            dependentCardNames.add(CardNames.LOOT_PILE)
        }

        if (namesInKingdom.any { it in CardNames.horseCards }) {
            dependentCardNames.add(CardNames.HORSE)
        }

        if (namesInKingdom.any { it in listOf(CardNames.BANDIT_CAMP, CardNames.MARAUDER, CardNames.PILLAGE) }) {
            dependentCardNames.add(CardNames.SPOILS)
        }

        if (namesInKingdom.any { it in CardNames.CoffersCards }) {
            dependentCardNames.add(CardNames.COFFERS_MAT)
        }

        if (namesInKingdom.any { it in (CardNames.VillagersCards) }) {
            dependentCardNames.add(CardNames.VILLAGERS_MAT)
        }

        if (namesInKingdom.any { it in CardNames.AltVPCards }) {
            dependentCardNames.addAll(listOf(CardNames.VICTORY_TOKEN_MAT, CardNames.VICTORY_TOKENS))
        }

        if (namesInKingdom.any { it in CardNames.CoinCards }) {
            dependentCardNames.add(CardNames.COIN_TOKENS)
        }

        if (namesInKingdom.any { it in CardNames.AdventureTokenCards }) {
            dependentCardNames.add(CardNames.ADVENTURES_TOKENS)
        }

        if (namesInKingdom.any { it in CardNames.DebtCards }) {
            dependentCardNames.add(CardNames.DEBT_TOKENS)
        }

        dependentCardNames.addAll(checkProsperityBasicCards(cards))

        val dependentCardMap = LinkedHashMap<String, Int>()
        dependentCardNames.forEach { cardName ->
            dependentCardMap[cardName] = 1 // Default 1
        }

        return dependentCardMap
    }

    private suspend fun checkProsperityBasicCards(randomCards: Set<Card>): List<String> {

        val prosperityCardsToAdd = mutableListOf<String>()
        val prosperityMode = userPrefsRepository.prosperityBasicCardsMode.first()

        val prosperityCount = randomCards.count {
            it.sets.contains(CardSet.PROSPERITY_1E) || it.sets.contains(CardSet.PROSPERITY_2E)
        }

        when (prosperityMode) {

            // Don't add in any case
            ProsperityMode.NEVER -> {
                return emptyList()
            }

            // 10% chance per prosperity card
            ProsperityMode.TEN_PERCENT_PER_CARD -> {
                if (prosperityCount > 0) {
                    if (isPercentChance(min(prosperityCount * 10.0, 100.0))) {
                        Log.i(
                            "KingdomGenerator",
                            "Adding Platinum and Colony - 10% per card ($prosperityCount) triggered"
                        )
                        prosperityCardsToAdd.add("Platinum")
                        prosperityCardsToAdd.add("Colony")
                    }
                }
            }

            // Always add Platinum and Colony if at least one Prosperity card is in the 10 random kingdom cards
            ProsperityMode.IF_PRESENT -> {
                if (prosperityCount > 0) {
                    Log.i(
                        "KingdomGenerator",
                        "Adding Platinum and Colony (Prosperity card present in Kingdom rule)"
                    )
                    prosperityCardsToAdd.add("Platinum")
                    prosperityCardsToAdd.add("Colony")
                }
            }
        }

        return prosperityCardsToAdd
    }

    private suspend fun getStartingCards(randomCards: Set<Card>): Map<String, Int> {

        val cards = mutableMapOf<String, Int>()
        val darkAgesMode = userPrefsRepository.darkAgesStarterCardsMode.first()
        val darkAgesCount = randomCards.count { it.sets.contains(CardSet.DARK_AGES) }

        when (darkAgesMode) {

            // Don't add in any case
            DarkAgesMode.NEVER -> {
                cards["Estate"] = 3
            }

            // 10% per Dark Ages card to use Shelters instead of Estates
            DarkAgesMode.TEN_PERCENT_PER_CARD -> {
                if (isPercentChance(min(darkAgesCount * 10.0, 100.0))) {
                    Log.i("KingdomGenerator", "Adding Shelters - 10% per card ($darkAgesCount) triggered")
                    cards["Overgrown Estate"] = 1
                    cards["Hovel"] = 1
                    cards["Necropolis"] = 1
                } else {
                    cards["Estate"] = 3
                }
            }

            // Always add Shelters if at least one Dark Ages card is in the 10 random kingdom cards
            DarkAgesMode.IF_PRESENT -> {
                if (darkAgesCount > 0) {
                    Log.i(
                        "KingdomGenerator",
                        "Adding Shelters because Dark Ages cards are present"
                    )
                    cards["Overgrown Estate"] = 1
                    cards["Hovel"] = 1
                    cards["Necropolis"] = 1
                } else {
                    cards["Estate"] = 3
                }
            }
        }

        // Add Heirlooms
        var heirloomCount = 0
        CardNames.heirloomPairs.forEach { (cardName, dependentCardName) ->
            if (randomCards.any { it.name == cardName }) {
                Log.d("Kingdom Generator", "Adding Heirloom: $cardName")
                cards[dependentCardName] = 1
                heirloomCount++
            }
        }

        // 1 less Copper per Heirloom
        cards["Copper"] = 7 - heirloomCount
        return cards
    }

    // Review
    suspend fun loadCards(cardsToLoad: Map<String, Int>): LinkedHashMap<Card, Int> {

        val cardNames = cardsToLoad.keys.toList()
        val loadedCardsList = cardDao.getCardsByNameList(cardNames)

        if (loadedCardsList.size != cardNames.size) {
            val missingNames =
                cardNames.filterNot { name -> loadedCardsList.any { it.name == name } }
            Log.e(
                "KingdomGenerator",
                "Critical error: Not all cards found in DB! Missing: $missingNames"
            )
            throw IllegalStateException("Failed to load some cards. Missing: $missingNames")
        }

        // Reconstruct the map with Card objects as keys and original Int values
        val result = LinkedHashMap<Card, Int>()
        loadedCardsList.forEach { card ->
            // Find the original amount from the input map.
            // The !! is safe here because of the size check above, ensuring card.name was in cardsToLoad.keys
            // Hmm
            result[card] = cardsToLoad[card.name]!!
        }
        return result
    }
}

object CardDependencies {

    val typeTriggers = mapOf(
        Type.FATE to listOf(CardNames.BOON_PILE, CardNames.WILL_O_WISP),
        Type.DOOM to listOf(
            CardNames.HEX_PILE,
            CardNames.CURSE,
            CardNames.DELUDED,
            CardNames.ENVIOUS,
            CardNames.MISERABLE,
            CardNames.TWICE_MISERABLE
        ),
        Type.LOOTER to listOf(CardNames.RUINS_PILE),
        Type.RESERVE to listOf(CardNames.TAVERN_MAT),
        Type.LIAISON to listOf(CardNames.FAVORS_MAT),
        Type.PROJECT to listOf(CardNames.WOODEN_CUBES),
        Type.OMEN to listOf(CardNames.SUN_TOKENS)
    )

    val categoryTriggers = mapOf(
        Category.CURSER to listOf(CardNames.CURSE),
        Category.TRASHER to listOf(CardNames.TRASH_MAT),
        Category.TRASH_FOR_BENEFIT to listOf(CardNames.TRASH_MAT),
        Category.EXILE to listOf(CardNames.EXILE_MAT)
    )

    val nameTriggers = mapOf(
        CardNames.TOURNAMENT to listOf(CardNames.PRIZE_PILE),
        CardNames.JOUST to listOf(CardNames.REWARD_PILE),
        CardNames.BORDER_GUARD to listOf(CardNames.LANTERN, CardNames.HORN),
        CardNames.FLAG_BEARER to listOf(CardNames.FLAG),
        CardNames.SWASHBUCKLER to listOf(CardNames.TREASURE_CHEST),
        CardNames.TREASURER to listOf(CardNames.KEY),
        CardNames.PAGE to listOf(
            CardNames.TREASURE_HUNTER,
            CardNames.WARRIOR,
            CardNames.HERO,
            CardNames.CHAMPION
        ),
        CardNames.PEASANT to listOf(
            CardNames.SOLDIER,
            CardNames.FUGITIVE,
            CardNames.DISCIPLE,
            CardNames.TEACHER
        ),
        CardNames.EXORCIST to listOf(CardNames.WILL_O_WISP, CardNames.IMP, CardNames.GHOST),
        CardNames.FOOL to listOf(CardNames.LOST_IN_THE_WOODS),
        CardNames.NECROMANCER to listOf(
            CardNames.ZOMBIE_APPRENTICE,
            CardNames.ZOMBIE_MASON,
            CardNames.ZOMBIE_SPY
        ),
        CardNames.VAMPIRE to listOf(CardNames.BAT),
        CardNames.SECRET_CAVE to listOf(CardNames.WISH),
        CardNames.LEPRECHAUN to listOf(CardNames.WISH),
        CardNames.HERMIT to listOf(CardNames.MADMAN),
        CardNames.URCHIN to listOf(CardNames.MERCENARY),
        CardNames.DEVILS_WORKSHOP to listOf(CardNames.IMP),
        CardNames.TORMENTOR to listOf(CardNames.IMP),
        CardNames.ISLAND to listOf(CardNames.ISLAND_MAT),
        CardNames.PIRATE_SHIP to listOf(CardNames.PIRATE_SHIP_MAT),
        CardNames.NATIVE_VILLAGE to listOf(CardNames.NATIVE_VILLAGE_MAT),
        CardNames.TRADE_ROUTE to listOf(CardNames.TRADE_ROUTE_MAT),
        CardNames.EMBARGO to listOf(CardNames.EMBARGO_TOKENS)
    )
}
