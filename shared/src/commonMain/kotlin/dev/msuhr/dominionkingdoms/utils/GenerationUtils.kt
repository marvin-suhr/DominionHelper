package dev.msuhr.dominionkingdoms.utils

import dev.msuhr.dominionkingdoms.model.Card
import kotlin.random.Random

fun isPercentChance(percentChance: Double): Boolean {
    require(percentChance in 0.0..100.0) { "percentChance must be between 0.0 and 100.0" }
    return Random.nextDouble(0.0, 100.0) < percentChance
}

fun listToMap(list: List<Card>): LinkedHashMap<Card, Int> {
    val map = linkedMapOf<Card, Int>()
    list.forEach { card ->
        map[card] = 1 // Default value of 1
    }
    return map
}
