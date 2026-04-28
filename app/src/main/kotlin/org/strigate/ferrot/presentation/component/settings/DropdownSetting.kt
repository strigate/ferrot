package org.strigate.ferrot.presentation.component.settings

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
import androidx.compose.ui.unit.dp
import org.strigate.ferrot.presentation.component.DropdownMenu
import org.strigate.ferrot.presentation.component.DropdownMenuItem

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
                        color = MaterialTheme.colorScheme.onSurface,
                        text = text,
                    )
                    description?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            text = it,
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
                                .clip(RoundedCornerShape(999.dp))
                                .background(chipColor)
                                .padding(
                                    start = 12.dp,
                                    end = 8.dp,
                                    top = 6.dp,
                                    bottom = 6.dp,
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
                                    .padding(start = 4.dp)
                                    .size(20.dp),
                                imageVector = Icons.Filled.ArrowDropDown,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                contentDescription = null,
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = {
                                expanded = false
                            },
                            items = options.map { option ->
                                DropdownMenuItem(
                                    id = option.id,
                                    text = option.text,
                                    onClick = {
                                        onOptionSelected(option)
                                    },
                                )
                            },
                            offset = DpOffset(x = 0.dp, y = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
