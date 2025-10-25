package org.strigate.ferrot.presentation.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit,
    positiveButtonText: String,
    negativeButtonText: String,
    onDismissRequest: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = title)
        },
        text = {
            Text(text = message)
        },
        dismissButton = {
            TextButton(onClick = onNegativeClick) {
                Text(text = negativeButtonText)
            }
        },
        confirmButton = {
            TextButton(onClick = onPositiveClick) {
                Text(text = positiveButtonText)
            }
        },
    )
}
