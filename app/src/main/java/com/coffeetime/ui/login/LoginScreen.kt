package com.coffeetime.ui.login

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeetime.CoffeeTimeApp
import com.coffeetime.R
import com.coffeetime.ui.theme.Size
import com.coffeetime.ui.theme.Space
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

private const val WEB_CLIENT_ID = "303089730950-7qr98ictsn32ri3glh3v3quvauncg65f.apps.googleusercontent.com"

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as CoffeeTimeApp
    val coroutineScope = rememberCoroutineScope()

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

    if (state.showForgotPasswordDialog) {
        ForgotPasswordDialog(
            email = state.forgotPasswordEmail,
            isSending = state.isSendingReset,
            isSuccess = state.resetSentSuccess,
            errorRes = state.resetError,
            onEmailChanged = loginViewModel::updateForgotPasswordEmail,
            onSendReset = loginViewModel::sendPasswordReset,
            onDismiss = loginViewModel::closeForgotPasswordDialog
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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

        Spacer(modifier = Modifier.height(Space.s4))

        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(Space.s2))

        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Space.s6))

        OutlinedTextField(
            value = state.email,
            onValueChange = loginViewModel::updateEmail,
            label = { Text(stringResource(R.string.auth_email_label)) },
            singleLine = true,
            enabled = !state.isLoading && !state.isAuthenticated,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Space.s3))

        OutlinedTextField(
            value = state.password,
            onValueChange = loginViewModel::updatePassword,
            label = { Text(stringResource(R.string.auth_password_label)) },
            singleLine = true,
            enabled = !state.isLoading && !state.isAuthenticated,
            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = loginViewModel::togglePasswordVisibility) {
                    Icon(
                        imageVector = if (state.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Mostrar/ocultar contraseña"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = loginViewModel::openForgotPasswordDialog,
                enabled = !state.isLoading
            ) {
                Text(
                    text = stringResource(R.string.forgot_password_link),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(Space.s2))

        state.loginError?.let { error ->
            Text(
                text = stringResource(error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(Space.s3))
        }

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(Size.button))
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

            Spacer(modifier = Modifier.height(Space.s3))

            // Botón "Continuar con Google" con Credential Manager
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val credentialManager = CredentialManager.create(context)
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(WEB_CLIENT_ID)
                                .build()

                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()

                            val result = credentialManager.getCredential(
                                context = context,
                                request = request
                            )

                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                            val idToken = googleIdTokenCredential.idToken

                            loginViewModel.signInWithGoogle(idToken)
                        } catch (e: GetCredentialException) {
                            Log.w("LoginScreen", "Google Sign-In canceled or failed", e)
                        } catch (e: Exception) {
                            Log.w("LoginScreen", "Google Sign-In exception", e)
                        }
                    }
                },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Size.buttonLarge)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(Size.iconSmall)
                )
                Spacer(modifier = Modifier.width(Space.s2))
                Text(text = stringResource(R.string.login_google))
            }
        }

        Spacer(modifier = Modifier.height(Space.s6))

        TextButton(
            onClick = onCreateAccount,
            enabled = !state.isLoading
        ) {
            Text(text = stringResource(R.string.login_create_account))
        }
    }
}

@Composable
private fun ForgotPasswordDialog(
    email: String,
    isSending: Boolean,
    isSuccess: Boolean,
    errorRes: Int?,
    onEmailChanged: (String) -> Unit,
    onSendReset: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.forgot_password_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s3)) {
                Text(
                    text = stringResource(R.string.forgot_password_instructions),
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChanged,
                    label = { Text(text = stringResource(R.string.auth_email_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = !isSending && !isSuccess,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isSuccess) {
                    Text(
                        text = stringResource(R.string.reset_link_sent_success),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                errorRes?.let { err ->
                    Text(
                        text = stringResource(err),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            if (isSuccess) {
                Button(onClick = onDismiss) {
                    Text(text = stringResource(R.string.nueva_orden))
                }
            } else {
                Button(
                    onClick = onSendReset,
                    enabled = email.isNotBlank() && !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(text = stringResource(R.string.send_reset_link))
                    }
                }
            }
        },
        dismissButton = {
            if (!isSuccess) {
                OutlinedButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancelar))
                }
            }
        }
    )
}
