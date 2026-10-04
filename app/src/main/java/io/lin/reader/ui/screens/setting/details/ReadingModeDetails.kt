package io.lin.reader.ui.screens.setting.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import io.lin.reader.data.preferences.PageAlignment
import io.lin.reader.data.preferences.ReaderPreferencesForTest
import io.lin.reader.data.preferences.ReaderPreferencesInterface
import io.lin.reader.data.preferences.ReadingMode
import io.lin.reader.ui.components.screenbar.DynamicTopAppBar
import io.lin.reader.ui.screens.setting.SettingList
import io.lin.reader.ui.theme.ReaderTheme
import io.lin.reader.ui.components.selection.SelectableBox

@Composable
fun ReadingModeDetailsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    readerPreferences: ReaderPreferencesInterface
) {
    val listState = rememberLazyListState()
    val scrollFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 150f).coerceIn(0f, 1f)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DynamicTopAppBar(
                title = stringResource(R.string.setting_interaction_style),
                scrollFraction = scrollFraction,
                onNavigationClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        ReadingModeDetails(
            modifier = Modifier.padding(innerPadding),
            readerPreferences = readerPreferences,
            listState = listState
        )
    }
}

@Composable
fun ReadingModeDetails(
    modifier: Modifier = Modifier,
    readerPreferences: ReaderPreferencesInterface,
    listState: LazyListState = rememberLazyListState()
) {
    fun isSelected(mode: ReadingMode, alignment: PageAlignment): Boolean {
        return readerPreferences.readingMode == mode && readerPreferences.pageAlignment == alignment
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    )
    {
        // 已完成的重构
        item {
            SettingList(
                title = { Text(stringResource(R.string.reading_mode_label_padding)) }
            ) {
                Column(
                    modifier = Modifier.padding(dimensionResource(R.dimen.inner_padding_of_container))
                ) {
                    Text(
                        text = "${(readerPreferences.pagePaddingRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End)
                    )
                    val sliderState = rememberSliderState(
                        value = readerPreferences.pagePaddingRatio,
                        steps = 9, trackRange = 0f..0.2f
                    )
                    Slider(
                        state = sliderState,
                        modifier = Modifier,
                        enabled = true,
                        onValueChange = {
                            readerPreferences.updatePagePaddingRatio(it)
                            sliderState.value = it
                        },
                        onValueChangeFinished = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
                }
            }
        }

        items(ReadingMode.entries, key = { it.name }) { mode ->
            ReadingModeSection(
                title = stringResource(mode.displayName),
                onVerticalClick = {
                    readerPreferences.updateReadingMode(mode)
                    readerPreferences.updatePageAlignment(PageAlignment.Vertical)
                },
                onHorizontalClick = {
                    readerPreferences.updateReadingMode(mode)
                    readerPreferences.updatePageAlignment(PageAlignment.Horizontal)
                },
                isVerticalSelected = isSelected(mode, PageAlignment.Vertical),
                isHorizontalSelected = isSelected(mode, PageAlignment.Horizontal),
                verticalContent = {
                    when (mode) {
                        ReadingMode.Single -> VerticalSinglePage()
                        ReadingMode.Dual -> VerticalDualPage(pagePaddingRatio = readerPreferences.pagePaddingRatio)
                        ReadingMode.Scroll -> VerticalScrollablePage(pagePaddingRatio = readerPreferences.pagePaddingRatio)
                    }
                },
                horizontalContent = {
                    when (mode) {
                        ReadingMode.Single -> HorizontalSinglePage()
                        ReadingMode.Dual -> HorizontalDualPage(pagePaddingRatio = readerPreferences.pagePaddingRatio)
                        ReadingMode.Scroll -> HorizontalScrollablePage(pagePaddingRatio = readerPreferences.pagePaddingRatio)
                    }
                }
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
    SettingList(
        title = {
            Text(
                text = title,
            )
        }
    ) {
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
                    text = stringResource(PageAlignment.Vertical.displayName),
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
                    text = stringResource(PageAlignment.Horizontal.displayName),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isHorizontalSelected) FontWeight.Bold else FontWeight.Normal
                )
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
    val radius = dimensionResource(R.dimen.composable_rounded_corner_radius)
    SelectableBox(
        selected = isSelected,
        onSelectedChange = { onClick() },
        modifier = modifier.widthIn(max = 180.dp),
        shape = RoundedCornerShape(radius)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    shape = RoundedCornerShape(radius)
                ),
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
        }
    }
}

/**
 * Page 组件支持两种尺寸模式：
 * 1. 传入 `width` (Dp)：固定宽度模式。
 * 2. 不传 `width` (null)：权重/填充模式，通常配合 `Modifier.weight` 使用。
 * 无论哪种模式，都会通过 `Modifier.aspectRatio` 强制保持 1.4 的高宽比 (Height = 1.4 * Width)。
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
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(4.dp)
            )
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
private fun ReadingModeDetailsScreenPreview() {
    ReaderTheme {
        ReadingModeDetailsScreen(
            readerPreferences = ReaderPreferencesForTest()
        )
    }
}