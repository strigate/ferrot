package org.strigate.ferrot.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import org.strigate.ferrot.R
import org.strigate.ferrot.presentation.theme.LocalDimens

@Composable
fun AvailableUpdateBanner(
    tag: String?,
    localFilePath: String?,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    if (!localFilePath.isNullOrBlank()) {
        Surface(
            modifier = modifier
                .clickable {
                    onClick(localFilePath)
                },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = dimens.tonalElevationHigh,
            shadowElevation = dimens.shadowElevationLow,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimens.spacingMedium,
                        vertical = dimens.spacingMediumAlt,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                )
                Spacer(Modifier.width(dimens.spacingMediumAlt))
                Text(
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    text = stringResource(R.string.available_update_ready, tag ?: ""),
                )
            }
        }
    }
}
