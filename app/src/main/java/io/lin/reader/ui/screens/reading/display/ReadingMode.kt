package io.lin.reader.ui.screens.reading.display

import android.graphics.PointF
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.toSize
import io.lin.reader.data.database.PageSetting
import io.lin.reader.data.preferences.PageAlignment
import io.lin.reader.data.preferences.ReadingMode
import io.lin.reader.ui.components.modifier.rotateWithLayout
import io.lin.reader.ui.screens.reading.LocalReadingTransform
import io.lin.reader.ui.screens.reading.LocalReadingViewModel
import kotlin.math.abs

private const val TAG = "ReadingMode.kt"
private const val PRERENDER_COUNT = 2
private const val A4_RATIO = 0.7f

@Composable
fun ReadingModeContainer(
    initialPageIndex: Int,
    onPageIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = LocalReadingViewModel.current!!
    val readingPreferences = viewModel.readerPreferences
    val uiState by viewModel.uiState.collectAsState()
    val pageCount = uiState.totalPage

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        // 监听设置变化，实现排版
        LaunchedEffect(
            widthPx, heightPx,
            readingPreferences.isReflow,
            viewModel.reflowPreferences.fontSize,
            viewModel.reflowPreferences.fontFamily,
            viewModel.reflowPreferences.fontBold,
            viewModel.reflowPreferences.fontItalic,
            viewModel.reflowPreferences.lineHeight,
            viewModel.reflowPreferences.horizontalMargin,
            viewModel.reflowPreferences.verticalMargin,
            viewModel.reflowPreferences.horizontalPadding,
            viewModel.reflowPreferences.verticalPadding,
            viewModel.reflowPreferences.textAlign,
            viewModel.reflowPreferences.textIndent,
            viewModel.reflowPreferences.paragraphSpacing,
            viewModel.reflowPreferences.hyphenation
        ) {
            viewModel.mCore.value?.let { core ->
                if (core.isReflowable) {
                    val widthPt = (widthPx / density.density) * (72f / 160f)
                    val heightPt = (heightPx / density.density) * (72f / 160f)
                    viewModel.performLayout(widthPt, heightPt)
                }
            }
        }
        when (readingPreferences.readingMode) {
            ReadingMode.Single -> {
                SinglePageLayout(
                    modifier = Modifier.fillMaxSize(),
                    initialPageIndex = initialPageIndex,
                    pageCount = pageCount,
                    onPageIndexChange = onPageIndexChange,
                )
            }

            ReadingMode.Dual -> {
                DualPageLayout(
                    modifier = Modifier.fillMaxSize(),
                    initialPageIndex = initialPageIndex,
                    pageCount = pageCount,
                    onPageIndexChange = onPageIndexChange,
                )
            }

            ReadingMode.Scroll -> {
                ScrollableLayout(
                    modifier = Modifier.fillMaxSize(),
                    initialPageIndex = initialPageIndex,
                    pageCount = pageCount,
                    onPageIndexChange = onPageIndexChange,
                )
            }
        }
    }
}

