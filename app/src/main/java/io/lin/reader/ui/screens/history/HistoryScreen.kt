package io.lin.reader.ui.screens.history

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import io.lin.reader.R
import io.lin.reader.data.database.ReadingHistory
import io.lin.reader.data.database.Volume
import io.lin.reader.data.database.VolumeWithSeries
import io.lin.reader.data.preferences.HistoryPreferences
import io.lin.reader.ui.AppViewModelForTest
import io.lin.reader.ui.LocalAppViewModel
import io.lin.reader.ui.ViewModelProvider
import io.lin.reader.ui.components.blankscreen.BlankScreenContent
import io.lin.reader.ui.components.cover.VolumeCover
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.modifier.settledMarquee
import io.lin.reader.ui.components.snackbar.StyledSnackbarHost
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    entryVolumeReading: (Volume) -> Unit = { },
    viewModel: HistoryScreenViewModel = viewModel(factory = ViewModelProvider.Factory),
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val historyPreferences = viewModel.historyPreferences
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
                historyPreferences = historyPreferences,
                onShowUnreadBooksChange = viewModel.historyPreferences::updateShowUnreadBooks,
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
            historyPreferences = historyPreferences,
            readVolumes = readVolumes,
            unreadVolumes = unreadVolumes,
            onUnreadBooksExpandedChange = viewModel.historyPreferences::updateUnreadBooksExpanded,
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
    var isClearingAllHistory by remember { mutableStateOf(false) }

    val displayUnreadStr = stringResource(R.string.history_top_bar_display_unread_books)
    val clearStr = stringResource(R.string.history_top_bar_clear_history)
    val onClearClick = { isClearingAllHistory = true }

    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                stringResource(R.string.navigation_label_history),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            // Display Unread Books
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Below
                ),
                tooltip = {
                    PlainTooltip(
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                            paneTitle = displayUnreadStr
                        }
                    ) {
                        Text(displayUnreadStr)
                    }
                },
                state = rememberTooltipState(),
            ) {
                IconButton(onClick = { onShowUnreadBooksChange(!historyPreferences.showUnreadBooks) }) {
                    Icon(
                        imageVector = if (historyPreferences.showUnreadBooks) Icons.Filled.RemoveRedEye else Icons.Outlined.VisibilityOff,
                        contentDescription = displayUnreadStr,
                    )
                }
            }

            // Clear All History
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Below
                ),
                tooltip = {
                    PlainTooltip(
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                            paneTitle = clearStr
                        }
                    ) {
                        Text(clearStr)
                    }
                },
                state = rememberTooltipState(),
            ) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = clearStr,
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior
    )

    if (isClearingAllHistory) {
        NotificationDialog(
            title = stringResource(R.string.history_dialog_title_clear),
            notification = stringResource(R.string.history_clear_history_notification),
            onDismiss = { isClearingAllHistory = false },
            onConfirm = {
                clearAllHistory()
                isClearingAllHistory = false
            }
        )
    }
}

