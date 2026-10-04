package io.lin.reader.ui.components.modifier

import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun Modifier.settledMarquee(): Modifier = this.then(
    Modifier.basicMarquee(
        iterations = Int.MAX_VALUE, // 轮播次数，Int.MAX_VALUE 表示无限循环
        repeatDelayMillis = 1200,          // 滚动到达终点后停顿多长时间再开始下一轮（毫秒）
        initialDelayMillis = 2000,   // 首次展示时的初始静止延迟（毫秒）
        velocity = 30.dp             // 滚动速度
    )
)