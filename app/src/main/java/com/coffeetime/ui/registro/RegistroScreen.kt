package com.coffeetime.ui.registro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.confirmarPassword,
            onValueChange = viewModel::onConfirmarPasswordChange,
            label = { Text(stringResource(R.string.registro_confirm_label)) },
            visualTransformation = PasswordVisualTransformation(),
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
