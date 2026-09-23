package dev.msuhr.dominionkingdoms.model

/**
 * Generation mode settings, shared between the Android app and iOS.
 * Previously defined in ui/SettingsViewModel.kt on Android.
 */
enum class RandomMode(val displayName: String) {
    EVEN_AMOUNTS("Even Amounts"),
    LIMITED_RANDOM("Limited Random"),
    FULL_RANDOM("Full Random")
}

enum class VetoMode(val displayName: String) {
    REROLL_SAME("Reroll from the same expansion"),
    REROLL_ANY("Reroll from any selected expansion"),
    NO_REROLL("Don't reroll (10 cards minimum)")
}

enum class DarkAgesMode(val displayName: String) {
    TEN_PERCENT_PER_CARD("10% per Dark Ages card"),
    IF_PRESENT("When at least one card is present"),
    NEVER("Never")
}

enum class ProsperityMode(val displayName: String) {
    TEN_PERCENT_PER_CARD("10% per Prosperity card"),
    IF_PRESENT("When at least one card is present"),
    NEVER("Never")
}

enum class PromoMode(val displayName: String) {
    POOL("Add to pool of available cards"),
    NEVER("Never add promo cards"),
    ALWAYS_ONE("Always add a promo card (if owned)")
}
