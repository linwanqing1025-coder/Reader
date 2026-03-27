package io.lin.reader.ui.components.menu

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import io.lin.reader.R

/**
 * 一个经过样式设计的DropdownMenu，提供统一的外观和感觉。
 *
 * 要使用它，请在content lambda中提供可组合的内容，通常是[StyledMenuItem]的列表。
 *
 * @param expanded 菜单是否展开。
 * @param onDismissRequest 当用户请求关闭菜单时调用。
 * @param modifier 应用于菜单的Modifier。
 * @param offset 添加到菜单位置的[DpOffset]。默认为(0, 0)。
 * @param properties 用于配置弹出窗口的[PopupProperties]。
 * @param content 菜单的内容，通常是一个或多个[StyledMenuItem]可组合项。
 */
@Composable
fun StyledMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(clippingEnabled = true),
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.padding(horizontal = 8.dp),
        offset = offset,
        properties = properties,
        shape = RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius) + 8.dp),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        content = content
    )
}

/**
 * 一个经过样式设计的Icon，用于[StyledMenuItem]的leadingIcon或trailingIcon。
 *
 * @param imageVector 要显示的图标的[ImageVector]。
 * @param contentDescription 图标内容的文字描述，用于无障碍访问。
 */
@Composable
fun StyledMenuIcon(
    imageVector: ImageVector,
    contentDescription: String?,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(22.dp),
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

/**
 * 一个经过样式设计的DropdownMenuItem，提供统一的外观和感觉。
 * 最好在[StyledMenu]内部使用。此重载接受一个字符串资源ID。
 *
 * @param textRes 要在菜单项中显示的文本的字符串资源ID。
 * @param onClick 当用户点击菜单项时调用。
 * @param enabled 控制菜单项的启用状态。默认为 true。
 * @param leadingIcon （可选）在菜单项开头显示的可组合内容，通常是一个图标。
 * @param trailingIcon （可选）在菜单项末尾显示的可组合内容，通常是一个图标，用于指示子菜单。
 */
@Composable
fun StyledMenuItem(
    @StringRes textRes: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val text = stringResource(textRes)
    StyledMenuItemImpl(
        text = text,
        onClick = onClick,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}

/**
 * 一个经过样式设计的DropdownMenuItem，提供统一的外观和感觉。
 * 最好在[StyledMenu]内部使用。此重载接受一个原始字符串。
 *
 * @param text 要在菜单项中显示的原始字符串。
 * @param onClick 当用户点击菜单项时调用。
 * @param enabled 控制菜单项的启用状态。默认为 true。
 * @param leadingIcon （可选）在菜单项开头显示的可组合内容，通常是一个图标。
 * @param trailingIcon （可选）在菜单项末尾显示的可组合内容，通常是一个图标，用于指示子菜单。
 */
@Composable
fun StyledMenuItem(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    StyledMenuItemImpl(
        text = text,
        onClick = onClick,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}

/**
 * [StyledMenuItem] 的私有实现，不应从外部调用。
 * 它包含了所有共享的样式和布局逻辑。
 */
@Composable
private fun StyledMenuItemImpl(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        },
        onClick = onClick,
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius))),
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}
