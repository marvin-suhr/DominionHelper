package com.marvinsuhr.dominionhelper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.marvinsuhr.dominionhelper.model.Expansion
import com.marvinsuhr.dominionhelper.model.Set
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpansionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expansions: List<Expansion>)

    @Query("SELECT * FROM expansions")
    fun getAll(): Flow<List<Expansion>>

    @Query("""
        SELECT * FROM expansions
        WHERE isOwned = 1
        ORDER BY RANDOM()
        LIMIT :count
    """)
    suspend fun getFixedAmountOfOwnedExpansions(count: Int): List<Expansion>

    @Query("""
        SELECT * FROM expansions 
        WHERE isOwned = 1
        AND :cardSets LIKE '%' || id || '%'
    """)
    suspend fun getSetsOfCard(cardSets: List<Set>): List<Expansion>

    // Turning this into a suspend fun crashes??
    @Query("SELECT * FROM expansions WHERE isOwned = 1")
    fun getOwned(): Flow<List<Expansion>>

    @Query("SELECT * FROM expansions WHERE isOwned = 1")
    suspend fun getOwnedOnce(): List<Expansion>

    @Query("SELECT * FROM expansions WHERE id = :id")
    suspend fun getExpansionById(id: Int): Expansion?

    @Query("UPDATE expansions SET isOwned = :isOwned WHERE name = :expansionName AND edition = 1")
    suspend fun updateFirstEditionOwned(expansionName: String, isOwned: Boolean)

    @Query("UPDATE expansions SET isOwned = :isOwned WHERE name = :expansionName AND edition = 2")
    suspend fun updateSecondEditionOwned(expansionName: String, isOwned: Boolean)

    @Query("SELECT isOwned FROM expansions WHERE name = :expansionName AND edition = 1")
    suspend fun isFirstEditionOwned(expansionName: String): Boolean

    @Query("SELECT isOwned FROM expansions WHERE name = :expansionName AND edition = 2")
    suspend fun isSecondEditionOwned(expansionName: String): Boolean

    @Query("SELECT COUNT(*) FROM expansions")
    suspend fun count(): Int

    @Query("SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM expansions WHERE isOwned = 1")
    fun hasAnyOwnedExpansion(): Flow<Boolean>
}