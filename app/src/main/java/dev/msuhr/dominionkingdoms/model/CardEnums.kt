package dev.msuhr.dominionkingdoms.model

import dev.msuhr.dominionkingdoms.R
import dev.msuhr.dominionkingdoms.ui.KingdomViewModel
import dev.msuhr.dominionkingdoms.ui.LibraryViewModel

enum class Set (val imageId: Int = 0, val displayName: String = "") {
    BASE_1E(R.drawable.set_dominion_1e, "Dominion"),
    BASE_2E(R.drawable.set_dominion_2e, "Dominion"),
    INTRIGUE_1E(R.drawable.set_intrigue_1e, "Intrigue"),
    INTRIGUE_2E(R.drawable.set_intrigue_2e, "Intrigue"),
    SEASIDE_1E(R.drawable.set_seaside_1e, "Seaside"),
    SEASIDE_2E(R.drawable.set_seaside_2e, "Seaside"),
    ALCHEMY(R.drawable.set_alchemy, "Alchemy"),
    PROSPERITY_1E(R.drawable.set_prosperity_1e, "Prosperity"),
    PROSPERITY_2E(R.drawable.set_prosperity_2e, "Prosperity"),
    CORNUCOPIA_1E(R.drawable.set_cornucopia, "Cornucopia"),
    CORNUCOPIA_GUILDS_2E(R.drawable.set_cornucopia_guilds_2e, "Cornucopia"),
    HINTERLANDS_1E(R.drawable.set_hinterlands_1e, "Hinterlands"),
    HINTERLANDS_2E(R.drawable.set_hinterlands_2e, "Hinterlands"),
    DARK_AGES(R.drawable.set_dark_ages, "Dark Ages"),
    GUILDS_1E(R.drawable.set_guilds, "Guilds"),
    ADVENTURES(R.drawable.set_adventures, "Adventures"),
    EMPIRES(R.drawable.set_empires, "Empires"),
    NOCTURNE(R.drawable.set_nocturne, "Nocturne"),
    RENAISSANCE(R.drawable.set_renaissance, "Renaissance"),
    MENAGERIE(R.drawable.set_menagerie, "Menagerie"),
    ALLIES(R.drawable.set_allies, "Allies"),
    PLUNDER(R.drawable.set_plunder, "Plunder"),
    RISING_SUN(R.drawable.set_rising_sun, "Rising Sun"),
    PROMO(R.drawable.set_promo, "Promo Cards"),
    PLACEHOLDER(displayName = "Placeholder") // TODO
}

enum class Type(
    val sortPriority: Int = Int.MAX_VALUE,
    val displayText: String? = null
) {
    // Basic Types (sortPriority here is only needed for base game)
    TREASURE(sortPriority = 100, displayText = "Treasure"),
    VICTORY(sortPriority = 101, displayText = "Victory"),
    CURSE(sortPriority = 102, displayText = "Curse"),
    ACTION(displayText = "Action"),
    ATTACK(displayText = "Attack"),
    REACTION(displayText = "Reaction"),
    DURATION(displayText = "Duration"),
    NIGHT(displayText = "Night"),
    COMMAND(displayText = "Command"),

    // --- Non-Landscape Card Types ---
    PRIZE(sortPriority = 0, displayText = "Prize"),             // Cornucopia 1E
    KNIGHT(sortPriority = 1, displayText = "Knight"),           // Dark Ages
    LOOTER(sortPriority = 2, displayText = "Looter"),           // Dark Ages
    RUINS(sortPriority = 3, displayText = "Ruins"),             // Dark Ages
    SHELTER(sortPriority = 4, displayText = "Shelter"),         // Dark Ages
    RESERVE(sortPriority = 6, displayText = "Reserve"),         // Adventures
    TRAVELLER(sortPriority = 5, displayText = "Traveller"),     // Adventures
    CASTLE(sortPriority = 7, displayText = "Castle"),           // Empires
    GATHERING(sortPriority = 8, displayText = "Gathering"),     // Empires
    DOOM(sortPriority = 9, displayText = "Doom"),               // Nocturne
    FATE(sortPriority = 10, displayText = "Fate"),              // Nocturne
    HEIRLOOM(sortPriority = 11, displayText = "Heirloom"),      // Nocturne
    SPIRIT(sortPriority = 12, displayText = "Spirit"),          // Nocturne
    ZOMBIE(sortPriority = 13, displayText = "Zombie"),          // Nocturne
    AUGUR(sortPriority = 14, displayText = "Augur"),            // Allies
    CLASH(sortPriority = 15, displayText = "Clash"),            // Allies
    FORT(sortPriority = 16, displayText = "Fort"),              // Allies
    ODYSSEY(sortPriority = 17, displayText = "Odyssey"),        // Allies
    TOWNSFOLK(sortPriority = 18, displayText = "Townsfolk"),    // Allies
    WIZARD(sortPriority = 19, displayText = "Wizard"),          // Allies
    LIAISON(sortPriority = 20, displayText = "Liaison"),        // Allies
    LOOT(sortPriority = 21, displayText = "Loot"),              // Plunder
    REWARD(sortPriority = 22, displayText = "Reward"),          // Cornucopia & Guilds 2E -> Move up?
    OMEN(sortPriority = 23, displayText = "Omen"),              // Rising Sun
    SHADOW(sortPriority = 24, displayText = "Shadow"),          // Rising Sun

    // --- Landscape Card Types ---
    EVENT(sortPriority = 25, displayText = "Event"),            // Adventures
    LANDMARK(sortPriority = 26, displayText = "Landmark"),      // Empires
    BOON(sortPriority = 27, displayText = "Boon"),              // Nocturne
    HEX(sortPriority = 28, displayText = "Hex"),                // Nocturne
    STATE(sortPriority = 29, displayText = "State"),            // Nocturne
    ARTIFACT(sortPriority = 30, displayText = "Artifact"),      // Renaissance
    PROJECT(sortPriority = 31, displayText = "Project"),        // Renaissance
    WAY(sortPriority = 32, displayText = "Way"),                // Menagerie
    ALLY(sortPriority = 33, displayText = "Ally"),              // Allies
    TRAIT(sortPriority = 34, displayText = "Trait"),            // Plunder
    PROPHECY(sortPriority = 35, displayText = "Prophecy"),      // Rising Sun
}

