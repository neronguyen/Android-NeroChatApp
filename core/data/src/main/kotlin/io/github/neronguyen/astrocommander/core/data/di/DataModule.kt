package io.github.neronguyen.astrocommander.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.neronguyen.astrocommander.core.data.repository.OfflineFirstPlaceholderRepository
import io.github.neronguyen.astrocommander.core.data.repository.PlaceholderRepository

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindsPlaceholderRepository(
        repository: OfflineFirstPlaceholderRepository
    ): PlaceholderRepository
}
