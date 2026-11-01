package io.github.neronguyen.chat.feature.auth.presentation.reset

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import arrow.core.Either
import io.github.neronguyen.chat.core.data.repository.AuthRepository
import io.github.neronguyen.chat.core.model.AuthState
import io.github.neronguyen.chat.core.model.User
import io.github.neronguyen.chat.core.model.error.AuthError
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyenvn.nerochat.contract.validation.auth.AuthValidationError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResetPasswordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: ResetPasswordViewModel
    private val testEmail = "user@example.com"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = ResetPasswordViewModel(testEmail, fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects email and default values`() {
        val state = viewModel.uiState.value
        assertEquals(testEmail, state.email)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isResending)
        assertEquals(0, state.resendCooldownSeconds)
        assertNull(state.validationError)
        assertNull(state.authError)
        assertNull(state.infoMessage)
    }

    @Test
    fun `empty code sets EmptyOtpCode validation error and makes no repository call`() = runTest {
        viewModel.codeState.setTextAndPlaceCursorAtEnd("")
        viewModel.newPasswordState.setTextAndPlaceCursorAtEnd("ValidPass123")
        viewModel.onEvent(ResetPasswordUiEvent.Reset)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, fakeRepository.resetPasswordCallCount)
        assertEquals(AuthValidationError.EmptyOtpCode, viewModel.uiState.value.validationError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `invalid code sets InvalidOtpCode validation error and makes no repository call`() = runTest {
        viewModel.codeState.setTextAndPlaceCursorAtEnd("123")
        viewModel.newPasswordState.setTextAndPlaceCursorAtEnd("ValidPass123")
        viewModel.onEvent(ResetPasswordUiEvent.Reset)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, fakeRepository.resetPasswordCallCount)
        assertEquals(AuthValidationError.InvalidOtpCode, viewModel.uiState.value.validationError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `empty new password sets EmptyNewPassword validation error`() = runTest {
        viewModel.codeState.setTextAndPlaceCursorAtEnd("123456")
        viewModel.newPasswordState.setTextAndPlaceCursorAtEnd("")
        viewModel.onEvent(ResetPasswordUiEvent.Reset)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, fakeRepository.resetPasswordCallCount)
        assertEquals(AuthValidationError.EmptyNewPassword, viewModel.uiState.value.validationError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `invalid new password sets InvalidNewPassword validation error`() = runTest {
        viewModel.codeState.setTextAndPlaceCursorAtEnd("123456")
        viewModel.newPasswordState.setTextAndPlaceCursorAtEnd("short")
        viewModel.onEvent(ResetPasswordUiEvent.Reset)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, fakeRepository.resetPasswordCallCount)
        assertEquals(AuthValidationError.InvalidNewPassword, viewModel.uiState.value.validationError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `valid inputs trigger reset and update state on success`() = runTest {
        viewModel.codeState.setTextAndPlaceCursorAtEnd("123456")
        viewModel.newPasswordState.setTextAndPlaceCursorAtEnd("ValidPassword123")
        viewModel.onEvent(ResetPasswordUiEvent.Reset)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.resetPasswordCallCount)
        assertEquals(testEmail, fakeRepository.lastResetEmail)
        assertEquals("123456", fakeRepository.lastResetCode)
        assertEquals("ValidPassword123", fakeRepository.lastResetNewPassword)
        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertFalse(state.isLoading)
        assertNull(state.authError)
        assertNull(state.validationError)
    }

    @Test
    fun `when onResetSuccessHandled called, isSuccess is reset to false`() = runTest {
        viewModel.codeState.setTextAndPlaceCursorAtEnd("123456")
        viewModel.newPasswordState.setTextAndPlaceCursorAtEnd("ValidPassword123")
        viewModel.onEvent(ResetPasswordUiEvent.Reset)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)

        viewModel.onResetSuccessHandled()
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun `when reset fails with SamePassword error, authError is updated`() = runTest {
        fakeRepository.resetPasswordResult = Either.Left(AuthError.SamePassword)
        viewModel.codeState.setTextAndPlaceCursorAtEnd("123456")
        viewModel.newPasswordState.setTextAndPlaceCursorAtEnd("ValidPassword123")
        viewModel.onEvent(ResetPasswordUiEvent.Reset)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.resetPasswordCallCount)
        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertEquals(AuthError.SamePassword, state.authError)
    }

    @Test
    fun `resend code triggers repository and starts cooldown countdown`() = runTest {
        viewModel.onEvent(ResetPasswordUiEvent.ResendCode)

        testDispatcher.scheduler.runCurrent()
        assertEquals(1, fakeRepository.forgotPasswordCallCount)
        assertEquals(testEmail, fakeRepository.lastForgotPasswordEmail)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isResending)
        assertEquals("Reset code resent", state.infoMessage)
        assertEquals(0, state.resendCooldownSeconds)
    }

    @Test
    fun `resend cooldown prevents subsequent resend attempts until expired`() = runTest {
        viewModel.startCooldown(30)
        assertEquals(30, viewModel.uiState.value.resendCooldownSeconds)

        viewModel.onEvent(ResetPasswordUiEvent.ResendCode)
        testDispatcher.scheduler.runCurrent()

        assertEquals(0, fakeRepository.forgotPasswordCallCount)

        testDispatcher.scheduler.advanceTimeBy(15_000)
        testDispatcher.scheduler.runCurrent()
        assertEquals(15, viewModel.uiState.value.resendCooldownSeconds)

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.resendCooldownSeconds)

        viewModel.onEvent(ResetPasswordUiEvent.ResendCode)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepository.forgotPasswordCallCount)
    }

    @Test
    fun `dismissInfo clears infoMessage`() = runTest {
        viewModel.onEvent(ResetPasswordUiEvent.ResendCode)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Reset code resent", viewModel.uiState.value.infoMessage)

        viewModel.onEvent(ResetPasswordUiEvent.DismissInfo)
        assertNull(viewModel.uiState.value.infoMessage)
    }
}

private class FakeAuthRepository : AuthRepository {
    var resetPasswordCallCount = 0
    var lastResetEmail: String? = null
    var lastResetCode: String? = null
    var lastResetNewPassword: String? = null
    var resetPasswordResult: Either<AuthError.ResetPassword, Unit> = Either.Right(Unit)

    var forgotPasswordCallCount = 0
    var lastForgotPasswordEmail: String? = null
    var forgotPasswordResult: Either<DataError, Unit> = Either.Right(Unit)

    override fun getAuthState(): Flow<AuthState> = flowOf(AuthState.Unauthenticated)

    override suspend fun login(email: String, password: String): Either<AuthError.Login, User> =
        Either.Right(User("1", email, "User", true))

    override suspend fun register(
        email: String,
        displayName: String,
        password: String,
    ): Either<AuthError.Register, User> =
        Either.Right(User("1", email, displayName, false))

    override suspend fun verifyEmail(email: String, code: String): Either<AuthError.VerifyEmail, Unit> =
        Either.Right(Unit)

    override suspend fun resendVerification(email: String): Either<DataError, Unit> =
        Either.Right(Unit)

    override suspend fun forgotPassword(email: String): Either<DataError, Unit> {
        forgotPasswordCallCount++
        lastForgotPasswordEmail = email
        return forgotPasswordResult
    }

    override suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
    ): Either<AuthError.ResetPassword, Unit> {
        resetPasswordCallCount++
        lastResetEmail = email
        lastResetCode = code
        lastResetNewPassword = newPassword
        return resetPasswordResult
    }

    override suspend fun logout(): Either<DataError, Unit> = Either.Right(Unit)
}
