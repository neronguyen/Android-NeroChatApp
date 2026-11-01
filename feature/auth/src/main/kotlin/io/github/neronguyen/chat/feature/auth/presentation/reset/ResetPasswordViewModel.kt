package io.github.neronguyen.chat.feature.auth.presentation.reset

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

@HiltViewModel(assistedFactory = ResetPasswordViewModel.Factory::class)
class ResetPasswordViewModel @AssistedInject constructor(
    @Assisted val email: String,
    private val authRepository: AuthRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(email: String): ResetPasswordViewModel
    }

    val codeState = TextFieldState()
    val newPasswordState = TextFieldState()

    private val _uiState = MutableStateFlow(ResetPasswordUiState(email = email))
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    fun onEvent(event: ResetPasswordUiEvent) {
        when (event) {
            ResetPasswordUiEvent.Reset -> reset()
            ResetPasswordUiEvent.ResendCode -> resendCode()
            ResetPasswordUiEvent.DismissInfo -> dismissInfo()
        }
    }

    fun onResetSuccessHandled() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    private fun reset() {
        if (_uiState.value.isLoading) return

        val normalizedEmail = EmailValidator.normalize(email)
        val codeText = codeState.text.toString().trim()
        val newPasswordText = newPasswordState.text.toString()

        val validationError = AuthValidator.validateResetPassword(
            email = normalizedEmail,
            code = codeText,
            newPassword = newPasswordText,
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
            authRepository.resetPassword(
                email = normalizedEmail,
                code = codeText,
                newPassword = newPasswordText,
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
            authRepository.forgotPassword(normalizedEmail)
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
                            infoMessage = "Reset code resent",
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