@Composable
private fun SinglePageLayout(
    modifier: Modifier,
    initialPageIndex: Int,
    pageCount: Int,
    onPageIndexChange: (Int) -> Unit,
) {
    val viewModel = LocalReadingViewModel.current!!
    val uiState by viewModel.uiState.collectAsState()
    val readingPreferences = viewModel.readerPreferences
    val readingTransform = LocalReadingTransform.current

    val pagerState = rememberPagerState(initialPage = initialPageIndex) { pageCount }

    var isInternalUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect {
            if (!isInternalUpdate) onPageIndexChange(it)
        }
    }

    LaunchedEffect(initialPageIndex) {
        if (pagerState.currentPage != initialPageIndex && initialPageIndex < pageCount) {
            try {
                isInternalUpdate = true
                if (abs(pagerState.currentPage - initialPageIndex) > 1) {
                    pagerState.scrollToPage(initialPageIndex)
                } else {
                    pagerState.animateScrollToPage(initialPageIndex)
                }
            } finally {
                isInternalUpdate = false
            }
        }
    }

    val isUserScrollEnabled = readingTransform.scaleAnim.value <= 1.05f

    val pageContent: @Composable (Int) -> Unit = { pageIndex ->
        val pageSetting = uiState.pageSettings[pageIndex]
        val pageSize = viewModel.mPageSizes[pageIndex]

        // 占满屏幕，保证 Pager 翻页效果
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (pageSize != null && pageSize.x > 0 && pageSize.y > 0) {
                // 作用于包裹阅读内容的容器
                val transformModifier = if (pageIndex == pagerState.currentPage) {
                    Modifier
                        .graphicsLayer {
                            scaleX = readingTransform.scaleAnim.value
                            scaleY = readingTransform.scaleAnim.value
                            translationX = readingTransform.offsetAnim.x.value
                            translationY = readingTransform.offsetAnim.y.value
                        }
                        .onSizeChanged {
                            if (pageIndex == pagerState.currentPage)
                                readingTransform.contentSize = it.toSize()
                        }
                } else Modifier

                // 这个容器包裹阅读内容
                // 这样就可以避免 MuPDFPage 响应旋转后导致滑动位移方向也跟着旋转
                // 实现位移方向不随阅读内容旋转而旋转
                TextSelectionContainer(
                    modifier = transformModifier,
                    hideMenu = pagerState.isScrollInProgress
                ) {
                    // 阅读内容
                    MuPDFPage(
                        modifier = Modifier.rotateWithLayout(pageSetting?.rotation ?: 0), // 响应旋转
                        mPageNumber = pageIndex,
                        pdfSize = pageSize,
                        mHighlightLinks = uiState.highlightLinks
                    )
                }
            } else {
                CircularProgressIndicator()
                LaunchedEffect(pageIndex) {
                    // Viewmodel中获取单页尺寸
                    viewModel.loadPageSize(pageIndex)
                }
            }
        }
    }

    if (readingPreferences.pageAlignment == PageAlignment.Vertical) {
        VerticalPager(
            state = pagerState,
            modifier = modifier,
            beyondViewportPageCount = PRERENDER_COUNT,
            userScrollEnabled = isUserScrollEnabled
        ) { pageContent(it) }
    } else {
        HorizontalPager(
            state = pagerState,
            modifier = modifier,
            beyondViewportPageCount = PRERENDER_COUNT,
            reverseLayout = readingPreferences.rtlMode,
            userScrollEnabled = isUserScrollEnabled
        ) { pageContent(it) }
    }
}

