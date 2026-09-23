package dev.msuhr.dominionkingdoms.model

object CardNames {

    val heirloomPairs = listOf(
        "Fool" to "Lucky Coin",
        "Cemetery" to "Haunted Mirror", // TODO: ADD GHOST AS WELL
        "Secret Cave" to "Magic Lamp", // TODO: ADD WISH AS WELL
        "Pixie" to "Goat",
        "Shepherd" to "Pasture",
        "Tracker" to "Pouch",
        "Pooka" to "Cursed Gold"
    )

    const val COPPER = "Copper"
    const val SILVER = "Silver"
    const val GOLD = "Gold"
    const val PLATINUM = "Platinum"
    const val ESTATE = "Estate"
    const val DUCHY = "Duchy"
    const val PROVINCE = "Province"

    val BASIC_CARDS = listOf(
        COPPER,
        SILVER,
        GOLD,
        ESTATE,
        DUCHY,
        PROVINCE
    )

    const val CURSE = "Curse"
    const val POTION = "Potion"
    const val WILL_O_WISP = "Will-o'-Wisp"
    const val DELUDED = "Deluded"
    const val ENVIOUS = "Envious"
    const val MISERABLE = "Miserable"
    const val TWICE_MISERABLE = "Twice Miserable"

    const val ISLAND = "Island"
    const val PIRATE_SHIP = "Pirate Ship"
    const val NATIVE_VILLAGE = "Native Village"
    const val TRADE_ROUTE = "Trade Route"

    val lootProviders = listOf(
        "Jewelled Egg",
        "Peril",
        "Search",
        "Foray",
        "Pickaxe",
        "Wealthy Village",
        "Cutthroat",
        "Looting",
        "Sack of Loot",
        "Invasion",
        "Prosper",
        "Cursed"
    )

    // For Spoils providers
    const val BANDIT_CAMP = "Bandit Camp"
    const val MARAUDER = "Marauder"
    const val PILLAGE = "Pillage"
    const val SPOILS = "Spoils"

    // Artifact related
    const val BORDER_GUARD = "Border Guard"
    const val LANTERN = "Lantern"
    const val HORN = "Horn"
    const val FLAG_BEARER = "Flag Bearer"
    const val FLAG = "Flag"
    const val SWASHBUCKLER = "Swashbuckler"
    const val TREASURE_CHEST = "Treasure Chest"
    const val TREASURER = "Treasurer"
    const val KEY = "Key"

    val CoffersCards = listOf(
        "Baker",
        "Butcher",
        "Candlestick Maker",
        "Footpad",
        "Joust",
        "Merchant Guild",
        "Plaza",
        "Ducat",
        "Patron",
        "Silk Merchant",
        "Spices",
        "Swashbuckler",
        "Villain",
        "Exploration",
        "Guildhall",
        "Pageant"
    )

    val VillagersCards = listOf(
        "Acting Troupe",
        "Lackeys",
        "Patron",
        "Recruiter",
        "Sculptor",
        "Silk Merchant",
        "Academy",
        "Exploration"
    )

    // Specific card interactions
    const val FOOL = "Fool"
    const val LOST_IN_THE_WOODS = "Lost in the Woods"
    const val NECROMANCER = "Necromancer"
    const val ZOMBIE_APPRENTICE = "Zombie Apprentice"
    const val ZOMBIE_MASON = "Zombie Mason"
    const val ZOMBIE_SPY = "Zombie Spy"
    const val VAMPIRE = "Vampire"
    const val BAT = "Bat"
    const val LEPRECHAUN = "Leprechaun"
    const val SECRET_CAVE = "Secret Cave"
    const val WISH = "Wish"
    const val HERMIT = "Hermit"
    const val MADMAN = "Madman"
    const val URCHIN = "Urchin"
    const val MERCENARY = "Mercenary"
    const val DEVILS_WORKSHOP = "Devil's Workshop"
    const val TORMENTOR = "Tormentor"
    const val IMP = "Imp"

    // Spirits
    const val EXORCIST = "Exorcist" // -> Imp, Will-o'-Wisp
    const val GHOST = "Ghost"

