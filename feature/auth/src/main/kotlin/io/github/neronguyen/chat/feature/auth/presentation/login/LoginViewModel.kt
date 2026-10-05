package io.github.neronguyen.chat.feature.auth.presentation.login

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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val emailState = TextFieldState()
    val passwordState = TextFieldState()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            LoginUiEvent.Login -> login()
        }
    }

    private fun login() {
        val emailText = emailState.text.toString().trim()
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
            authRepository.login(
                email = emailText,
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
