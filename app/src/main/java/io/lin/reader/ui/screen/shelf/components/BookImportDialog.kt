package io.lin.reader.ui.screen.shelf.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.paging.compose.LazyPagingItems
import io.lin.reader.R
import io.lin.reader.ui.viewmodel.BookDetails
import io.lin.reader.ui.viewmodel.ShelfItem

/**书籍导入确认对话框
 * @param booksDetails 要导入的书籍列表
 * @param seriesList 用于提供选择系列列表 (Paging 数据)
 * @param onCreateNewSeriesClick 提用于更新 isCreatingNewSeries
 * @param onExistingSeriesListClick 提供已存在系列id和系列名
 * @param onDismiss 取消回调（关闭对话框）
 * @param onSaveConfirm 确定回调（执行导入）*/
@Composable
fun BookImportDialog(
    booksDetails: List<BookDetails> = emptyList(),
    seriesList: LazyPagingItems<ShelfItem>,
    seriesNameToShow: String,
    onCreateNewSeriesClick: () -> Unit,
    onExistingSeriesListClick: (Long, String) -> Unit,
    onDismiss: () -> Unit,
    onSaveConfirm: (String) -> Unit,
) {
    //内部状态：控制系列列表展开/收起、当前选择的系列名、是否新建系列
    var isSeriesExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
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
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.inner_padding_of_container))
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
                // 3. 系列选择区域
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 3.1 系列选择触发栏
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = dimensionResource(R.dimen.composable_rounded_corner_radius),
                            topEnd = dimensionResource(R.dimen.composable_rounded_corner_radius),
                            // 展开时去掉底部圆角，与列表衔接
                            bottomStart = if (isSeriesExpanded) 0.dp else dimensionResource(R.dimen.composable_rounded_corner_radius),
                            bottomEnd = if (isSeriesExpanded) 0.dp else dimensionResource(R.dimen.composable_rounded_corner_radius)
                        ),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = { isSeriesExpanded = !isSeriesExpanded }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.import_dialog_series) + " ${seriesNameToShow.ifEmpty { "" }}",
                                modifier = Modifier.weight(1f), // 关键：占据所有剩余宽度
                                maxLines = Int.MAX_VALUE, // 关键：允许无限换行
                                overflow = TextOverflow.Visible, // 配合 maxLines，超出时换行而非省略
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            // 三角图标动画：旋转
                            val arrowRotation by animateFloatAsState(targetValue = if (isSeriesExpanded) 180f else 0f)
                            // 三角图标（展开/收起切换）
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription =
                                    if (isSeriesExpanded) stringResource(R.string.import_dialog_expand_series)
                                    else stringResource(R.string.import_dialog_collapse_series),
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(arrowRotation),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    if (isSeriesExpanded) {
                        // 3.2 展开后的系列列表
                        val dividerColor =
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawLine(
                                        color = dividerColor,
                                        start = Offset(x = 0f, y = 0f),
                                        end = Offset(x = size.width, y = 0f),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                },
                            shape = RoundedCornerShape(
                                bottomStart = 20.dp,
                                bottomEnd = 20.dp
                            ),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .heightIn(max = 250.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = stringResource(R.string.import_dialog_create_series),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.clickable {
                                        isSeriesExpanded = false
                                        onCreateNewSeriesClick()
                                    }
                                )
                                if (seriesList.itemCount > 0) {
                                    Text(
                                        text = stringResource(R.string.import_dialog_existing_series),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                    for (i in 0 until seriesList.itemCount) {
                                        val shelfItem = seriesList[i]
                                        if (shelfItem != null) {
                                            val series = shelfItem.series
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        onExistingSeriesListClick(
                                                            series.id,
                                                            series.seriesName
                                                        )
                                                        isSeriesExpanded = false
                                                    },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val isSeriesSelected =
                                                    (seriesNameToShow == series.seriesName)
                                                Icon(
                                                    imageVector = if (isSeriesSelected) Icons.Filled.Circle else Icons.Outlined.Circle,
                                                    contentDescription = stringResource(R.string.import_dialog_existing_series_selected),
                                                    tint = if (isSeriesSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .padding(start = 12.dp, end = 4.dp)
                                                )
                                                Text(
                                                    text = series.seriesName,
                                                    fontSize = 16.sp,
                                                    color = if (isSeriesSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 4. 取消/确定按钮
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = dimensionResource(R.dimen.inner_padding_of_container)),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius)),
                                modifier = Modifier
                                    .height(40.dp)
                                    .height(IntrinsicSize.Min),
                            ) {
                                Text(text = stringResource(R.string.cancel), fontSize = 16.sp)
                            }
                            Button(
                                onClick = { onSaveConfirm(seriesNameToShow) },
                                shape = RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius)),
                                modifier = Modifier
                                    .height(40.dp)
                                    .height(IntrinsicSize.Min),
                                enabled = seriesNameToShow.isNotBlank()
                            ) {
                                Text(text = stringResource(R.string.confirm), fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
