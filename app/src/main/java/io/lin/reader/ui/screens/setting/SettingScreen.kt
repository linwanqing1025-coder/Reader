package io.lin.reader.ui.screens.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.lin.reader.R
import io.lin.reader.navigation.NavKey
import io.lin.reader.ui.ViewModelProvider
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.snackbar.StyledSnackbarHost
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    modifier: Modifier = Modifier,
    onNavigateToDetail: (NavKey.SettingDetails) -> Unit,
    viewModel: SettingScreenViewModel = viewModel(factory = ViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val historyPreferences = viewModel.historyPreferences

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { StyledSnackbarHost(snackbarHostState) },
        topBar = {
            SettingScreenTopBar(
                scrollBehavior = scrollBehavior,
                onResetAll = {
                    val appContext = context.applicationContext
                    viewModel.resetAllSettings(
                        onResult = {
                            val text = appContext.getString(R.string.setting_reset_done)
                            scope.launch {
                                snackbarHostState.showSnackbar(text)
                            }
                        }
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // APP Appearance & Sort
            item {
                SettingList {
                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.setting_appearance)) },
                        onClick = {
                            onNavigateToDetail(NavKey.AppAppearance)
                        },
                        leadingIcon = Icons.Default.AutoAwesome
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                        )
                    }

                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.setting_sort)) },
                        onClick = {
                            onNavigateToDetail(NavKey.Sort)
                        },
                        leadingIcon = Icons.AutoMirrored.Filled.Sort
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                        )
                    }
                }
            }

            // history
            item {
                SettingList {
                    var isClearing by remember { mutableStateOf(false) }
                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.setting_clear_history)) },
                        onClick = { isClearing = true },
                        leadingIcon = Icons.Outlined.DeleteSweep
                    )
                    if (isClearing) {
                        NotificationDialog(
                            title = stringResource(R.string.history_dialog_title_clear),
                            notification = stringResource(R.string.history_clear_history_notification),
                            onDismiss = { isClearing = false },
                            onConfirm = {
                                isClearing = false
                                viewModel.clearAllHistory()
                            }
                        )
                    }

                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.setting_show_unread_books)) },
                        onClick = {
                            historyPreferences.updateShowUnreadBooks(!historyPreferences.showUnreadBooks)
                        },
                        leadingIcon = if (historyPreferences.showUnreadBooks) Icons.Filled.RemoveRedEye else Icons.Outlined.VisibilityOff
                    ) {
                        Switch(
                            checked = historyPreferences.showUnreadBooks,
                            onCheckedChange = { historyPreferences.updateShowUnreadBooks(it) }
                        )
                    }

                    if (historyPreferences.showUnreadBooks) {
                        SettingListItem(
                            headlineContent = { Text(stringResource(R.string.setting_expand_unread_books)) },
                            onClick = {
                                historyPreferences.updateUnreadBooksExpanded(!historyPreferences.unreadBooksExpanded)
                            },
                            leadingIcon = if (historyPreferences.unreadBooksExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown
                        ) {
                            Switch(
                                checked = historyPreferences.unreadBooksExpanded,
                                onCheckedChange = { historyPreferences.updateUnreadBooksExpanded(it) }
                            )
                        }
                    }
                }
            }

            // reading
            item {
                SettingList {
                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.setting_reader_color)) },
                        onClick = {
                            onNavigateToDetail(NavKey.ReaderColor)
                        },
                        leadingIcon = Icons.Default.ColorLens
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                        )
                    }

                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.setting_reading_mode)) },
                        onClick = {
                            onNavigateToDetail(NavKey.ReadingMode)
                        },
                        leadingIcon = Icons.AutoMirrored.Outlined.MenuBook
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                        )
                    }

                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.setting_interaction_style)) },
                        onClick = {
                            onNavigateToDetail(NavKey.Interaction)
                        },
                        leadingIcon = Icons.Default.TouchApp
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                        )
                    }

                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.setting_reflow)) },
                        onClick = {
                            onNavigateToDetail(NavKey.Reflow)
                        },
                        leadingIcon = Icons.AutoMirrored.Rounded.FormatAlignLeft
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                        )
                    }

                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.setting_other_reading_settings)) },
                        onClick = {
                            onNavigateToDetail(NavKey.OtherReading)
                        },
                        leadingIcon = Icons.Default.MoreHoriz
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingScreenTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onResetAll: () -> Unit
) {
    val isResetting = remember { mutableStateOf(false) }
    val resetStr = stringResource(R.string.setting_reset_all)

    TopAppBar(
        title = {
            Text(
                stringResource(R.string.navigation_label_setting),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Below
                ),
                tooltip = {
                    PlainTooltip(
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                            paneTitle = resetStr
                        }
                    ) {
                        Text(resetStr)
                    }
                },
                state = rememberTooltipState(),
            ) {
                IconButton(onClick = { isResetting.value = true }) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = resetStr,
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior
    )

    if (isResetting.value) {
        NotificationDialog(
            title = stringResource(R.string.setting_reset_dialog_title),
            notification = stringResource(R.string.setting_reset_notification),
            onDismiss = { isResetting.value = false },
            onConfirm = {
                onResetAll()
                isResetting.value = false
            }
        )
    }
}
