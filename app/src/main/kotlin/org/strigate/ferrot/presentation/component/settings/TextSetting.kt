package org.strigate.ferrot.presentation.component.settings

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TextSetting(
    text: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (enabled && (onClick != null || onLongClick != null)) {
        Modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = {
                    onClick?.invoke()
                },
                onLongClick = {
                    onLongClick?.invoke()
                },
            )
    } else Modifier

    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    val descriptionColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 24.dp),
            ) {
                Text(
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    text = text,
                )
                description?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        style = MaterialTheme.typography.bodySmall,
                        color = descriptionColor,
                        text = it,
                    )
                }
            }
        }
    }
}
