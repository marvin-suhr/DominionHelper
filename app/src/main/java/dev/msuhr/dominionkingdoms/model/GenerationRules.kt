package dev.msuhr.dominionkingdoms.model

import kotlinx.serialization.Serializable

/**
 * Defines the range for a specific constraint in the kingdom generation process.
 */
@Serializable
data class RuleOption(
    val min: Int = 0,
    val max: Int = MAX_CARDS
) {
    companion object {
        const val MAX_CARDS = 20
        val ALLOW = RuleOption(0, MAX_CARDS)
        val EXCLUDE = RuleOption(0, 0)
    }

    fun isAllow(): Boolean = min == 0 && max == MAX_CARDS
    fun isExclude(): Boolean = min == 0 && max == 0
}

enum class RuleTarget {
    PORTRAIT,
    LANDSCAPE
}

/**
 * Defines a specific constraint for the kingdom generation process.
 */
data class GenerationRule(
    val id: String,
    val name: String,
    val option: RuleOption = RuleOption.ALLOW,
    val imageName: String = "",
    val target: RuleTarget = RuleTarget.PORTRAIT,
    val condition: (Card) -> Boolean
)

/**
 * Utility to check if a rule's count requirement is satisfied.
 */
fun RuleOption.isSatisfied(count: Int): Boolean {
    return count in min..max
}
