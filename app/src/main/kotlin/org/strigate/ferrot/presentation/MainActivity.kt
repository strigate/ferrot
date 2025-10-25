package org.strigate.ferrot.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import org.strigate.ferrot.app.Constants.Action.ACTION_NAVIGATE_DOWNLOAD
import org.strigate.ferrot.app.Constants.Action.ACTION_START_DOWNLOAD_FROM_SHARE
import org.strigate.ferrot.app.Constants.Extras.EXTRA_ACTION
import org.strigate.ferrot.app.Constants.Extras.EXTRA_DOWNLOAD_ID
import org.strigate.ferrot.app.Constants.Extras.EXTRA_SHARED_URL
import org.strigate.ferrot.app.Constants.Extras.EXTRA_SHARED_URL_UID
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.presentation.theme.FerrotTheme
import org.strigate.ferrot.util.isAtLeastTiramisu

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val notificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        if (Intent.ACTION_APPLICATION_PREFERENCES == intent.action) {
            viewModel.navigateTo(
                route = Screen.Settings.route,
                popUpToDownloads = true,
            )
            return
        }
        when (intent.getStringExtra(EXTRA_ACTION)) {
            ACTION_START_DOWNLOAD_FROM_SHARE -> {
                val sharedUrlUid = intent.getStringExtra(EXTRA_SHARED_URL_UID)
                if (!sharedUrlUid.isNullOrBlank() && handledUids.contains(sharedUrlUid)) {
                    return
                }
                val sharedUrl = intent.getStringExtra(EXTRA_SHARED_URL)
                if (!sharedUrl.isNullOrBlank()) {
                    sharedUrlUid?.let {
                        handledUids.add(it)
                        viewModel.startDownload(sharedUrl)
                        viewModel.navigateTo(
                            route = Screen.Downloads.route,
                            popUpToDownloads = true,
                        )
                    }
                }
                return
            }

            ACTION_NAVIGATE_DOWNLOAD -> {
                val downloadIdAsString = intent.getStringExtra(EXTRA_DOWNLOAD_ID)
                val downloadId = downloadIdAsString?.toLongOrNull()
                if (downloadId != null && downloadId > 0) {
                    viewModel.navigateToDownload(downloadId)
                    return
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleIntent(intent)
        }
        enableEdgeToEdge()
        setContent {
            FerrotTheme {
                val navController = rememberNavController()
                var permissionsRequested by rememberSaveable { mutableStateOf(false) }
                val navigateRoute by viewModel
                    .navigateRoute
                    .collectAsStateWithLifecycle(initialValue = null)

                LaunchedEffect(navigateRoute) {
                    val event = navigateRoute ?: return@LaunchedEffect
                    val targetRoute = when (event) {
                        is NavigationEvent.Route -> event.route
                    }
                    val currentRoute = navController.currentDestination?.route
                    if (targetRoute == currentRoute) {
                        viewModel.resetNavigate()
                        return@LaunchedEffect
                    }
                    try {
                        navController.currentBackStackEntryFlow.first()
                        navController.navigate(targetRoute) {
                            if (event.popUpToDownloads) {
                                popUpTo(Screen.Downloads.route) {
                                    inclusive = targetRoute == Screen.Downloads.route
                                    saveState = false
                                }
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    } catch (throwable: Throwable) {
                        Log.w(LOG_TAG, "Navigation failed: ${throwable.message}", throwable)
                    } finally {
                        viewModel.resetNavigate()
                    }
                }
                LaunchedEffect(Unit) {
                    if (!permissionsRequested) {
                        requestNotificationsPermissionsIfNeeded()
                        permissionsRequested = true
                    }
                }
                MainNavHost(
                    navController = navController,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    fun requestNotificationsPermissionsIfNeeded() {
        if (!isAtLeastTiramisu()) {
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        private val handledUids = mutableListOf<String>()
    }
}
