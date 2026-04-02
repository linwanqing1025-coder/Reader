package io.lin.reader.ui.screen.reading

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import io.lin.reader.data.preferences.ReaderPreferences
import io.lin.reader.ui.screen.setting.details.PageAlignment
import io.lin.reader.ui.screen.setting.details.ReadingMode
import kotlin.math.abs

/**
 * 该组件的父容器 need 占满整个屏幕
 * @param readingPreferences 阅读偏好设置
 * @param initialPageIndex 初始页索引（0开始）
 * @param pageCount 总页数（1开始）
 * @param onPageIndexChange 页索引改变回调（返回0开始的索引）
 * @param getPageBitmap 接受页索引（包含0），返回 Bitmap
 */
@Composable
fun ReadingModeContainer(
    modifier: Modifier = Modifier,
    readingPreferences: ReaderPreferences,
    initialPageIndex: Int,
    pageCount: Int,
    onPageIndexChange: (Int) -> Unit,
    getPageBitmap: @Composable (pageIndex: Int) -> Bitmap?,
) {
    when (readingPreferences.readingMode) {
        ReadingMode.Single -> {
            SinglePageLayout(
                modifier = modifier,
                pageAlignment = readingPreferences.pageAlignment,
                initialPageIndex = initialPageIndex,
                pageCount = pageCount,
                pageColor = Color(readingPreferences.colors.page),
                onPageIndexChange = onPageIndexChange,
                getPageBitmap = getPageBitmap,
                isRtlMode = readingPreferences.rtlMode
            )
        }

        ReadingMode.Dual -> {
            DualPageLayout(
                modifier = modifier,
                pageAlignment = readingPreferences.pageAlignment,
                pagePaddingRatio = readingPreferences.pagePaddingRatio,
                initialPageIndex = initialPageIndex,
                pageCount = pageCount,
                pageColor = Color(readingPreferences.colors.page),
                onPageIndexChange = onPageIndexChange,
                getPageBitmap = getPageBitmap,
                isRtlMode = readingPreferences.rtlMode,
                separateCover = readingPreferences.separateCover
            )
        }

        ReadingMode.Scroll -> {
            ScrollableLayout(
                modifier = modifier,
                pageAlignment = readingPreferences.pageAlignment,
                pagePaddingRatio = readingPreferences.pagePaddingRatio,
                initialPageIndex = initialPageIndex,
                pageCount = pageCount,
                pageColor = Color(readingPreferences.colors.page),
                onPageIndexChange = onPageIndexChange,
                getPageBitmap = getPageBitmap,
                isRtlMode = readingPreferences.rtlMode
            )
        }
    }
}

@Composable
private fun SinglePageLayout(
    modifier: Modifier,
    pageAlignment: PageAlignment,
    isRtlMode: Boolean,
    initialPageIndex: Int,
    pageCount: Int,
    pageColor: Color,
    onPageIndexChange: (Int) -> Unit,
    getPageBitmap: @Composable (Int) -> Bitmap?,
) {
    val pagerState = rememberPagerState(initialPage = initialPageIndex) { pageCount }
    var isInternalUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect {
            if (!isInternalUpdate) {
                onPageIndexChange(it)
            }
        }
    }

    LaunchedEffect(initialPageIndex) {
        if (pagerState.currentPage != initialPageIndex) {
            try {
                isInternalUpdate = true
                // 距离过大时使用 scrollToPage 避免中间页回调干扰，距离小时保留动画效果
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

    if (pageAlignment == PageAlignment.Vertical) {
        VerticalPager(
            state = pagerState,
            modifier = modifier.fillMaxSize()
        ) { index ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PageImage(
                    pageColor = pageColor,
                    bitmap = getPageBitmap(index),
                    contentDescription = "Page ${index + 1}"
                )
            }
        }
    } else {
        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxSize(),
            reverseLayout = isRtlMode
        ) { index ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PageImage(
                    pageColor = pageColor,
                    bitmap = getPageBitmap(index),
                    contentDescription = "Page ${index + 1}"
                )
            }
        }
    }
}

