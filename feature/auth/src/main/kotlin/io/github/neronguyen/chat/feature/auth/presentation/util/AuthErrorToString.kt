package io.github.neronguyen.chat.feature.auth.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.neronguyen.chat.core.model.error.AuthError
import io.github.neronguyen.chat.core.ui.asUiText
import io.github.neronguyen.chat.feature.auth.R

@Composable
fun AuthError.Login.asUiText(): String = when (this) {
    AuthError.Login.InvalidCredentials -> stringResource(R.string.error_invalid_credentials)
    AuthError.Login.EmailNotVerified -> stringResource(R.string.error_email_not_verified)
    is AuthError.Login.Data -> error.asUiText()
}

@Composable
fun AuthError.Register.asUiText(): String = when (this) {
    AuthError.Register.EmailAlreadyInUse -> stringResource(R.string.error_email_already_in_use)
    is AuthError.Register.Data -> error.asUiText()
}
