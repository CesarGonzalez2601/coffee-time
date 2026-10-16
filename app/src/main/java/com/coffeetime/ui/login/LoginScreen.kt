
package com.coffeetime.ui.login

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalCafe
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeetime.CoffeeTimeApp
import com.coffeetime.R
import kotlin.math.roundToInt

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {

    // Obtener AuthenticationService desde AppContainer
    val app = LocalContext.current
        .applicationContext as CoffeeTimeApp

    // Inicializar LoginViewModel
    val loginViewModel: LoginViewModel = viewModel(
        factory = remember(app) {
            LoginViewModel.factory(
                app.container.authenticationService
            )
        }
    )

    // Estado del login
    val state = loginViewModel.state

    // Animación para PIN incorrecto
    val shakeOffset = remember {
        Animatable(0f)
    }

    // Navegar cuando el login sea exitoso
    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) {
            onLoginSuccess()
        }
    }

    // Vibración visual de 300 ms
    LaunchedEffect(state.errorAnimationId) {

        if (state.errorAnimationId > 0) {

            repeat(6) { index ->

                shakeOffset.animateTo(
                    targetValue = if (index % 2 == 0) {
                        10f
                    } else {
                        -10f
                    },
                    animationSpec = tween(
                        durationMillis = 50
                    )
                )
            }

            shakeOffset.snapTo(0f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 24.dp,
                vertical = 24.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // LOGO
        Icon(
            imageVector = Icons.Filled.LocalCafe,
            contentDescription = "Coffee Time",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // TÍTULO
        Text(
            text = stringResource(
                R.string.login_title
            ),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // SUBTÍTULO
        Text(
            text = stringResource(
                R.string.login_subtitle
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        // CAMPO ID
        OutlinedTextField(
            value = state.userId,
            onValueChange = {
                loginViewModel.updateId(it)
            },
            label = {
                Text("ID de usuario")
            },
            placeholder = {
                Text("Ingresa tu ID")
            },
            singleLine = true,
            enabled = !state.isLoading &&
                    !state.isAuthenticated,
            isError = state.idError != null,
            supportingText = state.idError?.let { error ->
                {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // TÍTULO PIN
        Text(
            text = "Ingresa tu PIN",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // INDICADORES PIN (4 PUNTOS)
        Row(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = shakeOffset.value.roundToInt(),
                        y = 0
                    )
                }
                .semantics {
                    contentDescription =
                        "${state.pinLength} de 4 dígitos ingresados"
                },
            horizontalArrangement = Arrangement.spacedBy(
                20.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            repeat(4) { index ->

                val isFilled = index < state.pinLength

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = if (isFilled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // MENSAJE ERROR
        if (state.loginError != null) {

            Text(
                text = state.loginError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )

        } else {

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // INDICADOR DE CARGA
        if (state.isLoading) {

            CircularProgressIndicator(
                modifier = Modifier.size(40.dp)
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }

        // TECLADO NUMÉRICO PERSONALIZADO
        PinPad(
            enabled = !state.isLoading &&
                    !state.isAuthenticated &&
                    state.pinLength < 4,
            onDigit = { digit ->
                loginViewModel.inputDigit(digit)
            },
            onDelete = {
                loginViewModel.deleteDigit()
            }
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        // CREAR CUENTA
        TextButton(
            onClick = onCreateAccount,
            enabled = !state.isLoading
        ) {

            Text(
                text = stringResource(
                    R.string.login_create_account
                )
            )
        }
    }
}
