package com.coffeetime.ui.registro

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.coffeetime.R
import com.coffeetime.ui.common.PlaceholderScreen

@Composable
fun RegistroExitoScreen(
    userId: Int,
    onGoToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        title = stringResource(R.string.registro_exito_title),
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.registro_exito_message, userId),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Button(onClick = onGoToLogin) {
            Text(text = stringResource(R.string.registro_exito_go_login))
        }
    }
}
