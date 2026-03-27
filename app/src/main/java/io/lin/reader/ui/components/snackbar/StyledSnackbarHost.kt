package io.lin.reader.ui.components.snackbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 基于 Material 3 设计的通用 StyledSnackbarHost。
 * 采用了品牌主色调背景，适用于需要高强调反馈的场景。
 *
 * @param hostState 控制 Snackbar 显示状态的 SnackbarHostState
 * @param modifier 布局修饰符
 */
@Composable
fun StyledSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        Snackbar(
            modifier = Modifier.padding(12.dp),
            snackbarData = data,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionColor = MaterialTheme.colorScheme.primary,
            actionContentColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StyledSnackbarHostPreview() {
    val hostState = remember { SnackbarHostState() }

    // 在预览中模拟显示一条消息
    LaunchedEffect(Unit) {
        hostState.showSnackbar(
            message = "这是一条品牌色样式的提示信息",
            actionLabel = "确定"
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StyledSnackbarHost(
            hostState = hostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}