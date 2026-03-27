package io.lin.reader.ui.components.screenbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.lin.reader.R

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun StyledScreenBarPreview() {
    Column {
        StyledScreenBar(
            title = "Default Screen Title",
            iconButtonTools = {
                StyledBarIconButton(
                    imageVector = Icons.Filled.ArrowBackIosNew,
                    contentDescription = "Navigate Back",
                    onClick = {},
                )
                StyledBarIconButton(
                    imageVector = Icons.Filled.ArrowBackIosNew,
                    contentDescription = "Navigate Back",
                    onClick = {},
                )
                StyledBarIconButton(
                    imageVector = Icons.Filled.ArrowBackIosNew,
                    contentDescription = "Navigate Back",
                    onClick = {},
                )
            },
            canNavigateBack = false,
            scrollBehavior = null
        )
        StyledScreenBar(
            title = "Default Screen Title",
            iconButtonTools = {
                StyledBarIconButton(
                    imageVector = Icons.Filled.ArrowBackIosNew,
                    contentDescription = "Navigate Back",
                    onClick = {},
                )
                StyledBarIconButton(
                    imageVector = Icons.Filled.ArrowBackIosNew,
                    contentDescription = "Navigate Back",
                    onClick = {},
                )
                StyledBarIconButton(
                    imageVector = Icons.Filled.ArrowBackIosNew,
                    contentDescription = "Navigate Back",
                    onClick = {},
                )
            },
            canNavigateBack = true,
            scrollBehavior = null
        )
    }
}

/**
 * App顶部应用栏
 * @param title 顶部应用栏标题
 * @param iconButtonTools 顶部应用栏右侧图标按钮（内部已经提供了 RowScope，需传入 StyledBarIconButton ）
 * @param canNavigateBack 是否可以返回（显示返回图标）
 * @param onNavIconClick 是否可以返回（返回图标行为）
 * @param scrollBehavior 滚动隐藏应用栏
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledScreenBar(
    modifier: Modifier = Modifier,
    title: String,
    iconButtonTools: @Composable (() -> Unit) = {},
    canNavigateBack: Boolean = false,
    onNavIconClick: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val dividerColor = MaterialTheme.colorScheme.onBackground

    CenterAlignedTopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .weight(1f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1, // 强制单行（省略号必要条件）
                    overflow = TextOverflow.Ellipsis, // 超出显示省略号（必要条件）
                    softWrap = false, // 禁止换行（必要条件）
                )

                Row(
                    modifier = Modifier.padding(
                        start = dimensionResource(R.dimen.inner_padding_of_container),
                        end = 8.dp
                    ),
                    horizontalArrangement = Arrangement.Center
                ) {
                    iconButtonTools()
                }
            }
        },
        navigationIcon = {
            if (canNavigateBack) {
                StyledBarIconButton(
                    imageVector = Icons.Filled.ArrowBackIosNew,
                    contentDescription = "Navigate Back",
                    onClick = onNavIconClick,
                )

            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
        },
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        scrollBehavior = scrollBehavior,
        modifier = modifier.drawWithContent {
            // Draw the actual TopAppBar content first.
            drawContent()
            // Then draw the line on top of it.
            val strokeWidth = 1.dp.toPx() // Standard divider thickness. You can change this value.
            val y = size.height - strokeWidth / 2
            drawLine(
                color = dividerColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth
            )
        }
    )
}

@Composable
fun StyledBarIconButton(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = LocalContentColor.current
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        content = {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.size(32.dp),
                tint = tint
            )
        }
    )
}
