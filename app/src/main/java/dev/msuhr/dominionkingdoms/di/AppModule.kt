package dev.msuhr.dominionkingdoms.di

import android.app.Application
import androidx.room.Room
import dev.msuhr.dominionkingdoms.R
import dev.msuhr.dominionkingdoms.data.AppDatabase
import dev.msuhr.dominionkingdoms.data.CardDao
import dev.msuhr.dominionkingdoms.data.DatabaseMigrations
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

        return Room.databaseBuilder(
            app.applicationContext,
            AppDatabase::class.java,
            databaseName
        ).addMigrations(DatabaseMigrations.MIGRATION_2_3)
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