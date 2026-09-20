package io.lin.reader.ui.screens.shelf

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import io.lin.reader.data.preferences.SortPreference
import io.lin.reader.navigation.NavKey
import io.lin.reader.ui.components.blankscreen.BlankScreenContent
import io.lin.reader.ui.components.dialog.DialogWithTextField
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.floatingbutton.FloatingImportButton
import io.lin.reader.ui.components.loading.LoadingOverlay
import io.lin.reader.ui.components.snackbar.StyledSnackbarHost
import io.lin.reader.ui.ViewModelProvider
import io.lin.reader.ui.maintab.shelf.components.BookImportDialog
import io.lin.reader.ui.maintab.shelf.components.SeriesBox
import io.lin.reader.ui.screens.shelf.components.ShelfScreenTopBar
import kotlinx.coroutines.launch

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScreen(
    modifier: Modifier = Modifier,
    onNavigate: (NavKey) -> Unit = { },
    viewModel: ShelfScreenViewModel = viewModel(factory = ViewModelProvider.Factory),
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // 控制 FAB 展开
    var fabExpanded by remember { mutableStateOf(true) }
    val fabNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10) {
                    fabExpanded = false
                } else if (available.y > 10) {
                    fabExpanded = true
                }
                return Offset.Zero
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 局部 UI 交互状态
    var isLoading by remember { mutableStateOf(false) }
    var fileChoosingFinished by remember { mutableStateOf(false) }
    var isCreatingNewSeries by remember { mutableStateOf(false) }
    var destinationSeriesName by remember { mutableStateOf("") }
    var destinationSeriesId by remember { mutableLongStateOf(0L) }
    var isShowingTextField by remember { mutableStateOf(false) }

    val seriesPagingItems = viewModel.seriesPagingItems.collectAsLazyPagingItems()
    val shelfPreferences = viewModel.shelfPreferences
    val importSuccessMessage = stringResource(R.string.import_success)

    Scaffold(
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .nestedScroll(fabNestedScrollConnection),
        snackbarHost = { StyledSnackbarHost(snackbarHostState) },
        topBar = {
            ShelfScreenTopBar(
                scrollBehavior = scrollBehavior,
                sortPreference = SortPreference(
                    shelfPreferences.seriesSortMethod,
                    shelfPreferences.seriesSortAscending
                ),
                onToggleSortAscending = viewModel.shelfPreferences::toggleSeriesSortAscending,
                onSortMethodChange = viewModel.shelfPreferences::updateSeriesSortMethod
            )
        },
        floatingActionButton = {
            FloatingImportButton(
                expanded = fabExpanded,
                enterLoadingState = { isLoading = true },
                afterChoose = {
                    isLoading = false
                    if (it.isNotEmpty()) {
                        fileChoosingFinished = true
                        viewModel.volumeImporter.updateVolumesToImportByUriList(it)
                    }
                }
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
