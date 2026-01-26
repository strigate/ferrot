package org.strigate.ferrot.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import org.strigate.ferrot.R
import org.strigate.ferrot.presentation.theme.LocalDimens
import java.util.Calendar

@Composable
fun Copyright(
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(dimens.spacingLarge))
        Icon(
            modifier = Modifier
                .height(dimens.iconLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.8f,
            ),
            painter = painterResource(R.drawable.strigate_logo),
            contentDescription = stringResource(R.string.content_description_strigate_logo),
        )
        Text(
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = 0.75f,
                ),
            ),
            textAlign = TextAlign.Center,
            text = stringResource(
                R.string.copyright,
                Calendar.getInstance().get(Calendar.YEAR),
            ),
        )
        Spacer(modifier = Modifier.height(dimens.spacingLarge))
    }
}
