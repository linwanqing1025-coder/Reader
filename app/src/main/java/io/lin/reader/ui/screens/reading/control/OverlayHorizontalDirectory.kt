package io.lin.reader.ui.screens.reading.control

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.lin.reader.data.database.Series
import io.lin.reader.data.database.Volume
import io.lin.reader.ui.components.modifier.settledMarquee
import io.lin.reader.ui.theme.ReaderTheme

/**
 * 独立的浮层侧边目录，不包裹 content，可直接放在页面的最上层 Box 中
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OverlayHorizontalDirectory(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    items: List<T>,
    isItemSelected: (T) -> Boolean,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    onItemClick: (T) -> Unit = {},
    trailingContent: (@Composable (T) -> Unit)? = null,
    rightDirection: Boolean = false
) {
    // 拦截物理返回键
    BackHandler(enabled = visible) {
        onDismissRequest()
    }

    // 根据方向设置 Box 的对齐方式
    val contentAlignment = if (!rightDirection) {
        Alignment.TopStart
    } else {
        Alignment.TopEnd
    }

    // 根据方向设置动画方向
    val slideMultiplier = if (!rightDirection) -1 else 1

    // 根据方向设置 shape
    val drawerShape = if (!rightDirection) {
        RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    } else {
        RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = contentAlignment
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(0.7f)) // 官方标准遮罩透明度
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, // 去除点击遮罩的水波纹
                        onClick = onDismissRequest
                    )
            )
        }
        AnimatedVisibility(
            visible = visible,
            // 使用 slideMultiplier 乘以偏移量：左侧为 -it，右侧为 +it
            enter = slideInHorizontally(animationSpec = tween(300)) { slideMultiplier * it },
            exit = slideOutHorizontally(animationSpec = tween(300)) { slideMultiplier * it }
        ) {
            val listState = rememberLazyListState()
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

            // 每次显示时滚动到当前选中项
            LaunchedEffect(visible, items) {
                if (visible) {
                    val index = items.indexOfFirst { isItemSelected(it) }
                    if (index != -1) {
                        listState.scrollToItem(index)
                    }
                }
            }

            // 使用 M3 Surface 替代 ModalDrawerSheet 以支持高度自适应
            Surface(
                modifier = modifier
                    .statusBarsPadding() // 避开顶部系统状态栏
                    .widthIn(max = 320.dp), // 指定最大宽度
                shape = drawerShape, // 传入圆角 Shape
                color = DrawerDefaults.modalContainerColor, // 维持 Modal 抽屉背景色
                tonalElevation = DrawerDefaults.PermanentDrawerElevation // 维持阴影
            ) {
                Column(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = title,
                                modifier = Modifier.fillMaxWidth().settledMarquee(),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Visible
                            )

                        },
                        windowInsets = WindowInsets(0, 0, 0, 0), // 避免内部 TopAppBar 再次添加状态栏间距
                        scrollBehavior = scrollBehavior
                    )
                    // 目录列表
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .weight(1f, fill = false) // 关键：fill = false 让高度根据内容自适应，超屏时滚动
                            .navigationBarsPadding() // 在列表底部处理导航栏避让
                    ) {
                        itemsIndexed(items) { _, item ->
                            val isSelected = isItemSelected(item)
                            val labelText = itemLabel(item) // 提取显示文本
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = labelText,
                                        modifier = // 选中则轮播
                                            if (isSelected) Modifier.settledMarquee()
                                            else Modifier,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                badge = trailingContent?.let { { it(item) } },
                                selected = isSelected,
                                onClick = { onItemClick(item) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DrawerHorizontalDirectoryPreview() {
    var visible by remember { mutableStateOf(true) }
    // 模拟状态
    var currentVolumeId by remember { mutableLongStateOf(2L) }

    val mockSeries = remember { Series(id = 1, seriesName = "剑风传奇", volumeCount = 7, createTime = 0L) }
    val mockVolumes = remember {
        List(7) { index ->
            Volume(
                id = index.toLong(),
                volumeName = "第 ${index + 1} 卷",
                bookFileUri = "1",
                seriesId = 1,
                mimeType = "application/pdf",
                createTime = 0L
            )
        }
    }

    ReaderTheme {
        // 主页面内容（阅读器主体）
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("当前正在阅读：第 ${currentVolumeId + 1} 卷")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { visible = true }) {
                        Text("打开目录抽屉")
                    }
                }
                OverlayHorizontalDirectory(
                    visible = visible,
                    onDismissRequest = { visible = false },
                    title = mockSeries.seriesName,
                    items = mockVolumes,
                    onItemClick = { volume ->
                        currentVolumeId = volume.id
                    },
                    isItemSelected = { volume -> volume.id == currentVolumeId },
                    itemLabel = { volume -> volume.volumeName },
                    /*rightDirection = true,
                    drawerShape = RoundedCornerShape(
                        topStart = 20.dp,
                        bottomStart = 20.dp
                    )*/
                )
            }
        }
    }
}