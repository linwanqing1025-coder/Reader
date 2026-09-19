package io.lin.reader.ui.components.cover

import android.content.Context
import android.graphics.BitmapFactory
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.lin.reader.R
import io.lin.reader.data.database.Volume
import io.lin.reader.ui.LocalAppViewModel
import io.lin.reader.utils.MuPDFUtils

/**
 * 单本书的封面组件（根据 MD3 重塑）
 * 具有智能比例显示逻辑和自发渲染功能
 */
@Composable
fun VolumeCover(
    volume: Volume,
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 140.dp
) {
    val context = LocalContext.current
    val appViewModel = LocalAppViewModel.current // 获取全局 ViewModel 以操作数据库

    // 使用 volume.coverUri 作为初始状态，并允许内部更新
    var currentCoverUri by remember(volume.coverUri) { mutableStateOf(volume.coverUri) }

    // 自发渲染：如果 coverUri 为空，则异步生成并持久化
    if (currentCoverUri.isNullOrBlank()) {
        LaunchedEffect(volume.id) {
            val generatedUri = MuPDFUtils.getCover(context, volume.bookFileUri.toUri())
            if (generatedUri != null) {
                currentCoverUri = generatedUri
                // 通过全局 AppViewModel 更新数据库
                appViewModel.saveCoverMapping(volume.bookFileUri, generatedUri)
            }
        }
    }

    // 核心解决方案：在图片渲染前，通过读取文件 Header 预先同步解析宽高比（耗时 < 1ms，不占用内存）
    val imageAspectRatio = remember(currentCoverUri) {
        calculateImageAspectRatio(context, currentCoverUri)
    }

    // 在 AsyncImage 首帧渲染前直接确定 ContentScale，避免首次渲染时误用默认的 Fit
    val contentScale = remember(imageAspectRatio) {
        if (imageAspectRatio != null && imageAspectRatio in 1f..2f) {
            ContentScale.FillBounds
        } else {
            ContentScale.Fit
        }
    }

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
            if (!currentCoverUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(currentCoverUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Book Cover",
                    onError = {
                        val brokenUri = currentCoverUri
                        if (!brokenUri.isNullOrBlank()) {
                            currentCoverUri = null
                            appViewModel.handleCoverError(volume.bookFileUri, brokenUri)
                        }
                    },
                    contentScale = contentScale,
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

/**
 * 快速解析图片的宽高比
 * 利用 inJustDecodeBounds = true 仅读取文件头元数据，不加载全图 Bitmap，几乎瞬间完成
 */
private fun calculateImageAspectRatio(context: Context, uriString: String?): Float? {
    if (uriString.isNullOrBlank()) return null
    return try {
        val uri = uriString.toUri()
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }
        if (options.outWidth > 0 && options.outHeight > 0) {
            options.outHeight.toFloat() / options.outWidth.toFloat()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}