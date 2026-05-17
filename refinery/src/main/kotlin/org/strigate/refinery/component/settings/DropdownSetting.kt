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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpOffset
import org.strigate.refinery.theme.LocalRefineryDimens

data class DropdownSettingOption(
    val id: String,
    val text: String,
)

@Composable
fun DropdownSetting(
    text: String,
    selectedText: String,
    options: List<DropdownSettingOption>,
    modifier: Modifier = Modifier,
    description: String? = null,
    onOptionSelected: (DropdownSettingOption) -> Unit,
) {
    val refineryDimens = LocalRefineryDimens.current

    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val chipColor = MaterialTheme.colorScheme.surface
    val chipTextColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = {
                        expanded = true
                    },
                )
                .padding(
                    horizontal = refineryDimens.spacingMedium,
                    vertical = refineryDimens.spacingMediumAlt,
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
                        modifier = Modifier,
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(refineryDimens.radiusPill))
                                .background(chipColor)
                                .padding(
                                    start = refineryDimens.spacingMediumAlt,
                                    end = refineryDimens.spacingSmall,
                                    top = refineryDimens.spacingXSmallAlt,
                                    bottom = refineryDimens.spacingXSmallAlt,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = selectedText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = chipTextColor,
                            )
                            Icon(
                                modifier = Modifier
                                    .padding(start = refineryDimens.spacingXSmall)
                                    .size(refineryDimens.iconXSmallAlt),
                                imageVector = Icons.Filled.ArrowDropDown,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                contentDescription = null,
                            )
                        }

                        SettingsDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = {
                                expanded = false
                            },
                            items = options.map { option ->
                                SettingsDropdownMenuItem(
                                    id = option.id,
                                    text = option.text,
                                    onClick = {
                                        onOptionSelected(option)
                                    },
                                )
                            },
                            offset = DpOffset(
                                x = refineryDimens.zero,
                                y = refineryDimens.spacingXSmall
                            ),
                        )
                    }
                }
            }
        }
    }
}
