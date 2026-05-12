package org.strigate.refinery.component.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.strigate.refinery.theme.LocalRefineryDimens

@Composable
fun ExpandableSettingsSection(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String? = null,
    initialExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    val refineryDimens = LocalRefineryDimens.current

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        tonalElevation = refineryDimens.tonalElevationHigh,
        shadowElevation = refineryDimens.shadowElevationLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                ) {
                    expanded = !expanded
                }
                .padding(vertical = refineryDimens.spacingMedium),
        ) {
            SettingsSectionHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = refineryDimens.spacingMedium),
                icon = icon,
                title = title,
            ) {
                Icon(
                    modifier = Modifier
                        .size(refineryDimens.iconXSmallAlt),
                    imageVector = if (expanded) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = null,
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(refineryDimens.spacingSmall))
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    content()
                }
            }
        }
    }
}
