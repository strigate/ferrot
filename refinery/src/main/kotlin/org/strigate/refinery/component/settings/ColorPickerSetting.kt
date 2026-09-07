package org.strigate.refinery.component.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.strigate.refinery.theme.LocalRefineryDimens

import java.util.Locale

@Composable
fun ColorPickerSetting(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    description: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val refineryDimens = LocalRefineryDimens.current
    val chipColor = MaterialTheme.colorScheme.surface
    val chipTextColor = MaterialTheme.colorScheme.onSurface
    val clickableModifier = if (onClick != null) {
        Modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
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
                        color = MaterialTheme.colorScheme.onSurface,
                        text = text,
                    )
                    description?.let { currentDescription ->
                        Spacer(modifier = Modifier.height(refineryDimens.spacingXSmall))
                        Text(
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            text = currentDescription,
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(refineryDimens.radiusPill))
                            .background(chipColor)
                            .padding(
                                start = refineryDimens.spacingSmall,
                                end = refineryDimens.spacingMediumAlt,
                                top = refineryDimens.spacingXSmallAlt,
                                bottom = refineryDimens.spacingXSmallAlt,
                            ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(refineryDimens.spacingSmallAlt),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(refineryDimens.iconXSmall)
                                    .clip(RoundedCornerShape(refineryDimens.radiusMedium))
                                    .background(color),
                            )
                            Text(
                                text = color.toHexString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = chipTextColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Color.toHexString(): String {
    val rgb = toArgb() and 0x00FFFFFF
    return String.format(Locale.US, "#%06X", rgb)
}
