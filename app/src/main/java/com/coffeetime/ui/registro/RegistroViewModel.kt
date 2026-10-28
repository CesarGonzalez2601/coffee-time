package com.coffeetime.ui.registro

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.coffeetime.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RegistroViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState

    fun onNombreChange(nombre: String) {
        _uiState.value = _uiState.value.copy(nombre = nombre, error = null)
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun onConfirmarPasswordChange(confirmarPassword: String) {
        _uiState.value = _uiState.value.copy(confirmarPassword = confirmarPassword, error = null)
    }

    fun onSubmitting(isSubmitting: Boolean) {
        _uiState.value = _uiState.value.copy(isSubmitting = isSubmitting)
    }

    fun onRegisterError(@StringRes error: Int) {
        _uiState.value = _uiState.value.copy(isSubmitting = false, error = error)
    }

    // Validación mínima; las reglas finales (8+ caracteres, pruebas) llegan en #24.
    fun validarFormulario(): Boolean {
        val state = _uiState.value

        val nombreLimpio = state.nombre.trim()
        if (nombreLimpio.length !in 3..50 || !nombreLimpio.all { it.isLetter() || it.isWhitespace() }) {
            _uiState.value = state.copy(error = R.string.registro_error_nombre)
            return false
        }

        if (!EMAIL_FORMAT.matches(state.email.trim())) {
            _uiState.value = state.copy(error = R.string.registro_error_email)
            return false
        }

        if (state.password.length < MIN_PASSWORD_LENGTH) {
            _uiState.value = state.copy(error = R.string.auth_error_weak_password)
            return false
        }

        if (state.password != state.confirmarPassword) {
            _uiState.value = state.copy(error = R.string.registro_error_confirm)
            return false
        }

        return true
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
        val EMAIL_FORMAT = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
    }
}

data class RegistroUiState(
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val confirmarPassword: String = "",
    val isSubmitting: Boolean = false,
    @StringRes val error: Int? = null
)
