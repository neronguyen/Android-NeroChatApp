package io.github.neronguyen.chat.core.datastore.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.neronguyen.chat.core.datastore.PreferencesTokenDataSource
import io.github.neronguyen.chat.core.datastore.TokenDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataStoreModule {

    @Binds
    @Singleton
    internal abstract fun bindTokenDataSource(
        impl: PreferencesTokenDataSource
    ): TokenDataSource
}
