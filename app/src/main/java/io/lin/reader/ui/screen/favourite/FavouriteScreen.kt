package io.lin.reader.ui.screen.favourite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.lin.reader.R
import io.lin.reader.data.database.Series
import io.lin.reader.data.database.Volume
import io.lin.reader.navigation.NavigationDestination
import io.lin.reader.navigation.ScreenRoute
import io.lin.reader.ui.components.blankscreen.BlankScreenContent
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.menu.StyledMenu
import io.lin.reader.ui.components.menu.StyledMenuIcon
import io.lin.reader.ui.components.menu.StyledMenuItem
import io.lin.reader.ui.components.screenbar.StyledBarIconButton
import io.lin.reader.ui.components.screenbar.StyledScreenBar
import io.lin.reader.ui.components.snackbar.StyledSnackbarHost
import io.lin.reader.ui.screen.setting.details.SeriesSortMethod
import io.lin.reader.ui.screen.shelf.components.VolumeBox
import io.lin.reader.ui.viewmodel.AppViewModelProvider
import io.lin.reader.ui.viewmodel.FavouriteScreenViewModel
import kotlinx.coroutines.launch

object FavouriteScreenDestination : NavigationDestination {
    override val route = ScreenRoute.FavouriteScreen.name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouriteScreen(
    modifier: Modifier = Modifier,
    viewModel: FavouriteScreenViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onVolumeClick: (Volume) -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { StyledSnackbarHost(snackbarHostState) },
        topBar = {
            FavouriteScreenTopBar(
                scrollBehavior = scrollBehavior,
                sortMethod = uiState.sortPreference.sortMethod,
                isAscending = uiState.sortPreference.isAscending,
                onSortMethodChange = viewModel::updateSortMethod,
                onToggleOrder = viewModel::toggleSortAscending,
                onRemoveAllFavourites = {
                    val appContext = context.applicationContext
                    if (uiState.groupedFavorites.isEmpty()) {
                        val text = appContext.getString(R.string.favourite_no_favourite_to_clear)
                        scope.launch {
                            snackbarHostState.showSnackbar(text)
                        }
                    } else viewModel.clearAllFavorites(
                        onResult = { count ->
                            val text = appContext.getString(R.string.favourite_favourite_cleared, count)
                            scope.launch {
                                snackbarHostState.showSnackbar(text)
                            }
                        }
                    )
                }
            )
        }
    ) { innerPadding ->
        FavouriteScreenContent(
            modifier = Modifier.padding(innerPadding),
            groupedFavorites = uiState.groupedFavorites,
            removeVolumeFromFavourite = viewModel::removeFromFavorite,
            onVolumeClick = onVolumeClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavouriteScreenTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    sortMethod: SeriesSortMethod,
    isAscending: Boolean,
    onSortMethodChange: (SeriesSortMethod) -> Unit,
    onToggleOrder: () -> Unit,
    onRemoveAllFavourites: () -> Unit,
) {
    val isRemovingAllFavourites = remember { mutableStateOf(false) }
    StyledScreenBar(
        title = stringResource(R.string.navigation_label_favourite),
        scrollBehavior = scrollBehavior,
        iconButtonTools = {
            Box {
                var mainMenuExpanded by remember { mutableStateOf(false) }
                var sortSubMenuExpanded by remember { mutableStateOf(false) }
                StyledBarIconButton(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(R.string.favourite_top_bar_sort),
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
                            if (isAscending) stringResource(R.string.sort_ascending)
                            else stringResource(R.string.sort_descending),
                        onClick = { onToggleOrder() },
                        leadingIcon = {
                            StyledMenuIcon(
                                imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
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
                                    contentDescription = stringResource(R.string.favourite_top_bar_sort_selected)
                                )
                            },
                            trailingIcon = {
                                StyledMenuIcon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                                    contentDescription = stringResource(R.string.favourite_top_bar_sort_submenu)
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
                                        onSortMethodChange(method)
                                        sortSubMenuExpanded = false
                                        mainMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        if (sortMethod == method) {
                                            StyledMenuIcon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = stringResource(R.string.favourite_top_bar_sort_selected)
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
            StyledBarIconButton(
                imageVector = Icons.Filled.DeleteSweep,
                contentDescription = stringResource(R.string.favourite_top_bar_clear_bookmark),
                onClick = { isRemovingAllFavourites.value = true },
            )
        }
    )
    if (isRemovingAllFavourites.value) {
        NotificationDialog(
            title = stringResource(R.string.favourite_dialog_title),
            notification = stringResource(R.string.favourite_clear_notification),
            onDismiss = { isRemovingAllFavourites.value = false },
            onConfirm = {
                onRemoveAllFavourites()
                isRemovingAllFavourites.value = false
            }
        )
    }
}

@Composable
private fun FavouriteScreenContent(
    modifier: Modifier = Modifier,
    groupedFavorites: Map<Series, List<Volume>>,
    removeVolumeFromFavourite: (Volume) -> Unit,
    onVolumeClick: (Volume) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (groupedFavorites.isEmpty()) {
            BlankScreenContent(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = dimensionResource(R.dimen.inner_padding_of_container)
                ),
            ) {
                groupedFavorites.forEach { (series, volumes) ->
                    item(key = "header_${series.id}") {
                        FavouriteSeriesHeader(
                            seriesName = series.seriesName,
                            count = volumes.size,
                            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.inner_padding_of_container))
                        )
                    }
                    item(key = "row_${series.id}") {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.inner_padding_of_container)),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = volumes,
                                key = { it.id }
                            ) { volume ->
                                var expanded by remember { mutableStateOf(false) }

                                Box(contentAlignment = Alignment.TopCenter) {
                                    VolumeBox(
                                        volumeName = volume.volumeName,
                                        coverUri = volume.coverUri,
                                        enterVolume = { onVolumeClick(volume) },
                                        activateSelecting = { expanded = true }
                                    )

                                    StyledMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        StyledMenuItem(
                                            text = stringResource(R.string.favourite_menu_remove),
                                            onClick = {
                                                removeVolumeFromFavourite(volume)
                                                expanded = false
                                            },
                                            leadingIcon = {
                                                StyledMenuIcon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = stringResource(R.string.favourite_menu_remove)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavouriteSeriesHeader(
    seriesName: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = seriesName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "( $count )",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        HorizontalDivider(
            modifier = Modifier
                .padding(start = 16.dp, bottom = 8.dp)
                .weight(1f),
            thickness = 3.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 600)
@Composable
fun FavouriteScreenContentPreview() {
    val series1 = Series(id = 1, seriesName = "Harry Potter", volumeCount = 7)
    val series2 = Series(id = 2, seriesName = "Lord of the Rings", volumeCount = 3)

    val grouped = mapOf(
        series1 to listOf(
            Volume(id = 1, volumeName = "Philosopher's Stone", bookFileUri = "", seriesId = 1),
            Volume(id = 2, volumeName = "Chamber of Secrets", bookFileUri = "", seriesId = 1),
            Volume(id = 3, volumeName = "Prisoner of Azkaban", bookFileUri = "", seriesId = 1),
            Volume(id = 4, volumeName = "Goblet of Fire", bookFileUri = "", seriesId = 1)
        ),
        series2 to listOf(
            Volume(
                id = 5,
                volumeName = "The Fellowship of the Ring",
                bookFileUri = "",
                seriesId = 2
            ),
            Volume(id = 6, volumeName = "The Two Towers", bookFileUri = "", seriesId = 2)
        )
    )

    MaterialTheme {
        FavouriteScreenContent(
            groupedFavorites = grouped,
            removeVolumeFromFavourite = {},
            onVolumeClick = {}
        )
    }
}
