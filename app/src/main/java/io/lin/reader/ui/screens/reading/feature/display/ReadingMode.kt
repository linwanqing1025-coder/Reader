package io.lin.reader.ui.maintab.reading.feature.display

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.toSize
import io.lin.reader.data.preferences.PageAlignment
import io.lin.reader.data.preferences.ReadingMode
import io.lin.reader.ui.maintab.reading.LocalReadingViewModel
import io.lin.reader.ui.maintab.reading.LocalReadingTransform
import kotlin.math.abs

private const val PRERENDER_COUNT = 2

@Composable
fun ReadingModeContainer(
    modifier: Modifier = Modifier,
    initialPageIndex: Int,
    onPageIndexChange: (Int) -> Unit,
) {
    val viewModel = LocalReadingViewModel.current!!
    val uiState by viewModel.uiState.collectAsState()
    val readingPreferences = viewModel.readerPreferences
    val pageCount = uiState.volume?.totalPages ?: 0

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
            ReadingMode.Scroll -> {
                ScrollableLayout(
                    modifier = Modifier.fillMaxSize(),
                    initialPageIndex = initialPageIndex,
                    pageCount = pageCount,
                    onPageIndexChange = onPageIndexChange,
                )
            }
            // TODO: 重构 DualPageLayout
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("TODO: Dual Mode")
                }
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
        val uiState by viewModel.uiState.collectAsState()
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
                Box(transformModifier) {
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
private fun ScrollableLayout(
    modifier: Modifier,
    initialPageIndex: Int,
    pageCount: Int,
    onPageIndexChange: (Int) -> Unit,
) {
    val viewModel = LocalReadingViewModel.current!!
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = scrollState,
        contentPadding = PaddingValues(bottom = 128.dp) // 仅留底部空白方便阅读最后一行
    ) {
        items(pageCount) { pageIndex ->
            val uiState by viewModel.uiState.collectAsState()
            val pageSize = viewModel.mPageSizes[pageIndex]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (pageSize != null && pageSize.x > 0 && pageSize.y > 0) {
                    MuPDFPage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(pageSize.x / pageSize.y),
                        mPageNumber = pageIndex,
                        pdfSize = pageSize,
                        mHighlightLinks = uiState.highlightLinks
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(600.dp),
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
    }
}