package io.github.neronguyen.chat.feature.auth.presentation.verify

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.neronguyen.chat.core.data.repository.AuthRepository
import io.github.neronguyen.chat.core.model.error.AuthError
import io.github.neronguyenvn.nerochat.contract.validation.EmailValidator
import io.github.neronguyenvn.nerochat.contract.validation.auth.AuthValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = VerifyEmailViewModel.Factory::class)
class VerifyEmailViewModel @AssistedInject constructor(
    @Assisted val email: String,
    private val authRepository: AuthRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(email: String): VerifyEmailViewModel
    }

    val codeState = TextFieldState()

    private val _uiState = MutableStateFlow(VerifyEmailUiState(email = email))
    val uiState: StateFlow<VerifyEmailUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    fun onEvent(event: VerifyEmailUiEvent) {
        when (event) {
            VerifyEmailUiEvent.Verify -> verify()
            VerifyEmailUiEvent.ResendCode -> resendCode()
            VerifyEmailUiEvent.DismissInfo -> dismissInfo()
        }
    }

    fun onVerifySuccessHandled() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    private fun verify() {
        if (_uiState.value.isLoading) return

        val normalizedEmail = EmailValidator.normalize(email)
        val codeText = codeState.text.toString().trim()

        val validationError = AuthValidator.validateVerifyEmail(
            email = normalizedEmail,
            code = codeText,
        ).firstOrNull()

        if (validationError != null) {
            _uiState.update {
                it.copy(
                    validationError = validationError,
                    authError = null,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                validationError = null,
                authError = null,
            )
        }

        viewModelScope.launch {
            authRepository.verifyEmail(
                email = normalizedEmail,
                code = codeText,
            )
                .onLeft { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authError = error,
                        )
                    }
                }
                .onRight {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                        )
                    }
                }
        }
    }

    private fun resendCode() {
        if (_uiState.value.resendCooldownSeconds > 0 || _uiState.value.isResending) return

        _uiState.update { it.copy(isResending = true, authError = null, validationError = null) }

        viewModelScope.launch {
            val normalizedEmail = EmailValidator.normalize(email)
            authRepository.resendVerification(normalizedEmail)
                .onLeft { error ->
                    _uiState.update {
                        it.copy(
                            isResending = false,
                            authError = AuthError.Data(error),
                        )
                    }
                }
                .onRight {
                    _uiState.update {
                        it.copy(
                            isResending = false,
                            infoMessage = "Verification code resent",
                        )
                    }
                    startCooldown(60)
                }
        }
    }

    fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        _uiState.update { it.copy(resendCooldownSeconds = seconds) }
        cooldownJob = viewModelScope.launch {
            for (remaining in seconds - 1 downTo 0) {
                delay(1000)
                _uiState.update { it.copy(resendCooldownSeconds = remaining) }
            }
        }
    }

    private fun dismissInfo() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}
