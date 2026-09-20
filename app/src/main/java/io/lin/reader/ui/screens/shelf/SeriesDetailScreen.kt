package io.lin.reader.ui.screens.shelf

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import io.lin.reader.data.database.Volume
import io.lin.reader.data.preferences.VolumeSortMethod
import io.lin.reader.ui.ViewModelProvider
import io.lin.reader.ui.components.blankscreen.BlankScreenContent
import io.lin.reader.ui.components.dialog.DialogWithTextField
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.file.LocalFileSelector
import io.lin.reader.ui.components.loading.LoadingOverlay
import io.lin.reader.ui.components.snackbar.StyledSnackbarHost
import io.lin.reader.ui.maintab.shelf.components.BookImportDialogToSeries
import io.lin.reader.ui.maintab.shelf.components.VolumeBox
import kotlinx.coroutines.launch

@Composable
fun SeriesDetailScreen(
    seriesId: Long,
    modifier: Modifier = Modifier,
    onNavBack: () -> Unit = {},
    entryVolumeReading: (Volume) -> Unit = { },
    viewModel: SeriesDetailScreenViewModel = viewModel(factory = ViewModelProvider.Factory),
) {
    // 注入 ID 到 ViewModel
    LaunchedEffect(seriesId) {
        viewModel.setSeriesId(seriesId)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    // viewModel 信息
    val series by viewModel.seriesStream.collectAsState()
    val volumesPagingItems = viewModel.volumesPagingItemsStream.collectAsLazyPagingItems()
    val selectedVolumes by viewModel.selectedVolumes.collectAsState()

    // 选择模式
    var selectingMode by remember { mutableStateOf(false) }
    val cancelSelectingMode = {
        viewModel.clearSelectedVolumes()
        selectingMode = false
    }
    // 拦截返回按键以退出选择模式
    BackHandler(enabled = selectingMode) {
        cancelSelectingMode()
    }

    // 删除对话框
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 导入
    val fileSelector = LocalFileSelector.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var fileChoosingFinished by remember { mutableStateOf(false) }
    val importSuccessMessage = stringResource(R.string.import_success)

    if (series == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingOverlay()
        }
    } else {
        Scaffold(
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = { StyledSnackbarHost(snackbarHostState) },
            topBar = {
                SeriesDetailScreenAppBar(
                    onNavBack = onNavBack,
                    seriesName = series!!.seriesName,
                    updateSeriesName = viewModel::updateSeriesName,
                    selectingMode = selectingMode,
                    cancelSelectingMode = cancelSelectingMode,
                    selectingAll = selectedVolumes.size == (series?.volumeCount ?: 0),
                    toggleSelectingAll = viewModel::toggleSelectAllVolumes,
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
                    updateSortMethod = viewModel.shelfPreferences::updateVolumeSortMethod,
                    onDeleteIconClick = { showDeleteDialog = true },
                    scrollBehavior = scrollBehavior
                )

                // DeleteDialog
                if (showDeleteDialog) {
                    val notification =
                        if (selectedVolumes.size < (series?.volumeCount ?: 0))
                            stringResource(R.string.volume_delete_notification, selectedVolumes.size)
                        else
                            stringResource(
                                R.string.volume_delete_notification,
                                selectedVolumes.size
                            ) + stringResource(R.string.volume_clear_warning)
                    NotificationDialog(
                        title = stringResource(R.string.shelf_dialog_title),
                        notification = notification,
                        onDismiss = { showDeleteDialog = false },
                        onConfirm = {
                            selectingMode = false
                            showDeleteDialog = false
                            viewModel.deleteSelectedVolumes(onNavBack)
                        }
                    )
                }
            }
        ) { innerPadding ->
            SeriesContent(
                modifier = Modifier.padding(innerPadding),
                lazyPagingItems = volumesPagingItems,
                isSelectingVolume = selectingMode,
                selectedVolume = selectedVolumes,
                changeSelectedState = viewModel::toggleSelectedVolume,
                enterVolume = entryVolumeReading,
                activateSelecting = {
                    // viewModel.clearSelectedVolumes()
                    selectingMode = true
                }
            )
        }

        if (fileChoosingFinished) {
            BookImportDialogToSeries(
                booksDetails = viewModel.volumesToImport.booksDetails,
                destinationSeriesName = series?.seriesName ?: "",
                onDismiss = {
                    fileChoosingFinished = false
                    viewModel.volumeImporter.resetVolumesToImport()
                },
                onSaveConfirm = {
                    viewModel.saveVolumes()
                    fileChoosingFinished = false
                    viewModel.volumeImporter.resetVolumesToImport()
                    scope.launch {
                        snackbarHostState.showSnackbar(importSuccessMessage)
                    }
                }
            )
        }

        if (isLoading) LoadingOverlay()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreenAppBar(
    onNavBack: () -> Unit,
    seriesName: String,
    updateSeriesName: (String) -> Unit,
    selectingMode: Boolean,
    cancelSelectingMode: () -> Unit,
    selectingAll: Boolean,
    toggleSelectingAll: () -> Unit,
    onImportIconClick: () -> Unit,
    updateSortMethod: (VolumeSortMethod) -> Unit, // TODO
    onDeleteIconClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val navBackStr = "Navigate Back"
    val importStr = "Import New Volumes"
    val editStr = "Sort"
    val sortStr = "Sort"
    val cancelStr = "Cancel Selecting"
    val deleteStr = "Delete Series"
    val selectAllStr = "Select All"

    var showTextField by remember { mutableStateOf(false) }
    val onEditIconClick = { showTextField = true }

    val onSort = {/*TODO*/ }

    MediumTopAppBar(
        title = {
            Text(
                text = seriesName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Above
                    ),
                tooltip = {
                    PlainTooltip(
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                            paneTitle = navBackStr
                        }
                    ) {
                        Text(navBackStr)
                    }
                },
                state = rememberTooltipState(),
            ) {
                IconButton(onClick = onNavBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = navBackStr
                    )
                }
            }
        },
        actions = {
            AnimatedContent(
                targetState = selectingMode,
                label = "ActionsAnimation"
            ) { isSelecting ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isSelecting) {
                        TooltipBox(
                            positionProvider =
                                TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Below
                                ),
                            tooltip = {
                                PlainTooltip(
                                    modifier = Modifier.semantics {
                                        liveRegion = LiveRegionMode.Assertive
                                        paneTitle = editStr
                                    }
                                ) {
                                    Text(editStr)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = onEditIconClick) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = editStr,
                                )
                            }
                        }

                        // Sort : TODO: 缺一个二级菜单
                        TooltipBox(
                            positionProvider =
                                TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Below
                                ),
                            tooltip = {
                                PlainTooltip(
                                    modifier = Modifier.semantics {
                                        liveRegion = LiveRegionMode.Assertive
                                        paneTitle = sortStr
                                    }
                                ) {
                                    Text(sortStr)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = onSort) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = sortStr,
                                )
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
                    } else {
                        // Delete Selecting
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
                                            paneTitle = deleteStr
                                        }
                                ) {
                                    Text(deleteStr)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = onDeleteIconClick) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = deleteStr,
                                )
                            }
                        }

                        // Selecting All
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
                                            paneTitle = selectAllStr
                                        }
                                ) {
                                    Text(selectAllStr)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            Checkbox(
                                checked = selectingAll,
                                onCheckedChange = { toggleSelectingAll() }
                            )
                        }

                        // Cancel Selecting
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
                                            paneTitle = cancelStr
                                        }
                                ) {
                                    Text(cancelStr)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = cancelSelectingMode) {
                                Icon(
                                    imageVector = Icons.Filled.Cancel,
                                    contentDescription = cancelStr,
                                )
                            }
                        }
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
    // Edit Series
    if (showTextField) {
        var buffer by remember { mutableStateOf(seriesName) }
        DialogWithTextField(
            value = buffer,
            title = stringResource(R.string.shelf_text_field_dialog_title_edit),
            placeholder = stringResource(R.string.shelf_text_field_dialog_place_holder),
            labelText = stringResource(R.string.shelf_text_field_dialog_label),
            errorText = stringResource(R.string.shelf_text_field_dialog_error),
            onValueChange = { buffer = it },
            validateInput = { buffer.isNotBlank() },
            onDismiss = {
                showTextField = false
            },
            onConfirm = {
                showTextField = false
                updateSeriesName(buffer)
            }
        )
    }
}

