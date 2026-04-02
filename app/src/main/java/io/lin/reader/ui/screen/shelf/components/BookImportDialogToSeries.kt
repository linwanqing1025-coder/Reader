package io.lin.reader.ui.screen.shelf.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.lin.reader.R
import io.lin.reader.ui.viewmodel.BookDetails

/**系列内书籍导入确认对话框
 * @param booksDetails 要导入的书籍
 * @param onDismiss 取消回调（关闭对话框）
 * @param onSaveConfirm 确定回调（执行导入）*/
@Preview
@Composable
fun BookImportDialogToSeries(
    booksDetails: List<BookDetails> = emptyList(),
    destinationSeriesName: String = "Default Series Name",
    onDismiss: () -> Unit = {},
    onSaveConfirm: () -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true
        )
    ) {
        Surface(
            modifier = Modifier.width(dimensionResource(R.dimen.dialog_size)),
            shape = RoundedCornerShape(
                dimensionResource(R.dimen.composable_rounded_corner_radius) + dimensionResource(
                    R.dimen.inner_padding_of_container
                )
            ),
            color = MaterialTheme.colorScheme.surface
        ) {
            @Suppress("DEPRECATION")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.inner_padding_of_container)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 对话框标题
                Text(
                    text = stringResource(R.string.import_dialog_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // 2. 书籍文件名文本框
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensionResource(R.dimen.inner_padding_of_container)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius)),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    val booksName = booksDetails.map { it.volumeName }
                    Text(
                        text =
                            if (booksName.size <= 3) stringResource(R.string.import_dialog_book)
                                    + " ${booksName.joinToString(", ")}"
                            else stringResource(R.string.import_dialog_book)
                                    + " ${booksName.take(3).joinToString(", ")} "
                                    + stringResource(
                                R.string.import_dialog_book_multiple,
                                booksName.size - 3
                            ),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                // 2. 目标系列名文本框
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius)),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.import_dialog_series) + " $destinationSeriesName",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                // 4. 取消/确定按钮（展开列表时隐藏）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensionResource(R.dimen.inner_padding_of_container)),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 取消按钮
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            dimensionResource(R.dimen.composable_rounded_corner_radius)
                        ),
                        modifier = Modifier
                            .height(40.dp)
                            .height(IntrinsicSize.Min),
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 16.sp,
                        )
                    }
                    // 确定按钮（必须选择系列后才可用）
                    Button(
                        onClick = {
                            onSaveConfirm()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            dimensionResource(R.dimen.composable_rounded_corner_radius)
                        ),
                        modifier = Modifier
                            .height(40.dp)
                            .height(IntrinsicSize.Min)
                    ) {
                        Text(
                            text = stringResource(R.string.confirm),
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}