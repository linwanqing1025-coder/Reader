package io.lin.reader.ui.screens.reading.control

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.lin.reader.data.preferences.CropMode
import io.lin.reader.ui.screens.reading.LocalReadingViewModel
import io.lin.reader.ui.screens.reading.ReadingScreenViewModelForTest
import io.lin.reader.ui.theme.ReaderTheme

@SuppressLint("ContextCastToActivity")
@Composable
fun ReadingControlsOverlay(
    visible: Boolean,
    onNavBack: () -> Unit,
    isScreenRotated: Boolean,
    onRotateScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = LocalReadingViewModel.current ?: return
    val uiState by viewModel.uiState.collectAsState()
    val readerPreferences = viewModel.readerPreferences

    val book = uiState.volume ?: return
    val isEdgeVolume = uiState.isEdgeVolume
    val muPdfCore by viewModel.mCore.collectAsState()
    val pageCount = book.totalPages ?: muPdfCore?.countPages() ?: 0 // TODO: 针对重排文档的适配
    val currentPage = uiState.currentPage

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

    var isShowingSettingMenu by rememberSaveable { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        // 底部栏（内部已包含 PageNumberIndicator 固定与动画处理，以及显示隐入动画）
        BottomControlBar(
            visible = visible,
            currentPageIndex = currentPage,
            pageCount = pageCount,
            onPageChange = { viewModel.goToPage(it) },
            fixedPageIndicator = readerPreferences.fixedPageIndicator,
            isEdgeVolume = isEdgeVolume,
            onNextVolume = { viewModel.switchVolume(1) },
            onPreviousVolume = { viewModel.switchVolume(-1) },
            onRotateIconClick = onRotateScreen,
            isScreenRotated = isScreenRotated,
            rtlMode = readerPreferences.rtlMode,
            onRtlIconClick = viewModel.readerPreferences::toggleRtlMode,
            cropMode = readerPreferences.cropMode,
            onCropIconClick = {
                val nextMode = when (readerPreferences.cropMode) {
                    CropMode.None -> CropMode.Horizontal
                    CropMode.Horizontal -> CropMode.Vertical
                    CropMode.Vertical -> CropMode.All
                    CropMode.All -> CropMode.None
                }
                viewModel.readerPreferences.updateCropMode(nextMode)
            },
            onSettingIconClick = {
                isShowingSettingMenu = !isShowingSettingMenu
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 顶部栏（内部已包含 visible 动画与顶部控制菜单）
        TopControlBar(
            visible = visible,
            onNavBack = onNavBack,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )

        // 底部设置抽屉
        BottomSettingSheet(
            visible = isShowingSettingMenu,
            onDismissRequest = { isShowingSettingMenu = false },
            readerPreferences = readerPreferences,
            modifier = Modifier
                .align(Alignment.BottomCenter)
        )
    }
}

@Preview
@Composable
private fun ReadingControlsOverlayPreview() {
    val fakeViewModel = remember { ReadingScreenViewModelForTest() }
    var showControls by rememberSaveable { mutableStateOf(true) }

    ReaderTheme {
        CompositionLocalProvider(LocalReadingViewModel provides fakeViewModel) {
            MaterialTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = { showControls = !showControls }) {
                        Text(text = "Toggle Visibility")
                    }
                    ReadingControlsOverlay(
                        visible = showControls,
                        isScreenRotated = false,
                        onRotateScreen = {},
                        onNavBack = {}
                    )
                }
            }
        }
    }
}