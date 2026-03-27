package io.lin.reader.ui.screen.setting.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.lin.reader.R
import io.lin.reader.ui.screen.setting.SettingSnippet

enum class ReadingMode {
    Single,
    Dual,
    Scroll
}

enum class PageAlignment {
    Vertical,
    Horizontal
}


@Composable
fun ReadingModeDetails(
    modifier: Modifier = Modifier,
    readingMode: ReadingMode = ReadingMode.Single,
    onReadingModeChange: (ReadingMode) -> Unit = { },
    pageAlignment: PageAlignment = PageAlignment.Vertical,
    onPageAlignmentChange: (PageAlignment) -> Unit = { },
    paddingRatio: Float = 0.02f,
    onPaddingRatioChange: (Float) -> Unit = { },
) {
    fun isSelected(mode: ReadingMode, alignment: PageAlignment): Boolean {
        return readingMode == mode && pageAlignment == alignment
    }
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.inner_padding_of_container)),
        contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.inner_padding_of_container)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.inner_padding_of_container))
    ) {
        item {
            Text(
                text = stringResource(R.string.reading_mode_label_padding),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = dimensionResource(R.dimen.half_inner_padding_of_container),
                    bottom = dimensionResource(R.dimen.half_inner_padding_of_container)
                )
            )
            SettingSnippet {
                Column(
                    modifier = Modifier.padding(dimensionResource(R.dimen.inner_padding_of_container))
                ) {
                    Text(
                        text = "${(paddingRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End)
                    )
                    Slider(
                        value = paddingRatio,
                        onValueChange = { onPaddingRatioChange(it) },
                        valueRange = 0f..0.2f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                            activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                            inactiveTickColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                alpha = 0.5f
                            )
                        )
                    )
                }
            }
        }
        item {
            ReadingModeSection(
                title = stringResource(R.string.reading_mode_label_single),
                onVerticalClick = {
                    onReadingModeChange(ReadingMode.Single)
                    onPageAlignmentChange(PageAlignment.Vertical)
                },
                onHorizontalClick = {
                    onReadingModeChange(ReadingMode.Single)
                    onPageAlignmentChange(PageAlignment.Horizontal)
                },
                isVerticalSelected = isSelected(ReadingMode.Single, PageAlignment.Vertical),
                isHorizontalSelected = isSelected(ReadingMode.Single, PageAlignment.Horizontal),
                verticalContent = { VerticalSinglePage() },
                horizontalContent = { HorizontalSinglePage() }
            )
        }
        item {
            ReadingModeSection(
                title = stringResource(R.string.reading_mode_label_dual),
                onVerticalClick = {
                    onReadingModeChange(ReadingMode.Dual)
                    onPageAlignmentChange(PageAlignment.Vertical)
                },
                onHorizontalClick = {
                    onReadingModeChange(ReadingMode.Dual)
                    onPageAlignmentChange(PageAlignment.Horizontal)
                },
                isVerticalSelected = isSelected(ReadingMode.Dual, PageAlignment.Vertical),
                isHorizontalSelected = isSelected(ReadingMode.Dual, PageAlignment.Horizontal),
                verticalContent = { VerticalDualPage(pagePaddingRatio = paddingRatio) },
                horizontalContent = { HorizontalDualPage(pagePaddingRatio = paddingRatio) },
            )
        }
        item {
            ReadingModeSection(
                title = stringResource(R.string.reading_mode_label_scrollable),
                onVerticalClick = {
                    onReadingModeChange(ReadingMode.Scroll)
                    onPageAlignmentChange(PageAlignment.Vertical)
                },
                onHorizontalClick = {
                    onReadingModeChange(ReadingMode.Scroll)
                    onPageAlignmentChange(PageAlignment.Horizontal)
                },
                isVerticalSelected = isSelected(ReadingMode.Scroll, PageAlignment.Vertical),
                isHorizontalSelected = isSelected(ReadingMode.Scroll, PageAlignment.Horizontal),
                verticalContent = { VerticalScrollablePage(pagePaddingRatio = paddingRatio) },
                horizontalContent = { HorizontalScrollablePage(pagePaddingRatio = paddingRatio) }
            )
        }
    }
}

@Composable
private fun ReadingModeSection(
    title: String,
    onVerticalClick: () -> Unit,
    onHorizontalClick: () -> Unit,
    isVerticalSelected: Boolean,
    isHorizontalSelected: Boolean,
    verticalContent: @Composable () -> Unit,
    horizontalContent: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.half_inner_padding_of_container),
                bottom = dimensionResource(R.dimen.half_inner_padding_of_container)
            )
        )
        SettingSnippet {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.inner_padding_of_container)),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // 左侧选项及文字
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ClickableContainer(
                        isSelected = isVerticalSelected,
                        onClick = onVerticalClick,
                        pageItem = verticalContent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.page_alignment_vertical),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isVerticalSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(Modifier.width(dimensionResource(R.dimen.inner_padding_of_container)))

                // 右侧选项及文字
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ClickableContainer(
                        isSelected = isHorizontalSelected,
                        onClick = onHorizontalClick,
                        pageItem = horizontalContent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.page_alignment_horizontal),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isHorizontalSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun ClickableContainer(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    pageItem: @Composable () -> Unit = {}
) {
    val borderThickness = if (isSelected) 2.dp else 1.dp
    val borderColor =
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant
    val containerColor =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        else MaterialTheme.colorScheme.surface

    Surface(
        onClick = onClick,
        modifier = modifier.widthIn(max = 180.dp),
        shape = RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius)),
        color = containerColor,
        border = BorderStroke(borderThickness, borderColor)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val maxHeight = maxWidth * 1.4f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .padding(dimensionResource(R.dimen.inner_padding_of_container)),
                contentAlignment = Alignment.Center
            ) {
                pageItem()
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
        }
    }
}

