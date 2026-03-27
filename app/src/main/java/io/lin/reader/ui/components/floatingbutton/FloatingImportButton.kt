package io.lin.reader.ui.components.floatingbutton

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import io.lin.reader.R
import io.lin.reader.ui.components.file.LocalFileSelector

/**书籍导入FAB
 * @param expanded 是否展开
 * @param afterChoose 提供要对返回的URI列表执行的操作
 * */
@Composable
fun FloatingImportButton(
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    enterLoadingState: () -> Unit = {},
    afterChoose: (List<String>) -> Unit = {},
) {
    // 获取全局懒加载文件选择器
    val fileSelector = LocalFileSelector.current
    ExtendedFloatingActionButton(
        onClick = {
            // 进入加载状态
            enterLoadingState()
            // 调用全局文件选择器（传入PDF类型，处理返回的Uri列表）
            fileSelector("application/pdf") { uriList ->
                afterChoose(uriList)
            }
        },
        expanded = expanded,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text(text = stringResource(R.string.import_book)) },
        modifier = modifier,
        shape = RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius))
    )
}
