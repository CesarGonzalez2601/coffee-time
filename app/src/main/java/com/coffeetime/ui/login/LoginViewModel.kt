package com.coffeetime.ui.login

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeetime.domain.service.AuthenticationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    @StringRes val loginError: Int? = null,
    val isAuthenticated: Boolean = false,
    val showForgotPasswordDialog: Boolean = false,
    val forgotPasswordEmail: String = "",
    val isSendingReset: Boolean = false,
    val resetSentSuccess: Boolean = false,
    @StringRes val resetError: Int? = null
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotEmpty() && !isLoading && !isAuthenticated
}

class LoginViewModel(
    private val auth: AuthenticationService
) : ViewModel() {

    var state by mutableStateOf(LoginUiState())
        private set

    fun updateEmail(value: String) {
        state = state.copy(
            email = value,
            loginError = null
        )
    }

    fun updatePassword(value: String) {
        state = state.copy(
            password = value,
            loginError = null
        )
    }

    fun togglePasswordVisibility() {
        state = state.copy(isPasswordVisible = !state.isPasswordVisible)
    }

    fun openForgotPasswordDialog() {
        state = state.copy(
            showForgotPasswordDialog = true,
            forgotPasswordEmail = state.email,
            resetSentSuccess = false,
            resetError = null
        )
    }

    fun closeForgotPasswordDialog() {
        state = state.copy(
            showForgotPasswordDialog = false,
            resetSentSuccess = false,
            resetError = null
        )
    }

    fun updateForgotPasswordEmail(value: String) {
        state = state.copy(
            forgotPasswordEmail = value,
            resetError = null
        )
    }

    fun sendPasswordReset() {
        val targetEmail = state.forgotPasswordEmail.trim()
        if (targetEmail.isBlank()) return

        state = state.copy(isSendingReset = true, resetError = null)

        viewModelScope.launch {
            try {
                auth.sendPasswordReset(targetEmail)
                state = state.copy(
                    isSendingReset = false,
                    resetSentSuccess = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(
                    isSendingReset = false,
                    resetError = authErrorMessage(e)
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        if (state.isLoading) return

        state = state.copy(
            isLoading = true,
            loginError = null
        )

        viewModelScope.launch {
            try {
                auth.loginWithGoogle(idToken)
                state = state.copy(
                    isLoading = false,
                    isAuthenticated = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    loginError = authErrorMessage(e)
                )
            }
        }
    }

    fun login() {
        if (!state.canSubmit) return

        state = state.copy(
            isLoading = true,
            loginError = null
        )

        viewModelScope.launch {
            try {
                auth.login(
                    email = state.email,
                    password = state.password
                )

                state = state.copy(
                    isLoading = false,
                    password = "",
                    isAuthenticated = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    loginError = authErrorMessage(e)
                )
            }
        }
    }

    companion object {
        fun factory(
            auth: AuthenticationService
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    require(
                        modelClass.isAssignableFrom(
                            LoginViewModel::class.java
                        )
                    )
                    return LoginViewModel(auth) as T
                }
            }
    }
}
