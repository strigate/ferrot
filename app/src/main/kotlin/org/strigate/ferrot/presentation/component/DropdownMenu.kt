package org.strigate.ferrot.presentation.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpOffset
import androidx.compose.material3.DropdownMenu as MaterialDropdownMenu

@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<DropdownMenuItem>,
    offset: DpOffset = DpOffset.Zero,
) {
    MaterialDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
    ) {
        items.forEach { item ->
            androidx.compose.material3.DropdownMenuItem(
                text = {
                    Text(item.text)
                },
                onClick = {
                    onDismissRequest()
                    item.onClick()
                },
            )
        }
    }
}

data class DropdownMenuItem(
    val id: String,
    val text: String,
    val onClick: () -> Unit,
)
