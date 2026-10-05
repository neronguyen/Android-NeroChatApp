package io.github.neronguyen.astrocommander.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.neronguyen.astrocommander.core.database.AscomDatabase
import io.github.neronguyen.astrocommander.core.database.dao.PlaceholderJsonDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    fun providePlaceholderJsonDao(database: AscomDatabase): PlaceholderJsonDao {
        return database.placeholderJsonDao()
    }

    @Provides
    @Singleton
    fun provideAscomDatabase(
        @ApplicationContext context: Context
    ): AscomDatabase {
        return Room.databaseBuilder(
            context,
            AscomDatabase::class.java,
            "ascom-database",
        ).build()
    }
}
