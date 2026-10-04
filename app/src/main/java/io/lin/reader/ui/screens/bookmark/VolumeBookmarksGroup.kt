package io.lin.reader.ui.screens.bookmark

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.lin.reader.data.database.ReadingHistory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.lin.reader.R
import io.lin.reader.data.database.Bookmark
import io.lin.reader.data.database.Volume
import io.lin.reader.data.database.VolumeWithBookmarks
import io.lin.reader.ui.components.cover.VolumeCover
import androidx.compose.runtime.CompositionLocalProvider
import io.lin.reader.ui.AppViewModelForTest
import io.lin.reader.ui.LocalAppViewModel
import io.lin.reader.ui.components.dialog.NotificationDialog
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private enum class DragAnchors {
    Start,
    End
}

/**
 * 书籍书签组组件
 */
@Composable
fun VolumeBookmarksGroup(
    volumeWithBookmarks: VolumeWithBookmarks,
    onBookmarkClick: (Bookmark) -> Unit,
    onBookmarkDelete: (Bookmark) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    // 旋转逻辑：默认指向左 (-90度)，展开时指向下 (0度)
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        label = "ArrowRotation"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // 书籍页眉部分 (交互区域)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large) // 为点击水波纹设置圆角
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VolumeCover(
                volume = volumeWithBookmarks.volume,
                width = 50.dp,
                height = 70.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = volumeWithBookmarks.volume.volumeName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(rotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // 书签总数统计
                Text(
                    text = stringResource(
                        R.string.bookmark_item_bookmark_count,
                        volumeWithBookmarks.bookmarks.size
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // 展开后的书签列表
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 24.dp, top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                volumeWithBookmarks.bookmarks.forEach { bookmark ->
                    BookmarkItem(
                        bookmark = bookmark,
                        volumeName = volumeWithBookmarks.volume.volumeName,
                        totalPages = volumeWithBookmarks.volume.totalPages,
                        onClick = { onBookmarkClick(bookmark) },
                        onDelete = { onBookmarkDelete(bookmark) }
                    )
                }
            }
        }
    }
}

/**
 * 书签列表项组件
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkItem(
    bookmark: Bookmark,
    volumeName: String,
    totalPages: Int?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val actionSize = 48.dp
    val actionSizePx = with(density) { actionSize.toPx() }
    val scope = rememberCoroutineScope()

    val decayAnimationSpec = rememberSplineBasedDecay<Float>()

    var showDeleteDialog by remember { mutableStateOf(false) }

    val state = remember(actionSizePx) {
        AnchoredDraggableState(
            initialValue = DragAnchors.Start,
            anchors = DraggableAnchors {
                DragAnchors.Start at 0f
                DragAnchors.End at -actionSizePx
            },
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = spring(),
            decayAnimationSpec = decayAnimationSpec
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 4.dp)
    ) {
        // 背景层：右侧删除按钮
        IconButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier
                .width(actionSize)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.bookmark_item_delete),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        }

        // 内容层：卡片主体
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    IntOffset(
                        x = if (state.offset.isNaN()) 0 else state.offset.roundToInt(),
                        y = 0
                    )
                }
                .anchoredDraggable(state, Orientation.Horizontal)
                .clip(MaterialTheme.shapes.medium)
                .clickable { onClick() },
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.inner_padding_of_container)),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bookmark.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.6f, fill = false)
                    )

                    Text(
                        text = volumeName,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(0.4f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val formatter = DateTimeFormatter.ofPattern("yyyy/M/d HH:mm")
                    val formattedTime = try {
                        LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(bookmark.history.time),
                            ZoneId.systemDefault()
                        ).format(formatter)
                    } catch (e: Exception) {
                        "--"
                    }

                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    if (totalPages != null && totalPages > 0) {
                        Text(
                            text = stringResource(
                                R.string.bookmark_item_pages,
                                (bookmark.history.pageIndex ?: 0) + 1,
                                totalPages
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        // TODO: 显示书签进度、章节名
                        val chapterInfo = bookmark.history.chapterInfo
                        val readingProgress = bookmark.history.readProgress
                        val progressStr = readingProgress.toString()
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                progress = { readingProgress },
                                modifier = Modifier.size(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                                trackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = chapterInfo ?: progressStr,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        val notificationText = stringResource(
            R.string.bookmark_delete_item_notification,
            volumeName,
            bookmark.history.pageIndex?.let { "Page ${it + 1}" } ?: "未知页码"
        )
        NotificationDialog(
            title = stringResource(R.string.bookmark_dialog_title_delete),
            notification = notificationText,
            onConfirm = {
                scope.launch {
                    state.animateTo(DragAnchors.Start)
                }
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VolumeBookmarksGroupPreview() {
    CompositionLocalProvider(
        LocalAppViewModel provides AppViewModelForTest()
    ) {
        MaterialTheme {
            Box(modifier = Modifier.padding(16.dp)) {
                VolumeBookmarksGroup(
                    volumeWithBookmarks = VolumeWithBookmarks(
                        volume = Volume(
                            volumeName = "The Great Gatsby",
                            bookFileUri = "",
                            totalPages = 366,
                            seriesId = 0L,
                            mimeType = "application/pdf",
                            createTime = System.currentTimeMillis()
                        ),
                        bookmarks = listOf(
                            Bookmark(
                                label = "Bookmark 1",
                                history = ReadingHistory(
                                    mupdfMark = 0L,
                                    time = System.currentTimeMillis(),
                                    pageIndex = 12
                                ),
                                volumeId = 0
                            ),
                            Bookmark(
                                label = "Bookmark 2",
                                history = ReadingHistory(
                                    mupdfMark = 0L,
                                    time = System.currentTimeMillis(),
                                    pageIndex = 122
                                ),
                                volumeId = 0
                            ),
                            Bookmark(
                                label = "Bookmark 3",
                                history = ReadingHistory(
                                    mupdfMark = 0L,
                                    time = System.currentTimeMillis(),
                                    pageIndex = 240
                                ),
                                volumeId = 0
                            )
                        )
                    ),
                    onBookmarkClick = {},
                    onBookmarkDelete = {}
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookmarkItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            // 分页文档
            BookmarkItem(
                bookmark = Bookmark(
                    label = "Chapter 3: The Beginning",
                    history = ReadingHistory(
                        mupdfMark = 0L,
                        time = System.currentTimeMillis(),
                        pageIndex = 45
                    ),
                    volumeId = 0
                ),
                volumeName = "The Great Gatsby",
                totalPages = 366,
                onClick = {},
                onDelete = {}
            )
            // 流式文档
            BookmarkItem(
                bookmark = Bookmark(
                    label = "Climax",
                    history = ReadingHistory(
                        mupdfMark = 0L,
                        time = System.currentTimeMillis(),
                        readProgress = 0.3f,
                        pageIndex = 45,
                        chapterInfo = "Chapter 3: The Beginning"
                    ),
                    volumeId = 0
                ),
                volumeName = "The Great Gatsby",
                totalPages = null,
                onClick = {},
                onDelete = {}
            )
        }
    }
}
