package dev.msuhr.dominionkingdoms.data.repositories

import dev.msuhr.dominionkingdoms.data.CardDao
import dev.msuhr.dominionkingdoms.data.KingdomDao
import dev.msuhr.dominionkingdoms.data.mappers.toDomainModel
import dev.msuhr.dominionkingdoms.data.mappers.toEntity
import dev.msuhr.dominionkingdoms.model.Kingdom
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
     *
     * Performance Optimization:
     * Instead of querying the database for cards for each kingdom (N+1 queries),
     * we fetch all unique card IDs for the entire list in one go and use a
     * pre-loaded map for mapping.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllKingdoms(): Flow<List<Kingdom>> {
        return combine(
            kingdomDao.getAllKingdomsFlow(),
            refreshTrigger.onStart { emit(Unit) }
        ) { kingdomEntities, _ ->
            kingdomEntities
        }.flatMapLatest { entities ->
            flow {
                if (entities.isEmpty()) {
                    emit(emptyList<Kingdom>())
                    return@flow
                }

                // 1. Collect all unique card IDs across all kingdoms
                val allCardIds = entities.flatMap { it.randomCardIds + it.landscapeCardIds }.toSet()

                // 2. Fetch all cards in one database query
                val cardMap = cardDao.getCardsByIds(allCardIds.toList()).associateBy { it.id }

                // 3. Map entities to domain models using the pre-loaded map
                val domainModels = entities.map { it.toDomainModel(cardMap) }
                emit(domainModels)
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