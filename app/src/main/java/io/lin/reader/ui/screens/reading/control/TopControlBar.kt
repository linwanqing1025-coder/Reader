package io.lin.reader.ui.screens.reading.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.lin.reader.R
import io.lin.reader.ui.components.dialog.BookmarkAddDialog
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.modifier.settledMarquee
import io.lin.reader.ui.screens.reading.LocalReadingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopControlBar(
    visible: Boolean,
    onNavBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // viewModel 信息
    val viewModel = LocalReadingViewModel.current ?: return
    val uiState by viewModel.uiState.collectAsState()

    val volume = uiState.volume ?: return
    val series = uiState.series ?: return
    val bookmarkList = uiState.bookmarkList
    val outlineList = uiState.outlineList
    val volumeList = uiState.volumeList
    val currentPage = uiState.currentPage
    val isFavorite = volume.isFavorite
    val isPageBookmarked = viewModel.isPageBookmarked(currentPage, bookmarkList)
    val hasOutline = outlineList.isNotEmpty()
    val onFavouriteClick = viewModel::toggleFavouriteState

    // 书签功能
    var showAddBookmarkDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteBookmarkDialog by rememberSaveable { mutableStateOf(false) }
    var bookmarkLabel by rememberSaveable { mutableStateOf("") }
    val onAddBookmarkClick = {
        bookmarkLabel = viewModel.generateAutoBookmarkLabel()
        showAddBookmarkDialog = true
    }

    // 目录菜单
    val directories = listOf(
        "Null", "Series", "Bookmark", "Outline"
    )
    var currentDirectory by remember { mutableStateOf(directories[0]) }
    val seriesDirectoryVisible = currentDirectory == directories[1]
    val bookmarkDirectoryVisible = currentDirectory == directories[2]
    val outlineDirectoryVisible = currentDirectory == directories[3]
    val showSeriesDirectory = { currentDirectory = directories[1] }
    val showBookmarkDirectory = { currentDirectory = directories[2] }
    val showOutlineDirectory = { currentDirectory = directories[3] }
    val closeDirectory = { currentDirectory = directories[0] }


    // Icon 颜色动画
    val favouriteTint by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300),
        label = "FavoriteColor"
    )
    val bookmarkTint by animateColorAsState(
        targetValue = if (isPageBookmarked) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300),
        label = "BookmarkColor"
    )

    // 声明各 Icon 的说明文字变量
    val backDescription = stringResource(R.string.navigate_back)
    val favouriteDescription = stringResource(R.string.reading_topbar_favourite)
    val bookmarkDescription = stringResource(R.string.reading_topbar_bookmark)
    val outlineDescription = stringResource(R.string.reading_topbar_outline)

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        Box {
            Column {
                // 控制栏
                TopAppBar(
                    windowInsets = TopAppBarDefaults.windowInsets,
                    title = {
                        Column(
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null, // 去除点击遮罩的水波纹
                                onClick = showSeriesDirectory,
                            )
                        ) {
                            Text(
                                text = volume.volumeName,
                                modifier = Modifier.settledMarquee(),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = series.seriesName,
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .settledMarquee(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    navigationIcon = {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Below
                            ),
                            tooltip = {
                                PlainTooltip {
                                    Text(backDescription)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = onNavBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = backDescription
                                )
                            }
                        }
                    },
                    actions = {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Below
                            ),
                            tooltip = {
                                PlainTooltip {
                                    Text(favouriteDescription)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = onFavouriteClick) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = favouriteDescription,
                                    tint = favouriteTint
                                )
                            }
                        }
                        
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Below
                            ),
                            tooltip = {
                                PlainTooltip {
                                    Text(bookmarkDescription)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .combinedClickable(
                                        onClick = showBookmarkDirectory,
                                        onLongClick = onAddBookmarkClick,
                                        indication = ripple(bounded = true, radius = 20.dp),
                                        interactionSource = remember { MutableInteractionSource() }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPageBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = bookmarkDescription,
                                    tint = bookmarkTint
                                )
                            }
                        }

                        if (hasOutline) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Below
                                ),
                                tooltip = {
                                    PlainTooltip {
                                        Text(outlineDescription)
                                    }
                                },
                                state = rememberTooltipState(),
                            ) {
                                IconButton(onClick = showOutlineDirectory) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.FormatListBulleted,
                                        contentDescription = outlineDescription
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier
                        .zIndex(1f)
                        .clickable(// 拦截手势
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                )
                // 书签目录
                BookmarkDirectory(
                    visible = bookmarkDirectoryVisible,
                    onDismissRequest = closeDirectory,
                    currentPage = currentPage,
                    bookmarkList = bookmarkList,
                    onBookmarkClick = {
                        viewModel.goToPage(it)
                        closeDirectory()
                    },
                    onAddBookmark = {
                        onAddBookmarkClick()
                        closeDirectory()
                    }
                )
            }
            // 系列目录
            OverlayHorizontalDirectory(
                visible = seriesDirectoryVisible,
                onDismissRequest = closeDirectory,
                title = series.seriesName,
                items = volumeList,
                isItemSelected = { it.id == volume.id },
                itemLabel = { it.volumeName },
                onItemClick = { viewModel.switchVolume(it.id) },
            )
            // 大纲目录
            OverlayHorizontalDirectory(
                visible = outlineDirectoryVisible,
                onDismissRequest = closeDirectory,
                title = "Outline",
                items = outlineList,
                isItemSelected = { outline ->
                    if (currentPage < outline.page) return@OverlayHorizontalDirectory false // 当前页码小于本条页码
                    val index = outlineList.indexOf(outline)
                    val next = outlineList.getOrNull(index + 1)
                    next == null || currentPage < next.page
                },
                itemLabel = { it.title },
                onItemClick = { viewModel.goToPage(it.page) },
                trailingContent = { Text(text = "${it.page + 1}") },
                rightDirection = true
            )
            // 书签对话框
            if (showAddBookmarkDialog) {
                val outlineItem = viewModel.getChapterByPage(uiState.currentPage)
                val chapterInfo = outlineItem?.title
                val location =
                    if (chapterInfo != null) chapterInfo
                    else {
                        val page = uiState.currentPage + 1
                        val core = viewModel.mCore.collectAsState().value
                        val pageCount = volume.totalPages ?: core?.countPages() ?: 0
                        stringResource(R.string.bookmark_item_pages, page, pageCount)
                    }
                BookmarkAddDialog(
                    label = bookmarkLabel,
                    onLabelChange = { bookmarkLabel = it },
                    validateLabelInput = { bookmarkLabel.isNotBlank() },
                    location = location,
                    onDismiss = { showAddBookmarkDialog = false },
                    onConfirm = {
                        viewModel.addBookmark(bookmarkLabel, uiState.currentPage, chapterInfo)
                        showAddBookmarkDialog = false
                    }
                )
            }
            if (showDeleteBookmarkDialog) {
                val notificationText =
                    stringResource(
                        R.string.reading_bookmark_delete_notification,
                        uiState.currentPage + 1
                    )
                NotificationDialog(
                    title = stringResource(R.string.bookmark_dialog_title_delete),
                    notification = notificationText,
                    onDismiss = { showDeleteBookmarkDialog = false },
                    onConfirm = {
                        viewModel.deleteBookmark(uiState.currentPage)
                        showDeleteBookmarkDialog = false
                    }
                )
            }
        }
    }
}
