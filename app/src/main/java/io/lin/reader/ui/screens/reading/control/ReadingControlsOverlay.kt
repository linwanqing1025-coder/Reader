package io.lin.reader.ui.maintab.reading.control

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CollectionsBookmark
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.lin.reader.R
import io.lin.reader.ui.components.menu.StyledMenu
import io.lin.reader.ui.components.menu.StyledMenuItem
import io.lin.reader.ui.maintab.reading.LocalReadingViewModel
import io.lin.reader.ui.maintab.setting.details.CropMode

@SuppressLint("ContextCastToActivity")
@Composable
fun ReadingControlsOverlay(
    visible: Boolean,
    onBackClick: () -> Unit,
    onAddBookmarkClick: () -> Unit,
    onDeleteBookmarkClick: () -> Unit,
) {
    val viewModel = LocalReadingViewModel.current ?: return
    val uiState by viewModel.uiState.collectAsState()
    val readerPreferences = viewModel.readerPreferences

    val book = uiState.volume ?: return
    val series = uiState.series ?: return
    val bookmarkList = uiState.bookmarkList
    val outlineList = uiState.outlineList
    val volumeList = uiState.volumeList
    val isEdgeVolume = uiState.isEdgeVolume
    val pageCount = book.totalPages
    val currentPage = uiState.currentPage
    val isPageBookmarked = viewModel.isPageBookmarked(currentPage, bookmarkList)

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

    var isShowingReaderColorMenu by rememberSaveable { mutableStateOf(false) }
    var isShowingSettingMenu by rememberSaveable { mutableStateOf(false) }
    var isShowingSeriesDirectory by rememberSaveable { mutableStateOf(false) }
    var isShowingBookmarkDirectory by rememberSaveable { mutableStateOf(false) }
    var isShowingDocumentDirectory by rememberSaveable { mutableStateOf(false) }
    var draggingPage by remember(currentPage) { mutableIntStateOf(currentPage) }

    val dismissAllMenus = {
        isShowingReaderColorMenu = false
        isShowingSettingMenu = false
        isShowingSeriesDirectory = false
        isShowingBookmarkDirectory = false
        isShowingDocumentDirectory = false
    }

    val animatedColor by animateColorAsState(targetValue = Color(0xFFE0E0E0), label = "color")

    val isTopMenuShowing = isShowingSeriesDirectory || isShowingBookmarkDirectory || isShowingDocumentDirectory
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
            if (readerPreferences.fixedPageIndicator) {
                PageNumberIndicator(
                    text = "${draggingPage + 1} / $pageCount",
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
                    onPageChange = { viewModel.goToPage(it) },
                    draggingPage = draggingPage,
                    onDraggingPageChange = { draggingPage = it },
                    fixedPageIndicator = readerPreferences.fixedPageIndicator,
                    animatedColor = animatedColor,
                    isEdgeVolume = isEdgeVolume,
                    onNextVolume = { viewModel.switchVolume(1) },
                    onPreviousVolume = { viewModel.switchVolume(-1) },
                    onReaderColorsClick = {
                        val target = !isShowingReaderColorMenu
                        dismissAllMenus()
                        isShowingReaderColorMenu = target
                    },
                    rtlMode = readerPreferences.rtlMode,
                    onToggleRtlModeClick = viewModel.readerPreferences::toggleRtlMode,
                    cropMode = readerPreferences.cropMode,
                    onCropModeChange = viewModel.readerPreferences::updateCropMode,
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
                    bookName = book.volumeName,
                    seriesName = series.seriesName,
                    isPageBookmarked = isPageBookmarked,
                    hasOutline = outlineList.isNotEmpty(),
                    onBackClick = onBackClick,
                    onFavouriteClick = viewModel::toggleFavouriteState,
                    isFavorite = book.isFavorite,
                    onDocumentMenuClick = {
                        val target = !isShowingDocumentDirectory
                        dismissAllMenus()
                        isShowingDocumentDirectory = target
                    },
                    onSeriesMenuClick = {
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
                            viewModel.goToPage(it)
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
                        onVolumeClick = {
                            viewModel.switchVolume(it)
                            dismissAllMenus()
                        }
                    )
                }

                this@Column.AnimatedVisibility(
                    visible = isShowingDocumentDirectory && visible,
                    enter = slideInHorizontally(initialOffsetX = { it }),
                    exit = slideOutHorizontally(targetOffsetX = { it })
                ) {
                    DocumentDirectory(
                        outlineList = outlineList,
                        currentPage = currentPage,
                        onOutlineClick = {
                            viewModel.goToPage(it)
                            dismissAllMenus()
                        }
                    )
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
                    // TODO: viewModel = viewModel
                )
            }
            AnimatedVisibility(
                visible = isShowingReaderColorMenu,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                ReaderColorMenu(
                    onDismiss = { dismissAllMenus() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TopControlBar(
    bookName: String,
    seriesName: String,
    isPageBookmarked: Boolean,
    isFavorite: Boolean,
    hasOutline: Boolean,
    onBackClick: () -> Unit,
    onFavouriteClick: () -> Unit,
    onDocumentMenuClick: () -> Unit,
    onSeriesMenuClick: () -> Unit,
    onBookmarkMenuClick: () -> Unit,
    onBookmarkLongClick: () -> Unit
) {
    val favouriteTint by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300),
        label = "FavoriteColor"
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
                    text = bookName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = seriesName,
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
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favourite",
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
            IconButton(onClick = onSeriesMenuClick) { Icon(Icons.Outlined.CollectionsBookmark, "Series List") }

            if (hasOutline) {
                IconButton(onClick = onDocumentMenuClick) {
                    Icon(Icons.AutoMirrored.Filled.FormatListBulleted, "Document Outline")
                }
            }
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
    cropMode: CropMode = CropMode.None,
    onCropModeChange: (CropMode) -> Unit = {},
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!fixedPageIndicator) {
            PageNumberIndicator(
                text = "${draggingPage + 1} / $pageCount",
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
                    valueRange = 0f..(pageCount - 1).toFloat().coerceAtLeast(0f),
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
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
            ,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            IconButton(onClick = onReaderColorsClick) { Icon(Icons.Default.ColorLens, "Colors") }
            IconButton(onClick = onToggleRtlModeClick) {
                Icon(
                    imageVector = if (rtlMode) Icons.Filled.TurnLeft else Icons.Filled.TurnRight,
                    contentDescription = "Right To Left Mode",
                    modifier = Modifier.size(32.dp),
                    tint = if (rtlMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
            Box{
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = {expanded = true}) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = "Remove Gutter",
                        modifier = Modifier.size(22.dp),
                        tint = if (cropMode != CropMode.None) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                // 裁剪选择菜单
                Box {
                    StyledMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        CropMode.entries.forEach { it ->
                            StyledMenuItem(
                                text = it.name,
                                onClick = {
                                    onCropModeChange(it)
                                    expanded = false
                                },
                                trailingIcon = if (cropMode == it) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, "Settings") }
        }
    }
}

@Composable
private fun SettingMenu() {
    //TODO
}

@Composable
private fun ReaderColorMenu(onDismiss: () -> Unit) {
    //TODO
}