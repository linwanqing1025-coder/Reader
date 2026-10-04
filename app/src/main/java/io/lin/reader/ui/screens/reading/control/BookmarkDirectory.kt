package io.lin.reader.ui.screens.reading.control

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.lin.reader.R
import io.lin.reader.data.database.Bookmark
import io.lin.reader.data.database.ReadingHistory
import io.lin.reader.ui.theme.ReaderTheme

/**
 * 美化后的空状态提示组件
 */
@Composable
private fun EmptyDirectoryText(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.BookmarkBorder,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 重构后的书签目录 (BookmarkDirectory)
 * 1. 内部处理 visible 状态与半透明遮罩背景，动画为顶部进出
 * 2. 布局定位在右上角，右侧保留 12.dp 微小边距，顶部紧贴显示
 */
@Composable
fun BookmarkDirectory(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    currentPage: Int,
    bookmarkList: List<Bookmark>,
    onBookmarkClick: (Int) -> Unit,
    onAddBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 拦截物理返回键
    BackHandler(enabled = visible) {
        onDismissRequest()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        // 半透明暗色遮罩背景
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(0.7f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, // 去除点击遮罩的水波纹
                        onClick = onDismissRequest
                    )
            )
        }

        // 顶端滑入/滑出的面板
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = tween(300)) { -it },
            exit = slideOutVertically(animationSpec = tween(300)) { -it }
        ) {
            Surface(
                modifier = modifier
                    .padding(end = 8.dp) // 与最右边保持微小间隔
                    .widthIn(min = 200.dp, max = 280.dp),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                color = DrawerDefaults.modalContainerColor,
                tonalElevation = DrawerDefaults.PermanentDrawerElevation
            ) {
                Column {
                    // 中间可滚动区域（自适应高度，最高 400.dp）
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 400.dp)
                    ) {
                        if (bookmarkList.isEmpty()) {
                            EmptyDirectoryText(stringResource(R.string.reading_bookmark_directory_no_bookmark))
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(bookmarkList) { bookmark ->
                                    NavigationDrawerItem(
                                        label = {
                                            Text(
                                                text = bookmark.label,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        badge = {
                                            Text(
                                                text = bookmark.history.pageIndex?.let { "${it + 1}" } ?: "位置未知", // TODO: 重排文档的显示适配
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        selected = currentPage == bookmark.history.pageIndex,
                                        onClick = { onBookmarkClick(bookmark.history.pageIndex ?: 0) }, // TODO: 针对重排文档书签跳转的适配
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )
                                }
                            }
                        }
                    }

                    // 底部添加书签操作栏
                    BottomAppBar(
                        modifier = Modifier
                            .height(48.dp)
                            .fillMaxWidth()
                            .clickable { onAddBookmark() },
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        Text(
                            text = stringResource(R.string.reading_bookmark_directory_add_bookmark),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun BookmarkDirectoryPreview() {
    var visible by remember { mutableStateOf(true) }
    val sampleBookmarks = remember {
        listOf(
            Bookmark(id = 1, volumeId = 1, label = "第一章 序幕", history = ReadingHistory(mupdfMark = 0L, time = 0L, pageIndex = 12)),
            Bookmark(
                id = 2,
                volumeId = 1,
                label = "第二章 降临与启示（较长标题测试）",
                history = ReadingHistory(mupdfMark = 0L, time = 0L, pageIndex = 45),
            ),
            Bookmark(id = 3, volumeId = 1, label = "第三章 决战之夜", history = ReadingHistory(mupdfMark = 0L, time = 0L, pageIndex = 128)),
            Bookmark(id = 4, volumeId = 1, label = "终章 归途", history = ReadingHistory(mupdfMark = 0L, time = 0L, pageIndex = 310))
        )
    }

    ReaderTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { visible = true }) {
                Text("打开书签目录")
            }
            BookmarkDirectory(
                visible = visible,
                onDismissRequest = { visible = false },
                currentPage = 45,
                bookmarkList = sampleBookmarks,
                onBookmarkClick = {},
                onAddBookmark = {}
            )
        }
    }
}

@Preview
@Composable
private fun BookmarkDirectoryEmptyPreview() {
    var visible by remember { mutableStateOf(true) }
    ReaderTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { visible = true }) {
                Text("打开空书签目录")
            }
            BookmarkDirectory(
                visible = visible,
                onDismissRequest = { visible = false },
                currentPage = 1,
                bookmarkList = emptyList(),
                onBookmarkClick = {},
                onAddBookmark = {}
            )
        }
    }
}