package io.github.neronguyen.chat.core.network.retrofit

import arrow.core.Either
import arrow.core.raise.either
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.AuthNetworkDataSource
import io.github.neronguyen.chat.core.network.BuildConfig
import io.github.neronguyen.chat.core.network.di.BaseOkHttp
import io.github.neronguyen.chat.core.network.di.RefreshOkHttp
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
    private val networkJson: Json,
    @BaseOkHttp baseClient: dagger.Lazy<OkHttpClient>,
    @RefreshOkHttp refreshClient: dagger.Lazy<OkHttpClient>,
) : AuthNetworkDataSource {

    private val publicApi by lazy { createApi(baseClient.get()) }
    private val refreshApi by lazy { createApi(refreshClient.get()) }

    override suspend fun register(request: RegisterRequest): Either<DataError.Network, UserDto> =
        either { safeCall { publicApi.register(request) } }

    override suspend fun login(request: LoginRequest): Either<DataError.Network, AuthenticatedUserDto> =
        either { safeCall { publicApi.login(request) } }

    override suspend fun refreshToken(request: RefreshTokenRequest): Either<DataError.Network, AuthenticatedUserDto> =
        either { safeCall { refreshApi.refreshToken(request) } }

    override suspend fun logout(request: RefreshTokenRequest): Either<DataError.Network, Unit> =
        either { safeCall { publicApi.logout(request) } }

    private fun createApi(client: OkHttpClient): RetrofitAuthNetworkApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(networkJson.asConverterFactory(contentType))
            .build()
            .create(RetrofitAuthNetworkApi::class.java)
    }
}
