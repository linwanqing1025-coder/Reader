package io.lin.reader.ui.components.cover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.lin.reader.R

/**
 * 单本书的封面组件（根据 MD3 重塑）
 * 具有智能比例显示逻辑
 */
@Preview
@Composable
fun VolumeCover(
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 140.dp,
    coverUri: String? = null,
) {
    // 状态记录图片的实际比例，初始默认为 0
    var imageAspectRatio by remember { mutableStateOf(0f) }

    ElevatedCard(
        modifier = modifier.size(width, height),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!coverUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Book Cover",
                    onSuccess = { state ->
                        // 动态获取图片的实际比例 (Height / Width)，
                        // 注意：这里为了方便判断 [1f, 2f]，取 高/宽 比
                        val intrinsicSize = state.painter.intrinsicSize
                        if (intrinsicSize.width > 0 && intrinsicSize.height > 0) {
                            imageAspectRatio = intrinsicSize.height / intrinsicSize.width
                        }
                    },
                    // 智能显示策略：
                    // 若高/宽比例在 [1.0, 2.0] 内（常见的书本比例），拉伸填满
                    // 否则（如超长或超扁）采用适应显示
                    contentScale = if (imageAspectRatio in 1f..2f) {
                        ContentScale.FillBounds
                    } else {
                        ContentScale.Fit
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
            } else {
                // 当无封面时的 MD3 占位样式
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(width / 3) // 图标大小随封面宽度缩放
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.cover_error),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
