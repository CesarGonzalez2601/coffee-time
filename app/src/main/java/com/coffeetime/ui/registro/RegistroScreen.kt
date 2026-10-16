package com.coffeetime.ui.registro

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.coffeetime.R
import com.coffeetime.ui.common.PlaceholderScreen

/** Placeholder: #10 reemplaza esto por el registro en tres pasos. */
@Composable
fun RegistroScreen(
    onRegistered: (userId: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        title = stringResource(R.string.registro_title),
        modifier = modifier
    ) {
        Button(onClick = { onRegistered(DEMO_USER_ID) }) {
            Text(text = stringResource(R.string.registro_demo_finish))
        }
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.action_back))
        }
    }
}

private const val DEMO_USER_ID = 7
