package org.strigate.ferrot.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.strigate.ferrot.presentation.screen.AboutScreen
import org.strigate.ferrot.presentation.screen.DownloadScreen
import org.strigate.ferrot.presentation.screen.DownloadsScreen
import org.strigate.ferrot.presentation.screen.SettingsScreen

@Composable
fun MainNavHost(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Downloads.route,
        enterTransition = {
            fadeIn(TweenSpec(easing = LinearEasing))
        },
        exitTransition = {
            fadeOut(TweenSpec(easing = LinearEasing))
        },
    ) {
        composable(Screen.Downloads.route) {
            DownloadsScreen(
                navController = navController,
            )
        }
        composable(
            route = Screen.Download.route,
            arguments = listOf(
                navArgument(Screen.Download.ARG_DOWNLOAD_ID) {
                    type = NavType.LongType
                },
            ),
        ) {
            DownloadScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
            )
        }
        composable(Screen.About.route) {
            AboutScreen()
        }
    }
}

sealed class Screen(val route: String) {
    data object Downloads : Screen("downloads")
    data object Download : Screen("download/{downloadId}") {
        const val ARG_DOWNLOAD_ID = "downloadId"
        fun route(id: Long) = "download/$id"
    }

    data object Settings : Screen("settings")
    data object About : Screen("about")
}
