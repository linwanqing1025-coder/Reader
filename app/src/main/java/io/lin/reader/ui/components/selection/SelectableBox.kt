package io.lin.reader.ui.components.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * 模式一：基础切换模式 (Simple Toggle)。
 * 点击即切换，选中时显示边框和圆形 Checkbox，无遮罩。
 */
@Composable
fun SelectableBox(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    activeColor: Color = MaterialTheme.colorScheme.primary,
    checkColor: Color = Color.White,
    content: @Composable BoxScope.() -> Unit
) {
    SelectableBoxImpl(
        selected = selected,
        onSelectedChange = onSelectedChange,
        isSelectionMode = null,
        modifier = modifier,
        shape = shape,
        activeColor = activeColor,
        checkColor = checkColor,
        content = content
    )
}

/**
 * 模式二：选择模式感知 (Selection Mode Aware)。
 */
@Composable
fun SelectableBox(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    isSelectionMode: Boolean,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(8.dp),
    activeColor: Color = MaterialTheme.colorScheme.primary,
    checkColor: Color = Color.White,
    content: @Composable BoxScope.() -> Unit
) {
    SelectableBoxImpl(
        selected = selected,
        onSelectedChange = onSelectedChange,
        isSelectionMode = isSelectionMode,
        onLongClick = onLongClick,
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        activeColor = activeColor,
        checkColor = checkColor,
        content = content
    )
}

/**
 * 统一的实现逻辑。
 */
@Composable
private fun SelectableBoxImpl(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    isSelectionMode: Boolean?,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(8.dp),
    activeColor: Color = MaterialTheme.colorScheme.primary,
    checkColor: Color = Color.White,
    content: @Composable BoxScope.() -> Unit
) {
    val isModeTwo = isSelectionMode != null
    val selectionActive = isSelectionMode == true

    // UI 逻辑定义
    val showUI = if (isModeTwo) selectionActive else selected
    val showBorder = selected && (if (isModeTwo) selectionActive else true)
    val showOverlay = isModeTwo && selectionActive && !selected
    val showCheckboxBorder = isModeTwo && selectionActive && !selected

    val borderWidth by animateDpAsState(
        targetValue = if (showBorder) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "BorderWidth"
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = if (showOverlay) 0.4f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "OverlayAlpha"
    )

    val uiAlpha by animateFloatAsState(
        targetValue = if (showUI) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "UIAlpha"
    )

    val bounceScale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "BounceScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            }
            .clip(shape)
            .combinedClickable(
                onClick = {
                    if (isModeTwo) {
                        if (selectionActive) onSelectedChange(!selected)
                        else onClick?.invoke()
                    } else {
                        onSelectedChange(!selected)
                    }
                },
                onLongClick = onLongClick
            )
            .then(
                if (borderWidth > 0.dp) Modifier.border(borderWidth, activeColor, shape)
                else Modifier
            )
    ) {
        content()

        if (overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = overlayAlpha }
                    .background(Color.Black)
            )
        }

        if (uiAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .graphicsLayer { alpha = uiAlpha }
                    .zIndex(2f)
            ) {
                CircularCheckbox(
                    checked = selected,
                    showUncheckedBorder = showCheckboxBorder,
                    activeColor = activeColor,
                    checkColor = checkColor,
                    onCheckedChange = { onSelectedChange(it) }
                )
            }
        }
    }
}

/**
 * 内部使用的圆形 Checkbox 组件。
 */
@Composable
private fun CircularCheckbox(
    checked: Boolean,
    showUncheckedBorder: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    checkColor: Color = Color.White,
    onCheckedChange: (Boolean) -> Unit
) {
    val checkboxColor by animateColorAsState(
        targetValue = if (checked) activeColor else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "CheckboxColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            checked -> activeColor
            showUncheckedBorder -> Color.White.copy(alpha = 0.8f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "BorderColor"
    )

    val checkAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "CheckAlpha"
    )

    val checkScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "CheckScale"
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = checkScale
                scaleY = checkScale
            }
            .clip(CircleShape)
            .background(checkboxColor)
            .border(2.dp, borderColor, CircleShape)
            .combinedClickable(
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checkAlpha > 0f) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = checkColor,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { alpha = checkAlpha }
            )
        }
    }
}

@Preview(showBackground = true, name = "Interactive Preview", showSystemUi = true)
@Composable
private fun InteractiveSelectableBoxPreview() {
    MaterialTheme {
        var isSelectionModeEnabled by remember { mutableStateOf(true) }
        var isSelected1 by remember { mutableStateOf(false) }
        var isSelected2 by remember { mutableStateOf(true) }

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("交互测试面板 (圆形 Checkbox)", style = MaterialTheme.typography.headlineSmall)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
                ) {
                    Text("开启选择模式: ")
                    Switch(
                        checked = isSelectionModeEnabled,
                        onCheckedChange = { isSelectionModeEnabled = it }
                    )
                }

                Text("模式二 (Mode Aware):", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        SelectableBox(
                            selected = isSelected1,
                            onSelectedChange = { isSelected1 = it },
                            isSelectionMode = isSelectionModeEnabled,
                            modifier = Modifier.size(100.dp, 140.dp)
                        ) {
                            Box(Modifier.fillMaxSize().background(Color.LightGray), contentAlignment = Alignment.Center) {
                                Text("Item 1")
                            }
                        }
                        Text("未选中显边框")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        SelectableBox(
                            selected = isSelected2,
                            onSelectedChange = { isSelected2 = it },
                            isSelectionMode = isSelectionModeEnabled,
                            modifier = Modifier.size(100.dp, 140.dp)
                        ) {
                            Box(Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                                Text("Item 2", color = Color.White)
                            }
                        }
                        Text("选中遮罩消失")
                    }
                }

                Spacer(modifier = Modifier.size(32.dp))

                Text("模式一 (Simple Toggle):", style = MaterialTheme.typography.titleMedium)
                var isSelected3 by remember { mutableStateOf(false) }
                SelectableBox(
                    selected = isSelected3,
                    onSelectedChange = { isSelected3 = it },
                    modifier = Modifier.size(100.dp, 140.dp).padding(top = 8.dp)
                ) {
                    Box(Modifier.fillMaxSize().background(Color.Cyan.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Text("Simple")
                    }
                }
                Text("仅在选中时显示")
            }
        }
    }
}
