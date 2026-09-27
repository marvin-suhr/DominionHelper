package dev.msuhr.dominionkingdoms.shared.utils

import dev.msuhr.dominionkingdoms.model.Card

/**
 * Inserts a new key-value pair into a LinkedHashMap at the position of a
 * specified existing key. The existing key will be removed. If the targetKey
 * is not found, the new entry is added at the end.
 * (Port of app/utils/Utils.kt)
 */
fun <K, V> insertOrReplaceAtKeyPosition(
    map: LinkedHashMap<K, V>,
    targetKey: K,
    newKey: K,
    newValue: V
): LinkedHashMap<K, V> {
    val result = LinkedHashMap<K, V>()
    var inserted = false

    if (targetKey == newKey) {
        map.forEach { (k, v) ->
            if (k == targetKey) {
                result[newKey] = newValue
            } else {
                result[k] = v
            }
        }
        return result
    }

    val tempMap = LinkedHashMap(map)
    if (tempMap.containsKey(newKey) && newKey != targetKey) {
        tempMap.remove(newKey)
    }

    for ((k, v) in tempMap) {
        if (k == targetKey) {
            result[newKey] = newValue
            inserted = true
        } else {
            result[k] = v
        }
    }

    if (!inserted) {
        result[newKey] = newValue
    }

    return result
}

/** Converts a card list to a LinkedHashMap of card -> 1. */
fun listToMap(cards: List<Card>): LinkedHashMap<Card, Int> {
    val map = LinkedHashMap<Card, Int>()
    cards.forEach { map[it] = 1 }
    return map
}
