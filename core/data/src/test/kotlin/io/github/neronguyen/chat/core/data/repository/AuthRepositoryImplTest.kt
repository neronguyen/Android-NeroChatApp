package io.github.neronguyen.chat.core.data.repository

import arrow.core.Either
import io.github.neronguyen.chat.core.datastore.TokenDataSource
import io.github.neronguyen.chat.core.model.AuthState
import io.github.neronguyen.chat.core.model.User
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.AuthNetworkDataSource
import io.github.neronguyen.chat.core.network.model.AuthenticatedUserDto
import io.github.neronguyen.chat.core.network.model.LoginRequest
import io.github.neronguyen.chat.core.network.model.RefreshTokenRequest
import io.github.neronguyen.chat.core.network.model.RegisterRequest
import io.github.neronguyen.chat.core.network.model.UserDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class AuthRepositoryImplTest {

    private class FakeTokenDataSource : TokenDataSource {
        var savedRefreshToken: String? = null
        var savedAccessToken: String? = null
        var savedUser: User? = null
        var clearCalled = false

        override val authState: Flow<AuthState> = emptyFlow()

        override suspend fun getRefreshToken(): String? = savedRefreshToken
        override suspend fun getAccessToken(): String? = savedAccessToken

        override suspend fun saveAuthData(user: User, accessToken: String, refreshToken: String) {
            savedUser = user
            savedAccessToken = accessToken
            savedRefreshToken = refreshToken
        }

        override suspend fun saveTokens(accessToken: String, refreshToken: String) {
            savedAccessToken = accessToken
            savedRefreshToken = refreshToken
        }

        override suspend fun clearAuthData() {
            clearCalled = true
            savedRefreshToken = null
            savedAccessToken = null
            savedUser = null
        }
    }

    private class FakeAuthNetworkDataSource : AuthNetworkDataSource {
        val refreshRequests = mutableListOf<RefreshTokenRequest>()
        var refreshResult: (RefreshTokenRequest) -> Either<DataError.Network, AuthenticatedUserDto> =
            { (reqToken) ->
                Either.Left(DataError.Network.Unknown)
            }
        var refreshDelayMs: Long = 0

        override suspend fun login(request: LoginRequest): Either<DataError.Network, AuthenticatedUserDto> {
            return Either.Left(DataError.Network.Unknown)
        }

        override suspend fun register(request: RegisterRequest): Either<DataError.Network, UserDto> {
            return Either.Left(DataError.Network.Unknown)
        }

        override suspend fun refreshToken(request: RefreshTokenRequest): Either<DataError.Network, AuthenticatedUserDto> {
            synchronized(refreshRequests) {
                refreshRequests.add(request)
            }
            if (refreshDelayMs > 0) {
                delay(refreshDelayMs.milliseconds)
            }
            return refreshResult(request)
        }

        override suspend fun logout(request: RefreshTokenRequest): Either<DataError.Network, Unit> {
            return Either.Right(Unit)
        }
    }

    @Test
    fun refreshToken_noRefreshTokenStored_returnsError() = runTest {
        val tokenDataSource = FakeTokenDataSource()
        val networkDataSource = FakeAuthNetworkDataSource()
        val repository = AuthRepositoryImpl(networkDataSource, tokenDataSource)

        val result = repository.refreshToken("stale_access_token")

        assertTrue(result.isLeft())
        assertEquals(DataError.Network.Unknown, (result as Either.Left).value)
    }

    @Test
    fun refreshToken_staleTokenMatchesCurrent_performsRefreshAndSavesData() = runTest {
        val tokenDataSource = FakeTokenDataSource().apply {
            savedAccessToken = "old_access_token"
            savedRefreshToken = "old_refresh_token"
        }
        val networkDataSource = FakeAuthNetworkDataSource().apply {
            refreshResult = {
                Either.Right(
                    AuthenticatedUserDto(
                        user = UserDto(
                            id = "user_1",
                            email = "user@test.com",
                            displayName = "User One",
                            isEmailVerified = true,
                        ),
                        accessToken = "new_access_token",
                        refreshToken = "new_refresh_token",
                    ),
                )
            }
        }
        val repository = AuthRepositoryImpl(networkDataSource, tokenDataSource)

        val result = repository.refreshToken("old_access_token")

        assertTrue(result.isRight())
        assertEquals("new_access_token", (result as Either.Right).value)
        assertEquals("new_refresh_token", tokenDataSource.savedRefreshToken)
        assertEquals("new_access_token", tokenDataSource.savedAccessToken)
        assertEquals(1, networkDataSource.refreshRequests.size)
    }

    @Test
    fun refreshToken_staleTokenDiffersFromCurrentAccessToken_returnsCurrentAccessTokenWithoutNetworkCall() =
        runTest {
            val tokenDataSource = FakeTokenDataSource().apply {
                savedAccessToken = "already_refreshed_access_token"
                savedRefreshToken = "current_refresh_token"
            }
            val networkDataSource = FakeAuthNetworkDataSource()
            val repository = AuthRepositoryImpl(networkDataSource, tokenDataSource)

            val result = repository.refreshToken("stale_access_token")

            assertTrue(result.isRight())
            assertEquals("already_refreshed_access_token", (result as Either.Right).value)
            // No network call made because current access token already differs from stale token
            assertEquals(0, networkDataSource.refreshRequests.size)
        }

    @Test
    fun refreshToken_concurrentCallsWithSameStaleToken_secondCallerUsesUpdatedAccessToken() =
        runTest {
            val tokenDataSource = FakeTokenDataSource().apply {
                savedAccessToken = "stale_token"
                savedRefreshToken = "refresh_token_1"
            }

            val networkDataSource = FakeAuthNetworkDataSource().apply {
                refreshDelayMs = 50
                refreshResult = {
                    Either.Right(
                        AuthenticatedUserDto(
                            user = UserDto(
                                id = "user_1",
                                email = "user@test.com",
                                displayName = "User One",
                                isEmailVerified = true,
                            ),
                            accessToken = "new_access_token",
                            refreshToken = "refresh_token_2",
                        ),
                    )
                }
            }

            val repository = AuthRepositoryImpl(networkDataSource, tokenDataSource)

            val call1 = async { repository.refreshToken("stale_token") }
            val call2 = async { repository.refreshToken("stale_token") }

            val results = awaitAll(call1, call2)

            assertTrue(results[0].isRight())
            assertTrue(results[1].isRight())
            assertEquals("new_access_token", (results[0] as Either.Right).value)
            assertEquals("new_access_token", (results[1] as Either.Right).value)

            // Only 1 network call was made; second caller acquired lock after call 1 updated access token
            assertEquals(1, networkDataSource.refreshRequests.size)
            assertEquals("refresh_token_2", tokenDataSource.savedRefreshToken)
        }

    @Test
    fun refreshToken_failure_returnsNetworkError() = runTest {
        val tokenDataSource = FakeTokenDataSource().apply {
            savedAccessToken = "stale_token"
            savedRefreshToken = "invalid_refresh_token"
        }
        val networkDataSource = FakeAuthNetworkDataSource().apply {
            refreshResult = { Either.Left(DataError.Network.RequestTimeout) }
        }
        val repository = AuthRepositoryImpl(networkDataSource, tokenDataSource)

        val result = repository.refreshToken("stale_token")

        assertTrue(result.isLeft())
        assertEquals(DataError.Network.RequestTimeout, (result as Either.Left).value)
    }
}
