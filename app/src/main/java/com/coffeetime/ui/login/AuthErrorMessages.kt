package com.coffeetime.ui.login

import androidx.annotation.StringRes
import com.coffeetime.R
import com.coffeetime.domain.exception.AuthenticationException
import com.coffeetime.domain.exception.InvalidInputException

/** Texto que ve la persona para un fallo de login o registro. */
@StringRes
fun authErrorMessage(error: Throwable): Int = when (error) {
    is AuthenticationException -> when (error.reason) {
        AuthenticationException.Reason.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
        AuthenticationException.Reason.EMAIL_ALREADY_IN_USE -> R.string.auth_error_email_in_use
        AuthenticationException.Reason.WEAK_PASSWORD -> R.string.auth_error_weak_password
        AuthenticationException.Reason.INVALID_EMAIL -> R.string.auth_error_invalid_email
        AuthenticationException.Reason.USER_DISABLED -> R.string.auth_error_user_disabled
        AuthenticationException.Reason.NETWORK -> R.string.auth_error_network
        AuthenticationException.Reason.UNKNOWN -> R.string.auth_error_unknown
    }
    is InvalidInputException -> R.string.auth_error_invalid_credentials
    else -> R.string.auth_error_unknown
}
