package dev.msuhr.dominionkingdoms.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.msuhr.dominionkingdoms.utils.Converters
import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.model.Expansion
import dev.msuhr.dominionkingdoms.data.entities.KingdomEntity
import dev.msuhr.dominionkingdoms.model.Edition
import dev.msuhr.dominionkingdoms.utils.Constants

@Database(
    entities = [
        Card::class,
        Expansion::class,
        Edition::class,
        KingdomEntity::class
    ],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun expansionDao(): ExpansionDao
    abstract fun kingdomDao(): KingdomDao
}