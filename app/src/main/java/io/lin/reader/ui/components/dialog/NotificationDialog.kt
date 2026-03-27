package io.lin.reader.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lin.reader.R

/**
 * 提醒对话框
 * @param title 对话框标题
 * @param notification 提醒内容（支持 \n 换行符和 HTML 标签）
 * @param onConfirm 点击「确认」的回调
 * @param onDismiss 点击「取消/外部区域」的回调
 */
@Composable
fun NotificationDialog(
    title: String,
    notification: String, // 接收原始 String 以保留 \n 换行符
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        shape = RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius) + 24.dp),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "  $title",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        },
        text = {
            // 使用 Column 强制进行物理隔离，解决单 Text 组件内 ParagraphStyle 缩进失效问题
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1. 先按换行符切分
                notification.split('\n')
                    .filter { it.isNotBlank() } // 过滤掉由于 \n\n 产生的纯空行，由 spacedBy 处理间距
                    .forEach { line ->
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = ParagraphStyle(textIndent = TextIndent(firstLine = 24.sp))) {
                                    // 2. 对每一段分别解析 HTML，确保 <b> <i> 等标签生效
                                    append(AnnotatedString.fromHtml(line))
                                }
                            },
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                shape = RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius)),
            ) {
                Text(
                    text = stringResource(R.string.confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius)),
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    )
}

@Preview
@Composable
private fun NotificationDialogPreview() {
    // 模拟从 XML 加载的带 HTML 格式且包含 \n\n 的复合字符串
    val notification = stringResource(R.string.series_delete_notification, "测试系列", 5) +
            "\n\n" +
            stringResource(R.string.volume_clear_warning)

    NotificationDialog(
        title = "Preview Title",
        notification = notification, // 直接传 String，不再由外部调用 fromHtml
        onConfirm = {},
        onDismiss = {}
    )
}
