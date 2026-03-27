package io.lin.reader.ui.screen.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import io.lin.reader.R
import io.lin.reader.data.database.Volume
import io.lin.reader.data.database.VolumeWithSeries
import io.lin.reader.data.preferences.HistoryPreferences
import io.lin.reader.navigation.NavigationDestination
import io.lin.reader.navigation.ScreenRoute
import io.lin.reader.ui.components.blankscreen.BlankScreenContent
import io.lin.reader.ui.components.cover.VolumeCover
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.screenbar.StyledBarIconButton
import io.lin.reader.ui.components.screenbar.StyledScreenBar
import io.lin.reader.ui.components.snackbar.StyledSnackbarHost
import io.lin.reader.ui.viewmodel.AppViewModelProvider
import io.lin.reader.ui.viewmodel.HistoryScreenViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object HistoryScreenDestination : NavigationDestination {
    override val route = ScreenRoute.HistoryScreen.name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    entryVolumeReading: (Volume) -> Unit = { },
    viewModel: HistoryScreenViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val historyScreenUiState by viewModel.historyScreenUiState.collectAsState()
    val readVolumes = viewModel.readVolumesUiState.collectAsLazyPagingItems()
    val unreadVolumes = viewModel.unreadVolumesUiState.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { StyledSnackbarHost(snackbarHostState) },
        topBar = {
            HistoryScreenTopBar(
                scrollBehavior = scrollBehavior,
                historyPreferences = historyScreenUiState.historyPreferences,
                onShowUnreadBooksChange = viewModel::updateShowUnreadBooks,
                clearAllHistory = {
                    val appContext = context.applicationContext
                    if (readVolumes.itemCount == 0) {
                        val text = appContext.getString(R.string.history_no_history_to_clear)
                        scope.launch {
                            snackbarHostState.showSnackbar(text)
                        }
                    } else viewModel.clearAllHistory(
                        onResult = { count ->
                            val text = appContext.getString(R.string.history_history_cleared, count)
                            scope.launch {
                                snackbarHostState.showSnackbar(text)
                            }
                        }
                    )
                },
            )
        },
    ) { innerPadding ->
        HistoryContent(
            modifier = Modifier.padding(innerPadding),
            historyPreferences = historyScreenUiState.historyPreferences,
            readVolumes = readVolumes,
            unreadVolumes = unreadVolumes,
            onUnreadBooksExpandedChange = viewModel::updateUnreadBooksExpanded,
            entryVolumeReading = { entryVolumeReading(it) },
            deleteVolumeHistory = { viewModel.clearHistoryInSingleVolume(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreenTopBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    historyPreferences: HistoryPreferences,
    onShowUnreadBooksChange: (Boolean) -> Unit,
    clearAllHistory: () -> Unit = { },
) {
    val isClearingAllHistory = remember { mutableStateOf(false) }
    StyledScreenBar(
        modifier = modifier,
        title = stringResource(R.string.navigation_label_history),
        iconButtonTools = {
            StyledBarIconButton(
                imageVector = if (historyPreferences.showUnreadBooks) Icons.Filled.RemoveRedEye else Icons.Outlined.VisibilityOff,
                contentDescription = stringResource(R.string.history_top_bar_display_unread_books),
                onClick = { onShowUnreadBooksChange(!historyPreferences.showUnreadBooks) },
            )
            StyledBarIconButton(
                imageVector = Icons.Filled.DeleteSweep,
                contentDescription = stringResource(R.string.history_top_bar_clear_history),
                onClick = { isClearingAllHistory.value = true },
            )
        },
        scrollBehavior = scrollBehavior
    )
    if (isClearingAllHistory.value) {
        NotificationDialog(
            title = stringResource(R.string.history_dialog_title_clear),
            notification = stringResource(R.string.history_clear_history_notification),
            onDismiss = { isClearingAllHistory.value = false },
            onConfirm = {
                clearAllHistory()
                isClearingAllHistory.value = false
            }
        )
    }
}

@Preview
@Composable
private fun HistoryItem(
    modifier: Modifier = Modifier,
    volume: Volume = Volume(bookFileUri = "", seriesId = 0, lastReadTime = null),
    seriesName: String = "Default Series Name",
    onCoverClick: () -> Unit = { },
    clearHistory: () -> Unit = { },
) {
    val isClearingItemHistory = remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.inner_padding_of_container),
                    end = dimensionResource(R.dimen.half_inner_padding_of_container),
                    top = dimensionResource(R.dimen.half_inner_padding_of_container),
                    bottom = dimensionResource(R.dimen.half_inner_padding_of_container),
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            VolumeCover(
                modifier = Modifier.clickable(onClick = onCoverClick),
                width = 50.dp,
                height = 70.dp,
                coverUri = volume.coverUri,
            )
            // 阅读记录信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(75.dp)
                    .padding(
                        start = dimensionResource(R.dimen.inner_padding_of_container)
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // 书籍名
                    Text(
                        text = volume.volumeName,
                        modifier = Modifier.padding(end = 24.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // 所属系列名
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = seriesName,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 上次阅读
                Column(
                    Modifier.align(Alignment.End),
                    horizontalAlignment = Alignment.End
                ) {
                    if (volume.lastReadTime != null) {
                        val formatter = DateTimeFormatter.ofPattern("yyyy/M/d HH:mm")
                        val formattedTime = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(volume.lastReadTime),
                            ZoneId.systemDefault()
                        ).format(formatter)
                        Text(
                            text = stringResource(
                                R.string.history_item_pages,
                                volume.lastReadPage,
                                volume.totalPages
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.history_item_never_read),
                            modifier = Modifier.padding(end = dimensionResource(R.dimen.inner_padding_of_container)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            if (volume.lastReadTime != null) {
                IconButton(
                    onClick = { isClearingItemHistory.value = true },
                    modifier = Modifier.padding(start = 8.dp),
                    content = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.history_item_delete),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                )
            }
        }
    }

    if (isClearingItemHistory.value) {
        NotificationDialog(
            title = stringResource(R.string.history_dialog_title_clear),
            notification = stringResource(
                R.string.history_delete_item_notification,
                volume.volumeName
            ),
            onDismiss = { isClearingItemHistory.value = false },
            onConfirm = {
                isClearingItemHistory.value = false
                clearHistory()
            }
        )
    }
}

@Composable
private fun HistoryContent(
    modifier: Modifier = Modifier,
    historyPreferences: HistoryPreferences,
    readVolumes: LazyPagingItems<VolumeWithSeries>,
    unreadVolumes: LazyPagingItems<VolumeWithSeries>,
    onUnreadBooksExpandedChange: (Boolean) -> Unit,
    entryVolumeReading: (Volume) -> Unit = { },
    deleteVolumeHistory: (Volume) -> Unit = { },
) {
    val showDivider = historyPreferences.showUnreadBooks

    val topWeight by animateFloatAsState(
        targetValue = if (!historyPreferences.unreadBooksExpanded) 1f else 0.001f,
        animationSpec = tween(durationMillis = 500),
        label = "topWeight"
    )
    val bottomWeight by animateFloatAsState(
        targetValue = if (historyPreferences.unreadBooksExpanded) 1f else 0.001f,
        animationSpec = tween(durationMillis = 500),
        label = "bottomWeight"
    )

    Column(modifier = modifier.fillMaxSize()) {
        // 历史记录列表容器
        AnimatedVisibility(
            visible = !historyPreferences.unreadBooksExpanded,
            modifier = Modifier
                .weight(topWeight)
                .fillMaxWidth(),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            if (readVolumes.itemCount == 0) {
                Box(Modifier.fillMaxSize()) {
                    BlankScreenContent(modifier = Modifier.align(alignment = Alignment.Center))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = dimensionResource(R.dimen.inner_padding_of_container)),
                    contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.inner_padding_of_container)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.inner_padding_of_container))
                ) {
                    items(readVolumes.itemCount) { index ->
                        val item = readVolumes[index]
                        if (item != null) {
                            HistoryItem(
                                volume = item.volume,
                                seriesName = item.series.seriesName,
                                onCoverClick = { entryVolumeReading(item.volume) },
                                clearHistory = { deleteVolumeHistory(item.volume) }
                            )
                        }
                    }
                }
            }
        }

        // 分隔线与动画
        AnimatedVisibility(
            visible = showDivider,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            ListDivider(
                text = stringResource(R.string.history_unread_books) + "${unreadVolumes.itemCount}",
                isExpanded = historyPreferences.unreadBooksExpanded,
                onClick = { onUnreadBooksExpandedChange(!historyPreferences.unreadBooksExpanded) }
            )
        }

        // 未读列表容器
        AnimatedVisibility(
            visible = historyPreferences.unreadBooksExpanded,
            modifier = Modifier
                .weight(bottomWeight)
                .fillMaxWidth(),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            if (unreadVolumes.itemCount > 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = dimensionResource(R.dimen.inner_padding_of_container)),
                    contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.inner_padding_of_container)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.inner_padding_of_container))
                ) {
                    items(unreadVolumes.itemCount) { index ->
                        val item = unreadVolumes[index]
                        if (item != null) {
                            HistoryItem(
                                volume = item.volume,
                                seriesName = item.series.seriesName,
                                onCoverClick = { entryVolumeReading(item.volume) },
                                clearHistory = { deleteVolumeHistory(item.volume) }
                            )
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize()) {
                    BlankScreenContent(modifier = Modifier.align(alignment = Alignment.Center))
                }
            }
        }
    }
}


@Composable
private fun ListDivider(
    modifier: Modifier = Modifier,
    text: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(id = R.dimen.half_inner_padding_of_container)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(dimensionResource(id = R.dimen.inner_padding_of_container)))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall
            )
            Icon(
                imageVector =
                    if (isExpanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
            )
            Spacer(Modifier.width(dimensionResource(id = R.dimen.inner_padding_of_container)))
        }
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}
