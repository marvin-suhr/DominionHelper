package dev.msuhr.dominionkingdoms.model

import dev.msuhr.dominionkingdoms.platform.logE
import kotlinx.serialization.json.Json

/**
 * Parses the bundled card / expansion JSON (assets on Android, bundle
 * resources on iOS). The platform layer only needs to supply the string.
 */
object CardJson {

    private val json = Json {
        ignoreUnknownKeys = false // Fail on unknown keys to catch typos/structural issues
        coerceInputValues = true // Use default values for missing fields
    }

    fun parseCards(jsonString: String): List<Card> {
        val cardList: List<Card> = json.decodeFromString(jsonString)

        // Expansion cards are enabled by default, Promo cards are disabled by default
        return cardList.map { card ->
            if (card.sets.contains(Set.PROMO)) {
                card // Keeps value from JSON (defaulting to false if missing)
            } else {
                card.copy(isEnabled = true)
            }
        }
    }

    fun parseExpansions(jsonString: String): ExpansionData {
        return json.decodeFromString(jsonString)
    }

    fun parseCardsFromAssetOrEmpty(assetName: String, readAsset: (String) -> String?): List<Card> {
        val jsonString = try {
            readAsset(assetName) ?: return emptyList()
        } catch (e: Exception) {
            logE("CardJson", "Error reading asset $assetName", e)
            return emptyList()
        }
        return parseCards(jsonString)
    }
}
