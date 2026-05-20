package dev.msuhr.dominionkingdoms.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import dev.msuhr.dominionkingdoms.R
import dev.msuhr.dominionkingdoms.model.Card
import kotlin.random.Random

fun getDrawableId(context: Context, imageName: String): Int {
    /* TODO: try this (should be faster)
    try {
        Class res = R.drawable.class;
        Field field = res.getField("drawableName");
        int drawableId = field.getInt(null);
    }
    catch (Exception e) {
        Log.e("MyTag", "Failure to get drawable id.", e);
    }*/
    val resourceId = context.resources.getIdentifier(
        imageName,
        "drawable",
        context.packageName
    )

    if (resourceId == 0) {
        Log.e("getDrawableId", "Could not find drawable resource with name: $imageName")
        return R.drawable.ic_launcher_foreground
    }

    return resourceId
}

fun navigateToActivity(context: Context, activityClass: Class<*>) {
    val intent = Intent(context, activityClass).apply {
        flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    context.startActivity(intent)
}

fun isPercentChance(percentChance: Double): Boolean {
    require(percentChance in 0.0..100.0) { "percentChance must be between 0.0 and 100.0" }
    return Random.nextDouble(0.0, 100.0) < percentChance
}

// Find the index of a specific card in a list by id (handles different object instances)
fun findIndexOfReference(list: List<Any>, target: Card): Int {
    for ((index, item) in list.withIndex()) {
        if (item is Card && item === target) return index
    }
    return -1
}

/**
 * Rebuilds a LinkedHashMap, replacing an existing key with a new key-value pair,
 * attempting to maintain the original position.
 * If the keyToReplace is not found, the new key-value pair is added at the end.
 *
 * @param originalMap The LinkedHashMap to operate on.
 * @param keyToReplace The key of the item to be replaced.
 * @param newKey The new key to insert.
 * @param newValue The new value to insert.
 * @return A new LinkedHashMap with the replacement made.
 */
// TODO: HUH? Where did I use this again
fun <K, V> replaceInLinkedHashMap(
    originalMap: LinkedHashMap<K, V>,
    keyToReplace: K,
    newKey: K,
    newValue: V
): LinkedHashMap<K, V> {
    val newMap = LinkedHashMap<K, V>()
    var replaced = false

    for ((currentKey, currentValue) in originalMap) {
        if (currentKey == keyToReplace) {
            newMap[newKey] = newValue
            replaced = true
        } else {
            // Avoid adding the newKey if it's already going to be the keyToReplace
            // or if it's the same as the currentKey we are iterating over (to prevent duplicates if newKey pre-existed)
            if (currentKey != newKey) {
                newMap[currentKey] = currentValue
            } else if (keyToReplace != newKey) {
                // If newKey was already in the map (and it's not the one we are replacing)
                // it will be added here. If newKey IS keyToReplace, it's handled above.
                newMap[currentKey] = currentValue
            }
        }
    }

    // If the keyToReplace was not found, or if newKey is different from keyToReplace
    // and newKey was not already in the map in a different position, add the new key-value pair at the end.
    // This logic simplifies: if replacement happened, newKey is in. If not, it means keyToReplace wasn't there,
    // so we add newKey/newValue unless newKey was *already* in the map (and wasn't keyToReplace).
    if (!replaced && !newMap.containsKey(newKey)) {
        newMap[newKey] = newValue
    }
    return newMap
}

/**
 * Inserts a new key-value pair into a LinkedHashMap at the position of a specified existing key.
 * The existing key will be removed. If the targetKey is not found, the new entry is added at the end.
 *
 * @param map The original LinkedHashMap.
 * @param targetKey The key at whose position the new entry should be inserted.
 * @param newKey The key of the new entry.
 * @param newValue The value of the new entry.
 * @return A new LinkedHashMap with the entry inserted/replaced.
 */
fun <K, V> insertOrReplaceAtKeyPosition(
    map: LinkedHashMap<K, V>,
    targetKey: K,
    newKey: K,
    newValue: V
): LinkedHashMap<K, V> {
    val result = LinkedHashMap<K, V>()
    var inserted = false

    if (targetKey == newKey) { // Simple replacement if keys are the same
        map.forEach { (k, v) ->
            if (k == targetKey) {
                result[newKey] = newValue
            } else {
                result[k] = v
            }
        }
        return result
    }

    // If newKey already exists and it's not the targetKey, remove it first to avoid duplicates
    // and ensure it's re-inserted at the targetKey's position.
    val tempMap = LinkedHashMap(map)
    if (tempMap.containsKey(newKey) && newKey != targetKey) {
        tempMap.remove(newKey)
    }


    for ((k, v) in tempMap) {
        if (k == targetKey) {
            result[newKey] = newValue // Insert the new key-value pair
            inserted = true
            // Do not add the old targetKey (k,v) back unless newKey is different AND targetKey should be preserved
            // In a "replace" scenario, we don't add k,v back.
        } else {
            result[k] = v
        }
    }

    if (!inserted) { // If targetKey was not found, add the new key-value to the end
        result[newKey] = newValue
    }

    return result
}

fun listToMap(list: List<Card>): LinkedHashMap<Card, Int> {
    val map = linkedMapOf<Card, Int>()
    list.forEach { card ->
        map[card] = 1 // Default value of 1
    }
    return map
}
