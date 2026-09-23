package dev.msuhr.dominionkingdoms.model

/**
 * Sort order for generated kingdoms. Was previously nested inside
 * ui.KingdomViewModel - moved here so the shared generator can use it.
 */
enum class KingdomSortType(val text: String) {
    EXPANSION("Sort by expansion"),
    ALPHABETICAL("Sort alphabetically"),
    COST("Sort by cost")
}
