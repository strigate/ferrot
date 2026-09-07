package org.strigate.ferrot.presentation.screen.downloads

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import org.strigate.ferrot.R
import org.strigate.ferrot.presentation.theme.LocalDimens
import org.strigate.ferrot.presentation.theme.TextStyles
import org.strigate.ferrot.presentation.transitions.Transitions
import org.strigate.refinery.theme.RefineryTopAppBarDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadsDefaultTopBar(
    hasDownloads: Boolean,
    hasFailedDownloads: Boolean,
    hasActiveDownloads: Boolean,
    searchActive: Boolean,
    searchQuery: String,
    isArchived: Boolean,
    gridLayoutEnabled: Boolean,
    searchFocusRequester: FocusRequester,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToArchived: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSelectAll: () -> Unit,
    onScrollToTop: () -> Unit,
    onRetryFailedAndScrollToTop: () -> Unit,
    onStopAllDownloads: () -> Unit,
    onToggleGridLayout: () -> Unit,
) {
    val view = LocalView.current
    val dimens = LocalDimens.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        colors = RefineryTopAppBarDefaults.colors(),
        navigationIcon = {
            if (isArchived) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.content_description_back),
                    )
                }
            } else {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_logo_appbar),
                        contentDescription = stringResource(R.string.app_name),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart,
            ) {
                AnimatedVisibility(
                    visible = !searchActive,
                    enter = Transitions.titleEnter,
                    exit = Transitions.titleExit,
                ) {
                    Text(
                        modifier = Modifier.combinedClickable(onClick = onScrollToTop),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = TextStyles.downloadsTitle(),
                        text = if (isArchived) {
                            stringResource(R.string.screen_title_archived)
                        } else {
                            stringResource(R.string.app_name)
                        },
                        maxLines = 1,
                    )
                }
                AnimatedVisibility(
                    visible = searchActive,
                    enter = Transitions.searchEnter,
                    exit = Transitions.searchExit,
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = dimens.spacingSmall)
                            .focusRequester(searchFocusRequester),
                        singleLine = true,
                        placeholder = {
                            Text(text = stringResource(R.string.hint_search))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                contentDescription = null,
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = {
                    onSearchActiveChange(!searchActive)
                    if (searchActive) {
                        onSearchQueryChange("")
                        keyboardController?.hide()
                    }
                },
            ) {
                Icon(
                    imageVector = if (searchActive) {
                        Icons.Filled.Close
                    } else {
                        Icons.Filled.Search
                    },
                    contentDescription = null,
                )
            }
            if (!searchActive) {
                IconButton(onClick = onToggleGridLayout) {
                    Icon(
                        imageVector = if (gridLayoutEnabled) {
                            Icons.AutoMirrored.Filled.ViewList
                        } else {
                            Icons.Filled.ViewModule
                        },
                        contentDescription = if (gridLayoutEnabled) {
                            stringResource(R.string.content_description_use_list_layout)
                        } else {
                            stringResource(R.string.content_description_use_grid_layout)
                        },
                    )
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = !menuExpanded }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = null,
                    )
                }
                DropdownMenu(
                    modifier = Modifier.padding(end = dimens.spacingSmall),
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    if (hasDownloads) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.select_all)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.SelectAll,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                onSelectAll()
                                menuExpanded = false
                            },
                        )
                    }
                    if (hasFailedDownloads) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.retry_failed)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.content_description_retry_failed),
                                )
                            },
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onRetryFailedAndScrollToTop()
                                menuExpanded = false
                            },
                        )
                    }
                    if (hasActiveDownloads) {
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(R.string.notification_action_stop_all))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.notification_action_stop_all),
                                )
                            },
                            onClick = {
                                onStopAllDownloads()
                                menuExpanded = false
                            },
                        )
                    }
                    if (!isArchived) {
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(R.string.screen_title_archived))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Archive,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                onNavigateToArchived()
                                menuExpanded = false
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(text = stringResource(R.string.screen_title_settings))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            onNavigateToSettings()
                            menuExpanded = false
                        },
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadsSelectionTopBar(
    selectionCountTitle: String,
    selectionSizeTitle: String,
    shouldMarkSelectionSeen: Boolean,
    isArchived: Boolean,
    onClearSelection: () -> Unit,
    onToggleAll: () -> Unit,
    onToggleSeen: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        colors = RefineryTopAppBarDefaults.colors(),
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                )
            }
        },
        title = {
            Column {
                Text(
                    text = selectionCountTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = selectionSizeTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            IconButton(onClick = onToggleAll) {
                Icon(
                    imageVector = Icons.Filled.SelectAll,
                    contentDescription = stringResource(R.string.content_description_select_all),
                )
            }
            IconButton(onClick = onToggleSeen) {
                Icon(
                    imageVector = if (shouldMarkSelectionSeen) {
                        Icons.Filled.Visibility
                    } else {
                        Icons.Filled.VisibilityOff
                    },
                    contentDescription = if (shouldMarkSelectionSeen) {
                        stringResource(R.string.content_description_mark_seen)
                    } else {
                        stringResource(R.string.content_description_mark_unseen)
                    },
                )
            }
            IconButton(onClick = onArchive) {
                Icon(
                    imageVector = if (isArchived) {
                        Icons.Filled.Unarchive
                    } else {
                        Icons.Filled.Archive
                    },
                    contentDescription = if (isArchived) {
                        stringResource(R.string.content_description_unarchive)
                    } else {
                        stringResource(R.string.content_description_archive)
                    },
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.content_description_delete),
                )
            }
        },
    )
}