@Composable
private fun DualPageLayout(
    modifier: Modifier,
    initialPageIndex: Int,
    pageCount: Int,
    onPageIndexChange: (Int) -> Unit,
) {
    val viewModel = LocalReadingViewModel.current!!
    val uiState by viewModel.uiState.collectAsState()
    val readingPreferences = viewModel.readerPreferences
    val readingTransform = LocalReadingTransform.current
    val pagePaddingRatio = readingPreferences.pagePaddingRatio

    // (原代码保留) 控制封面分离
    val separateCover = readingPreferences.separateCover

    // 1. 计算总屏数 (Screen Count)
    val screenCount = remember(pageCount, separateCover) {
        if (pageCount <= 0) 0
        else if (separateCover) 1 + pageCount / 2
        else (pageCount + 1) / 2
    }

    val getScreenIndex = { pageIndex: Int ->
        if (pageIndex < 0) 0
        else if (separateCover) {
            if (pageIndex == 0) 0 else (pageIndex - 1) / 2 + 1
        } else {
            pageIndex / 2
        }
    }

    val initialScreenIndex = remember(initialPageIndex, separateCover) {
        getScreenIndex(initialPageIndex)
    }

    val pagerState = rememberPagerState(initialPage = initialScreenIndex) { screenCount }
    var isInternalUpdate by remember { mutableStateOf(false) }

    // 2. 同步屏幕索引变化到外部 (Sync Out)
    LaunchedEffect(pagerState, separateCover) {
        snapshotFlow { pagerState.currentPage }.collect { screenIndex ->
            if (!isInternalUpdate) {
                val primaryPage = if (separateCover) {
                    if (screenIndex == 0) 0 else (screenIndex - 1) * 2 + 1
                } else {
                    screenIndex * 2
                }
                onPageIndexChange(primaryPage)
            }
        }
    }

    // 3. 响应外部逻辑页跳转 (Sync In)
    LaunchedEffect(initialPageIndex, separateCover) {
        val targetScreen = getScreenIndex(initialPageIndex)
        if (pagerState.currentPage != targetScreen && targetScreen < screenCount) {
            try {
                isInternalUpdate = true
                if (abs(pagerState.currentPage - targetScreen) > 1) {
                    pagerState.scrollToPage(targetScreen)
                } else {
                    pagerState.animateScrollToPage(targetScreen)
                }
            } finally {
                isInternalUpdate = false
            }
        }
    }

    val isUserScrollEnabled = readingTransform.scaleAnim.value <= 1.05f

    // 4. 单页内容的抽象组件
    val pageItem: @Composable (Int?, PointF?, PageSetting?) -> Unit =
        { pageIndex, pageSize, pageSetting ->
            if (pageIndex != null) {
                if (pageSize != null && pageSize.x > 0 && pageSize.y > 0) {
                    MuPDFPage(
                        modifier = Modifier.rotateWithLayout(pageSetting?.rotation ?: 0),
                        mPageNumber = pageIndex,
                        pdfSize = pageSize,
                        mHighlightLinks = uiState.highlightLinks
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .aspectRatio(A4_RATIO),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                        LaunchedEffect(pageIndex) {
                            viewModel.loadPageSize(pageIndex)
                        }
                    }
                }
            }
        }

    // 5. 渲染整屏内容 (基于 BoxWithConstraints 的完美比例实现)
    val screenContent: @Composable (Int) -> Unit = { screenIndex ->
        val (firstPage, secondPage) = remember(screenIndex, separateCover, pageCount) {
            if (separateCover) {
                if (screenIndex == 0) {
                    (if (pageCount > 0) 0 else null) to null
                } else {
                    val p1 = (screenIndex - 1) * 2 + 1
                    val p2 = p1 + 1
                    (if (p1 < pageCount) p1 else null) to (if (p2 < pageCount) p2 else null)
                }
            } else {
                val p1 = screenIndex * 2
                val p2 = p1 + 1
                (if (p1 < pageCount) p1 else null) to (if (p2 < pageCount) p2 else null)
            }
        }

        val leftIndex = if (readingPreferences.rtlMode) secondPage else firstPage
        val leftPageSize = viewModel.mPageSizes[leftIndex]
        val leftPageSetting = uiState.pageSettings[leftIndex]
        val leftContainerSize =
            if (leftPageSetting != null && (leftPageSetting.rotation == 90 || leftPageSetting.rotation == 270))
                leftPageSize?.let { PointF(it.y, it.x) }
            else leftPageSize

        val rightIndex = if (readingPreferences.rtlMode) firstPage else secondPage
        val rightPageSize = viewModel.mPageSizes[rightIndex]
        val rightPageSetting = uiState.pageSettings[rightIndex]
        val rightContainerSize =
            if (rightPageSetting != null && (rightPageSetting.rotation == 90 || rightPageSetting.rotation == 270))
                rightPageSize?.let { PointF(it.y, it.x) }
            else rightPageSize

        // 外层作为基准测量容器，铺满当前一屏
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight

            val transformModifier = if (screenIndex == pagerState.currentPage) {
                Modifier
                    .graphicsLayer {
                        scaleX = readingTransform.scaleAnim.value
                        scaleY = readingTransform.scaleAnim.value
                        translationX = readingTransform.offsetAnim.x.value
                        translationY = readingTransform.offsetAnim.y.value
                    }
                    .onSizeChanged {
                        if (screenIndex == pagerState.currentPage)
                            readingTransform.contentSize = it.toSize()
                    }
            } else Modifier

            if (leftIndex == null || rightIndex == null) {
                // 单页模式（比如分离封面的第一页，或者奇数页的最后一页）
                val validIndex = leftIndex ?: rightIndex
                val validPageSize = if (leftIndex != null) leftPageSize else rightPageSize
                val validPageSetting = if (leftIndex != null) leftPageSetting else rightPageSetting
                val validContainerSize =
                    if (leftIndex != null) leftContainerSize else rightContainerSize

                val ratio = if (validContainerSize != null && validContainerSize.y > 0) {
                    validContainerSize.x / validContainerSize.y
                } else 1f

                // 高度自适应核心逻辑
                val finalHeight = min(screenWidth / ratio, screenHeight)
                val finalWidth = finalHeight * ratio

                Box(
                    modifier = transformModifier
                        .width(finalWidth)
                        .height(finalHeight),
                    contentAlignment = Alignment.Center
                ) {
                    TextSelectionContainer(
                        hideMenu = pagerState.isScrollInProgress
                    ) {
                        pageItem(validIndex, validPageSize, validPageSetting)
                    }
                }
            } else {
                // 双页模式
                // 计算左页宽高比。如果还没加载出来，默认设为 A4 纸比例以显示 Loading
                val leftRatio =
                    if (leftContainerSize != null && leftContainerSize.y > 0) leftContainerSize.x / leftContainerSize.y
                    else A4_RATIO
                // 计算右页宽高比同理
                val rightRatio =
                    if (rightContainerSize != null && rightContainerSize.y > 0) rightContainerSize.x / rightContainerSize.y
                    else A4_RATIO

                val totalRatio = leftRatio + rightRatio

                // 使用屏幕宽度计算间距绝对值
                val spacerWidth = screenWidth * pagePaddingRatio
                val availableWidth = screenWidth - spacerWidth

                // 高度自适应核心逻辑 (宽度瓶颈 vs 高度瓶颈 取最小值)
                val maxHeightBasedOnWidth = availableWidth / totalRatio
                val finalHeight = min(maxHeightBasedOnWidth, screenHeight)

                // 反推每一页的精确宽度
                val leftWidth = finalHeight * leftRatio
                val rightWidth = finalHeight * rightRatio

                // 内部行，尺寸是严格计算出的完美尺寸
                TextSelectionContainer(
                    modifier = transformModifier,
                    hideMenu = pagerState.isScrollInProgress
                ) {
                    Row(
                        modifier = Modifier
                            .width(leftWidth + spacerWidth + rightWidth)
                            .height(finalHeight),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // 左侧页
                        Box(
                            modifier = Modifier
                                .width(leftWidth)
                                .height(finalHeight),
                            contentAlignment = Alignment.CenterEnd // 保留了向中间书脊靠拢的特性
                        ) {
                            pageItem(leftIndex, leftPageSize, leftPageSetting)
                        }

                        // 中间间距
                        if (pagePaddingRatio > 0f) {
                            Spacer(modifier = Modifier.width(spacerWidth))
                        }

                        // 右侧页
                        Box(
                            modifier = Modifier
                                .width(rightWidth)
                                .height(finalHeight),
                            contentAlignment = Alignment.CenterStart // 靠书脊
                        ) {
                            pageItem(rightIndex, rightPageSize, rightPageSetting)
                        }
                    }
                }
            }
        }
    }

    // 6. Pager 容器
    if (readingPreferences.pageAlignment == PageAlignment.Vertical) {
        VerticalPager(
            state = pagerState,
            modifier = modifier,
            beyondViewportPageCount = PRERENDER_COUNT,
            userScrollEnabled = isUserScrollEnabled
        ) { screenContent(it) }
    } else {
        HorizontalPager(
            state = pagerState,
            modifier = modifier,
            beyondViewportPageCount = PRERENDER_COUNT,
            reverseLayout = readingPreferences.rtlMode,
            userScrollEnabled = isUserScrollEnabled
        ) { screenContent(it) }
    }
}

