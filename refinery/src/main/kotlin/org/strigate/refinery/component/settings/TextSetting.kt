package org.strigate.refinery.component.settings

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
import org.strigate.refinery.theme.LocalRefineryDimens

@Composable
fun TextSetting(
    text: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val refineryDimens = LocalRefineryDimens.current
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
                horizontal = refineryDimens.spacingMedium,
                vertical = refineryDimens.spacingMediumAlt,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = refineryDimens.spacingLarge),
            ) {
                Text(
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    text = text,
                )
                description?.let {
                    Spacer(modifier = Modifier.height(refineryDimens.spacingXSmall))
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
