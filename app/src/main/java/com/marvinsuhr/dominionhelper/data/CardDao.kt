package com.marvinsuhr.dominionhelper.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.marvinsuhr.dominionhelper.model.Card
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<Card>)

    @Delete
    suspend fun delete(card: Card)

    @Update
    suspend fun update(card: Card)

    /*@Transaction
    @Query("SELECT * FROM cards WHERE id IN (SELECT cardId FROM card_category_cross_ref WHERE categoryId = :categoryId)")
    suspend fun getCardsByCategory(categoryId: Int): List<CardWithCategories>*/

    @Query("SELECT * FROM cards")
    suspend fun getAll(): List<Card>

    @Query(
        """
            SELECT * FROM cards
            WHERE supply = 1
            ORDER BY RANDOM() LIMIT :count
            """
    )
    suspend fun getRandomSupplyCards(count: Int): List<Card>

    @Query(
        """
    SELECT * FROM cards
    WHERE name LIKE '%' || :filter || '%'
       OR sets LIKE '%' || :filter || '%'
       OR types LIKE '%' || :filter || '%'
       OR CAST(cost AS TEXT) LIKE '%' || :filter || '%'
    """
    )
    suspend fun getFilteredCards(filter: String): List<Card>

    @Query("SELECT * FROM cards WHERE sets LIKE '%' || :id || '%'")
    suspend fun getCardsByExpansion(id: String): List<Card>


    @Query(
        """
        SELECT * FROM cards AS c
        WHERE sets LIKE '%' || :id || '%'
        AND c.isEnabled = 1
        AND c.basic = 0
        AND c.landscape = 0
        AND c.supply = 1
        """
    )
    suspend fun getPortraitsByExpansion(id: String): List<Card>

    @Query(
        """
        SELECT * FROM cards AS c
        WHERE sets LIKE '%' || :id || '%'
        AND c.landscape = 1
        AND c.isEnabled = 1
        """
    )
    suspend fun getLandscapesByExpansion(id: String): List<Card>

    @Query("SELECT * FROM cards ORDER BY RANDOM() LIMIT :amount")
    suspend fun getRandomCards(amount: Int): List<Card>

    @Query(
        """
        SELECT c.* FROM cards AS c
        INNER JOIN expansions AS e ON c.sets LIKE '%' || e.id || '%'
        WHERE e.isOwned
        AND c.isEnabled = 1
        AND c.landscape = 0
        AND c.basic = 0
        AND c.supply = 1
        ORDER BY RANDOM()
        LIMIT :amount
    """
    )
    suspend fun getRandomCardsFromOwnedExpansions(amount: Int): List<Card>

    @Query(
        """
        SELECT c.* FROM cards AS c
        INNER JOIN expansions AS e ON c.sets LIKE '%' || e.id || '%'
        WHERE e.isOwned
        AND c.isEnabled = 1
        AND c.landscape = 1
        AND c.basic = 0
        AND c.supply = 1
        ORDER BY RANDOM()
        LIMIT :amount
    """
    )
    suspend fun getRandomLandscapeCardsFromOwnedExpansions(amount: Int): List<Card>

    @Query(
        """
        SELECT c.* FROM cards AS c
        WHERE c.sets LIKE '%' || :expansionId || '%'
        AND c.isEnabled = 1
        AND c.landscape = 0
        AND c.basic = 0
        AND c.supply = 1
        ORDER BY RANDOM()
        LIMIT :amount
    """
    )
    suspend fun getRandomCardsFromExpansion(expansionId: String, amount: Int): List<Card>

    @Query(
        """
        SELECT c.* FROM cards AS c
        INNER JOIN expansions AS e ON c.sets LIKE '%' || e.id || '%'
        WHERE e.isOwned
        AND c.isEnabled = 1
        AND c.landscape = 0
        AND c.basic = 0
        AND c.supply = 1
        ORDER BY RANDOM()
    """
    )
    suspend fun getEnabledOwnedCards(): List<Card>

    @Query(
        """
        SELECT c.* FROM cards AS c
        INNER JOIN expansions AS e ON c.sets LIKE '%' || e.id || '%'
        WHERE e.isOwned
        AND c.isEnabled = 1
        AND c.landscape = 1
        AND c.basic = 0
        AND c.supply = 1 -- TODO Huh, are landscape cards considered in the supply?
        ORDER BY RANDOM()
    """
    )
    suspend fun getEnabledOwnedLandscapes(): List<Card>

    @Query(
        """
        SELECT c.* FROM cards AS c
        INNER JOIN expansions AS e ON c.sets LIKE '%' || e.id || '%'
        WHERE e.isOwned
        AND c.isEnabled = 1
        AND c.landscape = :isLandscape
        AND c.basic = 0
        AND c.supply = 1
        AND c.id NOT IN (:excludedCards)
        ORDER BY RANDOM()
        LIMIT 1
    """
    )
    suspend fun getSingleCardFromOwnedExpansionsWithExceptions(
        excludedCards: Set<Int>,
        isLandscape: Boolean
    ): Card?

    @Query(
        """
        SELECT c.* FROM cards AS c
        INNER JOIN expansions AS e ON c.sets LIKE '%' || e.id || '%'
        WHERE
            ( 
              (:set1 IS NOT NULL AND sets LIKE '%' || :set1 || '%') OR
              (:set2 IS NOT NULL AND sets LIKE '%' || :set2 || '%')
            )
        AND e.isOwned
        AND c.isEnabled = 1
        AND c.landscape = :isLandscape
        AND c.basic = 0
        AND c.supply = 1
        AND c.id NOT IN (:excludedCards)
        ORDER BY RANDOM()
        LIMIT 1
    """
    )
    suspend fun getSingleCardFromExpansionWithExceptions(
        set1: String,
        set2: String?,
        excludedCards: Set<Int>,
        isLandscape: Boolean
    ): Card?

    @Query("UPDATE cards SET isFavorite = :isFavorite WHERE id = :cardId")
    suspend fun toggleCardFavorite(cardId: Int, isFavorite: Boolean)

    @Query("UPDATE cards SET isEnabled = :isEnabled WHERE id = :cardId")
    suspend fun toggleCardEnabled(cardId: Int, isEnabled: Boolean)

    @Query("SELECT COUNT(*) FROM cards WHERE sets LIKE '%' || :expansionId || '%' AND basic = 0")
    suspend fun getTotalCardAmountForExpansion(expansionId: String): Int

    // AND supply = 1?
    @Query("SELECT COUNT(*) FROM cards WHERE sets LIKE '%' || :expansionId || '%' AND isEnabled = 1 AND basic = 0")
    suspend fun getEnabledCardAmountForExpansion(expansionId: String): Int

    @Query("SELECT * FROM cards WHERE name = :name")
    suspend fun getCardByName(name: String): Card?

    @Query("SELECT * FROM cards WHERE name IN (:names)")
    suspend fun getCardsByNameList(names: List<String>): List<Card>

    @Query("SELECT * FROM cards WHERE id IN (:ids)")
    suspend fun getCardsByIds(ids: List<Int>): List<Card>

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun count(): Int

    @Query("SELECT * FROM cards WHERE isEnabled = 0")
    suspend fun getDisabledCards(): List<Card>

    @Query("SELECT COUNT(*) FROM cards WHERE isEnabled = 0")
    fun getDisabledCardCount(): Flow<Int>

    @Query("SELECT * FROM cards WHERE isFavorite = 1")
    suspend fun getFavoriteCards(): List<Card>

    @Query("SELECT COUNT(*) FROM cards WHERE isFavorite = 1")
    fun getFavoriteCardCount(): Flow<Int>
}