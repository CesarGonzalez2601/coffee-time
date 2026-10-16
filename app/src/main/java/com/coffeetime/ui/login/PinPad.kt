
package com.coffeetime.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinPad(
    enabled: Boolean,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9)
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { digits ->
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(16.dp)
            ) {
                digits.forEach { digit ->
                    DigitKey(
                        digit = digit,
                        enabled = enabled,
                        onClick = { onDigit(digit) }
                    )
                }
            }
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {
            // Espacio inferior izquierdo.
            Box(
                modifier = Modifier.size(72.dp)
            )

            // Número cero.
            DigitKey(
                digit = 0,
                enabled = enabled,
                onClick = { onDigit(0) }
            )

            // Botón borrar.
            FilledTonalIconButton(
                onClick = onDelete,
                enabled = enabled,
                modifier = Modifier
                    .size(72.dp)
                    .semantics {
                        contentDescription = "Borrar"
                    }
            ) {
                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun DigitKey(
    digit: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(72.dp)
            .semantics {
                contentDescription = digit.toString()
            }
    ) {
        Text(
            text = digit.toString(),
            fontSize = 26.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
