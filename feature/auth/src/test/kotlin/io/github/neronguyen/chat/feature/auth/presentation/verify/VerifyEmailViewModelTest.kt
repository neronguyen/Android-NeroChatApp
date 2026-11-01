package io.github.neronguyen.chat.feature.auth.presentation.verify

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
class VerifyEmailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: VerifyEmailViewModel
    private val testEmail = "user@example.com"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = VerifyEmailViewModel(testEmail, fakeRepository)
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
    fun `when code entered, codeState is updated`() {
        viewModel.codeState.setTextAndPlaceCursorAtEnd("123456")
        assertEquals("123456", viewModel.codeState.text.toString())
    }

    @Test
    fun `empty code sets EmptyOtpCode validation error and makes no repository call`() = runTest {
        viewModel.codeState.setTextAndPlaceCursorAtEnd("")
        viewModel.onEvent(VerifyEmailUiEvent.Verify)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, fakeRepository.verifyCallCount)
        assertEquals(AuthValidationError.EmptyOtpCode, viewModel.uiState.value.validationError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `non-6-digit code sets InvalidOtpCode validation error and makes no repository call`() = runTest {
        viewModel.codeState.setTextAndPlaceCursorAtEnd("12345")
        viewModel.onEvent(VerifyEmailUiEvent.Verify)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, fakeRepository.verifyCallCount)
        assertEquals(AuthValidationError.InvalidOtpCode, viewModel.uiState.value.validationError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `valid 6-digit code triggers verify and succeeds`() = runTest {
        viewModel.codeState.setTextAndPlaceCursorAtEnd("123456")
        viewModel.onEvent(VerifyEmailUiEvent.Verify)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.verifyCallCount)
        assertEquals(testEmail, fakeRepository.lastVerifyEmail)
        assertEquals("123456", fakeRepository.lastVerifyCode)
        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertFalse(state.isLoading)
        assertNull(state.authError)
        assertNull(state.validationError)
    }

    @Test
    fun `when onVerifySuccessHandled called, isSuccess is reset to false`() = runTest {
        viewModel.codeState.setTextAndPlaceCursorAtEnd("123456")
        viewModel.onEvent(VerifyEmailUiEvent.Verify)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)

        viewModel.onVerifySuccessHandled()
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun `when verify fails with InvalidOtpCode error, authError is updated`() = runTest {
        fakeRepository.verifyResult = Either.Left(AuthError.InvalidOtpCode)
        viewModel.codeState.setTextAndPlaceCursorAtEnd("123456")
        viewModel.onEvent(VerifyEmailUiEvent.Verify)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.verifyCallCount)
        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertFalse(state.isLoading)
        assertEquals(AuthError.InvalidOtpCode, state.authError)
    }

    @Test
    fun `when verify fails with OtpExpired error, authError is updated`() = runTest {
        fakeRepository.verifyResult = Either.Left(AuthError.OtpExpired)
        viewModel.codeState.setTextAndPlaceCursorAtEnd("123456")
        viewModel.onEvent(VerifyEmailUiEvent.Verify)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertEquals(AuthError.OtpExpired, state.authError)
    }

    @Test
    fun `when verify fails with MaxAttemptsExceeded error, authError is updated`() = runTest {
        fakeRepository.verifyResult = Either.Left(AuthError.MaxAttemptsExceeded)
        viewModel.codeState.setTextAndPlaceCursorAtEnd("123456")
        viewModel.onEvent(VerifyEmailUiEvent.Verify)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertEquals(AuthError.MaxAttemptsExceeded, state.authError)
    }

    @Test
    fun `two immediate verify submissions start only one request`() = runTest {
        val completable = CompletableDeferred<Either<AuthError.VerifyEmail, Unit>>()
        fakeRepository.verifyCompletable = completable

        viewModel.codeState.setTextAndPlaceCursorAtEnd("123456")
        viewModel.onEvent(VerifyEmailUiEvent.Verify)
        viewModel.onEvent(VerifyEmailUiEvent.Verify)

        testDispatcher.scheduler.runCurrent()
        assertEquals(1, fakeRepository.verifyCallCount)
        assertTrue(viewModel.uiState.value.isLoading)

        completable.complete(Either.Right(Unit))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun `resend code triggers repository and starts cooldown countdown`() = runTest {
        viewModel.onEvent(VerifyEmailUiEvent.ResendCode)

        testDispatcher.scheduler.runCurrent()
        assertEquals(1, fakeRepository.resendCallCount)
        assertEquals(testEmail, fakeRepository.lastResendEmail)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isResending)
        assertEquals("Verification code resent", state.infoMessage)
        assertEquals(0, state.resendCooldownSeconds)
    }

    @Test
    fun `resend cooldown prevents subsequent resend attempts until expired`() = runTest {
        viewModel.startCooldown(30)
        assertEquals(30, viewModel.uiState.value.resendCooldownSeconds)

        viewModel.onEvent(VerifyEmailUiEvent.ResendCode)
        testDispatcher.scheduler.runCurrent()

        assertEquals(0, fakeRepository.resendCallCount)

        testDispatcher.scheduler.advanceTimeBy(15_000)
        testDispatcher.scheduler.runCurrent()
        assertEquals(15, viewModel.uiState.value.resendCooldownSeconds)

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.resendCooldownSeconds)

        viewModel.onEvent(VerifyEmailUiEvent.ResendCode)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepository.resendCallCount)
    }

    @Test
    fun `when resend fails, authError is populated`() = runTest {
        fakeRepository.resendResult = Either.Left(DataError.Remote.Unavailable)
        viewModel.onEvent(VerifyEmailUiEvent.ResendCode)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.resendCallCount)
        val state = viewModel.uiState.value
        assertFalse(state.isResending)
        assertEquals(AuthError.Data(DataError.Remote.Unavailable), state.authError)
    }

    @Test
    fun `dismissInfo clears infoMessage`() = runTest {
        viewModel.onEvent(VerifyEmailUiEvent.ResendCode)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Verification code resent", viewModel.uiState.value.infoMessage)

        viewModel.onEvent(VerifyEmailUiEvent.DismissInfo)
        assertNull(viewModel.uiState.value.infoMessage)
    }
}

private class FakeAuthRepository : AuthRepository {
    var verifyCallCount = 0
    var lastVerifyEmail: String? = null
    var lastVerifyCode: String? = null
    var verifyResult: Either<AuthError.VerifyEmail, Unit> = Either.Right(Unit)
    var verifyCompletable: CompletableDeferred<Either<AuthError.VerifyEmail, Unit>>? = null

    var resendCallCount = 0
    var lastResendEmail: String? = null
    var resendResult: Either<DataError, Unit> = Either.Right(Unit)

    override fun getAuthState(): Flow<AuthState> = flowOf(AuthState.Unauthenticated)

    override suspend fun login(email: String, password: String): Either<AuthError.Login, User> =
        Either.Right(User("1", email, "Test User", true))

    override suspend fun register(
        email: String,
        displayName: String,
        password: String,
    ): Either<AuthError.Register, User> =
        Either.Right(User("1", email, displayName, false))

    override suspend fun verifyEmail(
        email: String,
        code: String,
    ): Either<AuthError.VerifyEmail, Unit> {
        verifyCallCount++
        lastVerifyEmail = email
        lastVerifyCode = code
        val completable = verifyCompletable
        return completable?.await() ?: verifyResult
    }

    override suspend fun resendVerification(email: String): Either<DataError, Unit> {
        resendCallCount++
        lastResendEmail = email
        return resendResult
    }

    override suspend fun logout(): Either<DataError, Unit> = Either.Right(Unit)
}
