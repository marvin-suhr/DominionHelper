package dev.msuhr.dominionkingdoms.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.msuhr.dominionkingdoms.data.entities.KingdomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KingdomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKingdom(kingdom: KingdomEntity): Long

    @Query("SELECT * FROM kingdoms WHERE uuid = :uuid")
    suspend fun getKingdomById(uuid: String): KingdomEntity?

    @Query("SELECT * FROM kingdoms ORDER BY creationTimeStamp DESC")
    fun getAllKingdomsFlow(): Flow<List<KingdomEntity>>

    @Query("SELECT * FROM kingdoms ORDER BY creationTimeStamp DESC")
    suspend fun getAllKingdoms(): List<KingdomEntity>

    @Query("DELETE FROM kingdoms WHERE uuid = :uuid")
    suspend fun deleteKingdomById(uuid: String)

    @Query("UPDATE kingdoms SET isFavorite = :newIsFavorite WHERE uuid = :uuid")
    suspend fun toggleFavoriteKingdomById(uuid: String, newIsFavorite: Boolean)

    @Query("UPDATE kingdoms SET name = :newName WHERE uuid = :uuid")
    suspend fun changeKingdomName(uuid: String, newName: String)

}