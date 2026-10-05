package io.github.neronguyen.chat.core.security.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.neronguyen.chat.core.security.AndroidKeyStoreCryptoManager
import io.github.neronguyen.chat.core.security.CryptoManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SecurityModule {

    @Binds
    @Singleton
    internal abstract fun bindCryptoManager(
        impl: AndroidKeyStoreCryptoManager
    ): CryptoManager
}
