package dev.msuhr.dominionkingdoms.data.mappers

import dev.msuhr.dominionkingdoms.data.CardDao
import dev.msuhr.dominionkingdoms.data.entities.KingdomEntity
import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.model.Kingdom

// Convert Kingdom to KingdomEntity
fun Kingdom.toEntity(): KingdomEntity {
    return KingdomEntity(
        uuid = this.uuid,
        randomCardIds = this.randomCards.keys.map { it.id },
        landscapeCardIds = this.landscapeCards.keys.map { it.id },
        isFavorite = this.isFavorite,
        creationTimeStamp = this.creationTimeStamp,
        name = this.name
    )
}

// Convert KingdomEntity to Kingdom
suspend fun KingdomEntity.toDomainModel(cardDao: CardDao): Kingdom {
    val randomCardObjects = cardDao.getCardsByIds(this.randomCardIds)
    val landscapeCardObjects = cardDao.getCardsByIds(this.landscapeCardIds)

    // Reconstruct the LinkedHashMaps, default to count = 1
    val randomCardsMap = LinkedHashMap<Card, Int>()
    randomCardObjects.forEach { randomCardsMap[it] = 1 }

    val landscapeCardsMap = LinkedHashMap<Card, Int>()
    landscapeCardObjects.forEach { landscapeCardsMap[it] = 1 }

    return Kingdom(
        randomCards = randomCardsMap,
        landscapeCards = landscapeCardsMap,
        uuid = this.uuid,
        isFavorite = this.isFavorite,
        creationTimeStamp = this.creationTimeStamp,
        name = this.name
    )
}
