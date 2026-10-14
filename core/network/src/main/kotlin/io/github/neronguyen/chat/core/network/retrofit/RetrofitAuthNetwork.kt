package io.github.neronguyen.chat.core.network.retrofit

import arrow.core.Either
import arrow.core.raise.either
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.AuthNetworkDataSource
import io.github.neronguyen.chat.core.network.BuildConfig
import io.github.neronguyen.chat.core.network.di.BaseOkHttp
import io.github.neronguyen.chat.core.network.model.AuthenticatedUserDto
import io.github.neronguyen.chat.core.network.model.LoginRequest
import io.github.neronguyen.chat.core.network.model.RefreshTokenRequest
import io.github.neronguyen.chat.core.network.model.RegisterRequest
import io.github.neronguyen.chat.core.network.model.UserDto
import io.github.neronguyen.chat.core.network.util.safeCall
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

private interface RetrofitAuthNetworkApi {

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<UserDto>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthenticatedUserDto>

    @POST("api/auth/refresh-token")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthenticatedUserDto>

    @POST("api/auth/logout")
    suspend fun logout(
        @Body request: RefreshTokenRequest
    ): Response<Unit>
}

@Singleton
internal class RetrofitAuthNetwork @Inject constructor(
    networkJson: Json,
    @BaseOkHttp okHttpClient: dagger.Lazy<OkHttpClient>
) : AuthNetworkDataSource {

    private val networkApi: RetrofitAuthNetworkApi by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .callFactory { okHttpClient.get().newCall(it) }
            .addConverterFactory(networkJson.asConverterFactory(contentType))
            .build()
            .create(RetrofitAuthNetworkApi::class.java)
    }

    override suspend fun register(request: RegisterRequest): Either<DataError.Network, UserDto> =
        either { safeCall { networkApi.register(request) } }

    override suspend fun login(request: LoginRequest): Either<DataError.Network, AuthenticatedUserDto> =
        either { safeCall { networkApi.login(request) } }

    override suspend fun refreshToken(request: RefreshTokenRequest): Either<DataError.Network, AuthenticatedUserDto> =
        either { safeCall { networkApi.refreshToken(request) } }

    override suspend fun logout(request: RefreshTokenRequest): Either<DataError.Network, Unit> =
        either { safeCall { networkApi.logout(request) } }
}
