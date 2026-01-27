package org.strigate.ferrot.presentation.component.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.strigate.ferrot.presentation.theme.LocalDimens

@Composable
fun ErrorState(
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    val dimens = LocalDimens.current
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                modifier = Modifier
                    .size(dimens.iconXLarge),
                tint = MaterialTheme.colorScheme.error,
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
            )
            text?.let {
                Spacer(modifier = Modifier.height(dimens.spacingMedium))
                Column(
                    modifier = Modifier
                        .widthIn(max = dimens.contentMaxWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        text = it,
                    )
                }
            }
        }
    }
}