@Composable
private fun HistoryItem(
    volume: Volume,
    seriesName: String,
    modifier: Modifier = Modifier,
    onCoverClick: () -> Unit = { },
    clearHistory: () -> Unit = { },
) {
    var isClearingItemHistory by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            modifier = Modifier.clickable(onClick = onCoverClick),
            leadingContent = {
                VolumeCover(
                    width = 56.dp,
                    height = 80.dp,
                    volume = volume,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                )
            },
            trailingContent = {
                if (volume.history != null) {
                    IconButton(
                        onClick = { isClearingItemHistory = true }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.history_item_delete),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            overlineContent = {
                Text(
                    text = seriesName,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                if (volume.history != null) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        val progressFraction: Float
                        val progressText: String?

                        if (volume.totalPages != null && volume.totalPages > 0) { // 分页文档
                            val current = (volume.history.pageIndex ?: 0) + 1
                            val total = volume.totalPages
                            progressFraction =
                                (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                            progressText = stringResource(
                                R.string.history_item_pages,
                                current.toString(),
                                total.toString()
                            )
                        }
                        else { // 流式文档
                            progressFraction = volume.history.readProgress.coerceIn(0f, 1f)
                            progressText = volume.history.chapterInfo
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (progressText != null) {
                                Text(
                                    text = progressText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false).graphicsLayer().settledMarquee()
                                )
                            } else {
                                Spacer(Modifier)
                            }

                            val formatter = DateTimeFormatter.ofPattern("yyyy/M/d HH:mm")
                            val formattedTime = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(volume.history.time),
                                ZoneId.systemDefault()
                            ).format(formatter)

                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val percentStr = "${((progressFraction * 1000).toInt() / 10f)}%"
                            Text(
                                text = percentStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.history_item_never_read),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            content = {
                Text(
                    text = volume.volumeName,
                    modifier = Modifier.graphicsLayer().settledMarquee(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
        )
    }

    if (isClearingItemHistory) {
        NotificationDialog(
            title = stringResource(R.string.history_dialog_title_clear),
            notification = stringResource(
                R.string.history_delete_item_notification,
                volume.volumeName
            ),
            onDismiss = { isClearingItemHistory = false },
            onConfirm = {
                isClearingItemHistory = false
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
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
            Spacer(Modifier.width(dimensionResource(id = R.dimen.inner_padding_of_container)))
        }
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryItemPreview() {
    CompositionLocalProvider(
        LocalAppViewModel provides AppViewModelForTest()
    ) {
        MaterialTheme {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 固定版式 (PDF/CBZ) 且读到一半
                HistoryItem(
                    volume = Volume(
                        id = 1,
                        seriesId = 1,
                        volumeName = "Kotlin 核心编程",
                        mimeType = "application/pdf",
                        bookFileUri = "",
                        totalPages = 300, // 非空代表固定版式
                        createTime = System.currentTimeMillis(),
                        history = ReadingHistory(
                            mupdfMark = 0L,
                            time = System.currentTimeMillis() - 3600000,
                            pageIndex = 149,
                            readProgress = 0.5f
                        )
                    ),
                    seriesName = "编程语言系列"
                )

                // 重排文档 (EPUB) 带有章节信息
                HistoryItem(
                    volume = Volume(
                        id = 2,
                        seriesId = 1,
                        volumeName = "从你的全世界路过",
                        mimeType = "application/epub+zip",
                        bookFileUri = "",
                        totalPages = null, // null代表重排文档
                        createTime = System.currentTimeMillis(),
                        history = ReadingHistory(
                            mupdfMark = 123456L,
                            time = System.currentTimeMillis() - 86400000,
                            pageIndex = 42,
                            readProgress = 0.453f,
                            chapterInfo = "第三章：导航栏"
                        )
                    ),
                    seriesName = "随笔散文集"
                )

                // 重排文档 (EPUB) 不带章节信息
                HistoryItem(
                    volume = Volume(
                        id = 3,
                        seriesId = 1,
                        volumeName = "重排测试样书",
                        mimeType = "application/epub+zip",
                        bookFileUri = "",
                        totalPages = null, // null代表重排文档
                        createTime = System.currentTimeMillis(),
                        history = ReadingHistory(
                            mupdfMark = 123456L,
                            time = System.currentTimeMillis() - 86400000,
                            pageIndex = 2,
                            readProgress = 0.125f,
                            chapterInfo = null
                        )
                    ),
                    seriesName = "未分类系列"
                )

                // 从未阅读的书籍
                HistoryItem(
                    volume = Volume(
                        id = 4,
                        seriesId = 1,
                        volumeName = "三体：死神永生",
                        mimeType = "application/epub+zip",
                        bookFileUri = "",
                        totalPages = null,
                        createTime = System.currentTimeMillis(),
                        history = null // null代表未读
                    ),
                    seriesName = "三体系列"
                )
            }
        }
    }
}

