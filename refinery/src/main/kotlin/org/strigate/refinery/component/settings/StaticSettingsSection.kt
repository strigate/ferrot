package org.strigate.refinery.component.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.strigate.refinery.theme.LocalRefineryDimens

@Composable
fun StaticSettingsSection(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String? = null,
    content: @Composable () -> Unit,
) {
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
                .padding(vertical = refineryDimens.spacingMedium),
        ) {
            if (title != null || icon != null) {
                SettingsSectionHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = refineryDimens.spacingMedium),
                    icon = icon,
                    title = title,
                )
                Spacer(modifier = Modifier.height(refineryDimens.spacingSmall))
            }
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                content()
            }
        }
    }
}
