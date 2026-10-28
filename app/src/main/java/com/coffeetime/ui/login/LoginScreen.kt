package com.coffeetime.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeetime.CoffeeTimeApp
import com.coffeetime.R
import com.coffeetime.ui.theme.Size
import com.coffeetime.ui.theme.Space

// Versión mínima con correo y contraseña; el diseño final y Google llegan en #24.
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {

    val app = LocalContext.current
        .applicationContext as CoffeeTimeApp

    val loginViewModel: LoginViewModel = viewModel(
        factory = remember(app) {
            LoginViewModel.factory(
                app.container.authenticationService
            )
        }
    )

    val state = loginViewModel.state

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(Space.s6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = Icons.Filled.LocalCafe,
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(Space.s12 + Space.s6),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(
            modifier = Modifier.height(Space.s4)
        )

        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(Space.s2)
        )

        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(Space.s8)
        )

        OutlinedTextField(
            value = state.email,
            onValueChange = loginViewModel::updateEmail,
            label = {
                Text(stringResource(R.string.auth_email_label))
            },
            singleLine = true,
            enabled = !state.isLoading && !state.isAuthenticated,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(Space.s3)
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = loginViewModel::updatePassword,
            label = {
                Text(stringResource(R.string.auth_password_label))
            },
            singleLine = true,
            enabled = !state.isLoading && !state.isAuthenticated,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(Space.s4)
        )

        state.loginError?.let { error ->
            Text(
                text = stringResource(error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(
                modifier = Modifier.height(Space.s4)
            )
        }

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Size.button)
            )
        } else {
            Button(
                onClick = loginViewModel::login,
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Size.buttonLarge)
            ) {
                Text(stringResource(R.string.login_enter))
            }
        }

        Spacer(
            modifier = Modifier.height(Space.s6)
        )

        TextButton(
            onClick = onCreateAccount,
            enabled = !state.isLoading
        ) {
            Text(
                text = stringResource(R.string.login_create_account)
            )
        }
    }
}
