package com.coffeetime.ui.registro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeetime.R
import com.coffeetime.domain.service.AuthenticationService
import com.coffeetime.ui.login.authErrorMessage
import com.coffeetime.ui.theme.Space
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun RegistroScreen(
    authService: AuthenticationService,
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegistroViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Space.s6),
        verticalArrangement = Arrangement.spacedBy(Space.s4)
    ) {
        Text(
            text = stringResource(R.string.registro_title),
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = state.nombre,
            onValueChange = viewModel::onNombreChange,
            label = { Text(stringResource(R.string.registro_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text(stringResource(R.string.auth_email_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text(stringResource(R.string.registro_password_label)) },
            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = viewModel::togglePasswordVisibility) {
                    Icon(
                        imageVector = if (state.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Mostrar/ocultar contraseña"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.confirmarPassword,
            onValueChange = viewModel::onConfirmarPasswordChange,
            label = { Text(stringResource(R.string.registro_confirm_label)) },
            visualTransformation = if (state.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = viewModel::toggleConfirmPasswordVisibility) {
                    Icon(
                        imageVector = if (state.isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Mostrar/ocultar contraseña"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        state.error?.let {
            Text(text = stringResource(it), color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (viewModel.validarFormulario()) {
                    viewModel.onSubmitting(true)
                    coroutineScope.launch {
                        try {
                            authService.register(
                                name = state.nombre,
                                email = state.email,
                                password = state.password
                            )
                            onRegistered()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            viewModel.onRegisterError(authErrorMessage(e))
                        }
                    }
                }
            },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.registro_submit))
        }

        TextButton(
            onClick = onBack,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_back))
        }
    }
}
