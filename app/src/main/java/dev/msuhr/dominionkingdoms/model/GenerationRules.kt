package dev.msuhr.dominionkingdoms.model

enum class RuleOption {
    ALLOW,      // Any amount (default)
    EXCLUDE,    // 0 cards
    AT_LEAST_1, // >= 1
    AT_LEAST_2, // >= 2
    AT_MOST_1,  // <= 1
    AT_MOST_2,  // <= 2
    EXACTLY_1,  // == 1
    EXACTLY_2   // == 2
}

/**
 * Defines a specific constraint for the kingdom generation process.
 */
data class GenerationRule(
    val id: String,
    val name: String,
    val option: RuleOption = RuleOption.ALLOW,
    val condition: (Card) -> Boolean
)

/**
 * Utility to check if a rule's count requirement is satisfied.
 */
fun RuleOption.isSatisfied(count: Int): Boolean {
    return when (this) {
        RuleOption.EXCLUDE -> count == 0
        RuleOption.ALLOW -> true
        RuleOption.AT_LEAST_1 -> count >= 1
        RuleOption.AT_LEAST_2 -> count >= 2
        RuleOption.AT_MOST_1 -> count <= 1
        RuleOption.AT_MOST_2 -> count <= 2
        RuleOption.EXACTLY_1 -> count == 1
        RuleOption.EXACTLY_2 -> count == 2
    }
}

/**
 * Helper to get the required count from an option.
 * If negative, it's a "max" constraint (e.g., -1 means <= 1).
 * If positive, it's a "min" constraint (e.g., 2 means >= 2).
 * 0 means exactly 0.
 */
fun RuleOption.toCountConstraint(): Int? {
    return when (this) {
        RuleOption.EXCLUDE -> 0
        RuleOption.ALLOW -> null
        RuleOption.AT_LEAST_1 -> 1
        RuleOption.AT_LEAST_2 -> 2
        RuleOption.AT_MOST_1 -> -1
        RuleOption.AT_MOST_2 -> -2
        RuleOption.EXACTLY_1 -> 1 // Handled specially if we want exact
        RuleOption.EXACTLY_2 -> 2
    }
}
