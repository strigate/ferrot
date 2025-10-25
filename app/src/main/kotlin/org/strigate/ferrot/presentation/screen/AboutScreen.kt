package org.strigate.ferrot.presentation.screen

import android.content.Intent
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import org.strigate.ferrot.BuildConfig
import org.strigate.ferrot.R
import org.strigate.ferrot.extensions.copyToClipboard
import org.strigate.ferrot.presentation.component.settings.StaticSettingsSection
import org.strigate.ferrot.presentation.component.settings.TextSetting
import org.strigate.ferrot.presentation.viewmodel.AboutViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LaunchedEffect(Unit) {
        viewModel.logShown()
    }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.openUrl.collect { url ->
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            backDispatcher?.onBackPressed()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.screen_title_about),
                    )
                },
            )
        },
        content = { contentPadding ->
            Surface(
                modifier = modifier
                    .padding(contentPadding),
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    StaticSettingsSection(
                        text = stringResource(R.string.settings_section_app_info),
                    ) {
                        TextSetting(
                            text = stringResource(R.string.settings_title_build),
                            description = BuildConfig.VERSION_NAME,
                        ) {
                            context.copyToClipboard(BuildConfig.VERSION_NAME)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    StaticSettingsSection {
                        val urlWebsite = stringResource(R.string.url_website)
                        val urlPrivacy = stringResource(R.string.url_privacy)
                        val urlLicense = stringResource(R.string.url_license)
                        TextSetting(
                            text = stringResource(R.string.settings_title_website),
                            description = stringResource(R.string.settings_description_website),
                        ) {
                            viewModel.onUrlClicked(urlWebsite)
                        }
                        TextSetting(
                            text = stringResource(R.string.settings_title_privacy),
                            description = stringResource(R.string.settings_description_privacy),
                        ) {
                            viewModel.onUrlClicked(urlPrivacy)
                        }
                        TextSetting(
                            text = stringResource(R.string.settings_title_license),
                            description = stringResource(R.string.settings_description_license),
                        ) {
                            viewModel.onUrlClicked(urlLicense)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            modifier = Modifier
                                .height(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.9f,
                            ),
                            painter = painterResource(R.drawable.strigate_logo),
                            contentDescription = stringResource(R.string.content_description_strigate_logo),
                        )
                        Text(
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.75f,
                                ),
                                fontSize = 11.sp,
                            ),
                            textAlign = TextAlign.Center,
                            text = stringResource(
                                R.string.copyright,
                                Calendar.getInstance().get(Calendar.YEAR),
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        },
    )
}