    // Travellers
    const val PAGE = "Page"
    const val TREASURE_HUNTER = "Treasure Hunter"
    const val WARRIOR = "Warrior"
    const val HERO = "Hero"
    const val CHAMPION = "Champion"
    const val PEASANT = "Peasant"
    const val SOLDIER = "Soldier"
    const val FUGITIVE = "Fugitive"
    const val DISCIPLE = "Disciple"
    const val TEACHER = "Teacher"

    // Horse
    val horseCards = listOf(
        "Sleigh",
        "Supplies",
        "Scrap",
        "Cavalry",
        "Groom",
        "Hostelry",
        "Livery",
        "Paddock",
        "Ride",
        "Bargain",
        "Demand",
        "Stampede"
    )
    const val HORSE = "Horse"

    // Mats
    const val TRASH_MAT = "Trash Mat"
    const val ISLAND_MAT = "Island Mat"
    const val PIRATE_SHIP_MAT = "Pirate Ship Mat"
    const val NATIVE_VILLAGE_MAT = "Native Village Mat"
    const val TRADE_ROUTE_MAT = "Trade Route Mat"
    const val VICTORY_TOKEN_MAT = "Victory Token Mat"
    const val TAVERN_MAT = "Tavern Mat"
    const val COFFERS_MAT = "Coffers Mat"
    const val VILLAGERS_MAT = "Villagers Mat"
    const val EXILE_MAT = "Exile Mat"
    const val FAVORS_MAT = "Favors Mat"

    // Tokens
    const val VICTORY_TOKENS = "Victory Tokens"
    val AltVPCards = listOf(
        "Triumph",
        "Chariot Race",
        "Farmers' Market",
        "Bishop",
        "Crumbling Castle",
        "Investment",
        "Monument",
        "Ritual",
        "Sacrifice",
        "Salt the Earth",
        "Temple",
        "Wedding",
        "Collection",
        "Emporium",
        "Groundskeeper",
        "Plunder",
        "Wild Hunt",
        "Conquest",
        "Goons",
        "Grand Castle",
        "Dominate",
        "Aqueduct",
        "Arena",
        "Basilica",
        "Baths",
        "Battlefield",
        "Colonnade",
        "Defiled Shrine",
        "Labyrinth",
        "Mountain Pass",
        "Tomb"
    )

    const val COIN_TOKENS = "Coin Tokens"
    val CoinCards = listOf(
        "Pirate Ship",
        "Trade Route",
        "Sinister Plot",
        "Garrison"
    ) + CoffersCards + VillagersCards

    const val EMBARGO_TOKENS = "Embargo Tokens"
    const val EMBARGO = "Embargo"

    const val ADVENTURES_TOKENS = "Adventures Tokens"
    val AdventureTokenCards = listOf(
        "Teacher",
        "Lost Arts",
        "Seaway",
        "Pathfinding",
        "Training",
        "Ferry",
        "Plan",
        "Relic",
        "Borrow",
        "Raid",
        "Bridge Troll",
        "Ball",
        "Ranger",
        "Giant",
        "Pilgrimage",
        "Inheritance"
    )

    const val DEBT_TOKENS = "Debt Tokens"
    val DebtCards = listOf(
        "Engineer",
        "Mountain Shrine",
        "Triumph",
        "Daimyo",
        "Annex",
        "Artist",
        "City Quarter",
        "Continue",
        "Donate",
        "Overlord",
        "Royal Blacksmith",
        "Wedding",
        "Fortune",
        "Change",
        "Craftsman",
        "Gold Mine",
        "Imperial Envoy",
        "Litter",
        "Root Cellar",
        "Capital",
        "Credit",
        "Tax",
        "Harsh Winter",
        "Mountain Pass"
    )

    const val WOODEN_CUBES = "Wooden Cubes"
    // -> Projects

    const val SUN_TOKENS = "Sun Tokens"
    // -> Omen / Prophecy

    // Placeholders for entire piles
    const val BOON_PILE = "Boon Pile"
    const val HEX_PILE = "Hex Pile"
    const val LOOT_PILE = "Loot Pile"
    const val RUINS_PILE = "Ruins Pile"

    // Tournament / Joust
    const val TOURNAMENT = "Tournament"
    const val PRIZE_PILE = "Prize Pile"
    const val JOUST = "Joust"
    const val REWARD_PILE = "Reward Pile"
    const val CASTLES = "Castles"
}
