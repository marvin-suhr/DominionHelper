package dev.msuhr.dominionkingdoms.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.msuhr.dominionkingdoms.model.Edition
import dev.msuhr.dominionkingdoms.model.Expansion
import dev.msuhr.dominionkingdoms.model.ExpansionWithEditions
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpansionDao {

    @Transaction
    suspend fun insertAll(expansions: List<Expansion>, editions: List<Edition>) {
        insertExpansions(expansions)
        insertEditions(editions)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpansions(expansions: List<Expansion>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEditions(editions: List<Edition>)

    @Transaction
    @Query("SELECT * FROM expansions")
    fun getAllWithEditions(): Flow<List<ExpansionWithEditions>>

    @Transaction
    @Query("SELECT * FROM expansions")
    suspend fun getAllWithEditionsOnce(): List<ExpansionWithEditions>

    @Query("SELECT * FROM expansions WHERE id = :id")
    suspend fun getExpansionById(id: String): Expansion?

    @Query("SELECT * FROM editions WHERE id = :id")
    suspend fun getEditionById(id: String): Edition?

    @Query("UPDATE editions SET isOwned = :isOwned WHERE id = :editionId")
    suspend fun updateEditionOwnership(editionId: String, isOwned: Boolean)

    @Query("SELECT isOwned FROM editions WHERE expansionId = :expansionId AND editionNumber = :editionNumber")
    suspend fun isEditionOwned(expansionId: String, editionNumber: Int): Boolean

    @Query("SELECT COUNT(*) FROM editions")
    suspend fun countEditions(): Int

    @Query("SELECT COUNT(*) FROM expansions")
    suspend fun countExpansions(): Int

    @Query("SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM editions WHERE isOwned = 1")
    fun hasAnyOwnedEdition(): Flow<Boolean>

    /**
     * Picks a fixed amount of unique expansions (concepts) that have at least one owned edition.
     */
    @Transaction
    suspend fun getFixedAmountOfOwnedExpansions(count: Int): List<ExpansionWithEditions> {
        val allOwned = getAllWithEditionsOnce().filter { it.isAnyOwned() }
        return allOwned.shuffled().take(count)
    }

    @Transaction
    suspend fun getOwnedExpansionsWithEditions(): List<ExpansionWithEditions> {
        return getAllWithEditionsOnce().filter { it.isAnyOwned() }
    }
}
