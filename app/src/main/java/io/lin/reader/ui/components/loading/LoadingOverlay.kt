package io.lin.reader.ui.components.loading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 全屏加载遮罩组件：
 * 1. 半透明黑色背景，覆盖整个屏幕；
 * 2. 中间显示加载指示器；
 * 3. 消费所有点击事件，阻止用户操作其他元素；
 */
@Preview
@Composable
fun LoadingOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            // 半透明黑色背景（可调整alpha值）
            .background(Color.Black.copy(alpha = 0.5f))
            // 消费点击事件，阻止穿透到下层UI
            .pointerInput(Unit) { },
        contentAlignment = Alignment.Center
    ) {
        // 加载指示器（使用Material3默认样式）
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
    }
}