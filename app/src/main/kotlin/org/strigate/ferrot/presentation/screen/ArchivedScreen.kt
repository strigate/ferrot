package org.strigate.ferrot.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun ArchivedScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    DownloadsScreen(
        modifier = modifier,
        navController = navController,
        archived = true,
    )
}
