package io.lin.reader.ui.screens.shelf

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
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
import io.lin.reader.data.preferences.SortPreference
import io.lin.reader.data.preferences.VolumeSortMethod
import io.lin.reader.ui.ViewModelProvider
import io.lin.reader.ui.components.blankscreen.BlankScreenContent
import io.lin.reader.ui.components.dialog.DialogWithTextField
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.file.LocalFileSelector
import io.lin.reader.ui.components.loading.LoadingOverlay
import io.lin.reader.ui.components.menu.StyledDropdownMenu
import io.lin.reader.ui.components.menu.StyledDropdownMenuItem
import io.lin.reader.ui.components.modifier.settledMarquee
import io.lin.reader.ui.components.snackbar.StyledSnackbarHost
import io.lin.reader.ui.screens.shelf.components.BookImportDialogToSeries
import io.lin.reader.ui.screens.shelf.components.VolumeBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "SeriesDetailScreen.kt"

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
    // viewModel 信息
    val series by viewModel.seriesStream.collectAsState()
    val volumesPagingItems = viewModel.volumesPagingItemsStream.collectAsLazyPagingItems()
    val selectedVolumes by viewModel.selectedVolumes.collectAsState()
    val selectionAction = viewModel.selectionAction

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    // 选择模式
    var selectingMode by remember { mutableStateOf(false) }
    val cancelSelectingMode = {
        selectionAction.clearSelectedVolumes()
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
                    selectedCount = selectedVolumes.size,
                    selectingMode = selectingMode,
                    cancelSelectingMode = cancelSelectingMode,
                    selectingAll = selectedVolumes.size == (series?.volumeCount ?: 0),
                    toggleSelectingAll = selectionAction::toggleSelectAllVolumes,
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
                    sortPreference = SortPreference(
                        sortMethod = viewModel.shelfPreferences.volumeSortMethod,
                        isAscending = viewModel.shelfPreferences.volumeSortAscending
                    ),
                    updateSortMethod = viewModel.shelfPreferences::updateVolumeSortMethod,
                    toggleSortOrder = viewModel.shelfPreferences::toggleVolumeSortAscending,
                    onDeleteIconClick = { showDeleteDialog = true },
                    scrollBehavior = scrollBehavior
                )

                // DeleteDialog
                if (showDeleteDialog) {
                    val notification =
                        if (selectedVolumes.size < (series?.volumeCount ?: 0))
                            stringResource(
                                R.string.volume_delete_notification,
                                selectedVolumes.size
                            )
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
                            selectionAction.deleteSelectedVolumes(onNavBack)
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
                selectionAction = selectionAction,
                enterVolume = entryVolumeReading,
                activateSelecting = {
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
private fun SeriesDetailScreenAppBar(
    onNavBack: () -> Unit,
    seriesName: String,
    updateSeriesName: (String) -> Unit,
    selectedCount: Int,
    selectingMode: Boolean,
    cancelSelectingMode: () -> Unit,
    selectingAll: Boolean,
    toggleSelectingAll: () -> Unit,
    onImportIconClick: () -> Unit,
    sortPreference: SortPreference<VolumeSortMethod>,
    updateSortMethod: (VolumeSortMethod) -> Unit,
    toggleSortOrder: () -> Unit,
    onDeleteIconClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val navBackStr = stringResource(R.string.navigate_back)
    val importStr = stringResource(R.string.import_book)
    val editStr = stringResource(R.string.shelf_menu_edit_name)
    val sortIconStr = stringResource(R.string.sort_volume_sort)
    val sortByStr = stringResource(R.string.sort_by)
    val sortOrderStr = stringResource(R.string.sort_order)
    val ascendingStr =
        if (sortPreference.isAscending) stringResource(R.string.sort_ascending)
        else stringResource(R.string.sort_descending)
    val cancelStr = stringResource(R.string.series_top_bar_cancel_selecting)
    val deleteStr = stringResource(R.string.series_delete_volumes)
    val selectAllStr = stringResource(R.string.series_top_bar_select_all)

    var showTextField by remember { mutableStateOf(false) }
    val onEditIconClick = { showTextField = true }

    MediumTopAppBar(
        title = {
            AnimatedContent(
                targetState = selectingMode,
                label = "ActionsAnimation"
            ) { isSelecting ->
                val text: @Composable ((String) -> Unit) = {
                    Text(
                        text = it,
                        modifier = Modifier.settledMarquee(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!isSelecting) {
                    text(seriesName)
                } else {
                    text("$selectedCount " + stringResource(R.string.series_top_bar_selected))
                }
            }
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
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = editStr,
                                )
                            }
                        }

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
                        ) {
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
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.SwapVert,
                                                sortByStr
                                            )
                                        },
                                        onClick = { isSubMenuExpanded = !isSubMenuExpanded }
                                    )

                                    // 二级子菜单 (SubMenu)
                                    StyledDropdownMenu(
                                        expanded = isSubMenuExpanded,
                                        onDismissRequest = { isSubMenuExpanded = false },
                                    ) {
                                        VolumeSortMethod.entries.forEach { method ->
                                            StyledDropdownMenuItem(
                                                text = {
                                                    Text(
                                                        when (method) {
                                                            VolumeSortMethod.Name -> stringResource(
                                                                R.string.volume_sort_name
                                                            )

                                                            VolumeSortMethod.CreateTime -> stringResource(
                                                                R.string.volume_sort_create_time
                                                            )

                                                            VolumeSortMethod.LastReadTime -> stringResource(
                                                                R.string.volume_sort_last_read_time
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


/**
 * 根据坐标 Offset 查找当前可见区域内对应的 Item Index
 */
private fun LazyGridState.getItemIndexAtOffset(offset: Offset): Int? {
    return layoutInfo.visibleItemsInfo.firstOrNull { item ->
        val x = item.offset.x
        val y = item.offset.y
        val width = item.size.width
        val height = item.size.height
        offset.x >= x && offset.x <= x + width && offset.y >= y && offset.y <= y + height
    }?.index
}

@Composable
private fun SeriesContent(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<Volume>,
    isSelectingVolume: Boolean,
    selectedVolume: Set<Long>,
    selectionAction: SeriesDetailScreenViewModel.SelectionAction,
    enterVolume: (Volume) -> Unit,
    activateSelecting: () -> Unit
) {
    if (lazyPagingItems.itemCount == 0) {
        Box(modifier = modifier.fillMaxSize()) {
            BlankScreenContent(modifier = Modifier.align(alignment = Alignment.Center))
        }
    } else {
        Box(modifier.fillMaxSize()) {
            val gridState = rememberLazyGridState()
            val density = LocalDensity.current
            val viewConfiguration = LocalViewConfiguration.current
            val longPressTimeoutBuffer = viewConfiguration.longPressTimeoutMillis + 100L

            // 拖选过程中的临时起点与起始选中状态缓存
            var dragStartIndex by remember { mutableStateOf<Int?>(null) }
            var initialSelectedIds by remember { mutableStateOf(setOf<Long>()) }
            var lastLongPressTime by remember { mutableLongStateOf(0L) }


            val currentSelectedVolume by rememberUpdatedState(selectedVolume)
            val currentSelectionAction by rememberUpdatedState(selectionAction)
            val currentLazyPagingItems by rememberUpdatedState(lazyPagingItems)

            // 【新增】实时记录拖拽触点坐标与网格容器高度
            var currentPointerPosition by remember { mutableStateOf<Offset?>(null) }
            var gridHeightPx by remember { mutableIntStateOf(0) }

            // 触发滚动的边缘阈值（如 60.dp）
            val thresholdPx = with(density) { 60.dp.toPx() }

            // 【新增】辅助方法：计算触点对应的 Target Index（支持滑出屏幕外时的边界保护）
            fun getTargetIndex(offset: Offset): Int? {
                val visibleItems = gridState.layoutInfo.visibleItemsInfo
                if (visibleItems.isEmpty()) return null

                return when {
                    // 手指拖出顶部：选中当前最顶部可见项
                    offset.y < 0 -> visibleItems.first().index
                    // 手指拖出底部：选中当前最底部可见项
                    offset.y > gridHeightPx -> visibleItems.last().index
                    // 在视口内：精确定位或选最贴近的项
                    else -> gridState.getItemIndexAtOffset(offset)
                        ?: visibleItems.minByOrNull { item ->
                            val centerY = item.offset.y + item.size.height / 2f
                            abs(centerY - offset.y)
                        }?.index
                }
            }

            // 【新增】自动滚动循环协程
            LaunchedEffect(currentPointerPosition, dragStartIndex) {
                val pos = currentPointerPosition
                val start = dragStartIndex
                if (pos == null || start == null || gridHeightPx <= 0) return@LaunchedEffect

                // 计算当前触点在顶部还是底部触发区，算出滚动速度
                val scrollSpeed = when {
                    pos.y < thresholdPx -> {
                        val overDistance = thresholdPx - pos.y
                        -(overDistance * 0.4f).coerceIn(8f, 50f) // 向上滚动
                    }

                    pos.y > gridHeightPx - thresholdPx -> {
                        val overDistance = pos.y - (gridHeightPx - thresholdPx)
                        (overDistance * 0.4f).coerceIn(8f, 50f) // 向下滚动
                    }

                    else -> 0f
                }

                // 只要处在边缘触发区，就持续进行滚动与选中区域重算
                if (scrollSpeed != 0f) {
                    while (true) {
                        gridState.scrollBy(scrollSpeed)

                        // 滚动后重新根据最新位置更新选中项
                        currentPointerPosition?.let { currentPos ->
                            val current = getTargetIndex(currentPos)
                            if (current != null) {
                                val minIndex = min(start, current)
                                val maxIndex = max(start, current)
                                val rangeSet = (minIndex..maxIndex).toSet()
                                val idSet = rangeSet.mapNotNull { idx ->
                                    if (idx in 0 until currentLazyPagingItems.itemCount) {
                                        currentLazyPagingItems[idx]?.id
                                    } else null
                                }.toSet()

                                currentSelectionAction.setSelectedVolumes(initialSelectedIds + idSet)
                            }
                        }

                        delay(16.milliseconds) // 约 60fps 的刷新频率
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { gridHeightPx = it.height } // 获取容器实际高度
                    // 容器层处理长按及拖动手势
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                // 1. 命中检测，获取长按起始项
                                val index = gridState.getItemIndexAtOffset(offset)
                                if (index != null) {
                                    val volumeId =
                                        if (index in 0 until currentLazyPagingItems.itemCount) {
                                            currentLazyPagingItems[index]?.id
                                        } else null

                                    if (volumeId != null) {
                                        activateSelecting()
                                        dragStartIndex = index
                                        initialSelectedIds = currentSelectedVolume
                                        currentPointerPosition = offset // 记录初始触点
                                        lastLongPressTime = System.currentTimeMillis() // 记录长按触发时间
                                        Log.d(
                                            TAG,
                                            "onDragStart: initialSelectedIds = $initialSelectedIds"
                                        )
                                        // 将当前长按项加入选中列表
                                        currentSelectionAction.addToSelected(volumeId)
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPointerPosition = change.position // 实时更新触点

                                val start =
                                    dragStartIndex ?: return@detectDragGesturesAfterLongPress
                                // 2. 获取当前手指滑过位置对应的 Item (支持滑出边界的 getTargetIndex)
                                val current = getTargetIndex(change.position)
                                    ?: return@detectDragGesturesAfterLongPress

                                // 3. 计算起点到当前位置的闭区间，并合并到选中集合中
                                val minIndex = min(start, current)
                                val maxIndex = max(start, current)
                                val rangeSet = (minIndex..maxIndex).toSet()
                                val idSet = rangeSet.mapNotNull { idx ->
                                    if (idx in 0 until currentLazyPagingItems.itemCount) {
                                        currentLazyPagingItems[idx]?.id
                                    } else null
                                }.toSet()

                                currentSelectionAction.setSelectedVolumes(initialSelectedIds + idSet)
                            },
                            onDragEnd = {
                                dragStartIndex = null
                                currentPointerPosition = null // 清空触点，停止自动滚动
                            },
                            onDragCancel = {
                                dragStartIndex = null
                                currentPointerPosition = null
                            }
                        )
                    },
                contentPadding = PaddingValues(bottom = 200.dp) // 加底部间距以便看到最后一行
            ) {
                items(
                    count = lazyPagingItems.itemCount,
                    key = { index ->
                        lazyPagingItems[index]?.id ?: "vol_loading_$index"
                    }
                ) { index ->
                    val volume = lazyPagingItems[index]
                    if (volume != null) {
                        VolumeBox(
                            volume = volume,
                            isSelected = selectedVolume.contains(volume.id),
                            isSelectingVolume = isSelectingVolume,
                            onSelectedChange = {
                                if (System.currentTimeMillis() - lastLongPressTime > longPressTimeoutBuffer) {
                                    selectionAction.toggleVolumeSelected(volume.id)
                                }
                            },
                            onClick = {
                                if (System.currentTimeMillis() - lastLongPressTime > longPressTimeoutBuffer) {
                                    enterVolume(volume)
                                }
                            }
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


@Preview
@Composable
private fun SeriesDetailScreenAppBarPreview() {
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
                selectedCount = 0,
                selectingMode = selectingMode,
                cancelSelectingMode = { selectingMode = false },
                selectingAll = selectingAll,
                toggleSelectingAll = { selectingAll = !selectingAll },
                onImportIconClick = onImport,
                sortPreference = SortPreference(
                    sortMethod = VolumeSortMethod.Name,
                    isAscending = true
                ),
                updateSortMethod = {},
                toggleSortOrder = {},
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