@Composable
private fun DualPageLayout(
    modifier: Modifier,
    pageAlignment: PageAlignment,
    isRtlMode: Boolean,
    pagePaddingRatio: Float,
    initialPageIndex: Int,
    pageCount: Int,
    pageColor: Color,
    onPageIndexChange: (Int) -> Unit,
    getPageBitmap: @Composable (Int) -> Bitmap?,
    separateCover: Boolean = false,
) {
    val dualPageCount = remember(pageCount, separateCover) {
        if (pageCount <= 0) 0
        else if (!separateCover) (pageCount + 1) / 2
        else {
            if (pageCount % 2 != 0) (pageCount + 1) / 2
            else pageCount / 2 + 1
        }
    }

    val initialDualPageIndex = remember(initialPageIndex, separateCover, pageCount) {
        if (pageCount <= 0) 0
        else if (!separateCover) initialPageIndex / 2
        else {
            if (initialPageIndex == 0) 0
            else if (pageCount % 2 == 0 && initialPageIndex == pageCount - 1) dualPageCount - 1
            else (initialPageIndex - 1) / 2 + 1
        }
    }

    val pagerState = rememberPagerState(initialPage = initialDualPageIndex) { dualPageCount }
    val restRatio = (1f - pagePaddingRatio) / 2
    var isInternalUpdate by remember { mutableStateOf(false) }

    val getPagesForDualIndex = { dualIndex: Int ->
        if (!separateCover) {
            listOf(dualIndex * 2, dualIndex * 2 + 1).filter { it < pageCount }
        } else {
            if (dualIndex == 0) listOf(0)
            else if (pageCount % 2 == 0 && dualIndex == dualPageCount - 1) listOf(pageCount - 1)
            else {
                val first = (dualIndex - 1) * 2 + 1
                listOf(first, first + 1).filter { it < pageCount }
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { dualIndex ->
            if (isInternalUpdate) return@collect
            val pages = getPagesForDualIndex(dualIndex)
            if (initialPageIndex !in pages) {
                pages.firstOrNull()?.let { onPageIndexChange(it) }
            }
        }
    }

    LaunchedEffect(initialPageIndex) {
        val currentVisiblePages = getPagesForDualIndex(pagerState.currentPage)
        if (initialPageIndex !in currentVisiblePages) {
            val targetDualIndex = if (!separateCover) initialPageIndex / 2
            else {
                if (initialPageIndex == 0) 0
                else if (pageCount % 2 == 0 && initialPageIndex == pageCount - 1) dualPageCount - 1
                else (initialPageIndex - 1) / 2 + 1
            }
            if (pagerState.currentPage != targetDualIndex) {
                try {
                    isInternalUpdate = true
                    if (abs(pagerState.currentPage - targetDualIndex) > 1) {
                        pagerState.scrollToPage(targetDualIndex)
                    } else {
                        pagerState.animateScrollToPage(targetDualIndex)
                    }
                } finally {
                    isInternalUpdate = false
                }
            }
        }
    }

    val content: @Composable (Int) -> Unit = { dualIndex ->
        val currentPages = getPagesForDualIndex(dualIndex)
        if (currentPages.size == 1) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PageImage(
                    pageColor = pageColor,
                    bitmap = getPageBitmap(currentPages[0]),
                    contentDescription = "Page ${currentPages[0] + 1}"
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                val first = currentPages[0]
                val second = currentPages[1]
                val left = if (!isRtlMode) first else second
                val right = if (!isRtlMode) second else first

                Box(
                    modifier = Modifier
                        .weight(restRatio)
                        .fillMaxSize(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (left < pageCount) PageImage(
                        pageColor = pageColor,
                        bitmap = getPageBitmap(left),
                        contentDescription = "Page ${left + 1}"
                    )
                }
                if (pagePaddingRatio > 0f) Spacer(Modifier.weight(pagePaddingRatio))
                Box(
                    modifier = Modifier
                        .weight(restRatio)
                        .fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (right < pageCount) PageImage(
                        pageColor = pageColor,
                        bitmap = getPageBitmap(right),
                        contentDescription = "Page ${right + 1}"
                    )
                }
            }
        }
    }

    if (pageAlignment == PageAlignment.Vertical) {
        VerticalPager(state = pagerState, modifier = modifier.fillMaxSize()) { content(it) }
    } else {
        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxSize(),
            reverseLayout = isRtlMode
        ) { content(it) }
    }
}

@Composable
private fun ScrollableLayout(
    modifier: Modifier,
    pageAlignment: PageAlignment,
    isRtlMode: Boolean,
    pagePaddingRatio: Float,
    initialPageIndex: Int,
    pageCount: Int,
    pageColor: Color,
    onPageIndexChange: (Int) -> Unit,
    getPageBitmap: @Composable (Int) -> Bitmap?,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPageIndex)
        var lastEmittedIndex by remember { mutableIntStateOf(initialPageIndex) }
        var isInternalUpdate by remember { mutableStateOf(false) }

        val density = LocalDensity.current
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val viewportHeightPx = with(density) { maxHeight.toPx() }

        LaunchedEffect(listState) {
            snapshotFlow {
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isEmpty()) return@snapshotFlow lastEmittedIndex
                val viewportCenter =
                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val centerItem =
                    visibleItems.find { it.offset <= viewportCenter && (it.offset + it.size) >= viewportCenter }
                        ?: visibleItems.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                centerItem?.index ?: lastEmittedIndex
            }.collect { centerIndex ->
                if (lastEmittedIndex != centerIndex && !isInternalUpdate) {
                    lastEmittedIndex = centerIndex
                    onPageIndexChange(centerIndex)
                }
            }
        }

        LaunchedEffect(initialPageIndex) {
            if (initialPageIndex != lastEmittedIndex) {
                val estimatedSize =
                    if (pageAlignment == PageAlignment.Vertical) viewportWidthPx / 0.707f else viewportHeightPx * 0.707f
                val viewportSize =
                    if (pageAlignment == PageAlignment.Vertical) viewportHeightPx else viewportWidthPx
                val offset = (-(viewportSize - estimatedSize) / 2).toInt()
                try {
                    isInternalUpdate = true
                    if (abs(initialPageIndex - lastEmittedIndex) > 1) {
                        listState.scrollToItem(initialPageIndex, offset)
                    } else {
                        listState.animateScrollToItem(initialPageIndex, offset)
                    }
                } finally {
                    lastEmittedIndex = initialPageIndex
                    isInternalUpdate = false
                }
            }
        }

        if (pageAlignment == PageAlignment.Vertical) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(pageCount) { index ->
                    val bitmap = getPageBitmap(index)
                    val aspectRatio =
                        if (bitmap != null) bitmap.width.toFloat() / bitmap.height.toFloat() else 1f / 1.4f
                    PageImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio),
                        pageColor = pageColor,
                        bitmap = bitmap,
                        contentDescription = "Page ${index + 1}"
                    )
                    if (pagePaddingRatio > 0f) {
                        Spacer(modifier = Modifier.height(maxHeight * pagePaddingRatio))
                    }
                }
            }
        } else {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                reverseLayout = isRtlMode
            ) {
                items(pageCount) { index ->
                    val bitmap = getPageBitmap(index)
                    val aspectRatio =
                        if (bitmap != null) bitmap.width.toFloat() / bitmap.height.toFloat() else 1f / 1.4f
                    PageImage(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(aspectRatio),
                        pageColor = pageColor,
                        bitmap = bitmap,
                        contentDescription = "Page ${index + 1}"
                    )
                    if (pagePaddingRatio > 0f) {
                        Spacer(modifier = Modifier.width(maxWidth * pagePaddingRatio))
                    }
                }
            }
        }
    }
}

@Composable
private fun PageImage(
    modifier: Modifier = Modifier,
    pageColor: Color,
    bitmap: Bitmap?,
    contentDescription: String
) {
    if (bitmap != null) {
        val aspectRatio = remember(bitmap) { bitmap.width.toFloat() / bitmap.height.toFloat() }
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .aspectRatio(aspectRatio)
                .background(pageColor)
        )
    } else {
        Box(modifier = modifier.aspectRatio(1f / 1.4f), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        }
    }
}