@Composable
fun SeriesContent(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<Volume>,
    isSelectingVolume: Boolean,
    selectedVolume: Set<Long>,
    changeSelectedState: (Long) -> Unit,
    enterVolume: (Volume) -> Unit,
    activateSelecting: () -> Unit
) {
    if (lazyPagingItems.itemCount == 0) {
        Box(modifier = modifier.fillMaxSize()) {
            BlankScreenContent(modifier = Modifier.align(alignment = Alignment.Center))
        }
    } else {
        Box(modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(bottom = 200.dp) // 加底部间距以便看到最后一行
            ) {
                // 关键修复：移除 volume != null 的判断，确保槽位始终被占据，并使用更稳定的 key
                items(
                    count = lazyPagingItems.itemCount,
                    key = { index -> lazyPagingItems[index]?.id ?: "vol_loading_$index" }
                ) { index ->
                    val volume = lazyPagingItems[index]
                    if (volume != null) {
                        VolumeBox(
                            volume = volume,
                            isSelected = selectedVolume.contains(volume.id),
                            isSelectingVolume = isSelectingVolume,
                            changeSelectedState = { changeSelectedState(volume.id) },
                            enterVolume = { enterVolume(volume) },
                            activateSelecting = activateSelecting,
                        )
                    } else {
                        // 加载占位
                        Box(modifier = Modifier.size(120.dp))
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun SeriesDetailScreenAppBarPreview() {
    var selectingMode by remember { mutableStateOf(false) }
    var selectingAll by remember { mutableStateOf(false) }

    var seriesName by remember { mutableStateOf("Harry Potter") }

    val onNavBack = {}
    val onImport = {}

    var showDeleteDialog by remember { mutableStateOf(false) }
    val onDelete = { showDeleteDialog = true }


    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SeriesDetailScreenAppBar(
                onNavBack = onNavBack,
                seriesName = seriesName,
                updateSeriesName = { seriesName = it },
                selectingMode = selectingMode,
                cancelSelectingMode = { selectingMode = false },
                selectingAll = selectingAll,
                toggleSelectingAll = { selectingAll = !selectingAll },
                onImportIconClick = onImport,
                updateSortMethod = {},
                onDeleteIconClick = onDelete,
                scrollBehavior = scrollBehavior
            )
            // Keep
            if (showDeleteDialog) {
                val notification =
                    stringResource(R.string.volume_delete_notification, 0)
                NotificationDialog(
                    title = stringResource(R.string.shelf_dialog_title),
                    notification = notification,
                    onDismiss = { showDeleteDialog = false },
                    onConfirm = {
                        showDeleteDialog = false
                    }
                )
            }
        },
        content = { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Button(
                        onClick = {
                            selectingMode = !selectingMode
                        }
                    ) {
                        Text("Toggle Selecting Mode")
                    }
                }

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