package org.strigate.refinery.component.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.strigate.refinery.theme.LocalRefineryDimens

@Composable
fun SettingsSectionDivider() {
    val refineryDimens = LocalRefineryDimens.current
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = refineryDimens.spacingMedium),
        thickness = refineryDimens.dividerThin,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
fun IconAlignedSettingsSectionDivider() {
    val refineryDimens = LocalRefineryDimens.current
    HorizontalDivider(
        modifier = Modifier.padding(
            start = refineryDimens.spacingMedium +
                    refineryDimens.iconXSmallAlt +
                    refineryDimens.spacingMediumAlt,
            end = refineryDimens.spacingMedium,
        ),
        thickness = refineryDimens.dividerThin,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
