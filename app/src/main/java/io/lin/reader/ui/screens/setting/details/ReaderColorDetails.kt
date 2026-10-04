package io.lin.reader.ui.screens.setting.details

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.lin.reader.R
import io.lin.reader.data.preferences.ReaderColor
import io.lin.reader.ui.components.colorpicker.ReaderColorPicker
import io.lin.reader.ui.components.screenbar.DynamicTopAppBar
import io.lin.reader.ui.screens.setting.SettingList
import io.lin.reader.ui.screens.setting.SettingListItem
import io.lin.reader.ui.theme.ReaderTheme

@Composable
fun ReaderColorDetailsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    readerColor: ReaderColor,
    onBackgroundColorChange: (Int) -> Unit,
    onPageColorChange: (Int) -> Unit,
    onFilterColorChange: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    val scrollFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 150f).coerceIn(0f, 1f)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DynamicTopAppBar(
                title = stringResource(R.string.setting_reader_color),
                scrollFraction = scrollFraction,
                onNavigationClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        ReaderColorDetails(
            modifier = Modifier.padding(innerPadding),
            listState = listState,
            readerColor = readerColor,
            onBackgroundColorChange = onBackgroundColorChange,
            onPageColorChange = onPageColorChange,
            onFilterColorChange = onFilterColorChange
        )
    }
}

@Composable
fun ReaderColorDetails(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    readerColor: ReaderColor,
    onBackgroundColorChange: (Int) -> Unit,
    onPageColorChange: (Int) -> Unit,
    onFilterColorChange: (Int) -> Unit
) {
    var isShowingColorSelector by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }

    BackHandler(enabled = isShowingColorSelector) {
        isShowingColorSelector = false
    }
    val background = stringResource(R.string.reader_color_background)
    val page = stringResource(R.string.reader_color_page)
    val filter = stringResource(R.string.reader_color_filter)

    // Internal state to track current selection for the ColorSettingSection
    var currentColor by remember(readerColor, isShowingColorSelector, title) {
        mutableStateOf(
            when (title) {
                background -> Color(readerColor.background)
                page -> Color(readerColor.page)
                filter -> Color(readerColor.filter)
                else -> Color(readerColor.background)
            }
        )
    }
    var onColorChangeLambda by remember { mutableStateOf<(Int) -> Unit>({}) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EffectPreview(
                readerColor = readerColor
            )
        }
        item {
            AnimatedContent(
                targetState = isShowingColorSelector,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(
                            animationSpec = tween(300)
                        ))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(
                                animationSpec = tween(300)
                            ))
                    } else {
                        (slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(
                            animationSpec = tween(300)
                        ))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(
                                animationSpec = tween(300)
                            ))
                    }
                },
                label = stringResource(R.string.color_picker)
            ) { showingSelector ->
                if (!showingSelector) {
                    SettingList(
                        title = { Text(stringResource(R.string.reader_color_label_color)) }
                    ) {
                        SettingListItem(
                            headlineContent = { Text(background) },
                            onClick = {
                                title = background
                                currentColor = Color(readerColor.background)
                                onColorChangeLambda = onBackgroundColorChange
                                isShowingColorSelector = true
                            },
                        )
                        {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(readerColor.background))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            CircleShape
                                        )
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }

                        SettingListItem(
                            headlineContent = { Text(page) },
                            onClick = {
                                title = page
                                currentColor = Color(readerColor.page)
                                onColorChangeLambda = onPageColorChange
                                isShowingColorSelector = true
                            },
                        )
                        {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(readerColor.page))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            CircleShape
                                        )
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }

                        SettingListItem(
                            headlineContent = { Text(filter) },
                            onClick = {
                                title = filter
                                currentColor = Color(readerColor.filter)
                                onColorChangeLambda = onFilterColorChange
                                isShowingColorSelector = true
                            },
                        )
                        {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(readerColor.filter))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            CircleShape
                                        )
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                    }
                } else {
                    SettingList(
                        title = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.color_picker_navigation),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(50))
                                        .clickable(onClick = { isShowingColorSelector = false })
                                )
                                Text(title)
                            }
                        }
                    ) {
                        Box(Modifier.padding(16.dp)) {
                            ReaderColorPicker(
                                currentColor = currentColor,
                                onColorChange = {
                                    currentColor = it
                                    onColorChangeLambda(it.toArgb())
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun EffectPreview(
    modifier: Modifier = Modifier,
    readerColor: ReaderColor = ReaderColor(),
) {
    val backgroundColor = Color(readerColor.background)
    val pageColor = Color(readerColor.page)
    val filterColor = Color(readerColor.filter)

    // 获取当前设备屏幕真正的宽高比
    val configuration = LocalConfiguration.current
    val screenAspectRatio =
        configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()

    val simulationHeight = 180.dp
    val simulationWidth = simulationHeight * screenAspectRatio

    SettingList(
        modifier = modifier,
        title = { Text(stringResource(R.string.reader_color_label_preview)) }
    ) {
        Box(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.inner_padding_of_container))
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .height(simulationHeight)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 真实层级模拟 + 完美等于设备当前屏幕宽高比
                Box(
                    modifier = Modifier
                        .width(simulationWidth)
                        .height(simulationHeight)
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius)))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius))
                        ),
                    contentAlignment = Alignment.Center
                )
                {
                    // 层级 1（最底层）：阅读器整体背景 (backgroundColor)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor)
                    )

                    // 层级 2（中间层）：书籍书页 (pageColor)
                    Box(
                        modifier = Modifier
                            .aspectRatio(0.7f) // A4纸
                            .fillMaxSize()
                            .background(pageColor)
                    )

                    // 层级 3（最顶层）：护眼/颜色滤镜 (filterColor)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(filterColor)
                    )
                }

                // Legend
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceAround
                )
                {
                    LegendItem(
                        color = backgroundColor,
                        label = stringResource(R.string.reader_color_background)
                    )
                    LegendItem(
                        color = pageColor,
                        label = stringResource(R.string.reader_color_page)
                    )
                    LegendItem(
                        color = filterColor,
                        label = stringResource(R.string.reader_color_filter)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun ReaderColorDetailsScreenPreview() {
    ReaderTheme {
        ReaderColorDetailsScreen(
            readerColor = ReaderColor(),
            onPageColorChange = {},
            onFilterColorChange = {},
            onBackgroundColorChange = {}
        )
    }
}