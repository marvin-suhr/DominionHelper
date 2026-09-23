package dev.msuhr.dominionkingdoms.model

import android.content.Context
import android.util.Log
import dev.msuhr.dominionkingdoms.model.ExpansionData
import java.io.IOException

/**
 * Platform loaders for the bundled card / expansion JSON. The parsing logic
 * lives in the shared module (CardJson); this file only reads the Android
 * assets. Kept in the model package so existing imports keep working.
 */
fun loadCardsFromAssets(context: Context): List<Card> {
    val jsonString: String
    try {
        val inputStream = context.assets.open("cards.json")
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        jsonString = String(buffer, Charsets.UTF_8)
    } catch (e: IOException) {
        Log.e("loadCardsFromAssets", "Error reading from assets", e)
        return emptyList()
    }

    return CardJson.parseCards(jsonString)
}

fun loadExpansionsFromAssets(context: Context): ExpansionData {
    val jsonString: String
    try {
        val inputStream = context.assets.open("sets.json")
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        jsonString = String(buffer, Charsets.UTF_8)
    } catch (e: IOException) {
        Log.e("loadExpansionsFromAssets", "Error reading from assets", e)
        return ExpansionData(emptyList(), emptyList())
    }

    return CardJson.parseExpansions(jsonString)
}
