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

    // For Loot providers
    const val JEWELLED_EGG = "Jewelled Egg"
    const val PERIL = "Peril"
    const val SEARCH = "Search"
    const val FORAY = "Foray"
    const val PICKAXE = "Pickaxe"
    const val WEALTHY_VILLAGE = "Wealthy Village"
    const val CUTTHROAT = "Cutthroat"
    const val LOOTING = "Looting"
    const val SACK_OF_LOOT = "Sack of Loot"
    const val INVASION = "Invasion"
    const val PROSPER = "Prosper"
    const val CURSED = "Cursed"

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

    // Coffers Guilds
    const val BAKER = "Baker"
    const val BUTCHER = "Butcher"
    const val CANDLESTICK_MAKER = "Candlestick Maker"
    const val FOOTPAD = "Footpad"
    // const val JOUST = "Joust"
    const val MERCHANT_GUILD = "Merchant Guild"
    const val PLAZA = "Plaza"

    // Coffers Renaissance
    const val DUCAT = "Ducat"
    const val PATRON = "Patron"
    const val SILK_MERCHANT = "Silk Merchant"
    const val SPICES = "Spices"
    // const val SWASHBUCKLER= "Swashbuckler"
    const val VILLAIN = "Villain"
    const val EXPLORATION = "Exploration"
    const val GUILDHALL = "Guildhall"
    const val PAGEANT = "Pageant"

    // Villagers
    const val ACTING_TROUPE = "Acting Troupe"
    const val LACKEYS = "Lackeys"
    //const val PATRON = "Patron"
    const val RECRUITER = "Recruiter"
    const val SCULPTOR = "Sculptor"
    //const val SILK_MERCHANT = "Silk Merchant"
    const val ACADEMY = "Academy"
    //const val EXPLORATION = "Exploration"

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
    const val SLEIGH = "Sleigh"
    const val SUPPLIES = "Supplies"
    const val SCRAP = "Scrap"
    const val CAVALRY = "Cavalry"
    const val GROOM = "Groom"
    const val HOSTELRY = "Hostelry"
    const val LIVERY = "Livery"
    const val PADDOCK = "Paddock"
    const val RIDE = "Ride"
    const val BARGAIN = "Bargain"
    const val DEMAND = "Demand"
    const val STAMPEDE = "Stampede"
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
    const val COFFERS_VILLAGERS_MAT = "Coffers / Villagers Mat"
    const val EXILE_MAT = "Exile Mat"
    const val FAVORS_MAT = "Favors Mat"

    // Placeholders for entire piles
    const val BOON_PILE = "Boon pile"
    const val HEX_PILE = "Hex pile"
    const val LOOT_PILE = "Loot pile"
    const val RUINS_PILE = "Ruins pile"

    // Tournament / Joust
    const val TOURNAMENT = "Tournament"
    const val PRIZE_PILE = "Prizes"
    const val JOUST = "Joust"
    const val REWARD_PILE = "Rewards"
}