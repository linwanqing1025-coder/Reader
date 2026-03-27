package io.lin.reader.ui.screen.reading

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.lin.reader.R
import io.lin.reader.data.preferences.ReaderPreferences
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.textfield.StyledOutlinedTextField
import io.lin.reader.ui.screen.reading.readingcontrol.ReadingControlsOverlay
import io.lin.reader.ui.screen.setting.details.ReadingMode
import io.lin.reader.ui.viewmodel.ReadingScreenViewModel
import io.lin.reader.utils.InteractionMask
import io.lin.reader.utils.interactionStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("AutoxingStateCreation")
@Composable
fun ReaderSurface(
    modifier: Modifier = Modifier,
    viewModel: ReadingScreenViewModel,
    navigateBack: () -> Unit = { },
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val uiState by viewModel.uiState.collectAsState()
    val readingPreferences by viewModel.readingSetting.state.collectAsState()

    val book = uiState.volume
    val series = uiState.series

    if (book == null || series == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val pdfRenderer by viewModel.pdfRenderer.collectAsState()
    val pageCount by viewModel.pageCount.collectAsState()
    val cachedBitmaps = viewModel.cachedBitmaps

    val initialPage = remember(book.id, uiState.initialPage) {
        uiState.initialPage ?: if (book.lastReadPage > 0) book.lastReadPage else 1
    }

    var currentPageNumber by rememberSaveable(
        book.id,
        initialPage
    ) { mutableIntStateOf(initialPage) }

    LaunchedEffect(uiState.initialPage) {
        uiState.initialPage?.let { page ->
            if (page > 0 && page != currentPageNumber) {
                currentPageNumber = page
                viewModel.updateLastReadPage(page)
            }
        }
    }

    var showControls by rememberSaveable { mutableStateOf(false) }
    var showAddBookmarkDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteBookmarkDialog by rememberSaveable { mutableStateOf(false) }
    var bookmarkLabel by rememberSaveable { mutableStateOf("") }

    val scaleAnim = remember { Animatable(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }

    val favoriteRemovedText = stringResource(R.string.reading_removed_from_favourite)
    val favoriteAddedText = stringResource(R.string.reading_added_to_favourite)
    val removeGutterOnText = stringResource(R.string.reading_remove_gutter_on)
    val removeGutterOffText = stringResource(R.string.reading_remove_gutter_off)
    val rtlModeOnText = stringResource(R.string.reading_rlt_mode_on)
    val rtlModeOffText = stringResource(R.string.reading_rlt_mode_off)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(readingPreferences.colors.background))
    ) {
        val density = LocalDensity.current.density
        val viewWidthPx = constraints.maxWidth.toFloat()
        val viewHeightPx = constraints.maxHeight.toFloat()
        val bitmapWidth = (maxWidth.value * density).toInt()

        LaunchedEffect(
            pdfRenderer,
            currentPageNumber,
            readingPreferences.readingMode,
            bitmapWidth
        ) {
            if (pdfRenderer == null) return@LaunchedEffect

            val isDual = readingPreferences.readingMode == ReadingMode.Dual
            val bufferSize = if (isDual) 10 else 5
            val offset = if (isDual) 4 else 2

            val currentIndex = currentPageNumber - 1
            val targetPages = (0 until bufferSize).map {
                currentIndex - offset + it
            }.filter { it in 0 until pageCount }

            viewModel.prefetchPages(targetPages, bitmapWidth)
        }

        GestureInteractionLayer(
            scaleAnim = scaleAnim,
            offsetXAnim = offsetXAnim,
            offsetYAnim = offsetYAnim,
            readerPreferences = readingPreferences,
            widthPx = viewWidthPx,
            heightPx = viewHeightPx,
            onPreviousClick = {
                val step = if (readingPreferences.readingMode == ReadingMode.Dual) 2 else 1
                val newPage = (currentPageNumber - step).coerceAtLeast(1)
                currentPageNumber = newPage
                viewModel.updateLastReadPage(newPage)
            },
            onNextClick = {
                val step = if (readingPreferences.readingMode == ReadingMode.Dual) 2 else 1
                val newPage = (currentPageNumber + step).coerceAtMost(pageCount)
                currentPageNumber = newPage
                viewModel.updateLastReadPage(newPage)
            },
            onCenterClick = {
                showControls = !showControls
            },
            clearInteractionStyleHint = {
                viewModel.readingSetting.clearInteractionStyleHint()
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        translationX = offsetXAnim.value
                        translationY = offsetYAnim.value
                    },
                contentAlignment = Alignment.Center
            ) {
                if (pdfRenderer != null) {
                    ReadingModeContainer(
                        readingPreferences = readingPreferences,
                        initialPageIndex = currentPageNumber - 1,
                        pageCount = pageCount,
                        onPageIndexChange = { index ->
                            val newPage = index + 1
                            if (newPage != currentPageNumber) {
                                currentPageNumber = newPage
                                viewModel.updateLastReadPage(newPage)
                            }
                        },
                        getPageBitmap = { index -> cachedBitmaps[index] }
                    )
                }
            }
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color(readingPreferences.colors.filter)))

        ReadingControlsOverlay(
            visible = showControls,
            book = book,
            series = series,
            volumeList = uiState.volumeList,
            isPageBookmarked = viewModel.isPageBookmarked(currentPageNumber, uiState.bookmarkList),
            bookmarkList = uiState.bookmarkList,
            onBackClick = navigateBack,
            toggleFavouriteState = {
                val text = if (book.isFavorite) favoriteRemovedText else favoriteAddedText
                scope.launch { snackbarHostState.showSnackbar(text) }
                viewModel.toggleFavouriteState()
            },
            onVolumeClick = { viewModel.onVolumeSelected(it) },
            onAddBookmarkClick = {
                bookmarkLabel = viewModel.generateAutoBookmarkLabel()
                showAddBookmarkDialog = true
            },
            onDeleteBookmarkClick = { showDeleteBookmarkDialog = true },
            currentPage = currentPageNumber,
            pageCount = pageCount,
            onPageChange = { newPage ->
                currentPageNumber = newPage
                viewModel.updateLastReadPage(newPage)
            },
            isEdgeVolume = uiState.isEdgeVolume,
            onPreviousVolume = { viewModel.switchToPreviousVolume() },
            onNextVolume = { viewModel.switchToNextVolume() },
            readingMode = readingPreferences.readingMode,
            onReadingModeChange = viewModel.readingSetting::updateReadingMode,
            pageAlignment = readingPreferences.pageAlignment,
            onPageAlignmentChange = viewModel.readingSetting::updatePageAlignment,
            paddingRatio = readingPreferences.pagePaddingRatio,
            onPaddingRatioChange = viewModel.readingSetting::updatePagePaddingRatio,
            interactionStyle = readingPreferences.interactionStyle,
            updateInteractionStyle = viewModel.readingSetting::updateInteractionStyle,
            readerColor = readingPreferences.colors,
            onBackgroundColorChange = { viewModel.readingSetting.updateBackgroundColor(it.toArgb()) },
            onPageColorChange = { viewModel.readingSetting.updatePageColor(it.toArgb()) },
            onFilterColorChange = { viewModel.readingSetting.updateFilterColor(it.toArgb()) },
            rtlMode = readingPreferences.rtlMode,
            toggleRtlMode = {
                val text = if (!readingPreferences.rtlMode) rtlModeOnText else rtlModeOffText
                scope.launch { snackbarHostState.showSnackbar(text) }
                viewModel.readingSetting.toggleRtlMode()
            },
            separateCover = readingPreferences.separateCover,
            toggleSeparateCover = viewModel.readingSetting::toggleSeparateCover,
            removeGutter = readingPreferences.removeGutter,
            toggleRemoveGutter = {
                val text =
                    if (!readingPreferences.removeGutter) removeGutterOnText else removeGutterOffText
                scope.launch { snackbarHostState.showSnackbar(text) }
                viewModel.readingSetting.toggleRemoveGutter()
            },
            fixedPageIndicator = readingPreferences.fixedPageIndicator,
            toggleFixedPageIndicator = viewModel.readingSetting::toggleFixedPageIndicator
        )

        if (showAddBookmarkDialog) {
            BookmarkAddDialog(
                currentPage = currentPageNumber,
                totalPages = pageCount,
                value = bookmarkLabel,
                onValueChange = { bookmarkLabel = it },
                validateInput = { bookmarkLabel.isNotBlank() },
                onDismiss = { showAddBookmarkDialog = false },
                onConfirm = {
                    viewModel.addBookmark(bookmarkLabel, currentPageNumber)
                    showAddBookmarkDialog = false
                }
            )
        }
        if (showDeleteBookmarkDialog) {
            val notificationText =
                stringResource(R.string.reading_bookmark_delete_notification, currentPageNumber)
            NotificationDialog(
                title = stringResource(R.string.bookmark_dialog_title_delete),
                notification = notificationText,
                onDismiss = { showDeleteBookmarkDialog = false },
                onConfirm = {
                    viewModel.deleteBookmark(currentPageNumber)
                    showDeleteBookmarkDialog = false
                }
            )
        }
    }
}

