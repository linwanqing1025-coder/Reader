package io.lin.reader.ui.feature.setting

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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.lin.reader.R
import io.lin.reader.ui.components.screenbar.StyledTitleScreenBar
import io.lin.reader.ui.components.snackbar.StyledSnackbarHost
import io.lin.reader.ui.ViewModelProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    modifier: Modifier = Modifier,
    onNavigateToDetail: (io.lin.reader.navigation.NavKey.SettingDetails) -> Unit,
    viewModel: SettingScreenViewModel = viewModel(factory = ViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val historyPreferences = viewModel.historyPreferences

    Scaffold(
        modifier = modifier,
        snackbarHost = { StyledSnackbarHost(snackbarHostState) },
        topBar = {
            StyledTitleScreenBar(title = stringResource(R.string.navigation_label_setting))
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
                            onNavigateToDetail(io.lin.reader.navigation.NavKey.SettingDetails.AppAppearance)
                        },
                        leadingIcon = Icons.Default.AutoAwesome
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.setting_sort)) },
                        onClick = {
                            onNavigateToDetail(io.lin.reader.navigation.NavKey.SettingDetails.Sort)
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
                        leadingIcon = Icons.Filled.DeleteSweep
                    )
                    if (isClearing) {
                        io.lin.reader.ui.components.dialog.NotificationDialog(
                            title = stringResource(R.string.history_dialog_title_clear),
                            notification = stringResource(R.string.history_clear_history_notification),
                            onDismiss = { isClearing = false },
                            onConfirm = {
                                isClearing = false
                                viewModel.clearAllHistory()
                            }
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
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
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
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
                            onNavigateToDetail(io.lin.reader.navigation.NavKey.SettingDetails.ReaderColor)
                        },
                        leadingIcon = Icons.Default.ColorLens
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.setting_reading_mode)) },
                        onClick = {
                            onNavigateToDetail(io.lin.reader.navigation.NavKey.SettingDetails.ReadingMode)
                        },
                        leadingIcon = Icons.AutoMirrored.Outlined.MenuBook
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.setting_interaction_style)) },
                        onClick = {
                            onNavigateToDetail(io.lin.reader.navigation.NavKey.SettingDetails.Interaction)
                        },
                        leadingIcon = Icons.Default.TouchApp
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.setting_reflow)) },
                        onClick = {
                            onNavigateToDetail(io.lin.reader.navigation.NavKey.SettingDetails.Reflow)
                        },
                        leadingIcon = Icons.AutoMirrored.Rounded.FormatAlignLeft
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.setting_other_reading_settings)) },
                        onClick = {
                            onNavigateToDetail(io.lin.reader.navigation.NavKey.SettingDetails.OtherReading)
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

            // reset
            item {
                SettingList {
                    var isResetting by remember { mutableStateOf(false) }
                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.setting_reset_all)) },
                        onClick = { isResetting = true },
                        leadingIcon = Icons.Default.RestartAlt
                    )
                    if (isResetting) {
                        io.lin.reader.ui.components.dialog.NotificationDialog(
                            title = stringResource(R.string.setting_reset_dialog_title),
                            notification = stringResource(R.string.setting_reset_notification),
                            onDismiss = { isResetting = false },
                            onConfirm = {
                                isResetting = false
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
                    }
                }
            }
        }
    }
}