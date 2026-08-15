package org.strigate.ferrot.presentation.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.strigate.ferrot.presentation.theme.TextStyles

@Composable
fun ConfirmDialog(
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit,
    onDismissRequest: () -> Unit = {},
    title: String,
    message: String,
    positiveButtonText: String,
    negativeButtonText: String,
    isDestructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                style = TextStyles.dialogTitle(),
            )
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
                Text(
                    text = positiveButtonText,
                    color = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color.Unspecified
                    },
                )
            }
        },
    )
}
