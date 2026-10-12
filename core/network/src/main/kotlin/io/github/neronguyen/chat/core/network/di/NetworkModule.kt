package io.github.neronguyen.chat.core.network.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.neronguyen.chat.core.network.AuthNetworkDataSource
import io.github.neronguyen.chat.core.network.BuildConfig
import io.github.neronguyen.chat.core.network.ChatNetworkDataSource
import io.github.neronguyen.chat.core.network.ChatRealtimeDataSource
import io.github.neronguyen.chat.core.network.interceptor.AuthInterceptor
import io.github.neronguyen.chat.core.network.retrofit.RetrofitAuthNetwork
import io.github.neronguyen.chat.core.network.retrofit.RetrofitChatNetwork
import io.github.neronguyen.chat.core.network.websocket.ChatWebSocketClient
import jakarta.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import kotlin.time.Duration.Companion.seconds

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindAuthNetworkDataSource(
        impl: RetrofitAuthNetwork
    ): AuthNetworkDataSource

    @Binds
    @Singleton
    abstract fun bindChatNetworkDataSource(
        impl: RetrofitChatNetwork
    ): ChatNetworkDataSource

    @Binds
    @Singleton
    abstract fun bindChatRealtimeDataSource(
        impl: ChatWebSocketClient
    ): ChatRealtimeDataSource

    internal companion object {

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
        fun provideOkHttpClient(
            authInterceptor: AuthInterceptor
        ): OkHttpClient {
            return OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        if (BuildConfig.DEBUG) setLevel(HttpLoggingInterceptor.Level.BODY)
                        else setLevel(HttpLoggingInterceptor.Level.NONE)
                    }
                )
                .callTimeout(15.seconds)
                .pingInterval(30.seconds)
                .build()
        }

        @Provides
        @Singleton
        fun provideNetworkJson(): Json = Json {
            ignoreUnknownKeys = true
        }
    }
}
