package io.lin.reader.ui.components.screenbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp

/**
 * A TopAppBar that animates its content based on a scroll fraction.
 *
 * @param title The title text.
 * @param scrollFraction A value from 0.0 (fully expanded) to 1.0 (collapsed).
 * @param onNavigationClick Callback for when the navigation icon is clicked.
 * @param navigationIcon The icon to display for navigation (defaults to ArrowBack).
 * @param actions The actions to display on the right side of the app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopAppBar(
    title: String,
    scrollFraction: Float,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                // Navigation Icon (Fades out as scrollFraction increases)
                IconButton(
                    onClick = onNavigationClick,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent),
                    modifier = Modifier.alpha(1f - scrollFraction)
                ) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = "Navigate Back"
                    )
                }

                // Title (Moves from Center to Start)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .align(
                            BiasAlignment(
                                horizontalBias = lerp(0f, -1f, scrollFraction),
                                verticalBias = 0f
                            )
                        )
                        .padding(start = lerp(0.dp, 16.dp, scrollFraction))
                )

                // Actions (Right aligned)
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun DynamicTopAppBarPreview() {
    val listState = rememberLazyListState()
    val scrollFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 300f).coerceIn(0f, 1f)
        }
    }

    Scaffold(
        topBar = {
            DynamicTopAppBar(
                title = "Dynamic Top Bar",
                scrollFraction = scrollFraction,
                onNavigationClick = {}
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(50) { index ->
                ListItem(
                    headlineContent = { Text("Item $index") },
                    supportingContent = { Text("Scroll to see the top bar animation") }
                )
            }
        }
    }
}
