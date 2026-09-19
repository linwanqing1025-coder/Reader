package io.lin.reader.ui.feature.reading

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.input.pointer.pointerInput
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
import io.lin.reader.data.preferences.ReadingMode
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.textfield.StyledOutlinedTextField
import io.lin.reader.ui.feature.reading.control.ReadingControlsOverlay
import io.lin.reader.ui.feature.reading.control.ToolsBar
import io.lin.reader.ui.feature.reading.feature.display.ReadingModeContainer
import io.lin.reader.ui.feature.reading.control.InteractionMask
import io.lin.reader.ui.feature.reading.control.interactionStyle
import io.lin.reader.ui.feature.reading.feature.display.MAX_SCALE
import io.lin.reader.ui.feature.reading.feature.display.MIN_SCALE
import io.lin.reader.ui.feature.reading.feature.display.ReadingTransform
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "BookReader.kt"

@SuppressLint("AutoxingStateCreation")
@Composable
fun BookReader(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit = { },
    snackbarHostState: SnackbarHostState,
) {
    val viewModel = LocalReadingViewModel.current!!
    val uiState by viewModel.uiState.collectAsState()
    val readingPreferences = viewModel.readerPreferences

    val book = uiState.volume
    val series = uiState.series

    if (book == null || series == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val muPdfCore by viewModel.mCore.collectAsState()
    val pageCount = uiState.volume!!.totalPages

    val initialPage = remember(book.id, uiState.currentPage) {
        uiState.currentPage
    }

    var currentPageNumber by rememberSaveable(
        book.id,
        initialPage
    ) { mutableIntStateOf(initialPage) }

    var showControls by rememberSaveable { mutableStateOf(false) }
    var showAddBookmarkDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteBookmarkDialog by rememberSaveable { mutableStateOf(false) }
    var bookmarkLabel by rememberSaveable { mutableStateOf("") }

    val readingTransform = remember { ReadingTransform() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(readingPreferences.readerColor.background))
    ) {
        //手势操控 + 阅读内容
        CompositionLocalProvider(
            LocalReadingTransform provides readingTransform,
        ) {
            GestureInteractionLayer(
                onCenterClick = { showControls = !showControls }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (muPdfCore != null) {
                        ReadingModeContainer(
                            initialPageIndex = currentPageNumber,
                            onPageIndexChange = { index ->
                                if (index != currentPageNumber) {
                                    currentPageNumber = index
                                    viewModel.goToPage(index)
                                }
                            }
                        )
                    }
                }
            }
        }

        //滤镜层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(readingPreferences.readerColor.filter))
        )

        //工具栏
        AnimatedVisibility(
            visible = showControls,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 72.dp, start = 8.dp, end = 8.dp)
        ) {
            ToolsBar(
                snackbarHostState = snackbarHostState,
            )
        }

        //上下控制栏
        ReadingControlsOverlay(
            visible = showControls,
            onBackClick = navigateBack,
            onAddBookmarkClick = {
                bookmarkLabel = viewModel.generateAutoBookmarkLabel()
                showAddBookmarkDialog = true
            },
            onDeleteBookmarkClick = { showDeleteBookmarkDialog = true }
        )

        if (showAddBookmarkDialog) {
            BookmarkAddDialog(
                currentPage = currentPageNumber + 1,
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
                stringResource(R.string.reading_bookmark_delete_notification, currentPageNumber + 1)
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
    onCenterClick: () -> Unit,
    readingContent: @Composable () -> Unit
) {
    val viewModel = LocalReadingViewModel.current!!
    val transform = LocalReadingTransform.current
    val readerPreferences = viewModel.readerPreferences

    val scaleAnim = transform.scaleAnim
    val offsetXAnim = transform.offsetAnim.x
    val offsetYAnim = transform.offsetAnim.y

    val scope = rememberCoroutineScope()
    var showMask by remember { mutableStateOf(false) }

    LaunchedEffect(readerPreferences.interactionStyle, readerPreferences.isInteractionHintPending) {
        if (readerPreferences.isInteractionHintPending) {
            showMask = true
            delay(5000.milliseconds)
            if (showMask) {
                showMask = false
                viewModel.readerPreferences.clearInteractionStyleHint()
            }
        } else {
            showMask = false
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val contentSize = transform.contentSize

        val viewConfiguration = LocalViewConfiguration.current
        val overrodeViewConfiguration = remember(viewConfiguration) {
            object : ViewConfiguration by viewConfiguration {
                override val doubleTapTimeoutMillis: Long = 200L
            }
        }

        CompositionLocalProvider(
            LocalViewConfiguration provides overrodeViewConfiguration
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(scaleAnim.value > 1f, widthPx, heightPx, contentSize) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (readerPreferences.isReflow) {
                                // TODO: 待实现重排模式下缩放逻辑
                                Log.d(TAG, "待实现重排模式下缩放逻辑")
                            } else {
                                val newScale = (scaleAnim.value * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                                val scaledWidth = contentSize.width * newScale
                                val scaledHeight = contentSize.height * newScale
                                val maxOffsetX =
                                    if (scaledWidth > widthPx) (scaledWidth - widthPx) / 2f else 0f
                                val maxOffsetY =
                                    if (scaledHeight > heightPx) (scaledHeight - heightPx) / 2f else 0f

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
                    .pointerInput(
                        readerPreferences.interactionStyle,
                        readerPreferences.rtlMode,
                        showMask,
                        contentSize
                    ) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                Log.d(
                                    TAG,
                                    "双击触发，scaleAnim.value = ${transform.scaleAnim.value}"
                                )

                                val isZoomed = scaleAnim.value > 1.05f
                                val targetScale = if (isZoomed) 1f else 2.5f
                                scope.launch {
                                    if (!isZoomed) {
                                        val scaledWidth = contentSize.width * targetScale
                                        val scaledHeight = contentSize.height * targetScale
                                        val maxOffsetX =
                                            if (scaledWidth > widthPx) (scaledWidth - widthPx) / 2f else 0f
                                        val maxOffsetY =
                                            if (scaledHeight > heightPx) (scaledHeight - heightPx) / 2f else 0f

                                        val targetOffsetX =
                                            (-(offset.x - widthPx / 2f) * (targetScale - 1f))
                                                .coerceIn(-maxOffsetX, maxOffsetX)
                                        val targetOffsetY =
                                            (-(offset.y - heightPx / 2f) * (targetScale - 1f))
                                                .coerceIn(-maxOffsetY, maxOffsetY)

                                        Log.d(TAG, "targetScale = ${targetScale}")

                                        launch { scaleAnim.animateTo(targetScale, tween(300)) }
                                        launch { offsetXAnim.animateTo(targetOffsetX, tween(300)) }
                                        launch { offsetYAnim.animateTo(targetOffsetY, tween(300)) }
                                    } else {
                                        launch { scaleAnim.animateTo(1f, tween(300)) }
                                        launch { offsetXAnim.animateTo(0f, tween(300)) }
                                        launch { offsetYAnim.animateTo(0f, tween(300)) }
                                    }
                                }
                            },
                            onTap = { offset ->
                                if (showMask) {
                                    showMask = false
                                    viewModel.readerPreferences.clearInteractionStyleHint()
                                } else {
                                    val uiState = viewModel.uiState.value
                                    val step =
                                        if (readerPreferences.readingMode == ReadingMode.Dual) 2 else 1
                                    interactionStyle(
                                        style = readerPreferences.interactionStyle,
                                        isRtl = readerPreferences.rtlMode,
                                        width = widthPx.toInt(),
                                        height = heightPx.toInt(),
                                        offset = offset,
                                        onPreviousClick = {
                                            // 未缩放：上一页
                                            if (scaleAnim.value <= 1.05f) {
                                                viewModel.goToPage(uiState.currentPage - step)
                                            } else {
                                                // 缩放：位移至当页结尾
                                                val scaledWidth =
                                                    contentSize.width * scaleAnim.value
                                                val scaledHeight =
                                                    contentSize.height * scaleAnim.value
                                                val maxOffsetX =
                                                    if (scaledWidth > widthPx) (scaledWidth - widthPx) / 2f else 0f
                                                val maxOffsetY =
                                                    if (scaledHeight > heightPx) (scaledHeight - heightPx) / 2f else 0f
                                                val targetOffsetX =
                                                    if (readerPreferences.rtlMode) -maxOffsetX else maxOffsetX

                                                // 已到结尾：翻页
                                                if (
                                                    abs(offsetXAnim.value - targetOffsetX) < 2f
                                                    && abs(offsetYAnim.value - maxOffsetY) < 2f
                                                ) {
                                                    viewModel.goToPage(uiState.currentPage - step)
                                                }
                                                // 未到结尾：位移至结尾
                                                else {
                                                    scope.launch {
                                                        launch {
                                                            offsetXAnim.animateTo(
                                                                targetOffsetX,
                                                                tween(300)
                                                            )
                                                        }
                                                        launch {
                                                            offsetYAnim.animateTo(
                                                                maxOffsetY,
                                                                tween(300)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onNextClick = {
                                            // 未缩放：下一页
                                            if (scaleAnim.value <= 1.05f) {
                                                viewModel.goToPage(uiState.currentPage + step)
                                            } else {
                                                // 缩放：位移至当页开始
                                                val scaledWidth =
                                                    contentSize.width * scaleAnim.value
                                                val scaledHeight =
                                                    contentSize.height * scaleAnim.value
                                                val maxOffsetX =
                                                    if (scaledWidth > widthPx) (scaledWidth - widthPx) / 2f else 0f
                                                val maxOffsetY =
                                                    if (scaledHeight > heightPx) (scaledHeight - heightPx) / 2f else 0f
                                                val targetOffsetY = -maxOffsetY
                                                val targetOffsetX =
                                                    if (readerPreferences.rtlMode) maxOffsetX else -maxOffsetX

                                                // 已到开始：翻页
                                                if (
                                                    abs(offsetXAnim.value - targetOffsetX) < 2f
                                                    && abs(offsetYAnim.value - targetOffsetY) < 2f
                                                ) {
                                                    viewModel.goToPage(uiState.currentPage + step)
                                                }
                                                // 未到开始：位移至开始
                                                else {
                                                    scope.launch {
                                                        launch {
                                                            offsetXAnim.animateTo(
                                                                targetOffsetX,
                                                                tween(300)
                                                            )
                                                        }
                                                        launch {
                                                            offsetYAnim.animateTo(
                                                                targetOffsetY,
                                                                tween(300)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        },
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
            usePlatformDefaultWidth = true
        )
    ) {
        Surface(
            modifier = modifier.width(dimensionResource(R.dimen.dialog_size)),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
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
                            Text(
                                text = stringResource(R.string.reading_bookmark_dialog_location),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
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