@Composable
private fun GestureInteractionLayer(
    modifier: Modifier = Modifier,
    scaleAnim: Animatable<Float, AnimationVector1D>,
    offsetXAnim: Animatable<Float, AnimationVector1D>,
    offsetYAnim: Animatable<Float, AnimationVector1D>,
    readerPreferences: ReaderPreferences,
    widthPx: Float,
    heightPx: Float,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onCenterClick: () -> Unit,
    clearInteractionStyleHint: () -> Unit,
    readingContent: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val viewConfiguration = LocalViewConfiguration.current
    val customViewConfiguration = remember(viewConfiguration) {
        object : ViewConfiguration by viewConfiguration {
            override val doubleTapTimeoutMillis: Long = 150L
        }
    }

    var showMask by remember { mutableStateOf(false) }

    LaunchedEffect(readerPreferences.interactionStyle, readerPreferences.isInteractionHintPending) {
        if (readerPreferences.isInteractionHintPending) {
            showMask = true
            delay(5000)
            if (showMask) {
                showMask = false
                clearInteractionStyleHint()
            }
        } else {
            showMask = false
        }
    }

    CompositionLocalProvider(LocalViewConfiguration provides customViewConfiguration) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(scaleAnim.value > 1f, widthPx, heightPx) {
                    if (scaleAnim.value > 1f) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scaleAnim.value * zoom).coerceIn(1f, 5f)
                            val maxOffsetX =
                                if (newScale > 1f) (widthPx * (newScale - 1f)) / 2f else 0f
                            val maxOffsetY =
                                if (newScale > 1f) (heightPx * (newScale - 1f)) / 2f else 0f

                            scope.launch {
                                scaleAnim.snapTo(newScale)
                                offsetXAnim.snapTo(
                                    (offsetXAnim.value + pan.x).coerceIn(
                                        -maxOffsetX,
                                        maxOffsetX
                                    )
                                )
                                offsetYAnim.snapTo(
                                    (offsetYAnim.value + pan.y).coerceIn(
                                        -maxOffsetY,
                                        maxOffsetY
                                    )
                                )
                            }
                        }
                    }
                }
                .pointerInput(readerPreferences.interactionStyle, readerPreferences.rtlMode, showMask) {
                    detectTapGestures(
                        onDoubleTap = {
                            val targetScale = if (scaleAnim.value > 1f) 1f else 2.5f
                            scope.launch {
                                launch { scaleAnim.animateTo(targetScale, tween(300)) }
                                launch { offsetXAnim.animateTo(0f, tween(300)) }
                                launch { offsetYAnim.animateTo(0f, tween(300)) }
                            }
                        },
                        onTap = { offset ->
                            if (showMask) {
                                showMask = false
                                clearInteractionStyleHint()
                            }
                            else if (scaleAnim.value <= 1.05f) {
                                interactionStyle(
                                    style = readerPreferences.interactionStyle,
                                    isRtl = readerPreferences.rtlMode,
                                    width = widthPx.toInt(),
                                    height = heightPx.toInt(),
                                    offset = offset,
                                    onPreviousClick = onPreviousClick,
                                    onNextClick = onNextClick,
                                    onCenterClick = onCenterClick
                                )
                            }
                        }
                    )
                }
        ) {
            readingContent()
            
            AnimatedVisibility(
                visible = showMask,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500))
            ) {
                InteractionMask(
                    style = readerPreferences.interactionStyle,
                    isRtl = readerPreferences.rtlMode
                )
            }
        }
    }
}

