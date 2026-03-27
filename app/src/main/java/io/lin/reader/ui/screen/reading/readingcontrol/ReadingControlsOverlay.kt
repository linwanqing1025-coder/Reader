package io.lin.reader.ui.screen.reading.readingcontrol

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BorderVertical
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.BorderVertical
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.lin.reader.R
import io.lin.reader.data.database.Bookmark
import io.lin.reader.data.database.Series
import io.lin.reader.data.database.Volume
import io.lin.reader.ui.screen.setting.details.PageAlignment
import io.lin.reader.ui.screen.setting.details.ReaderColor
import io.lin.reader.ui.screen.setting.details.ReadingMode
import io.lin.reader.utils.InteractionStyle

@Composable
fun ReadingControlsOverlay(
    visible: Boolean,
    book: Volume,
    series: Series,
    volumeList: List<Volume>,
    isPageBookmarked: Boolean,
    bookmarkList: List<Bookmark>,
    onBackClick: () -> Unit,
    toggleFavouriteState: () -> Unit,
    onVolumeClick: (Long) -> Unit,
    onAddBookmarkClick: () -> Unit,
    onDeleteBookmarkClick: () -> Unit,
    currentPage: Int,
    pageCount: Int,
    onPageChange: (Int) -> Unit,
    isEdgeVolume: Pair<Boolean, Boolean>,
    onPreviousVolume: () -> Unit,
    onNextVolume: () -> Unit,
    readingMode: ReadingMode,
    onReadingModeChange: (ReadingMode) -> Unit,
    pageAlignment: PageAlignment,
    onPageAlignmentChange: (PageAlignment) -> Unit,
    paddingRatio: Float,
    onPaddingRatioChange: (Float) -> Unit,
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
    val view = LocalView.current
    val activity = LocalContext.current as? Activity

    LaunchedEffect(visible) {
        if (activity != null) {
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, view)
            if (visible) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (activity != null) {
                val window = activity.window
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // 使用 rememberSaveable 确保菜单显示状态在旋转屏幕后得以保持
    var isShowingReaderColorMenu by rememberSaveable { mutableStateOf(false) }
    var isShowingSettingMenu by rememberSaveable { mutableStateOf(false) }
    var isShowingSeriesDirectory by rememberSaveable { mutableStateOf(false) }
    var isShowingBookmarkDirectory by rememberSaveable { mutableStateOf(false) }
    var draggingPage by remember(currentPage) { mutableIntStateOf(currentPage) }

    val dismissAllMenus = {
        isShowingReaderColorMenu = false
        isShowingSettingMenu = false
        isShowingSeriesDirectory = false
        isShowingBookmarkDirectory = false
    }

    val animatedColor by animateColorAsState(targetValue = Color(0xFFE0E0E0), label = "color")

    val isTopMenuShowing = isShowingSeriesDirectory || isShowingBookmarkDirectory
    val isBottomMenuShowing = isShowingReaderColorMenu || isShowingSettingMenu

    BackHandler(enabled = isTopMenuShowing || isBottomMenuShowing) {
        dismissAllMenus()
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (fixedPageIndicator) {
                PageNumberIndicator(
                    text = "$draggingPage / $pageCount",
                    color = animatedColor,
                    modifier = Modifier
                        .padding(bottom = dimensionResource(R.dimen.half_inner_padding_of_container))
                )
            }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + expandVertically() + slideInVertically { it },
                exit = fadeOut() + shrinkVertically() + slideOutVertically { it }
            ) {
                BottomControlBar(
                    currentPage = currentPage,
                    pageCount = pageCount,
                    onPageChange = onPageChange,
                    draggingPage = draggingPage,
                    onDraggingPageChange = { draggingPage = it },
                    fixedPageIndicator = fixedPageIndicator,
                    animatedColor = animatedColor,
                    isEdgeVolume = isEdgeVolume,
                    onNextVolume = onNextVolume,
                    onPreviousVolume = onPreviousVolume,
                    onReaderColorsClick = {
                        val target = !isShowingReaderColorMenu
                        dismissAllMenus()
                        isShowingReaderColorMenu = target
                    },
                    rtlMode = rtlMode,
                    onToggleRtlModeClick = toggleRtlMode,
                    removeGutter = removeGutter,
                    onToggleRemoveGutter = toggleRemoveGutter,
                    onSettingsClick = {
                        val target = !isShowingSettingMenu
                        dismissAllMenus()
                        isShowingSettingMenu = target
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = isTopMenuShowing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(0.7f))
                    .pointerInput(Unit) {
                        detectTapGestures { dismissAllMenus() }
                    }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.zIndex(2f)
            ) {
                TopControlBar(
                    book = book,
                    series = series,
                    isPageBookmarked = isPageBookmarked,
                    onBackClick = onBackClick,
                    onFavouriteClick = toggleFavouriteState,
                    onMenuClick = {
                        val target = !isShowingSeriesDirectory
                        dismissAllMenus()
                        isShowingSeriesDirectory = target
                    },
                    onBookmarkMenuClick = {
                        val target = !isShowingBookmarkDirectory
                        dismissAllMenus()
                        isShowingBookmarkDirectory = target
                    },
                    onBookmarkLongClick = {
                        if (isPageBookmarked) onDeleteBookmarkClick()
                        else onAddBookmarkClick()
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f),
                contentAlignment = Alignment.TopEnd
            ) {
                this@Column.AnimatedVisibility(
                    visible = isShowingBookmarkDirectory && visible,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it })
                ) {
                    BookmarkDirectory(
                        totalPages = book.totalPages,
                        bookmarkList = bookmarkList,
                        onBookmarkClick = {
                            onPageChange(it)
                            dismissAllMenus()
                        },
                        onAddBookmark = onAddBookmarkClick
                    )
                }

                this@Column.AnimatedVisibility(
                    visible = isShowingSeriesDirectory && visible,
                    enter = slideInHorizontally(initialOffsetX = { it }),
                    exit = slideOutHorizontally(targetOffsetX = { it })
                ) {
                    SeriesDirectory(
                        currentVolumeId = book.id,
                        series = series,
                        volumeList = volumeList,
                        onVolumeClick = { onVolumeClick(it); dismissAllMenus() })
                }
            }
        }

        AnimatedVisibility(
            visible = isShowingSettingMenu,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(0.7f))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            isShowingSettingMenu = false
                            isShowingReaderColorMenu = false
                        }
                    }
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AnimatedVisibility(
                visible = isShowingSettingMenu,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                SettingMenu(
                    readingMode = readingMode,
                    onReadingModeChange = onReadingModeChange,
                    pageAlignment = pageAlignment,
                    onPageAlignmentChange = onPageAlignmentChange,
                    paddingRatio = paddingRatio,
                    onPaddingRatioChange = onPaddingRatioChange,
                    interactionStyle = interactionStyle,
                    updateInteractionStyle = updateInteractionStyle,
                    readerColor = readerColor,
                    onBackgroundColorChange = onBackgroundColorChange,
                    onPageColorChange = onPageColorChange,
                    onFilterColorChange = onFilterColorChange,
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
            AnimatedVisibility(
                visible = isShowingReaderColorMenu,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                ReaderColorMenu(
                    readerColor = readerColor,
                    onBackgroundColorChange = onBackgroundColorChange,
                    onPageColorChange = onPageColorChange,
                    onFilterColorChange = onFilterColorChange,
                    onDismiss = { dismissAllMenus() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TopControlBar(
    book: Volume,
    series: Series,
    isPageBookmarked: Boolean,
    onBackClick: () -> Unit,
    onFavouriteClick: () -> Unit,
    onMenuClick: () -> Unit,
    onBookmarkMenuClick: () -> Unit,
    onBookmarkLongClick: () -> Unit
) {
    val favouriteTint by animateColorAsState(
        targetValue = if (book.isFavorite) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300),
        label = "BookmarkColor"
    )

    val bookmarkTint by animateColorAsState(
        targetValue = if (isPageBookmarked) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300),
        label = "BookmarkColor"
    )

    TopAppBar(
        windowInsets = TopAppBarDefaults.windowInsets,
        title = {
            Column {
                Text(
                    text = book.volumeName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = series.seriesName,
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = onFavouriteClick) {
                Icon(
                    imageVector = if (book.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Table of Contents",
                    tint = favouriteTint
                )
            }
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = onBookmarkMenuClick,
                        onLongClick = onBookmarkLongClick,
                        indication = ripple(bounded = true, radius = 20.dp),
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPageBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = bookmarkTint
                )
            }
            IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Table of Contents") }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {})
    )
}

@Composable
private fun PageNumberIndicator(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                drawStyle = Stroke(miter = 10f, width = 4f, join = StrokeJoin.Round)
            ),
            color = Color.Black.copy(alpha = 0.7f)
        )
        Text(
            text = text,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
private fun BottomControlBar(
    currentPage: Int,
    pageCount: Int,
    onPageChange: (Int) -> Unit,
    draggingPage: Int,
    onDraggingPageChange: (Int) -> Unit,
    fixedPageIndicator: Boolean,
    animatedColor: Color,
    isEdgeVolume: Pair<Boolean, Boolean>,
    onPreviousVolume: () -> Unit,
    onNextVolume: () -> Unit,
    onReaderColorsClick: () -> Unit,
    rtlMode: Boolean,
    onToggleRtlModeClick: () -> Unit,
    removeGutter: Boolean,
    onToggleRemoveGutter: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!fixedPageIndicator) {
            PageNumberIndicator(
                text = "$draggingPage / $pageCount",
                color = animatedColor,
                modifier = Modifier
                    .padding(bottom = dimensionResource(R.dimen.half_inner_padding_of_container))
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousVolume,
                enabled = !isEdgeVolume.first
            ) { Icon(Icons.Filled.SkipPrevious, "Prev") }
            var sliderPosition by remember(currentPage) { mutableFloatStateOf(currentPage.toFloat()) }
            val direction = if (rtlMode) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        sliderPosition = it
                        onDraggingPageChange(it.toInt())
                    },
                    onValueChangeFinished = { onPageChange(sliderPosition.toInt()) },
                    valueRange = 1f..pageCount.toFloat().coerceAtLeast(1f),
                    steps = (pageCount - 2).coerceAtLeast(0),
                    modifier = Modifier.weight(1f)
                )
            }
            IconButton(
                onClick = onNextVolume,
                enabled = !isEdgeVolume.second
            ) { Icon(Icons.Filled.SkipNext, "Next") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surfaceContainer)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            IconButton(onClick = onReaderColorsClick) { Icon(Icons.Default.ColorLens, "Colors") }
            IconButton(onClick = onToggleRtlModeClick) {
                Icon(
                    imageVector = if (rtlMode) Icons.Filled.TurnLeft else Icons.Filled.TurnRight,
                    contentDescription = "Right To Left Mode",
                    tint = if (rtlMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
            IconButton(onClick = onToggleRemoveGutter) {
                Icon(
                    imageVector = if (removeGutter) Icons.Filled.BorderVertical else Icons.Outlined.BorderVertical,
                    contentDescription = "Remove Gutter",
                    tint = if (removeGutter) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
            IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, "Settings") }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun ReadingControlsPreview() {
    var visible by remember { mutableStateOf(true) }
    val dummySeries = Series(seriesName = "Test Series")
    val dummyBook = Volume(
        id = 1,
        volumeName = "Volume 1",
        bookFileUri = "",
        totalPages = 200,
        lastReadPage = 50,
        seriesId = 1L
    )
    val dummyVolumes =
        List(15) { i -> dummyBook.copy(id = i.toLong(), volumeName = "Volume ${i + 1}") }
    MaterialTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
                .clickable(onClick = { visible = !visible })
        ) {
            ReadingControlsOverlay(
                visible = visible,
                book = dummyBook,
                series = dummySeries,
                volumeList = dummyVolumes,
                isPageBookmarked = false,
                bookmarkList = emptyList(),
                onBackClick = {},
                toggleFavouriteState = {},
                onVolumeClick = {},
                onAddBookmarkClick = {},
                onDeleteBookmarkClick = {},
                currentPage = 50,
                pageCount = 200,
                onPageChange = {},
                isEdgeVolume = false to false,
                onPreviousVolume = {},
                onNextVolume = {},
                removeGutter = true,
                toggleRemoveGutter = {},
                readingMode = ReadingMode.Single,
                onReadingModeChange = {},
                pageAlignment = PageAlignment.Vertical,
                onPageAlignmentChange = {},
                paddingRatio = 0.02f,
                onPaddingRatioChange = {},
                separateCover = false,
                toggleSeparateCover = {},
                interactionStyle = InteractionStyle.Style0,
                rtlMode = true,
                toggleRtlMode = {},
                updateInteractionStyle = {},
                readerColor = ReaderColor(),
                onBackgroundColorChange = {},
                onPageColorChange = {},
                onFilterColorChange = {},
                fixedPageIndicator = true,
                toggleFixedPageIndicator = {}
            )
        }
    }
}
