package dev.msuhr.dominionkingdoms.model

import java.util.UUID

// Class representing a randomly generated round of Dominion
data class Kingdom(

    // Amount is needed because of victory cards like Garden
    val randomCards: LinkedHashMap<Card, Int> = linkedMapOf(),
    // Amount is needed for basic victory cards
    val basicCards: LinkedHashMap<Card, Int> = linkedMapOf(),
    // Amount is needed for cards like Ruins
    val dependentCards: LinkedHashMap<Card, Int> = linkedMapOf(),
    // Copper and Estate can have varying amounts
    val startingCards: LinkedHashMap<Card, Int> = linkedMapOf(),
    // Amount not really needed
    val landscapeCards: LinkedHashMap<Card, Int> = linkedMapOf(),
    val uuid: String = UUID.randomUUID().toString(),
    val creationTimeStamp: Long = System.currentTimeMillis(),
    var isFavorite: Boolean = false,
    var name: String = "Unnamed Kingdom",
    val warningMessage: String? = null // TODO do we need this on the kingdom object?
) {

    fun hasDependentCards(): Boolean {
        return dependentCards.isNotEmpty()
    }

    fun hasLandscapeCards(): Boolean {
        return landscapeCards.isNotEmpty()
    }

    fun isEmpty(): Boolean {
        return randomCards.isEmpty() && basicCards.isEmpty() && dependentCards.isEmpty() && startingCards.isEmpty() && landscapeCards.isEmpty()
    }

    fun getAllCards(): List<Card> {
        return randomCards.keys.toList() + basicCards.keys.toList() + dependentCards.keys.toList() + startingCards.keys.toList() + landscapeCards.keys.toList()
    }
}