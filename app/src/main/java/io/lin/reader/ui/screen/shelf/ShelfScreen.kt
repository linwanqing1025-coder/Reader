package io.lin.reader.ui.screen.shelf

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import io.lin.reader.R
import io.lin.reader.data.database.Volume
import io.lin.reader.navigation.NavigationDestination
import io.lin.reader.navigation.ScreenRoute
import io.lin.reader.ui.components.blankscreen.BlankScreenContent
import io.lin.reader.ui.components.dialog.DialogWithTextField
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.floatingbutton.FloatingImportButton
import io.lin.reader.ui.components.loading.LoadingOverlay
import io.lin.reader.ui.components.snackbar.StyledSnackbarHost
import io.lin.reader.ui.screen.shelf.components.BookImportDialog
import io.lin.reader.ui.screen.shelf.components.BookImportDialogToSeries
import io.lin.reader.ui.screen.shelf.components.SeriesBox
import io.lin.reader.ui.screen.shelf.components.SeriesScreenBar
import io.lin.reader.ui.screen.shelf.components.ShelfScreenTopBar
import io.lin.reader.ui.screen.shelf.components.VolumeBox
import io.lin.reader.ui.viewmodel.AppViewModelProvider
import io.lin.reader.ui.viewmodel.ShelfItem
import io.lin.reader.ui.viewmodel.ShelfScreenViewModel
import kotlinx.coroutines.launch

