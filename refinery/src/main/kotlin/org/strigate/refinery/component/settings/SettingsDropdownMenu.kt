package org.strigate.refinery.component.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import org.strigate.refinery.theme.LocalRefineryDimens

internal data class SettingsDropdownMenuItem(
    val text: String,
    val enabled: Boolean = true,
    val leadingIcon: (@Composable (() -> Unit))? = null,
    val onClick: () -> Unit,
)

@Composable
internal fun SettingsDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<SettingsDropdownMenuItem>,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    offset: DpOffset = DpOffset.Zero,
) {
    if (!expanded) {
        return
    }
    val refineryDimens = LocalRefineryDimens.current
    DropdownMenu(
        modifier = modifier
            .padding(end = refineryDimens.spacingSmall),
        onDismissRequest = onDismissRequest,
        expanded = expanded,
        shape = MaterialTheme.shapes.medium,
        containerColor = containerColor,
        offset = offset,
        tonalElevation = refineryDimens.zero,
        shadowElevation = refineryDimens.shadowElevationLow,
    ) {
        items.forEach { dropdownMenuItem ->
            DropdownMenuItem(
                leadingIcon = dropdownMenuItem.leadingIcon,
                text = {
                    Text(
                        color = textColor,
                        text = dropdownMenuItem.text,
                    )
                },
                onClick = {
                    dropdownMenuItem.onClick()
                    onDismissRequest()
                },
                enabled = dropdownMenuItem.enabled,
            )
        }
    }
}
