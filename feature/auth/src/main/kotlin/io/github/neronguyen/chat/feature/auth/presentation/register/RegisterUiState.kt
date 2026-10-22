package io.github.neronguyen.chat.feature.auth.presentation.register

import io.github.neronguyen.chat.core.model.error.AuthError
import io.github.neronguyen.chat.feature.auth.presentation.util.AuthValidationError

data class RegisterUiState(
    val isLoading: Boolean = false,
    val validationError: AuthValidationError? = null,
    val authError: AuthError.Register? = null,
    val isSuccess: Boolean = false
)
