package io.lin.reader.ui.feature.reading

import androidx.compose.runtime.staticCompositionLocalOf
import io.lin.reader.ui.feature.reading.feature.display.ReadingTransform

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