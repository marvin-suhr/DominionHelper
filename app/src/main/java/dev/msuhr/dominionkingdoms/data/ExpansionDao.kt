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

    @Query("UPDATE editions SET isOwned = :isOwned WHERE id = :editionId")
    suspend fun updateEditionOwnership(editionId: String, isOwned: Boolean)

    @Query("SELECT COUNT(*) FROM expansions")
    suspend fun countExpansions(): Int

    @Query("SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM editions WHERE isOwned = 1")
    fun hasAnyOwnedEdition(): Flow<Boolean>

    @Transaction
    suspend fun getOwnedExpansionsWithEditions(): List<ExpansionWithEditions> {
        return getAllWithEditionsOnce().filter { it.isAnyOwned() }
    }
}
