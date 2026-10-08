package io.github.neronguyen.chat.feature.auth.presentation.register

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.neronguyen.chat.core.data.repository.AuthRepository
import io.github.neronguyen.chat.feature.auth.presentation.util.AuthValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val emailState = TextFieldState()
    val displayNameState = TextFieldState()
    val passwordState = TextFieldState()

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEvent(event: RegisterUiEvent) {
        when (event) {
            RegisterUiEvent.Register -> register()
        }
    }

    fun onRegisterSuccessHandled() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    private fun register() {
        val emailText = emailState.text.toString().trim()
        val displayNameText = displayNameState.text.toString().trim()
        val passwordText = passwordState.text.toString()

        if (emailText.isBlank()) {
            _uiState.update {
                it.copy(
                    validationError = AuthValidationError.EmptyEmail,
                    dataError = null
                )
            }
            return
        }
        if (displayNameText.isBlank()) {
            _uiState.update {
                it.copy(
                    validationError = AuthValidationError.EmptyDisplayName,
                    dataError = null
                )
            }
            return
        }
        if (passwordText.isBlank()) {
            _uiState.update {
                it.copy(
                    validationError = AuthValidationError.EmptyPassword,
                    dataError = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, validationError = null, dataError = null) }
            authRepository.register(
                email = emailText,
                displayName = displayNameText,
                password = passwordText
            ).fold(
                ifLeft = { networkError ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            dataError = networkError
                        )
                    }
                },
                ifRight = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true
                        )
                    }
                }
            )
        }
    }
}
