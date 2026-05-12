package org.strigate.refinery.component.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.strigate.refinery.theme.LocalRefineryDimens

@Composable
internal fun SettingsSectionHeader(
    icon: ImageVector?,
    title: String?,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val refineryDimens = LocalRefineryDimens.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier
                    .size(refineryDimens.iconXSmallAlt),
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (icon != null && title != null) {
            Spacer(modifier = Modifier.width(refineryDimens.spacingMediumAlt))
        }
        if (title != null) {
            Text(
                modifier = Modifier
                    .weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                text = title,
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        trailingContent?.invoke()
    }
}