@Composable
private fun BookmarkAddDialog(
    currentPage: Int,
    totalPages: Int,
    value: String,
    onValueChange: (String) -> Unit,
    validateInput: () -> Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier.width(dimensionResource(R.dimen.dialog_size)),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            @Suppress("DEPRECATION")
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.reading_bookmark_dialog_title_add),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                StyledOutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    value = value,
                    placeholder = stringResource(R.string.reading_bookmark_dialog_placeholder),
                    labelText = stringResource(R.string.reading_bookmark_dialog_label),
                    errorText = stringResource(R.string.reading_bookmark_dialog_error),
                    onValueChange = onValueChange,
                    validateInput = validateInput
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            @Suppress("DEPRECATION")
                            Text(
                                text = stringResource(R.string.reading_bookmark_dialog_location),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            @Suppress("DEPRECATION")
                            Text(
                                text = stringResource(
                                    R.string.reading_bookmark_dialog_location_detail,
                                    currentPage,
                                    totalPages
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(
                            dimensionResource(R.dimen.composable_rounded_corner_radius)
                        ),
                        modifier = Modifier
                            .height(40.dp)
                            .height(IntrinsicSize.Min),
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 16.sp,
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(
                            dimensionResource(R.dimen.composable_rounded_corner_radius)
                        ),
                        modifier = Modifier
                            .height(40.dp)
                            .height(IntrinsicSize.Min)
                    ) {
                        Text(
                            text = stringResource(R.string.confirm),
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}
