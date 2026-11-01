package io.github.neronguyen.chat.feature.auth.presentation.forgot

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.neronguyen.chat.core.data.repository.AuthRepository
import io.github.neronguyen.chat.core.model.error.AuthError
import io.github.neronguyenvn.nerochat.contract.validation.EmailValidator
import io.github.neronguyenvn.nerochat.contract.validation.auth.AuthValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val emailState = TextFieldState()

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEvent(event: ForgotPasswordUiEvent) {
        when (event) {
            ForgotPasswordUiEvent.Submit -> submit()
        }
    }

    fun onForgotPasswordSuccessHandled() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    private fun submit() {
        if (_uiState.value.isLoading) return

        val rawEmail = emailState.text.toString().trim()
        val validationError = when {
            rawEmail.isBlank() -> AuthValidationError.EmptyEmail
            !EmailValidator.isValidEmail(rawEmail) -> AuthValidationError.InvalidEmail
            else -> null
        }

        if (validationError != null) {
            _uiState.update {
                it.copy(
                    validationError = validationError,
                    authError = null,
                )
            }
            return
        }

        val normalizedEmail = EmailValidator.normalize(rawEmail)

        _uiState.update {
            it.copy(
                isLoading = true,
                validationError = null,
                authError = null,
            )
        }

        viewModelScope.launch {
            authRepository.forgotPassword(normalizedEmail)
                .onLeft { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authError = AuthError.Data(error),
                        )
                    }
                }
                .onRight {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            submittedEmail = normalizedEmail,
                        )
                    }
                }
        }
    }
}