@Composable
private fun ScrollableLayout(
    modifier: Modifier,
    initialPageIndex: Int,
    pageCount: Int,
    onPageIndexChange: (Int) -> Unit,
) {
    val viewModel = LocalReadingViewModel.current!!
    val uiState by viewModel.uiState.collectAsState()
    val readingPreferences = viewModel.readerPreferences
    val pagePaddingRatio = readingPreferences.pagePaddingRatio
    val readingTransform = LocalReadingTransform.current

    // 作用于包裹阅读内容的容器
    val transformModifier = Modifier
        .graphicsLayer {
            scaleX = readingTransform.scaleAnim.value
            scaleY = readingTransform.scaleAnim.value
            translationX = readingTransform.offsetAnim.x.value
            translationY = readingTransform.offsetAnim.y.value
        }
        .onSizeChanged { readingTransform.contentSize = it.toSize() }

    val scrollState = rememberLazyListState(initialFirstVisibleItemIndex = initialPageIndex)

    var isInternalUpdate by remember { mutableStateOf(false) }

    // 同步滚动位置到 ViewModel
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.firstVisibleItemIndex }.collect {
            if (!isInternalUpdate) onPageIndexChange(it)
        }
    }

    // 处理外部跳转请求
    LaunchedEffect(initialPageIndex) {
        if (scrollState.firstVisibleItemIndex != initialPageIndex && initialPageIndex < pageCount) {
            try {
                isInternalUpdate = true
                scrollState.scrollToItem(initialPageIndex)
            } finally {
                isInternalUpdate = false
            }
        }
    }

    val isUserScrollEnabled = readingTransform.scaleAnim.value <= 1.05f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(transformModifier)
    ) {
        val pagePaddingValue =
            if (readingPreferences.pageAlignment == PageAlignment.Vertical) maxHeight * pagePaddingRatio
            else maxWidth * pagePaddingRatio

        val pageContent: @Composable (Int) -> Unit = { pageIndex ->
            val fillModifier =
                if (readingPreferences.pageAlignment == PageAlignment.Vertical) Modifier.fillMaxWidth()
                else Modifier.fillMaxHeight()
            val wrapModifier =
                if (readingPreferences.pageAlignment == PageAlignment.Vertical) Modifier.wrapContentHeight()
                else Modifier.wrapContentWidth()
            val pageSize = viewModel.mPageSizes[pageIndex]

            Box(
                modifier = fillModifier.then(wrapModifier),
                contentAlignment = Alignment.Center
            ) {
                if (pageSize != null && pageSize.x > 0 && pageSize.y > 0) {
                    MuPDFPage(
                        modifier = fillModifier
                            .aspectRatio(pageSize.x / pageSize.y),
                        mPageNumber = pageIndex,
                        pdfSize = pageSize,
                        mHighlightLinks = uiState.highlightLinks
                    )
                } else {
                    Box(
                        modifier = fillModifier.aspectRatio(A4_RATIO),// A4纸比例
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    LaunchedEffect(pageIndex) {
                        viewModel.loadPageSize(pageIndex)
                    }
                }
            }
        }

        val selectionState = remember { TextSelectionState() }

        // 监听列表滚动，实时更新手柄和菜单的位置
        LaunchedEffect(scrollState) {
            snapshotFlow {
                // 同时监听 index 和 offset，确保任何微小的滚动都会触发
                scrollState.firstVisibleItemIndex to scrollState.firstVisibleItemScrollOffset
            }.collect {
                selectionState.updateHandleAndMenuPositions()
            }
        }

        TextSelectionContainer(
            selectionState = selectionState,
            hideMenu = scrollState.isScrollInProgress
        ) {
            CompositionLocalProvider(LocalTextSelectionState provides selectionState) {
                if (readingPreferences.pageAlignment == PageAlignment.Vertical) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(pagePaddingValue),
                        contentPadding = PaddingValues(bottom = 128.dp), // 仅留底部空白方便阅读最后一行
                        userScrollEnabled = isUserScrollEnabled
                    ) {
                        items(pageCount) { pageIndex ->
                            pageContent(pageIndex)
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        state = scrollState,
                        reverseLayout = readingPreferences.rtlMode,
                        horizontalArrangement = Arrangement.spacedBy(pagePaddingValue),
                        userScrollEnabled = isUserScrollEnabled
                    ) {
                        items(pageCount) { pageIndex ->
                            pageContent(pageIndex)
                        }
                        item {
                            Box(modifier = Modifier.size(128.dp)) // 仅留最后空白方便阅读最后一行
                        }
                    }
                }
            }
        }
    }
}