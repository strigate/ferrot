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
import org.strigate.ferrot.presentation.screen.ArchivedScreen
import org.strigate.ferrot.presentation.screen.DownloadScreen
import org.strigate.ferrot.presentation.screen.DownloadsScreen
import org.strigate.ferrot.presentation.screen.SettingsScreen
import org.strigate.ferrot.presentation.screen.UpdatesScreen

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
                archived = false,
            )
        }
        composable(Screen.Archived.route) {
            ArchivedScreen(
                navController = navController,
            )
        }
        composable(
            route = Screen.Download.route,
            arguments = listOf(
                navArgument(Screen.Download.ARG_DOWNLOAD_ID) {
                    type = NavType.LongType
                },
                navArgument(Screen.ARG_ARCHIVED) {
                    type = NavType.BoolType
                    defaultValue = false
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
        composable(Screen.Updates.route) {
            UpdatesScreen()
        }
        composable(Screen.About.route) {
            AboutScreen()
        }
    }
}

sealed class Screen(val route: String) {
    companion object {
        const val ARG_ARCHIVED = "archived"
    }

    data object Downloads : Screen("downloads")
    data object Archived : Screen("archived")
    data object Download : Screen("download/{downloadId}?archived={archived}") {
        const val ARG_DOWNLOAD_ID = "downloadId"
        fun route(id: Long, archived: Boolean = false) = "download/$id?archived=$archived"
    }

    data object Settings : Screen("settings")
    data object Updates : Screen("updates")
    data object About : Screen("about")
}
