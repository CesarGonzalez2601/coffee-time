package com.coffeetime.ui.login

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.coffeetime.R
import com.coffeetime.ui.common.PlaceholderScreen

/** Placeholder: #9 reemplaza esto por el login con PinPad. */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        title = stringResource(R.string.login_title),
        modifier = modifier
    ) {
        Button(onClick = onLoginSuccess) {
            Text(text = stringResource(R.string.login_demo_enter))
        }
        TextButton(onClick = onCreateAccount) {
            Text(text = stringResource(R.string.login_create_account))
        }
    }
}
