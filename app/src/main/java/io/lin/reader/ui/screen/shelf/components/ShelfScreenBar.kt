package io.lin.reader.ui.screen.shelf.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.lin.reader.R
import io.lin.reader.data.database.Series
import io.lin.reader.data.preferences.SortPreference
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.menu.StyledMenu
import io.lin.reader.ui.components.menu.StyledMenuIcon
import io.lin.reader.ui.components.menu.StyledMenuItem
import io.lin.reader.ui.components.screenbar.StyledBarIconButton
import io.lin.reader.ui.components.screenbar.StyledScreenBar
import io.lin.reader.ui.screen.setting.details.SeriesSortMethod
import io.lin.reader.ui.screen.setting.details.VolumeSortMethod

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ShelfScreenTopBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    sortPreference: SortPreference<SeriesSortMethod> = SortPreference(SeriesSortMethod.Name, true),
    onToggleSortAscending: () -> Unit = {},
    onSortMethodChange: (SeriesSortMethod) -> Unit = {},
) {
    StyledScreenBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        title = stringResource(R.string.navigation_label_shelf),
        iconButtonTools = {
            Box {
                var mainMenuExpanded by remember { mutableStateOf(false) }
                var sortSubMenuExpanded by remember { mutableStateOf(false) }
                StyledBarIconButton(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(R.string.sort_series_sort),
                    onClick = { mainMenuExpanded = !mainMenuExpanded }
                )
                StyledMenu(
                    expanded = mainMenuExpanded,
                    onDismissRequest = {
                        sortSubMenuExpanded = false
                        mainMenuExpanded = false
                    }
                ) {
                    StyledMenuItem(
                        text =
                            if (sortPreference.isAscending) stringResource(R.string.sort_ascending)
                            else stringResource(R.string.sort_descending),
                        onClick = { onToggleSortAscending() },
                        leadingIcon = {
                            StyledMenuIcon(
                                imageVector = if (sortPreference.isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = stringResource(R.string.sort_order)
                            )
                        }
                    )
                    Box {
                        StyledMenuItem(
                            text = stringResource(R.string.sort_series_sort),
                            onClick = { sortSubMenuExpanded = !sortSubMenuExpanded },
                            leadingIcon = {
                                StyledMenuIcon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = stringResource(R.string.shelf_top_bar_sort_selected)
                                )
                            },
                            trailingIcon = {
                                StyledMenuIcon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                                    contentDescription = stringResource(R.string.shelf_top_bar_sort_submenu)
                                )
                            }
                        )
                        StyledMenu(
                            expanded = sortSubMenuExpanded,
                            onDismissRequest = { }
                        ) {
                            SeriesSortMethod.entries.forEach { method ->
                                StyledMenuItem(
                                    text = when (method) {
                                        SeriesSortMethod.Name -> stringResource(R.string.series_sort_name)
                                        SeriesSortMethod.CreateTime -> stringResource(R.string.series_sort_create_time)
                                    },
                                    onClick = {
                                        onSortMethodChange(SeriesSortMethod.Name)
                                        sortSubMenuExpanded = false
                                        mainMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        if (sortPreference.sortMethod == method) {
                                            StyledMenuIcon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = stringResource(R.string.shelf_top_bar_sort_selected)
                                            )
                                        } else {
                                            Spacer(Modifier.size(22.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun SeriesScreenBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    series: Series? = null,
    isSelectingVolume: Boolean = false,
    selectedVolume: Set<Long> = emptySet(),
    onNavIconClick: () -> Unit = {},
    onDeleteIconClick: () -> Unit = {},
    onSelectIconClick: () -> Unit = {},
    onCancelIconClick: () -> Unit = {},
    sortPreference: SortPreference<VolumeSortMethod> = SortPreference(VolumeSortMethod.Name, true),
    onToggleSortAscending: () -> Unit = {},
    onSortMethodChange: (VolumeSortMethod) -> Unit = {}
) {
    val isDeleting = remember { mutableStateOf(false) }
    var mainMenuExpanded by remember { mutableStateOf(false) }
    var sortSubMenuExpanded by remember { mutableStateOf(false) }
    StyledScreenBar(
        modifier = modifier,
        title =
            if (isSelectingVolume) "${selectedVolume.size} " + stringResource(R.string.series_top_bar_selected)
            else series?.seriesName ?: "",
        iconButtonTools = {
            if (isSelectingVolume) {
                if (selectedVolume.isNotEmpty()) {
                    StyledBarIconButton(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.series_delete_volumes),
                        onClick = { isDeleting.value = true }
                    )
                }
                StyledBarIconButton(
                    imageVector =
                        if (selectedVolume.size < (series?.volumeCount
                                ?: 0)
                        ) Icons.Filled.CheckBoxOutlineBlank
                        else Icons.Filled.CheckBox,
                    contentDescription = stringResource(R.string.series_top_bar_select_all),
                    onClick = onSelectIconClick
                )
                StyledBarIconButton(
                    imageVector = Icons.Sharp.Cancel,
                    contentDescription = stringResource(R.string.series_top_bar_cancel_selecting),
                    onClick = onCancelIconClick
                )
            } else {
                Box {
                    StyledBarIconButton(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.sort_volume_sort),
                        onClick = { mainMenuExpanded = !mainMenuExpanded }
                    )
                    StyledMenu(
                        expanded = mainMenuExpanded,
                        onDismissRequest = {
                            sortSubMenuExpanded = false
                            mainMenuExpanded = false
                        }
                    ) {
                        StyledMenuItem(
                            text =
                                if (sortPreference.isAscending) stringResource(R.string.sort_ascending)
                                else stringResource(R.string.sort_descending),
                            onClick = {
                                onToggleSortAscending()
                                sortSubMenuExpanded = false
                                mainMenuExpanded = false
                            },
                            leadingIcon = {
                                StyledMenuIcon(
                                    imageVector = if (sortPreference.isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = stringResource(R.string.sort_order)
                                )
                            }
                        )
                        Box {
                            StyledMenuItem(
                                text = stringResource(R.string.sort_volume_sort),
                                onClick = { sortSubMenuExpanded = !sortSubMenuExpanded },
                                leadingIcon = {
                                    StyledMenuIcon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = stringResource(R.string.sort_volume_sort)
                                    )
                                },
                                trailingIcon = {
                                    StyledMenuIcon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                                        contentDescription = stringResource(R.string.series_top_bar_sort_submenu)
                                    )
                                }
                            )
                            StyledMenu(
                                expanded = sortSubMenuExpanded,
                                onDismissRequest = { }
                            ) {
                                VolumeSortMethod.entries.forEach { method ->
                                    StyledMenuItem(
                                        text = when (method) {
                                            VolumeSortMethod.Name -> stringResource(R.string.volume_sort_name)
                                            VolumeSortMethod.CreateTime -> stringResource(R.string.volume_sort_create_time)
                                            VolumeSortMethod.LastReadTime -> stringResource(R.string.volume_sort_last_read_time)
                                        },
                                        onClick = {
                                            onSortMethodChange(method)
                                            sortSubMenuExpanded = false
                                            mainMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            if (sortPreference.sortMethod == method) {
                                                StyledMenuIcon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = stringResource(R.string.series_top_bar_sort_selected)
                                                )
                                            } else {
                                                Spacer(Modifier.size(22.dp))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        canNavigateBack = true,
        onNavIconClick = onNavIconClick,
        scrollBehavior = scrollBehavior
    )
    if (isDeleting.value) {
        val notification =
            if (selectedVolume.size < (series?.volumeCount ?: 0))
                stringResource(R.string.volume_delete_notification, selectedVolume.size)
            else
                stringResource(
                    R.string.volume_delete_notification,
                    selectedVolume.size
                ) + stringResource(R.string.volume_clear_warning)
        NotificationDialog(
            title = stringResource(R.string.shelf_dialog_title),
            notification = notification,
            onDismiss = { isDeleting.value = false },
            onConfirm = {
                isDeleting.value = false
                onDeleteIconClick()
            }
        )
    }
}