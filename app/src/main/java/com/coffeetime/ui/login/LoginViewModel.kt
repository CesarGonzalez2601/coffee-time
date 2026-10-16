
package com.coffeetime.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coffeetime.domain.service.AuthenticationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LoginUiState(
    val userId: String = "",
    val pinLength: Int = 0,
    val isLoading: Boolean = false,
    val idError: String? = null,
    val loginError: String? = null,
    val errorAnimationId: Int = 0,
    val isAuthenticated: Boolean = false
)

class LoginViewModel(
    private val auth: AuthenticationService
) : ViewModel() {

    var state by mutableStateOf(LoginUiState())
        private set

    // El PIN no se almacena en el estado de la pantalla.
    private var pin = ""

    fun updateId(value: String) {
        if (state.isLoading || state.isAuthenticated) return

        val digits = value.filter { it in '0'..'9' }

        state = state.copy(
            userId = digits,
            idError = if (value != digits) {
                "El ID debe contener solo números"
            } else {
                null
            },
            loginError = null
        )
    }

    fun inputDigit(digit: Int) {
        if (
            state.isLoading ||
            state.isAuthenticated ||
            digit !in 0..9 ||
            pin.length >= 4
        ) return

        val id = state.userId.toIntOrNull()

        if (id == null || id <= 0) {
            state = state.copy(
                idError = "Ingresa un ID numérico válido"
            )
            return
        }

        pin += digit.toString()

        state = state.copy(
            pinLength = pin.length,
            idError = null,
            loginError = null
        )

        if (pin.length == 4) {
            authenticate()
        }
    }

    fun deleteDigit() {
        if (
            state.isLoading ||
            state.isAuthenticated ||
            pin.isEmpty()
        ) return

        pin = pin.dropLast(1)

        state = state.copy(
            pinLength = pin.length,
            loginError = null
        )
    }

    private fun authenticate() {
        val id = state.userId.toIntOrNull() ?: return
        val submittedPin = pin

        state = state.copy(
            isLoading = true,
            loginError = null
        )

        viewModelScope.launch {
            try {
                auth.login(id, submittedPin)

                pin = ""

                state = state.copy(
                    isLoading = false,
                    pinLength = 0,
                    isAuthenticated = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    loginError = "ID o PIN incorrectos",
                    errorAnimationId =
                        state.errorAnimationId + 1
                )

                delay(300)

                pin = ""

                state = state.copy(
                    pinLength = 0
                )
            }
        }
    }

    override fun onCleared() {
        pin = ""
        super.onCleared()
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
