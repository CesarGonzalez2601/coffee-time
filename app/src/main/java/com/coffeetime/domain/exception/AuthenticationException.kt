package com.coffeetime.domain.exception

/** Fallo de inicio de sesión o registro. La UI muestra un texto según [reason]. */
class AuthenticationException(
    val reason: Reason,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    enum class Reason {
        INVALID_CREDENTIALS,
        EMAIL_ALREADY_IN_USE,
        WEAK_PASSWORD,
        INVALID_EMAIL,
        USER_DISABLED,
        NETWORK,
        UNKNOWN
    }
}
