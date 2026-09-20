package io.lin.reader.ui.maintab.reading.feature.display

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size

const val MAX_SCALE = 5f
const val MIN_SCALE = 1f

data class OffsetAnim(
    var x: Animatable<Float, AnimationVector1D> = Animatable(0f),
    var y: Animatable<Float, AnimationVector1D> = Animatable(0f)
)

data class ReadingTransform(
    var scaleAnim: Animatable<Float, AnimationVector1D> = Animatable(1f),
    var offsetAnim: OffsetAnim = OffsetAnim()
) {
    var contentSize by mutableStateOf(Size.Zero)
}