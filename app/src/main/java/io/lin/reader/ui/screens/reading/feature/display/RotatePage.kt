package io.lin.reader.ui.maintab.reading.feature.display

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints


/**
 * 自定义旋转 Modifier：同时重构测量约束与绘制旋转，只能传递 90 的倍数，否则不知道发生什么事。
 * 注意：
 * 1. 因为此修饰符会重构约束，因此必须放在第一位使用。先应用 rotateWithLayout，再使用 graphicsLayer 缩放指定 aspectRatio。
 * 2. 不需要且不推荐再使用 fillMaxWidth() 修饰符，指定 aspectRatio 后会自动实现自适应对齐
 */
fun Modifier.rotateWithLayout(degrees: Int): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val normalizedDegrees = (degrees % 360 + 360) % 360
        val reverseRatio = normalizedDegrees == 90 || normalizedDegrees == 270

        // 1. 如果旋转 90/270 度，将父容器的宽高约束互换后传递给子组件测量
        val targetConstraints = if (reverseRatio) {
            Constraints(
                minWidth = constraints.minHeight,
                maxWidth = constraints.maxHeight,
                minHeight = constraints.minWidth,
                maxHeight = constraints.maxWidth
            )
        } else {
            constraints
        }

        val placeable = measurable.measure(targetConstraints)

        // 2. 重新计算并向外部父布局汇报的实际占用宽高
        val layoutWidth = if (reverseRatio) placeable.height else placeable.width
        val layoutHeight = if (reverseRatio) placeable.width else placeable.height

        // 3. 在新布局中心放置并旋转内容
        layout(layoutWidth, layoutHeight) {
            placeable.placeWithLayer(
                x = (layoutWidth - placeable.width) / 2,
                y = (layoutHeight - placeable.height) / 2,
                layerBlock = {
                    rotationZ = degrees.toFloat()
                }
            )
        }
    }
)