enum class Category(val displayName: String) {
    CANTRIP("Cantrip"),
    NONTERMINAL_DRAW("Non-terminal Draw"),
    TERMINAL_DRAW("Terminal Draw"),
    CURSER("Curser"),
    NONTERMINAL("Non-terminal"), // Skipped: Cantrips, Villages, Non-terminal draw, peddlers, disappearing money
    TERMINAL("Terminal"), // Skipped: Way too many cards
    THRONEROOM_VARIANT("Throne Room Variant"),
    VILLAGE("Village"),
    TRASHER("Trasher"),
    ALT_VP("Alt VP"),
    PLUSBUY("Plus Buy"),
    DECK_INSPECTOR("Deck Inspector"),
    TRASH_FOR_BENEFIT("Trash for Benefit"),
    HANDSIZE_ATTACK("Handsize Attack"),

    // ^done
    // v 2. Runde

    GAINER("Gainer"),
    DRAW_TO_X("Draw to X"),
    JUNKER("Junker"),
    DECK_ORDER_ATTACK("Deck Order Attack"),
    TRASHING_ATTACK("Trashing Attack"),
    PEDDLER_VARIANT("Peddler Variant"),
    TERMINAL_SILVER("Terminal Silver"),
    SIFTER("Sifter"),


    DIGGING("Digging"),
    DISCARD("Discard"), // For benefit
    COST_REDUCTION("Cost Reduction"),
    DISAPPEARING_MONEY("Disappearing Money"),
    VIRTUAL_COIN("Virtual Coin"),
    VIRTUAL_BUY("Virtual Buy"), // ?
    ATTACK_IMMUNITY("Attack Immunity"),
    TURN_WORSENING_ATTACK("Turn Worsening Attack"), // ?
    DURATION_DRAW("Duration Draw"),
    COMMAND_VARIANT("Command Variant"),
    NON_ATTACK_INTERACTION("Non-attack Interaction"),
    ONE_SHOT("One-shot"),
    REMODELER("Remodeler"),
    SPLIT_PILE("Split Pile"),
    TOP_DECKER("Top Decker"),
    VANILLA("Vanilla"), // ?
    EXTRA_TURN("Extra Turn") // ?
}

enum class CardDisplayCategory {
    SUPPLY, // Normal kingdom cards
    SPECIAL, // Additional cards dependent on other cards
    LANDSCAPE // Landscape cards
}

sealed class AppSortType(val text: String) {
    data class Kingdom(val sortType: KingdomViewModel.SortType) : AppSortType(sortType.text)
    data class Library(val sortType: LibraryViewModel.SortType) : AppSortType(sortType.text)
}