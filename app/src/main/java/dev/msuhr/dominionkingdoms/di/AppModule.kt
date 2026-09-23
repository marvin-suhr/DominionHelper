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
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.msuhr.dominionkingdoms.CardDependencyResolver
import dev.msuhr.dominionkingdoms.KingdomGenerator
import dev.msuhr.dominionkingdoms.data.CardDataUpdater
import dev.msuhr.dominionkingdoms.data.CardDataSource
import dev.msuhr.dominionkingdoms.data.ExpansionDataSource
import dev.msuhr.dominionkingdoms.data.UserPrefsSource
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

    // Domain classes moved to the shared KMP module: their constructors are no
    // longer @Inject-annotated, so they are provided here.

    @Provides
    @Singleton
    fun provideCardDependencyResolver(
        cardDao: CardDao,
        userPrefsRepository: UserPrefsRepository
    ): CardDependencyResolver {
        return CardDependencyResolver(cardDao, userPrefsRepository)
    }

    @Provides
    @Singleton
    fun provideKingdomGenerator(
        cardDao: CardDao,
        expansionDao: ExpansionDao,
        userPrefsRepository: UserPrefsRepository,
        cardDependencyResolver: CardDependencyResolver
    ): KingdomGenerator {
        return KingdomGenerator(cardDao, expansionDao, userPrefsRepository, cardDependencyResolver)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    abstract fun bindCardDataSource(impl: CardDao): CardDataSource

    @Binds
    abstract fun bindExpansionDataSource(impl: ExpansionDao): ExpansionDataSource

    @Binds
    abstract fun bindUserPrefsSource(impl: UserPrefsRepository): UserPrefsSource
}