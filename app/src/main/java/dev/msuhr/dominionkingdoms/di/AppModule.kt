package dev.msuhr.dominionkingdoms.di

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.msuhr.dominionkingdoms.R
import dev.msuhr.dominionkingdoms.data.AppDatabase
import dev.msuhr.dominionkingdoms.data.CardDao
import dev.msuhr.dominionkingdoms.data.ExpansionDao
import dev.msuhr.dominionkingdoms.data.KingdomDao
import dev.msuhr.dominionkingdoms.data.UserPrefsRepository
import dev.msuhr.dominionkingdoms.data.repositories.KingdomRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.msuhr.dominionkingdoms.data.CardDataUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob())
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        app: Application
    ): AppDatabase {
        val databaseName = app.getString(R.string.database_name)

        // Migration from version 2 to 3: Drop and recreate cards table
        // This preserves expansions (with ownership state) and generated kingdoms
        val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop the old cards table
                db.execSQL("DROP TABLE IF EXISTS cards")

                // Recreate the cards table with the new schema
                // id is now a primary key from JSON instead of auto-generated
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cards (
                        id INTEGER PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        imageName TEXT NOT NULL,
                        sets TEXT NOT NULL,
                        types TEXT NOT NULL,
                        categories TEXT NOT NULL,
                        cost INTEGER,
                        overpay INTEGER NOT NULL,
                        specialCost INTEGER NOT NULL,
                        potion INTEGER NOT NULL,
                        debt INTEGER NOT NULL,
                        supply INTEGER NOT NULL,
                        landscape INTEGER NOT NULL,
                        basic INTEGER NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        isFavorite INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        return Room.databaseBuilder(
            app.applicationContext,
            AppDatabase::class.java,
            databaseName
        ).addMigrations(migration2To3)
            .build()
    }

    @Provides
    @Singleton
    fun provideCardDataUpdater(
        app: Application,
        userPrefsRepository: UserPrefsRepository,
        cardDao: CardDao
    ): CardDataUpdater {
        return CardDataUpdater(app.applicationContext, userPrefsRepository, cardDao)
    }

    @Provides
    @Singleton
    fun provideCardDao(database: AppDatabase): CardDao {
        return database.cardDao() // Replace with the abstract DAO getter method inside your AppDatabase class
    }

    @Provides
    fun provideExpansionDao(appDatabase: AppDatabase): ExpansionDao {
        return appDatabase.expansionDao()
    }

    @Provides
    fun provideKingdomDao(appDatabase: AppDatabase): KingdomDao {
        return appDatabase.kingdomDao()
    }

    @Provides
    @Singleton
    fun provideKingdomRepository(
        kingdomDao: KingdomDao,
        cardDao: CardDao
    ): KingdomRepository {
        return KingdomRepository(kingdomDao, cardDao, Dispatchers.IO)
    }
}