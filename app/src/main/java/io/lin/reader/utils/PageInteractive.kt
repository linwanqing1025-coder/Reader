package io.lin.reader.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.lin.reader.R

enum class InteractionStyle {
    Style0, Style1, Style2, Style3
}

enum class InteractionAction {
    PREVIOUS, NEXT, CENTER
}

/**
 * 外界调用页面点击交互方式的接口
 * */
fun interactionStyle(
    style: InteractionStyle = InteractionStyle.Style0,
    isRtl: Boolean = false,
    width: Int,
    height: Int,
    offset: Offset,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onCenterClick: () -> Unit
) {
    val col = (offset.x / (width / 3f)).toInt().coerceIn(0, 2)
    val row = (offset.y / (height / 3f)).toInt().coerceIn(0, 2)

    when (getInteractionAction(style, isRtl, row, col)) {
        InteractionAction.PREVIOUS -> onPreviousClick()
        InteractionAction.NEXT -> onNextClick()
        InteractionAction.CENTER -> onCenterClick()
    }
}

/**
 * 获取特定位置的交互动作
 */
private fun getInteractionAction(
    style: InteractionStyle,
    isRtl: Boolean,
    row: Int,
    col: Int
): InteractionAction {
    return when (style) {
        InteractionStyle.Style0 -> {
            when (row) {
                0 -> InteractionAction.PREVIOUS
                1 -> InteractionAction.CENTER
                else -> InteractionAction.NEXT
            }
        }

        InteractionStyle.Style1 -> {
            if (!isRtl) {
                when (col) {
                    0 -> InteractionAction.PREVIOUS
                    1 -> InteractionAction.CENTER
                    else -> InteractionAction.NEXT
                }
            } else {
                when (col) {
                    0 -> InteractionAction.NEXT
                    1 -> InteractionAction.CENTER
                    else -> InteractionAction.PREVIOUS
                }
            }
        }

        InteractionStyle.Style2 -> {
            if (!isRtl) {
                when (Pair(row, col)) {
                    Pair(1, 1) -> InteractionAction.CENTER
                    Pair(1, 2), Pair(0, 2), Pair(2, 1), Pair(2, 2) -> InteractionAction.NEXT
                    else -> InteractionAction.PREVIOUS
                }
            } else {
                when (Pair(row, col)) {
                    Pair(1, 1) -> InteractionAction.CENTER
                    Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(2, 1) -> InteractionAction.NEXT
                    else -> InteractionAction.PREVIOUS
                }
            }
        }

        InteractionStyle.Style3 -> {
            if (!isRtl) {
                when (Pair(row, col)) {
                    Pair(1, 1) -> InteractionAction.CENTER
                    Pair(1, 2), Pair(2, 0), Pair(2, 1), Pair(2, 2) -> InteractionAction.NEXT
                    else -> InteractionAction.PREVIOUS
                }
            } else {
                when (Pair(row, col)) {
                    Pair(1, 1) -> InteractionAction.CENTER
                    Pair(1, 0), Pair(2, 0), Pair(2, 1), Pair(2, 2) -> InteractionAction.NEXT
                    else -> InteractionAction.PREVIOUS
                }
            }
        }
    }
}

@Composable
private fun getInteractionColor(action: InteractionAction): Color {
    return when (action) {
        InteractionAction.PREVIOUS -> MaterialTheme.colorScheme.surfaceVariant
        InteractionAction.NEXT -> MaterialTheme.colorScheme.primaryContainer
        InteractionAction.CENTER -> MaterialTheme.colorScheme.tertiaryContainer
    }
}

@Composable
private fun getInteractionOnColor(action: InteractionAction): Color {
    return when (action) {
        InteractionAction.PREVIOUS -> MaterialTheme.colorScheme.onSurfaceVariant
        InteractionAction.NEXT -> MaterialTheme.colorScheme.onPrimaryContainer
        InteractionAction.CENTER -> MaterialTheme.colorScheme.onTertiaryContainer
    }
}

@Composable
private fun getActionText(action: InteractionAction): String {
    return when (action) {
        InteractionAction.PREVIOUS -> stringResource(R.string.interaction_hint_previous)
        InteractionAction.NEXT -> stringResource(R.string.interaction_hint_next)
        InteractionAction.CENTER -> stringResource(R.string.interaction_hint_menu)
    }
}

// 供 GestureInteractionLayer 使用
@Composable
fun InteractionMask(
    modifier: Modifier = Modifier,
    style: InteractionStyle,
    isRtl: Boolean
) {
    Column(modifier = modifier.fillMaxSize()) {
        (0..2).forEach { rowIndex ->
            Row(Modifier.weight(1f)) {
                (0..2).forEach { colIndex ->
                    val action = getInteractionAction(style, isRtl, rowIndex, colIndex)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(getInteractionColor(action).copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // 提示文字逻辑：中心固定显示，四周根据 RTL 调整
                        val showText = when (Pair(rowIndex, colIndex)) {
                            Pair(1, 1) -> true
                            Pair(0, 0) -> !isRtl
                            Pair(2, 2) -> !isRtl
                            Pair(0, 2) -> isRtl
                            Pair(2, 0) -> isRtl
                            else -> false
                        }
                        if (showText) {
                            Text(
                                text = getActionText(action),
                                color = getInteractionOnColor(action),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InteractionMaskPreview() {
    MaterialTheme {
        InteractionMask(
            modifier = Modifier.fillMaxSize(),
            style = InteractionStyle.Style3,
            isRtl = false
        )
    }
}

@Composable
private fun BaseInteractionPreview(width: Dp, style: InteractionStyle, isRtl: Boolean) {
    Column(modifier = Modifier.width(width).aspectRatio(2 / 3f)) {
        (0..2).forEach { rowIndex ->
            Row(Modifier.weight(1f)) {
                (0..2).forEach { colIndex ->
                    val action = getInteractionAction(style, isRtl, rowIndex, colIndex)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(getInteractionColor(action).copy(alpha = 0.8f))
                    )
                }
            }
        }
    }
}

// 供 InteractionDetails.kt 使用
@Composable fun InteractionStyle0Preview(width: Dp) = BaseInteractionPreview(width, InteractionStyle.Style0, false)
@Composable fun InteractionStyle1Preview(width: Dp) = BaseInteractionPreview(width, InteractionStyle.Style1, false)
@Composable fun InteractionStyle1RtlPreview(width: Dp) = BaseInteractionPreview(width, InteractionStyle.Style1, true)
@Composable fun InteractionStyle2Preview(width: Dp) = BaseInteractionPreview(width, InteractionStyle.Style2, false)
@Composable fun InteractionStyle2RtlPreview(width: Dp) = BaseInteractionPreview(width, InteractionStyle.Style2, true)
@Composable fun InteractionStyle3Preview(width: Dp) = BaseInteractionPreview(width, InteractionStyle.Style3, false)
@Composable fun InteractionStyle3RtlPreview(width: Dp) = BaseInteractionPreview(width, InteractionStyle.Style3, true)

@Preview(showBackground = true)
@Composable
private fun AllInteractionStylesPreview() {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        InteractionStyle.entries.forEach { style ->
            item {
                Text(style.name, modifier = Modifier.padding(vertical = 8.dp))
                Row {
                    BaseInteractionPreview(120.dp, style, false)
                    Box(Modifier.width(16.dp))
                    BaseInteractionPreview(120.dp, style, true)
                }
            }
        }
    }
}
