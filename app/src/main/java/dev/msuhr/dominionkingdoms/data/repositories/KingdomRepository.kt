package dev.msuhr.dominionkingdoms.data.repositories

import dev.msuhr.dominionkingdoms.data.CardDao
import dev.msuhr.dominionkingdoms.data.KingdomDao
import dev.msuhr.dominionkingdoms.data.mappers.toDomainModel
import dev.msuhr.dominionkingdoms.data.mappers.toEntity
import dev.msuhr.dominionkingdoms.model.Kingdom
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KingdomRepository @Inject constructor(
    private val kingdomDao: KingdomDao,
    private val cardDao: CardDao,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * Triggers a manual refresh of the kingdoms list.
     * Useful when dependent data (like cards) has been updated.
     */
    fun refresh() {
        refreshTrigger.tryEmit(Unit)
    }

    /**
     * Retrieves all kingdoms from the database as a Flow of domain models.
     * Each KingdomEntity is mapped to a Kingdom domain model.
     * Note: This mapping involves fetching card details for each kingdom,
     * which could be performance-intensive for large lists.
     *
     * We combine it with cardDao.getAllCardsFlow() and a manual refreshTrigger
     * to ensure that the list refreshes whenever the card database is updated.
     */
    fun getAllKingdoms(): Flow<List<Kingdom>> {
        return combine(
            kingdomDao.getAllKingdomsFlow(),
            cardDao.getAllCardsFlow(),
            refreshTrigger.onStart { emit(Unit) }
        ) { kingdomEntities, _, _ ->
            // Map each KingdomEntity to the Kingdom domain model
            // This requires cardDao for fetching associated cards.
            kingdomEntities.map { entity ->
                entity.toDomainModel(cardDao)
            }
        }
    }

    /**
     * Retrieves a specific kingdom by its database ID.
     * @param uuid The Int ID of the kingdom in the database.
     * @return The Kingdom domain model if found, null otherwise.
     */
    suspend fun getKingdomById(uuid: String): Kingdom? {
        // Perform DB operations on the injected dispatcher (e.g., Dispatchers.IO)
        return withContext(defaultDispatcher) {
            val kingdomEntity = kingdomDao.getKingdomById(uuid)
            kingdomEntity?.toDomainModel(cardDao)
        }
    }

    /**
     * Saves a kingdom to the database.
     * The Kingdom domain model is first converted to a KingdomEntity.
     * @param kingdom The Kingdom domain model to save.
     * @return The row ID of the newly inserted kingdom, or -1 if an error occurred.
     */
    suspend fun saveKingdom(kingdom: Kingdom): Long {
        return withContext(defaultDispatcher) {
            val kingdomEntity = kingdom.toEntity()
            kingdomDao.insertKingdom(kingdomEntity)
        }
    }

    /**
     * Deletes a kingdom from the database by its ID.
     * @param kingdomId The Int ID of the kingdom to delete.
     */
    suspend fun deleteKingdomById(uuid: String) {
        withContext(defaultDispatcher) {
            kingdomDao.deleteKingdomById(uuid)
        }
    }

    suspend fun favoriteKingdomById(uuid: String, newIsFavorite: Boolean) {
        withContext(defaultDispatcher) {
            kingdomDao.toggleFavoriteKingdomById(uuid, newIsFavorite)
        }
    }

    suspend fun changeKingdomName(uuid: String, newName: String) {
        withContext(defaultDispatcher) {
            kingdomDao.changeKingdomName(uuid, newName)
        }
    }
}