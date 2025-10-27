package org.strigate.ferrot.presentation.component.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import org.strigate.ferrot.presentation.theme.LocalDimens

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    title: String,
    body: String,
    icon: ImageVector? = null,
    iconContentDescription: String? = null,
) {
    val dimens = LocalDimens.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon?.let {
            Icon(
                modifier = Modifier
                    .size(dimens.iconXXLarge),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                imageVector = it,
                contentDescription = iconContentDescription,
            )
        }
        Spacer(modifier = Modifier.height(dimens.spacingMedium))
        Column(
            modifier = Modifier
                .widthIn(max = dimens.contentMaxWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                text = title,
            )
            Spacer(modifier = Modifier.height(dimens.spacingSmall))
            Text(
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                text = body,
            )
        }
    }
}
