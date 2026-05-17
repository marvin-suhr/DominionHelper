package dev.msuhr.dominionkingdoms.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "kingdoms")
data class KingdomEntity(
    @PrimaryKey val uuid: String = UUID.randomUUID().toString(),
    val creationTimeStamp: Long = System.currentTimeMillis(),
    val randomCardIds: List<Int>,
    val landscapeCardIds: List<Int>,
    val isFavorite: Boolean = false,
    var name: String = "Unnamed Kingdom"
)
