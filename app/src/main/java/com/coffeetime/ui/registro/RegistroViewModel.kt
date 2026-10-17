package com.coffeetime.ui.registro

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RegistroViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState

    fun onNombreChange(nombre: String) {
        _uiState.value = _uiState.value.copy(nombre = nombre, error = null)
    }

    fun onPinChange(pin: String) {
        if (pin.length <= 4 && pin.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(pin = pin, error = null)
        }
    }

    fun onConfirmarPinChange(confirmarPin: String) {
        if (confirmarPin.length <= 4 && confirmarPin.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(confirmarPin = confirmarPin, error = null)
        }
    }

    fun validarFormulario(): Boolean {
        val state = _uiState.value

        val nombreLimpio = state.nombre.trim()
        if (nombreLimpio.length !in 3..50 || !nombreLimpio.all { it.isLetter() || it.isWhitespace() }) {
            _uiState.value = state.copy(error = "El nombre debe tener entre 3 y 50 letras.")
            return false
        }

        val pinesDebiles = setOf("0000", "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999", "1234", "9876")
        if (state.pin.length != 4 || pinesDebiles.contains(state.pin)) {
            _uiState.value = state.copy(error = "PIN débil o inválido. Evita repetidos o secuencias como 1234.")
            return false
        }

        if (state.pin != state.confirmarPin) {
            _uiState.value = state.copy(error = "Los PINs no coinciden.")
            return false
        }

        return true
    }
}

data class RegistroUiState(
    val nombre: String = "",
    val pin: String = "",
    val confirmarPin: String = "",
    val error: String? = null
)