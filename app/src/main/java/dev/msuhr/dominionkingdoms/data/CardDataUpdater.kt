package dev.msuhr.dominionkingdoms.data

import android.content.Context
import android.util.Log
import dev.msuhr.dominionkingdoms.model.loadCardsFromAssets
import dev.msuhr.dominionkingdoms.utils.Constants
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardDataUpdater @Inject constructor(
    private val context: Context,
    private val userPrefsRepository: UserPrefsRepository,
    private val cardDao: CardDao
) {
    suspend fun checkAndUpdateIfNeeded() {
        try {
            val currentVersion = Constants.CARD_DATA_VERSION
            val storedVersion = userPrefsRepository.cardDataVersion.first()

            if (currentVersion > storedVersion) {
                Log.i("CardDataUpdater", "Updating card data from version $storedVersion to $currentVersion")
                updateCardData()
                userPrefsRepository.setCardDataVersion(currentVersion)
                userPrefsRepository.setShowCardUpdateDialog(true)
                Log.i("CardDataUpdater", "Card data updated successfully to version $currentVersion")
            } else {
                Log.d("CardDataUpdater", "Card data is up to date (version $storedVersion)")
            }
        } catch (e: Exception) {
            Log.e("CardDataUpdater", "Failed to check/update card data", e)
        }
    }

    private suspend fun updateCardData() {
        // 1. Fetch what's currently in the DB and map it by its stable hardcoded ID
        val existingCards = cardDao.getAllCards().associateBy { it.id }

        // 2. Load the fresh cards from the updated JSON file
        val freshCardsFromJson = loadCardsFromAssets(context)

        var newCardsCount = 0
        var changedCardsCount = 0

        // 3. Merge user preferences into the new JSON models in-memory
        val processedCards = freshCardsFromJson.map { jsonCard ->
            val existingCard = existingCards[jsonCard.id]

            if (existingCard != null) {
                // Card exists! Keep the user's settings, use the updated JSON metadata
                val updatedCard = jsonCard.copy(
                    isEnabled = existingCard.isEnabled,
                    isFavorite = existingCard.isFavorite
                )

                // If they aren't equal now, a metadata field (cost, name, etc.) changed
                if (updatedCard != existingCard) {
                    changedCardsCount++
                }

                updatedCard
            } else {
                // Brand new card added to the game in an app update
                newCardsCount++
                jsonCard
            }
        }

        // 4. Overwrite the table in one single transaction
        cardDao.replaceAllCards(processedCards)

        Log.i(
            "CardDataUpdater",
            "Card database update complete! Added $newCardsCount new cards. Updated $changedCardsCount cards with metadata changes. Total cards: ${processedCards.size}"
        )
    }
}
