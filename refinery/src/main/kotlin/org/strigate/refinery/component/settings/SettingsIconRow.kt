package org.strigate.refinery.component.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import org.strigate.refinery.theme.LocalRefineryDimens

data class SettingsIconRowItem(
    val painter: Painter,
    val contentDescription: String?,
    val onClick: () -> Unit,
)

@Composable
fun SettingsIconRow(
    items: List<SettingsIconRowItem>,
    modifier: Modifier = Modifier,
) {
    val refineryDimens = LocalRefineryDimens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = refineryDimens.spacingSmallAlt),
        horizontalArrangement = Arrangement.spacedBy(refineryDimens.zero),
    ) {
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides refineryDimens.zero,
        ) {
            items.forEach { item ->
                IconButton(
                    modifier = Modifier
                        .size(refineryDimens.iconSmall + refineryDimens.spacingXSmallAlt),
                    onClick = item.onClick,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(refineryDimens.iconXSmall),
                        painter = item.painter,
                        contentDescription = item.contentDescription,
                    )
                }
            }
        }
    }
}
