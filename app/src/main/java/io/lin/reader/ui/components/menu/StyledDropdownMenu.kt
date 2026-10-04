package io.lin.reader.ui.components.menu

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

/**
 * 参考 AndroidMenu.android.kt 实现全部 DropdownMenu 接口，
 * 将默认水平 Padding 设为 8.dp。现内部自带 all = 8.dp 的 PaddingValues。
 * 默认 16.dp 圆角以迎合前述自带间距
 */
@Composable
fun StyledDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = MenuDefaults.DefaultMenuProperties,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = MenuDefaults.containerColor,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.padding(horizontal = 8.dp),
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        shape = shape,
        containerColor = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
        content = content,
    )
}

@Deprecated(
    level = DeprecationLevel.HIDDEN,
    replaceWith =
        ReplaceWith(
            expression =
                "TestDropdownMenu(\n" +
                        "    expanded = expanded,\n" +
                        "    onDismissRequest = onDismissRequest,\n" +
                        "    modifier = modifier,\n" +
                        "    offset = offset,\n" +
                        "    scrollState = scrollState,\n" +
                        "    properties = properties,\n" +
                        "    shape = MenuDefaults.shape,\n" +
                        "    containerColor = MenuDefaults.containerColor,\n" +
                        "    tonalElevation = MenuDefaults.TonalElevation,\n" +
                        "    shadowElevation = MenuDefaults.ShadowElevation,\n" +
                        "    border = null,\n" +
                        "    content = content\n" +
                        ")"
        ),
    message =
        "Maintained for binary compatibility. Use overload with parameters for shape, " +
                "color, elevation, and border.",
)
@Composable
@SuppressLint("ComposableNaming")
fun StyledDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
): Unit =
    StyledDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        shape = MenuDefaults.shape,
        containerColor = MenuDefaults.containerColor,
        tonalElevation = MenuDefaults.TonalElevation,
        shadowElevation = MenuDefaults.ShadowElevation,
        border = null,
        content = content,
    )

@Deprecated(
    level = DeprecationLevel.HIDDEN,
    replaceWith =
        ReplaceWith(
            expression =
                "TestDropdownMenu(expanded, onDismissRequest, modifier, offset, " +
                        "rememberScrollState(), properties, content)",
            "androidx.compose.foundation.rememberScrollState",
        ),
    message = "Replaced by a TestDropdownMenu function with a ScrollState parameter",
)
@Composable
@SuppressLint("ComposableNaming")
fun StyledDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
): Unit =
    StyledDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = rememberScrollState(),
        properties = properties,
        content = content,
    )

/**
 * 一个经过样式设计的 DropdownMenuItem，默认 8.dp 圆角
 * 最好在[StyledDropdownMenu]内部使用。
 */
@Composable
fun StyledDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.clip(RoundedCornerShape(8.dp)),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    DropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )
}

@Preview(showBackground = true)
@Composable
private fun StyledDropdownMenuPreview() {
    var expanded by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .wrapContentSize(Alignment.Center)
    ) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
        }
        StyledDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            StyledDropdownMenuItem(
                text = { Text("Edit") },
                onClick = { expanded = false },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
            )
            StyledDropdownMenuItem(
                text = { Text("Settings") },
                onClick = { expanded = false },
                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) }
            )
            HorizontalDivider()
            StyledDropdownMenuItem(
                text = { Text("Send Feedback") },
                onClick = { expanded = false },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) }
            )
        }
    }
}
