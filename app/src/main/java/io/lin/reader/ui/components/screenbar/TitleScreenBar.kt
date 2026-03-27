package io.lin.reader.ui.components.screenbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 只有标题的顶部应用栏
 * @param title 顶部应用栏标题
 * @param canNavigateBack 是否可以返回（显示返回图标）
 * @param onNavIconClick 是否可以返回（返回图标行为）
 * @param scrollBehavior 滚动隐藏应用栏
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledTitleScreenBar(
    modifier: Modifier = Modifier,
    title: String,
    canNavigateBack: Boolean = false,
    onNavIconClick: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val dividerColor = MaterialTheme.colorScheme.onBackground

    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

        },
        navigationIcon = {
            if (canNavigateBack) {
                StyledBarIconButton(
                    imageVector = Icons.Filled.ArrowBackIosNew,
                    contentDescription = "Navigate Back",
                    onClick = onNavIconClick,
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun StyledTitleScreenBarPreview() {
    StyledTitleScreenBar(
        title = "Default Screen Title",
        canNavigateBack = true
    )
}
