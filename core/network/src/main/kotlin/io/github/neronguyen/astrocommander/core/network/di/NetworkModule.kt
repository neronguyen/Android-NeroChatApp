package io.github.neronguyen.astrocommander.core.network.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.neronguyen.astrocommander.core.network.AscomNetworkDataSource
import io.github.neronguyen.astrocommander.core.network.retrofit.RetrofitAscomNetworkClient
import jakarta.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import kotlin.time.Duration.Companion.seconds

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    @Provides
    @Singleton
    fun provideNetworkDataSource(
        okHttpClient: OkHttpClient,
        networkJson: Json
    ): AscomNetworkDataSource {
        return RetrofitAscomNetworkClient(
            okHttpClient = okHttpClient,
            networkJson = networkJson
        )
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(okHttpClient))
            }
            .crossfade(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    setLevel(HttpLoggingInterceptor.Level.BODY)
                }
            )
            .callTimeout(15.seconds)
            .build()
    }

    @Provides
    @Singleton
    fun provideNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
    }
}
