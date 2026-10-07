package io.github.neronguyen.chat.feature.auth.presentation.register

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import arrow.core.Either
import io.github.neronguyen.chat.core.data.repository.AuthRepository
import io.github.neronguyen.chat.core.model.AuthState
import io.github.neronguyen.chat.core.model.User
import io.github.neronguyen.chat.core.model.error.DataError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = RegisterViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when fields changed, states are updated`() {
        viewModel.emailState.setTextAndPlaceCursorAtEnd("test@example.com")
        viewModel.displayNameState.setTextAndPlaceCursorAtEnd("Test User")
        viewModel.passwordState.setTextAndPlaceCursorAtEnd("password123")

        assertEquals("test@example.com", viewModel.emailState.text.toString())
        assertEquals("Test User", viewModel.displayNameState.text.toString())
        assertEquals("password123", viewModel.passwordState.text.toString())
    }

    @Test
    fun `when register succeeds, isSuccess is true`() = runTest {
        viewModel.emailState.setTextAndPlaceCursorAtEnd("test@example.com")
        viewModel.displayNameState.setTextAndPlaceCursorAtEnd("Test User")
        viewModel.passwordState.setTextAndPlaceCursorAtEnd("password123")
        viewModel.onEvent(RegisterUiEvent.Register)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertFalse(state.isLoading)
    }
}

private class FakeAuthRepository : AuthRepository {
    override fun getAuthState(): Flow<AuthState> = flowOf(AuthState.Unauthenticated)

    override suspend fun login(
        email: String,
        password: String
    ): Either<DataError.Network, User> {
        val user = User("1", email, "Test User", true)
        return Either.Right(user)
    }

    override suspend fun register(
        email: String,
        displayName: String,
        password: String
    ): Either<DataError.Network, User> {
        val user = User("1", email, displayName, false)
        return Either.Right(user)
    }

    override suspend fun refreshToken(staleToken: String): Either<DataError.Network, String> {
        return Either.Right("new_access_token")
    }

    override suspend fun logout(): Either<DataError.Network, Unit> = Either.Right(Unit)
}
