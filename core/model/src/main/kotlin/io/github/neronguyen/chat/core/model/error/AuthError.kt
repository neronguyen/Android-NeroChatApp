package io.github.neronguyen.chat.core.model.error

sealed interface AuthError {

    sealed interface Login : AuthError
    sealed interface Register : AuthError
    sealed interface VerifyEmail : AuthError
    sealed interface ResetPassword : AuthError

    data object InvalidCredentials : Login
    data object EmailNotVerified : Login
    data object EmailAlreadyInUse : Register
    data object InvalidOtpCode : VerifyEmail, ResetPassword
    data object OtpExpired : VerifyEmail, ResetPassword
    data object MaxAttemptsExceeded : VerifyEmail, ResetPassword
    data object SamePassword : ResetPassword

    data class Data(val error: DataError) : Login, Register, VerifyEmail, ResetPassword
}
