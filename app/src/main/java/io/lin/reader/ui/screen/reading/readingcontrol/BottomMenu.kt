package io.lin.reader.ui.screen.reading.readingcontrol

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.lin.reader.R
import io.lin.reader.ui.screen.setting.SettingSnippet
import io.lin.reader.ui.screen.setting.details.ColorItem
import io.lin.reader.ui.screen.setting.details.ColorPicker
import io.lin.reader.ui.screen.setting.details.InteractionDetails
import io.lin.reader.ui.screen.setting.details.OtherReadingSettingDetails
import io.lin.reader.ui.screen.setting.details.PageAlignment
import io.lin.reader.ui.screen.setting.details.ReaderColor
import io.lin.reader.ui.screen.setting.details.ReaderColorDetails
import io.lin.reader.ui.screen.setting.details.ReadingMode
import io.lin.reader.ui.screen.setting.details.ReadingModeDetails
import io.lin.reader.utils.InteractionStyle

@Composable
fun SettingMenu(
    modifier: Modifier = Modifier,
    paddingRatio: Float,
    onPaddingRatioChange: (Float) -> Unit,
    readingMode: ReadingMode,
    onReadingModeChange: (ReadingMode) -> Unit,
    pageAlignment: PageAlignment,
    onPageAlignmentChange: (PageAlignment) -> Unit,
    interactionStyle: InteractionStyle,
    updateInteractionStyle: (InteractionStyle) -> Unit,
    readerColor: ReaderColor,
    onBackgroundColorChange: (Color) -> Unit,
    onPageColorChange: (Color) -> Unit,
    onFilterColorChange: (Color) -> Unit,
    rtlMode: Boolean,
    toggleRtlMode: () -> Unit,
    separateCover: Boolean,
    toggleSeparateCover: () -> Unit,
    removeGutter: Boolean,
    toggleRemoveGutter: () -> Unit,
    fixedPageIndicator: Boolean,
    toggleFixedPageIndicator: () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxHeightDp = with(LocalDensity.current) { (constraints.maxHeight * 0.8f).toDp() }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxHeightDp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(
                topStart = dimensionResource(R.dimen.composable_rounded_corner_radius),
                topEnd = dimensionResource(R.dimen.composable_rounded_corner_radius)
            ),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp, shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(50))
            ) {
                var selectedTabIndex by remember { mutableIntStateOf(0) }
                val tabs = listOf(
                    stringResource(R.string.reading_bookmark_menu_tab_reading),
                    stringResource(R.string.reading_bookmark_menu_tab_interaction),
                    stringResource(R.string.reading_bookmark_menu_tab_color),
                    stringResource(R.string.reading_bookmark_menu_tab_others)
                )
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    divider = {},
                    indicator = { tabPositions ->
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                .padding(horizontal = 32.dp)
                                .height(3.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                )
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTabIndex = index },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    AnimatedContent(
                        targetState = selectedTabIndex,
                        transitionSpec = {
                            val duration = 200
                            if (targetState > initialState) {
                                slideInHorizontally(animationSpec = tween(duration)) { it } togetherWith
                                        slideOutHorizontally(animationSpec = tween(duration)) { -it }
                            } else {
                                slideInHorizontally(animationSpec = tween(duration)) { -it } togetherWith
                                        slideOutHorizontally(animationSpec = tween(duration)) { it }
                            }
                        },
                        label = "settingContent"
                    ) { targetIndex ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            when (targetIndex) {
                                0 -> ReadingModeDetails(
                                    readingMode = readingMode,
                                    onReadingModeChange = onReadingModeChange,
                                    pageAlignment = pageAlignment,
                                    onPageAlignmentChange = onPageAlignmentChange,
                                    paddingRatio = paddingRatio,
                                    onPaddingRatioChange = onPaddingRatioChange,
                                )

                                1 -> InteractionDetails(
                                    interactionStyle = interactionStyle,
                                    rtlMode = rtlMode,
                                    updateInteractionStyle = updateInteractionStyle
                                )

                                2 -> ReaderColorDetails(
                                    readerColor = readerColor,
                                    onBackgroundColorChange = onBackgroundColorChange,
                                    onPageColorChange = onPageColorChange,
                                    onFilterColorChange = onFilterColorChange
                                )

                                3 -> OtherReadingSettingDetails(
                                    rtlMode = rtlMode,
                                    toggleRtlMode = toggleRtlMode,
                                    separateCover = separateCover,
                                    toggleSeparateCover = toggleSeparateCover,
                                    removeGutter = removeGutter,
                                    toggleRemoveGutter = toggleRemoveGutter,
                                    fixedPageIndicator = fixedPageIndicator,
                                    toggleFixedPageIndicator = toggleFixedPageIndicator
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderColorMenu(
    modifier: Modifier = Modifier,
    readerColor: ReaderColor,
    onBackgroundColorChange: (Color) -> Unit,
    onPageColorChange: (Color) -> Unit,
    onFilterColorChange: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var isShowingColorSelector by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    BackHandler(enabled = isShowingColorSelector) {
        isShowingColorSelector = false
    }
    val background = stringResource(R.string.reader_color_background)
    val page = stringResource(R.string.reader_color_page)
    val filter = stringResource(R.string.reader_color_filter)

    // Internal state to track current selection for the ColorSettingSection
    var currentColor by remember(readerColor, isShowingColorSelector, title) {
        mutableStateOf(
            when (title) {
                background -> Color(readerColor.background)
                page -> Color(readerColor.page)
                filter -> Color(readerColor.filter)
                else -> Color(readerColor.background)
            }
        )
    }
    var onColorChangeLambda by remember { mutableStateOf<(Color) -> Unit>({}) }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { onDismiss() }
                }
        )
        val maxHeightDp = with(LocalDensity.current) { (constraints.maxHeight * 0.8f).toDp() }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxHeightDp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(
                topStart = dimensionResource(R.dimen.composable_rounded_corner_radius),
                topEnd = dimensionResource(R.dimen.composable_rounded_corner_radius)
            ),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp, shadowElevation = 8.dp
        ) {
            LazyColumn(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.inner_padding_of_container))
                    .animateContentSize(animationSpec = tween(50))
            ) {
                item {
                    AnimatedContent(
                        targetState = isShowingColorSelector,
                        transitionSpec = {
                            if (targetState) {
                                (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(
                                    animationSpec = tween(300)
                                ))
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(
                                        animationSpec = tween(300)
                                    ))
                            } else {
                                (slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(
                                    animationSpec = tween(300)
                                ))
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(
                                        animationSpec = tween(300)
                                    ))
                            }
                        },
                        label = "ColorDetailsSwitch"
                    ) { showingSelector ->
                        if (!showingSelector) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.reader_color_label_color),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(
                                        start = dimensionResource(R.dimen.half_inner_padding_of_container),
                                        bottom = dimensionResource(R.dimen.half_inner_padding_of_container)
                                    )
                                )
                                SettingSnippet {
                                    Column {
                                        ColorItem(
                                            hint = background,
                                            color = Color(readerColor.background),
                                            onClick = {
                                                title = background
                                                onColorChangeLambda = onBackgroundColorChange
                                                isShowingColorSelector = true
                                            }
                                        )
                                        ColorItem(
                                            hint = page,
                                            color = Color(readerColor.page),
                                            onClick = {
                                                title = page
                                                onColorChangeLambda = onPageColorChange
                                                isShowingColorSelector = true
                                            }
                                        )
                                        ColorItem(
                                            hint = filter,
                                            color = Color(readerColor.filter),
                                            onClick = {
                                                title = filter
                                                onColorChangeLambda = onFilterColorChange
                                                isShowingColorSelector = true
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            ColorPicker(
                                title = title,
                                currentColor = currentColor,
                                onColorChange = {
                                    currentColor = it
                                    onColorChangeLambda(it)
                                },
                                onBackClick = { isShowingColorSelector = false }
                            )
                        }
                    }
                }

            }
        }
    }
}
