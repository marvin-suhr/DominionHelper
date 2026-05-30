package dev.msuhr.dominionkingdoms.di

import android.app.Application
import androidx.room.Room
import dev.msuhr.dominionkingdoms.R
import dev.msuhr.dominionkingdoms.data.AppDatabase
import dev.msuhr.dominionkingdoms.data.CardDao
import dev.msuhr.dominionkingdoms.data.ExpansionDao
import dev.msuhr.dominionkingdoms.data.KingdomDao
import dev.msuhr.dominionkingdoms.data.repositories.KingdomRepository
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
        )//.fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCardDao(appDatabase: AppDatabase): CardDao {
        return appDatabase.cardDao()
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

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}