package io.github.neronguyen.chat.core.network.api

import io.github.neronguyen.chat.core.network.model.AuthenticatedUserDto
import io.github.neronguyen.chat.core.network.model.LoginRequest
import io.github.neronguyen.chat.core.network.model.RefreshTokenRequest
import io.github.neronguyen.chat.core.network.model.RegisterRequest
import io.github.neronguyen.chat.core.network.model.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface RetrofitAuthNetworkApi {

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