object ShelfScreenDestination : NavigationDestination {
    override val route = ScreenRoute.ShelfScreen.name
}

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScreen(
    modifier: Modifier = Modifier,
    entryVolumeReading: (Volume) -> Unit = { },
    viewModel: ShelfScreenViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    // 改进：使用基于滚动方向的逻辑来控制 FAB 展开
    var fabExpanded by remember { mutableStateOf(true) }
    val fabNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10) { // 向下滚动（内容上移），折叠 FAB
                    fabExpanded = false
                } else if (available.y > 10) { // 向上滚动（内容下移），展开 FAB
                    fabExpanded = true
                }
                return Offset.Zero
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currentSeriesId by viewModel.currentSeriesId.collectAsState()
    val showingSeriesDetails = currentSeriesId != 0L
    val isSelectingVolume by viewModel.isSelectingVolume.collectAsState()
    
    // 接管返回按键
    BackHandler(enabled = showingSeriesDetails) {
        if (isSelectingVolume) {
            viewModel.setSelectingVolume(false)
            viewModel.clearSelectedVolume()
        } else {
            viewModel.resetSeriesId()
        }
    }

    // 局部 UI 交互状态
    var isLoading by remember { mutableStateOf(false) }
    var fileChoosingFinished by remember { mutableStateOf(false) }
    var isCreatingNewSeries by remember { mutableStateOf(false) }
    var destinationSeriesName by remember { mutableStateOf("") }
    var destinationSeriesId by remember { mutableStateOf(0L) }
    var isShowingTextField by remember { mutableStateOf(false) }

    val seriesPagingItems = viewModel.seriesPagingItems.collectAsLazyPagingItems()
    val seriesSortPreference by viewModel.seriesSortPreference.collectAsState()

    val currentSeries by viewModel.currentSeries.collectAsState()
    val selectedVolume by viewModel.selectedVolume.collectAsState()
    val currentSeriesVolumesPagingItems =
        viewModel.currentSeriesVolumesPagingItems.collectAsLazyPagingItems()
    val volumeSortPreference by viewModel.volumeSortPreference.collectAsState()

    // 监听导入成功的反馈
    val importSuccessMessage = stringResource(R.string.import_success)
    Scaffold(
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .nestedScroll(fabNestedScrollConnection), // 添加 FAB 的滚动监听
        snackbarHost = { StyledSnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedContent(
                targetState = showingSeriesDetails,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(
                            animationSpec = tween(300)
                        ))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(
                                animationSpec = tween(300)
                            ))
                    } else {
                        (slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(
                            animationSpec = tween(300)
                        ))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(
                                animationSpec = tween(300)
                            ))
                    }
                },
                label = "ShelfTopBarTransition"
            ) { isShowingDetails ->
                if (isShowingDetails) {
                    SeriesScreenBar(
                        scrollBehavior = scrollBehavior,
                        series = currentSeries,
                        isSelectingVolume = isSelectingVolume,
                        selectedVolume = selectedVolume,
                        onNavIconClick = {
                            viewModel.resetSeriesId()
                        },
                        onDeleteIconClick = {
                            viewModel.deleteSelectedVolumes()
                        },
                        onSelectIconClick = viewModel::toggleSelectAllVolumes,
                        onCancelIconClick = {
                            viewModel.setSelectingVolume(false)
                            viewModel.clearSelectedVolume()
                        },
                        sortPreference = volumeSortPreference,
                        onToggleSortAscending = viewModel::toggleVolumeSortAscending,
                        onSortMethodChange = viewModel::updateVolumeSortMethod
                    )
                } else {
                    ShelfScreenTopBar(
                        scrollBehavior = scrollBehavior,
                        sortPreference = seriesSortPreference,
                        onToggleSortAscending = viewModel::toggleSeriesSortAscending,
                        onSortMethodChange = viewModel::updateSeriesSortMethod
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingImportButton(
                expanded = fabExpanded,
                enterLoadingState = { isLoading = true },
                afterChoose = {
                    isLoading = false
                    if (it.isNotEmpty()) {
                        fileChoosingFinished = true
                        viewModel.updateVolumesToImportByUriList(it)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AnimatedContent(
                targetState = showingSeriesDetails,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(
                            animationSpec = tween(300)
                        ))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(
                                animationSpec = tween(300)
                            ))
                    } else {
                        (slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(
                            animationSpec = tween(300)
                        ))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(
                                animationSpec = tween(300)
                            ))
                    }
                },
                label = "ShelfContentTransition"
            ) { isShowingDetails ->
                if (isShowingDetails) {
                    SeriesContent(
                        lazyPagingItems = currentSeriesVolumesPagingItems,
                        isSelectingVolume = isSelectingVolume,
                        selectedVolume = selectedVolume,
                        changeSelectedState = { viewModel.toggleSelectedVolume(it) },
                        enterVolume = { entryVolumeReading(it) },
                        activateSelecting = { viewModel.setSelectingVolume(true) }
                    )
                } else {
                    ShelfContent(
                        shelfUiState = seriesPagingItems,
                        enterSeries = {
                            viewModel.updateSeriesId(it)
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
        }
    }

    if (fileChoosingFinished) {
        if (!showingSeriesDetails) {
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
                    viewModel.resetVolumesToImport()
                    destinationSeriesName = ""
                    destinationSeriesId = 0
                },
                onSaveConfirm = { finalSeriesName ->
                    if (isCreatingNewSeries) {
                        viewModel.saveVolumesWithNewSeries(finalSeriesName)
                    } else if (destinationSeriesId > 0) {
                        viewModel.saveVolumeWithExistingSeries(destinationSeriesId)
                    }
                    fileChoosingFinished = false
                    isCreatingNewSeries = false
                    viewModel.resetVolumesToImport()
                    destinationSeriesName = ""
                    destinationSeriesId = 0
                    // 显示提示
                    scope.launch {
                        snackbarHostState.showSnackbar(importSuccessMessage)
                    }
                }
            )
        } else {
            BookImportDialogToSeries(
                booksDetails = viewModel.volumesToImport.booksDetails,
                destinationSeriesName = currentSeries?.seriesName ?: "",
                onDismiss = { fileChoosingFinished = false },
                onSaveConfirm = {
                    if (currentSeriesId > 0) viewModel.saveVolumeWithExistingSeries(currentSeriesId)
                    viewModel.resetVolumesToImport()
                    fileChoosingFinished = false
                    // 显示提示
                    scope.launch {
                        snackbarHostState.showSnackbar(importSuccessMessage)
                    }
                }
            )
        }
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
private fun SeriesContent(
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
            ) {
                // 关键修复：移除 volume != null 的判断，确保槽位始终被占据，并使用更稳定的 key
                items(
                    count = lazyPagingItems.itemCount,
                    key = { index -> lazyPagingItems[index]?.id ?: "vol_loading_$index" }
                ) { index ->
                    val volume = lazyPagingItems[index]
                    VolumeBox(
                        volumeName = volume?.volumeName ?: "...",
                        isSelected = if (volume != null) selectedVolume.contains(volume.id) else false,
                        isSelectingVolume = isSelectingVolume,
                        coverUri = volume?.coverUri,
                        changeSelectedState = { volume?.let { changeSelectedState(it.id) } },
                        enterVolume = { volume?.let { enterVolume(it) } },
                        activateSelecting = activateSelecting
                    )
                }
            }
        }
    }
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
            ) {
                // 关键修复：移除 shelfItem != null 的判断，使用稳定 key
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
                            coverUriList = shelfItem.top3CoverUris,
                            enterSeries = { enterSeries(shelfItem.series.id) },
                            editSeriesName = {
                                editSeriesName(
                                    shelfItem.series.id,
                                    shelfItem.series.seriesName
                                )
                            },
                            deleteSeries = { isDeleting = true }
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
                        // 加载中的系列占位（可选）
                        SeriesBox(seriesName = "...", volumeCount = 1, coverUriList = emptyList())
                    }
                }
            }
        }
    }
}