/**
 * Page 组件支持两种尺寸模式：
 * 1. 传入 [width] (Dp)：固定宽度模式。
 * 2. 不传 [width] (null)：权重/填充模式，通常配合 [Modifier.weight] 使用。
 * 无论哪种模式，都会通过 [Modifier.aspectRatio] 强制保持 1.4 的高宽比 (Height = 1.4 * Width)。
 */
@Composable
private fun Page(
    modifier: Modifier = Modifier,
    width: Dp? = 100.dp
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .aspectRatio(1f / 1.4f)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(4.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(6) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (it % 2 == 0) 0.8f else 0.6f)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                )
            }
        }
    }
}

/**
 * 为 RowScope 提供的便捷函数，支持直接传入权重。
 */
@Composable
private fun RowScope.Page(
    modifier: Modifier = Modifier,
    weight: Float
) {
    Page(
        modifier = modifier.weight(weight),
        width = null
    )
}

@Composable
private fun VerticalSinglePage(modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(initialPage = 4) { 9 }
    VerticalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / 1.4f),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.half_inner_padding_of_container)),
            contentAlignment = Alignment.Center
        ) {
            Page(width = null)
        }
    }
}

@Composable
private fun HorizontalSinglePage(modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(initialPage = 4) { 9 }
    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / 1.4f),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.half_inner_padding_of_container)),
            contentAlignment = Alignment.Center
        ) {
            Page(width = null)
        }
    }
}

@Composable
private fun VerticalDualPage(
    modifier: Modifier = Modifier,
    pagePaddingRatio: Float = 0f
) {
    val pagerState = rememberPagerState(initialPage = 4) { 9 }
    val restRatio = (1f - pagePaddingRatio) / 2
    VerticalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / 0.7f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.half_inner_padding_of_container)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Page(weight = restRatio)
            if (pagePaddingRatio > 0f) {
                Spacer(Modifier.weight(pagePaddingRatio))
            }
            Page(weight = restRatio)
        }
    }
}

@Composable
private fun HorizontalDualPage(
    modifier: Modifier = Modifier,
    pagePaddingRatio: Float = 0.04f
) {
    val pagerState = rememberPagerState(initialPage = 4) { 9 }
    val restRatio = (1f - pagePaddingRatio) / 2
    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / 0.7f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.half_inner_padding_of_container)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Page(weight = restRatio)
            if (pagePaddingRatio > 0f) {
                Spacer(Modifier.weight(pagePaddingRatio))
            }
            Page(weight = restRatio)
        }
    }
}

@Composable
private fun VerticalScrollablePage(
    modifier: Modifier = Modifier,
    pagePaddingRatio: Float = 0f
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(1000.dp)
    ) {
        val viewportHeight = maxWidth * 1.4f
        val pageHeight = (maxWidth * 0.7f) * 1.4f
        val gap = viewportHeight * pagePaddingRatio

        val density = LocalDensity.current
        // 计算居中偏移量: Page + Gap - (Viewport - Page)/2
        val initialOffsetPx =
            with(density) { (pageHeight + gap - (viewportHeight - pageHeight) / 2).roundToPx() }
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = 3,
            initialFirstVisibleItemScrollOffset = initialOffsetPx
        )

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            items(9) {
                Page(modifier = Modifier.fillMaxWidth(0.7f), width = null)
            }
        }
    }
}

@Composable
private fun HorizontalScrollablePage(
    modifier: Modifier = Modifier,
    pagePaddingRatio: Float = 0f
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(1000.dp)
    ) {
        val viewportWidth = maxWidth
        val pageWidth = maxWidth * 0.7f
        val gap = viewportWidth * pagePaddingRatio

        val density = LocalDensity.current
        // 计算居中偏移量: Page + Gap - (Viewport - Page)/2
        val initialOffsetPx =
            with(density) { (pageWidth + gap - (viewportWidth - pageWidth) / 2).roundToPx() }
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = 3,
            initialFirstVisibleItemScrollOffset = initialOffsetPx
        )

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            items(9) {
                Page(modifier = Modifier.fillMaxHeight(0.7f), width = null)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReadingModeDetailsPreview() {
    MaterialTheme {
        ReadingModeDetails()
    }
}
