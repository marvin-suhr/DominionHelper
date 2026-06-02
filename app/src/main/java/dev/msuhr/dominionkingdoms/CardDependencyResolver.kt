package dev.msuhr.dominionkingdoms

import android.util.Log
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
import dev.msuhr.dominionkingdoms.model.Set
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

        // TODO: If a trait is present, choose a random card

        val dependencyRules = CardDependencies().dependencyRules
        val dependentCardNames = mutableSetOf<String>()

        // TODO Efficiency: When a dependencyRule is met, the other ones are still checked.
        // We should not check further rules when one is found, as this is just a waste of resources.
        // We can change this by using any().
        // -> Is this true?
        dependencyRules.forEach { rule ->
            cards.forEach { card ->
                if (rule.condition(card)) {
                    dependentCardNames.addAll(rule.dependentCardNames)
                }
            }
        }

        dependentCardNames.addAll(checkProsperityBasicCards(cards))

        val dependentCardMap = LinkedHashMap<String, Int>()
        dependentCardNames.forEach { cardName ->
            dependentCardMap[cardName] = 1 // Default 1
        }

        return dependentCardMap
    }

    private suspend fun checkProsperityBasicCards(randomCards: kotlin.collections.Set<Card>): List<String> {

        val prosperityCardsToAdd = mutableListOf<String>()
        val prosperityMode = userPrefsRepository.prosperityBasicCardsMode.first()

        val prosperityCount = randomCards.count {
            it.sets.contains(Set.PROSPERITY_1E) || it.sets.contains(Set.PROSPERITY_2E)
        }

        when (prosperityMode) {

            // Don't add in any case
            ProsperityMode.NEVER -> {
                return emptyList()
            }

            // 10% chance per prosperity card
            ProsperityMode.TEN_PERCENT_PER_CARD -> {
                if (prosperityCount > 0) {
                    if (isPercentChance(prosperityCount * 10.0)) {
                        Log.i(
                            "KingdomGenerator",
                            "Adding Platinum and Colony - 10% per card ($prosperityCount)"
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

    private suspend fun getStartingCards(randomCards: kotlin.collections.Set<Card>): Map<String, Int> {

        val cards = mutableMapOf<String, Int>()
        val darkAgesMode = userPrefsRepository.darkAgesStarterCardsMode.first()
        val darkAgesCount = randomCards.count { it.sets.contains(Set.DARK_AGES) }

        when (darkAgesMode) {

            // Don't add in any case
            DarkAgesMode.NEVER -> {
                cards["Estate"] = 3
            }

            // 10% per Dark Ages card to use Shelters instead of Estates
            DarkAgesMode.TEN_PERCENT_PER_CARD -> {
                if (isPercentChance(darkAgesCount * 10.0)) {
                    Log.i("KingdomGenerator", "Adding Shelters - 10% per card ($darkAgesCount)")
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

class CardDependencies {

    // Data class to represent a dependency rule
    data class DependencyRule(
        val condition: (Card) -> Boolean,
        val dependentCardNames: List<String>
    )

    val dependencyRules = listOf(

        // TODO Schwierig: Ferryman, Young Witch, Black Market, Riverboat, Approaching Army, Divine Wind, Inherited, Way of the Mouse
        // -> Data driven? Store dependencies in db?

        // If there is a Curser present, add Curse card
        DependencyRule(
            condition = { it.categories.contains(Category.CURSER) },
            dependentCardNames = listOf(CardNames.CURSE)
        ),

        // If there is an Alchemy card present, add Potion
        DependencyRule(
            condition = { it.sets.contains(Set.ALCHEMY) },
            dependentCardNames = listOf(CardNames.POTION)
        ),

        // If there is a Fate card present, add all Boons
        DependencyRule(
            condition = { it.types.contains(Type.FATE) },
            dependentCardNames = listOf(CardNames.BOON_PILE, CardNames.WILL_O_WISP)
        ),

        // If there is a Doom card present, add all Hexes and corresponding States
        DependencyRule(
            condition = { it.types.contains(Type.DOOM) },
            dependentCardNames = listOf(
                CardNames.HEX_PILE,
                CardNames.CURSE,
                CardNames.DELUDED,
                CardNames.ENVIOUS,
                CardNames.MISERABLE,
                CardNames.TWICE_MISERABLE // -> State Pile
            )
        ),

        // If there is a card present that rewards loot, add all Loots
        DependencyRule(
            condition = { card ->
                listOf(
                    CardNames.JEWELLED_EGG,
                    CardNames.PERIL,
                    CardNames.SEARCH,
                    CardNames.FORAY,
                    CardNames.PICKAXE,
                    CardNames.WEALTHY_VILLAGE,
                    CardNames.CUTTHROAT,
                    CardNames.LOOTING,
                    CardNames.SACK_OF_LOOT,
                    CardNames.INVASION,
                    CardNames.PROSPER,
                    CardNames.CURSED
                ).contains(card.name)
            },
            dependentCardNames = listOf(CardNames.LOOT_PILE)
        ),

        // If there is a Looter card present, add Ruins cards
        DependencyRule(
            condition = { it.types.contains(Type.LOOTER) },
            dependentCardNames = listOf(CardNames.RUINS_PILE)
        ),

        // Tournament -> add Prizes
        DependencyRule(
            condition = { it.name == CardNames.TOURNAMENT },
            dependentCardNames = listOf(CardNames.PRIZE_PILE)
        ),
        // Joust -> Add Rewards
        DependencyRule(
            condition = { it.name == CardNames.JOUST },
            dependentCardNames = listOf(CardNames.REWARD_PILE)
        ),

        // If there is a Bandit Camp, Marauder or Pillage card present, add Spoils cards
        DependencyRule(
            condition = { card ->
                listOf(
                    CardNames.BANDIT_CAMP, CardNames.MARAUDER, CardNames.PILLAGE
                ).contains(card.name)
            },
            dependentCardNames = listOf(CardNames.SPOILS)
        ),

        // ARTIFACTS
        // If there is Border Guard present, add Lantern and Horn
        DependencyRule(
            condition = { it.name == CardNames.BORDER_GUARD },
            dependentCardNames = listOf(CardNames.LANTERN, CardNames.HORN)
        ),
        // If there is Flag Bearer present, add Flag
        DependencyRule(
            condition = { it.name == CardNames.FLAG_BEARER },
            dependentCardNames = listOf(CardNames.FLAG)
        ),
        // If there is Swashbuckler present, add Treasure Chest
        DependencyRule(
            condition = { it.name == CardNames.SWASHBUCKLER },
            dependentCardNames = listOf(CardNames.TREASURE_CHEST)
        ),
        // If there is Treasurer present, add Key
        DependencyRule(
            condition = { it.name == CardNames.TREASURER },
            dependentCardNames = listOf(CardNames.KEY)
        ),

        // Travellers
        DependencyRule(
            condition = { it.name == CardNames.PAGE },
            dependentCardNames = listOf(
                CardNames.TREASURE_HUNTER,
                CardNames.WARRIOR,
                CardNames.HERO,
                CardNames.CHAMPION
            )
        ),
        DependencyRule(
            condition = { it.name == CardNames.PEASANT },
            dependentCardNames = listOf(
                CardNames.SOLDIER,
                CardNames.FUGITIVE,
                CardNames.DISCIPLE,
                CardNames.TEACHER
            )
        ),

        // Spirits
        DependencyRule(
            condition = { it.name == CardNames.EXORCIST },
            dependentCardNames = listOf(
                CardNames.WILL_O_WISP,
                CardNames.IMP,
                CardNames.GHOST
            )
        ),

        // Horse
        DependencyRule(
            condition = { card ->
                listOf(
                    CardNames.SLEIGH,
                    CardNames.SUPPLIES,
                    CardNames.SCRAP,
                    CardNames.CAVALRY,
                    CardNames.GROOM,
                    CardNames.HOSTELRY,
                    CardNames.LIVERY,
                    CardNames.PADDOCK,
                    CardNames.RIDE,
                    CardNames.BARGAIN,
                    CardNames.DEMAND,
                    CardNames.STAMPEDE
                ).contains(card.name)
            },
            dependentCardNames = listOf(CardNames.HORSE)
        ),

        // Specific card interactions
        DependencyRule(
            condition = { it.name == CardNames.FOOL },
            dependentCardNames = listOf(CardNames.LOST_IN_THE_WOODS)
        ),
        DependencyRule(
            condition = { it.name == CardNames.NECROMANCER },
            dependentCardNames = listOf(
                CardNames.ZOMBIE_APPRENTICE,
                CardNames.ZOMBIE_MASON,
                CardNames.ZOMBIE_SPY
            )
        ),
        DependencyRule(
            condition = { it.name == CardNames.VAMPIRE },
            dependentCardNames = listOf(CardNames.BAT)
        ),
        DependencyRule(
            condition = { it.name == CardNames.SECRET_CAVE || it.name == CardNames.LEPRECHAUN },
            dependentCardNames = listOf(CardNames.WISH)
        ),
        DependencyRule(
            condition = { it.name == CardNames.HERMIT },
            dependentCardNames = listOf(CardNames.MADMAN)
        ),
        DependencyRule(
            condition = { it.name == CardNames.URCHIN },
            dependentCardNames = listOf(CardNames.MERCENARY)
        ),
        DependencyRule(
            condition = { it.name == CardNames.DEVILS_WORKSHOP || it.name == CardNames.TORMENTOR },
            dependentCardNames = listOf(CardNames.IMP)
        ),

        //////////
        // MATS //
        //////////

        // If there is a trasher present, add Trash Mat
        DependencyRule(
            condition = {
                it.categories.contains(Category.TRASHER) || it.categories.contains(Category.TRASH_FOR_BENEFIT)
            },
            dependentCardNames = listOf(CardNames.TRASH_MAT) // Trash card??
        ),

        // If Island is present, add Island Mat
        DependencyRule(
            condition = {
                it.name == CardNames.ISLAND
            },
            dependentCardNames = listOf(CardNames.ISLAND_MAT)
        ),

        // If Pirate Ship is present, add Pirate Ship Mat
        DependencyRule(
            condition = {
                it.name == CardNames.PIRATE_SHIP
            },
            dependentCardNames = listOf(CardNames.PIRATE_SHIP_MAT)
        ),

        // If Native Village is present, add Native Village Mat
        DependencyRule(
            condition = {
                it.name == CardNames.NATIVE_VILLAGE
            },
            dependentCardNames = listOf(CardNames.NATIVE_VILLAGE_MAT)
        ),

        // If Trade Route is present, add Trade Route Mat
        DependencyRule(
            condition = {
                it.name == CardNames.TRADE_ROUTE
            },
            dependentCardNames = listOf(CardNames.TRADE_ROUTE_MAT)
        ),

        // VP Tokens
        DependencyRule(
            condition = {
                it.categories.contains(Category.TRASHER) || it.categories.contains(Category.TRASH_FOR_BENEFIT)
            },
            dependentCardNames = listOf(CardNames.TRASH_MAT)
        ),

        // If any Reserve card is present, add Tavern Mat
        DependencyRule(
            condition = {
                it.types.contains(Type.RESERVE)
            },
            dependentCardNames = listOf(CardNames.TAVERN_MAT)
        ),

        // If any coffers card from Guilds is present, add Coffers Mat
        DependencyRule(
            condition = { card ->
                listOf(
                    CardNames.BAKER,
                    CardNames.BUTCHER,
                    CardNames.CANDLESTICK_MAKER,
                    CardNames.FOOTPAD,
                    CardNames.JOUST, // -> Huge Turnip (lazy)
                    CardNames.MERCHANT_GUILD,
                    CardNames.PLAZA
                ).contains(card.name)
            },
            dependentCardNames = listOf(CardNames.COFFERS_MAT)
        ),

        // TODO ^v if category = ... and sets contains ...

        // If any coffers card from Renaissance or any villagers card is present, add Coffers / Villagers Mat
        DependencyRule(
            condition = { card ->
                listOf(
                    CardNames.DUCAT,
                    CardNames.PATRON,
                    CardNames.SILK_MERCHANT,
                    CardNames.SPICES,
                    CardNames.SWASHBUCKLER,
                    CardNames.VILLAIN,
                    CardNames.EXPLORATION,
                    CardNames.GUILDHALL,
                    CardNames.PAGEANT,
                    CardNames.ACTING_TROUPE,
                    CardNames.LACKEYS,
                    CardNames.RECRUITER,
                    CardNames.SCULPTOR,
                    CardNames.ACADEMY
                ).contains(card.name)
            },
            dependentCardNames = listOf(CardNames.COFFERS_VILLAGERS_MAT)
        ),

        // If any exile card is present, add Exile Mat
        DependencyRule(
            condition = {
                it.categories.contains(Category.EXILE)
            },
            dependentCardNames = listOf(CardNames.EXILE_MAT)
        ),

        // If any Liaison card is present, add Favors Mat
        DependencyRule(
            condition = {
                it.types.contains(Type.LIAISON)
            },
            dependentCardNames = listOf(CardNames.FAVORS_MAT)
        ),

        ////////////
        // TOKENS //
        ////////////
        // TODO

        // If any alt VP card is present, add Victory tokens
        DependencyRule(
            condition = {
                it.types.contains(Type.LIAISON)
            },
            dependentCardNames = listOf(CardNames.FAVORS_MAT)
        ),

        // Coin tokens
        DependencyRule(
            condition = {
                it.types.contains(Type.LIAISON)
            },
            dependentCardNames = listOf(CardNames.FAVORS_MAT)
        ),

        // If Embargo is present, add embargo tokens
        DependencyRule(
            condition = {
                it.types.contains(Type.LIAISON)
            },
            dependentCardNames = listOf(CardNames.FAVORS_MAT)
        ),

        // Adventures tokens
        DependencyRule(
            condition = {
                it.types.contains(Type.LIAISON)
            },
            dependentCardNames = listOf(CardNames.FAVORS_MAT)
        ),

        // Debt tokens
        DependencyRule(
            condition = {
                it.types.contains(Type.LIAISON)
            },
            dependentCardNames = listOf(CardNames.FAVORS_MAT)
        ),

        // Renaissance cubes
        DependencyRule(
            condition = {
                it.types.contains(Type.LIAISON)
            },
            dependentCardNames = listOf(CardNames.FAVORS_MAT)
        ),

        // Prophecy tokens
        DependencyRule(
            condition = {
                it.types.contains(Type.LIAISON)
            },
            dependentCardNames = listOf(CardNames.FAVORS_MAT)
        )
    )
}