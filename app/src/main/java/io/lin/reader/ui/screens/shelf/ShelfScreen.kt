package io.lin.reader.ui.screens.shelf

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapVert
import io.lin.reader.ui.components.menu.StyledDropdownMenu
import io.lin.reader.ui.components.menu.StyledDropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import io.lin.reader.R
import io.lin.reader.data.preferences.SeriesSortMethod
import io.lin.reader.data.preferences.SortPreference
import io.lin.reader.navigation.NavKey
import io.lin.reader.ui.components.blankscreen.BlankScreenContent
import io.lin.reader.ui.components.dialog.DialogWithTextField
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.loading.LoadingOverlay
import io.lin.reader.ui.components.snackbar.StyledSnackbarHost
import io.lin.reader.ui.ViewModelProvider
import io.lin.reader.ui.components.file.LocalFileSelector
import io.lin.reader.ui.screens.shelf.components.BookImportDialog
import io.lin.reader.ui.screens.shelf.components.SeriesBox
import kotlinx.coroutines.launch

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScreen(
    modifier: Modifier = Modifier,
    onNavigate: (NavKey) -> Unit = { },
    viewModel: ShelfScreenViewModel = viewModel(factory = ViewModelProvider.Factory),
) {
    // ViewModel 信息
    val seriesPagingItems = viewModel.seriesPagingItems.collectAsLazyPagingItems()
    val shelfPreferences = viewModel.shelfPreferences

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    // 导入书籍
    val fileSelector = LocalFileSelector.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var fileChoosingFinished by remember { mutableStateOf(false) }
    var isCreatingNewSeries by remember { mutableStateOf(false) }
    var isShowingTextField by remember { mutableStateOf(false) }
    var destinationSeriesName by remember { mutableStateOf("") }
    var destinationSeriesId by remember { mutableLongStateOf(0L) }
    val importSuccessMessage = stringResource(R.string.import_success)

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { StyledSnackbarHost(snackbarHostState) },
        topBar = {
            ShelfScreenTopBar(
                sortPreference = SortPreference(
                    shelfPreferences.seriesSortMethod,
                    shelfPreferences.seriesSortAscending
                ),
                updateSortMethod = viewModel.shelfPreferences::updateSeriesSortMethod,
                toggleSortOrder = viewModel.shelfPreferences::toggleSeriesSortAscending,
                onImportIconClick = {
                    isLoading = true
                    fileSelector("application/pdf") { uris ->
                        isLoading = false
                        if (uris.isNotEmpty()) {
                            viewModel.volumeImporter.updateVolumesToImportByUriList(uris)
                            fileChoosingFinished = true
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            ShelfContent(
                shelfUiState = seriesPagingItems,
                enterSeries = {
                    onNavigate(NavKey.SeriesDetail(it))
                },
                editSeriesName = { seriesId, originalName ->
                    destinationSeriesId = seriesId
                    destinationSeriesName = originalName
                    isShowingTextField = true
                },
                deleteSeries = { viewModel.deleteSeries(it) }
            )
        }
    }

    if (fileChoosingFinished) {
        BookImportDialog(
            booksDetails = viewModel.volumesToImport.booksDetails,
            seriesList = seriesPagingItems,
            seriesNameToShow = destinationSeriesName,
            onCreateNewSeriesClick = {
                destinationSeriesName = ""
                destinationSeriesId = 0
                isCreatingNewSeries = true
                isShowingTextField = true
            },
            onExistingSeriesListClick = { existingSeriesId, existingSeriesName ->
                destinationSeriesId = existingSeriesId
                destinationSeriesName = existingSeriesName
                isCreatingNewSeries = false
            },
            onDismiss = {
                fileChoosingFinished = false
                viewModel.volumeImporter.resetVolumesToImport()
                destinationSeriesName = ""
                destinationSeriesId = 0
            },
            onSaveConfirm = { finalSeriesName ->
                if (isCreatingNewSeries) {
                    viewModel.volumeImporter.saveVolumesWithNewSeries(finalSeriesName)
                } else if (destinationSeriesId > 0) {
                    viewModel.volumeImporter.saveVolumeWithExistingSeries(destinationSeriesId)
                }
                fileChoosingFinished = false
                isCreatingNewSeries = false
                viewModel.volumeImporter.resetVolumesToImport()
                destinationSeriesName = ""
                destinationSeriesId = 0
                scope.launch {
                    snackbarHostState.showSnackbar(importSuccessMessage)
                }
            }
        )
    }

    if (isShowingTextField) {
        DialogWithTextField(
            value = destinationSeriesName,
            title =
                if (isCreatingNewSeries) stringResource(R.string.shelf_text_field_dialog_title_create)
                else stringResource(R.string.shelf_text_field_dialog_title_edit),
            placeholder = stringResource(R.string.shelf_text_field_dialog_place_holder),
            labelText = stringResource(R.string.shelf_text_field_dialog_label),
            errorText = stringResource(R.string.shelf_text_field_dialog_error),
            onValueChange = { destinationSeriesName = it },
            validateInput = { destinationSeriesName.isNotBlank() },
            onDismiss = {
                isShowingTextField = false
                if (isCreatingNewSeries) {
                    destinationSeriesName = ""
                }
            },
            onConfirm = {
                isShowingTextField = false
                if (!isCreatingNewSeries) {
                    viewModel.updateSeriesName(
                        newSeriesName = destinationSeriesName,
                        seriesId = destinationSeriesId
                    )
                    destinationSeriesName = ""
                    destinationSeriesId = 0
                }
            }
        )
    }

    if (isLoading) LoadingOverlay()
}

@Composable
private fun ShelfContent(
    modifier: Modifier = Modifier,
    shelfUiState: LazyPagingItems<ShelfItem>,
    enterSeries: (Long) -> Unit,
    editSeriesName: (Long, String) -> Unit,
    deleteSeries: (Long) -> Unit
) {
    if (shelfUiState.itemCount == 0) {
        Box(modifier = modifier.fillMaxSize()) {
            BlankScreenContent(modifier = Modifier.align(alignment = Alignment.Center))
        }
    } else {
        Box(modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(bottom = 200.dp) // 加底部间距以便看到最后一行
            ) {
                items(
                    count = shelfUiState.itemCount,
                    key = { index -> shelfUiState[index]?.series?.id ?: "shelf_loading_$index" }
                ) { index ->
                    val shelfItem = shelfUiState[index]
                    if (shelfItem != null) {
                        var isDeleting by remember { mutableStateOf(false) }
                        SeriesBox(
                            seriesName = shelfItem.series.seriesName,
                            volumeCount = shelfItem.series.volumeCount,
                            volumes = shelfItem.top3Volumes,
                            enterSeries = { enterSeries(shelfItem.series.id) },
                            editSeriesName = {
                                editSeriesName(
                                    shelfItem.series.id,
                                    shelfItem.series.seriesName
                                )
                            },
                            deleteSeries = { isDeleting = true },
                        )
                        if (isDeleting) {
                            NotificationDialog(
                                title = stringResource(R.string.shelf_dialog_title),
                                notification = stringResource(
                                    R.string.series_delete_notification,
                                    shelfItem.series.seriesName,
                                    shelfItem.series.volumeCount
                                ),
                                onDismiss = { isDeleting = false },
                                onConfirm = {
                                    deleteSeries(shelfItem.series.id)
                                    isDeleting = false
                                }
                            )
                        }
                    } else {
                        SeriesBox(seriesName = "...", volumeCount = 1, volumes = emptyList())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShelfScreenTopBar(
    modifier: Modifier = Modifier,
    sortPreference: SortPreference<SeriesSortMethod>,
    updateSortMethod: (SeriesSortMethod) -> Unit,
    toggleSortOrder: () -> Unit,
    onImportIconClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
){
    val sortIconStr = stringResource(R.string.sort_series_sort)
    val sortByStr = stringResource(R.string.sort_by)
    val sortOrderStr = stringResource(R.string.sort_order)
    val ascendingStr =
        if (sortPreference.isAscending) stringResource(R.string.sort_ascending)
        else stringResource(R.string.sort_descending)
    val importStr = stringResource(R.string.import_book)
    TopAppBar(
        modifier = modifier,
        title = {
            Text(stringResource(R.string.navigation_label_shelf), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        actions = {
            // Sort
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Below
                    ),
                tooltip = {
                    PlainTooltip(
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                            paneTitle = sortIconStr
                        }
                    ) {
                        Text(sortIconStr)
                    }
                },
                state = rememberTooltipState(),
            )
            {
                var isMainMenuExpanded by remember { mutableStateOf(false) }
                var isSubMenuExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { isMainMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = sortIconStr,
                    )
                }
                // 一级菜单
                StyledDropdownMenu(
                    expanded = isMainMenuExpanded,
                    onDismissRequest = {
                        isMainMenuExpanded = false
                        isSubMenuExpanded = false
                    },
                ) {
                    StyledDropdownMenuItem(
                        text = { Text(sortOrderStr) },
                        onClick = toggleSortOrder,
                        leadingIcon = {
                            Icon(
                                if (sortPreference.isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                ascendingStr
                            )
                        }
                    )

                    Box {
                        StyledDropdownMenuItem(
                            text = { Text(sortByStr) },
                            leadingIcon = { Icon(Icons.Default.SwapVert, sortByStr) },
                            onClick = { isSubMenuExpanded = !isSubMenuExpanded }
                        )

                        // 二级子菜单 (SubMenu)
                        StyledDropdownMenu(
                            expanded = isSubMenuExpanded,
                            onDismissRequest = { isSubMenuExpanded = false },
                        ) {
                            SeriesSortMethod.entries.forEach { method ->
                                StyledDropdownMenuItem(
                                    text = {
                                        Text(
                                            when (method) {
                                                SeriesSortMethod.Name -> stringResource(
                                                    R.string.series_sort_name
                                                )
                                                SeriesSortMethod.CreateTime -> stringResource(
                                                    R.string.series_sort_create_time
                                                )
                                            }
                                        )
                                    },
                                    onClick = {
                                        updateSortMethod(method)
                                    },
                                    leadingIcon = if (sortPreference.sortMethod == method) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                stringResource(R.string.shelf_top_bar_sort_selected)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // Import New Volumes
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Below
                    ),
                tooltip = {
                    PlainTooltip(
                        modifier =
                            Modifier.semantics {
                                liveRegion = LiveRegionMode.Assertive
                                paneTitle = importStr
                            }
                    ) {
                        Text(importStr)
                    }
                },
                state = rememberTooltipState(),
            ) {
                IconButton(onClick = onImportIconClick) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = importStr,
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
@Suppress("DEPRECATION") // Move to currentWindowAdaptiveInfoV2 when dependency is updated
private fun ShelfScreenScreenTopBarPreview() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ShelfScreenTopBar(
                sortPreference = SortPreference(
                    SeriesSortMethod.Name,
                    true
                ),
                updateSortMethod = {},
                toggleSortOrder = {},
                onImportIconClick = {},
                scrollBehavior = scrollBehavior
            )
        },
        content = { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}