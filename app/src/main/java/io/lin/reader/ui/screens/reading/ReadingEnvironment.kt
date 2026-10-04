package io.lin.reader.ui.screens.reading

import androidx.compose.runtime.staticCompositionLocalOf
import io.lin.reader.ui.screens.reading.feature.display.ReadingTransform
import io.lin.reader.ui.screens.reading.ReadingScreenViewmodelInterface

/**
 * 阅读器 ViewModel
 */
val LocalReadingViewModel = staticCompositionLocalOf<ReadingScreenViewmodelInterface?> {
    null
}

/**
 * 页面位置调整：缩放、位移
 */
val LocalReadingTransform = staticCompositionLocalOf {
    ReadingTransform()
}