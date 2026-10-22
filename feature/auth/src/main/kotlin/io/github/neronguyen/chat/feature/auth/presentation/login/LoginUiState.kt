package io.github.neronguyen.chat.feature.auth.presentation.login

import io.github.neronguyen.chat.core.model.error.AuthError
import io.github.neronguyen.chat.feature.auth.presentation.util.AuthValidationError

data class LoginUiState(
    val isLoading: Boolean = false,
    val validationError: AuthValidationError? = null,
    val authError: AuthError.Login? = null,
    val isSuccess: Boolean = false
)
