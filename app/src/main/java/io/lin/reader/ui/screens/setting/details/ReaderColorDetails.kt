package io.lin.reader.ui.maintab.setting.details

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import io.lin.reader.R
import io.lin.reader.data.preferences.ReaderColor
import io.lin.reader.ui.components.screenbar.DynamicTopAppBar
import io.lin.reader.ui.components.textfield.StyledOutlinedTextField
import io.lin.reader.ui.maintab.setting.SettingList
import io.lin.reader.ui.maintab.setting.SettingListItem
import io.lin.reader.ui.theme.ReaderTheme
import java.util.Locale


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
                            onClick =  {
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
                            onClick =  {
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
                    ColorPicker(
                        title = title,
                        currentColor = currentColor,
                        onColorChange = {
                            currentColor = it
                            onColorChangeLambda(it.toArgb())
                        },
                        onBackClick = { isShowingColorSelector = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun EffectPreview(
    modifier: Modifier = Modifier,
    readerColor: ReaderColor = ReaderColor(),
) {
    val backgroundColor = Color(readerColor.background)
    val pageColor = Color(readerColor.page)
    val filterColor = Color(readerColor.filter)

    SettingList(
        modifier = modifier,
        title = { Text(stringResource(R.string.reader_color_label_preview)) }
    ) {
        Row(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.inner_padding_of_container))
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
        ) {
            // Simulated Phone Screen
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius)))
                    .background(Color.Black)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(dimensionResource(R.dimen.composable_rounded_corner_radius))
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(backgroundColor)
                    )
                    // Reading Content area
                    Box(
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxWidth()
                            .background(pageColor)
                    )
                    // Bottom Bar area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(backgroundColor)
                    )
                }
                // Filter layer covering the entire screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(filterColor)
                )
            }

            // Legend
            Column(
                modifier = Modifier.height(180.dp),
                verticalArrangement = Arrangement.SpaceAround
            ) {
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

@Composable
fun ColorPicker(
    title: String = "Default Color Item",
    currentColor: Color = Color.White,
    onColorChange: (Color) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var customHex by remember(currentColor) { mutableStateOf(currentColor.toHexString()) }

    val standardColors = when (title) {
        // 阅读器整体背景（侧重沉浸感、边框过渡与夜间契合度）
        stringResource(R.string.reader_color_background) -> listOf(
            Color(0xFFF8F9FA), // 极简纯白 / 明亮底色
            Color(0xFFEFECE6), // 温和暖白 / 纸张周边
            Color(0xFFE2D8C3), // 复古羊皮纸背景
            Color(0xFFD0E1D4), // 清爽浅绿背景
            Color(0xFFD6DFEB), // 柔和冷灰蓝背景
            Color(0xFF2C2C2E), // 极简暗灰 / 夜间过渡
            Color(0xFF1E1E24), // 深蓝灰 / 沉浸式夜间
            Color(0xFF181818), // 暗黑模式背景
            Color(0xFF121212), // Material 深色首选
            Color(0xFF000000)  // 纯黑 Oled 省电背景
        )

        // 阅读书页颜色（侧重文字对比度、护眼度与质感）
        stringResource(R.string.reader_color_page) -> listOf(
            Color(0xFFFFFFFF), // 纯白 - 经典高对比度
            Color(0xFFFFFBE8), // 奶白/暖香槟 - 降低刺眼感
            Color(0xFFF5F5DC), // 羊皮纸/米黄 - 经典纸张质感
            Color(0xFFE8DFCD), // 复古黄 - 牛皮纸/老书感觉
            Color(0xFFCCE8CF), // 豆沙绿 - 经典护眼色
            Color(0xFFE3EDCD), // 浅草绿 - 清新护眼
            Color(0xFFE8E8E8), // 浅灰 - 柔和无彩质感
            Color(0xFF333333), // 深灰 - 柔和夜间模式（不刺眼）
            Color(0xFF1C2526), // 墨绿暗色 - 舒适夜间
            Color(0xFF000000)  // 纯黑 - OLED 极致夜间
        )

        // 阅读滤镜颜色（自带透明度：抗蓝光暖光、降低亮度、色温微调）
        stringResource(R.string.reader_color_filter) -> listOf(
            Color(0x33FF9800), // 20% 暖琥珀 - 轻度夜间去蓝光
            Color(0x4DFF9800), // 30% 暖橘 - 深度夜间去蓝光
            Color(0x33FF5722), // 20% 暖红光 - 极暗环境助眠滤镜
            Color(0x264CAF50), // 15% 护眼绿 - 减轻视觉疲劳
            Color(0x33009688), // 20% 青绿 - 微调冷色屏色温
            Color(0x1F2196F3), // 12% 柔蓝 - 阳光下提升对比度感
            Color(0x33000000), // 20% 灰滤镜 - 适度降低屏幕全局亮度
            Color(0x66000000), // 40% 深灰 - 极暗环境压制刺眼强光
            Color(0x263F51B5), // 15% 靛蓝 - 冷色调沉浸滤镜
            Color(0x338BC34A)  // 20% 黄绿 - 模拟防蓝光眼镜视觉
        )

        // 没法判断时，为背景、书页、滤镜都推荐一些颜色
        else -> listOf(
            Color(0xFFFFFFFF), // 纯白 - 经典书页
            Color(0xFFF5F5DC), // 米色 - 柔和护眼书页
            Color(0xFFC7EDCC), // 豆沙绿 - 经典护眼色
            Color(0xFFEAEAEF), // 浅灰蓝 - 现代阅读底色
            Color(0xFF1A1A1A), // 深黑 - 夜间模式背景
            Color(0xFF2E2E2E), // 深灰 - 护眼暗色背景
            Color(0xFF000000), // 极黑 - OLED 省电背景
            Color(0x4DFF9800), // 暖橘滤镜 - 30% 透明度，夜间去蓝光
            Color(0x66000000), // 灰色滤镜 - 40% 透明度，降低全局亮度
            Color(0x264CAF50)  // 绿意滤镜 - 15% 透明度，微调屏幕色温
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            .clickable(onClick = onBackClick)
                    )
                    Text(title)
                }
            }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.color_picker_standard))

                // 10 Standard color blocks in 2 rows
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        standardColors.take(5).forEach { color ->
                            ColorOptionCircle(
                                color = color,
                                isSelected = currentColor.toArgb() == color.toArgb(),
                                onClick = {
                                    onColorChange(color)
                                    customHex = color.toHexString()
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        standardColors.drop(5).forEach { color ->
                            ColorOptionCircle(
                                color = color,
                                isSelected = currentColor.toArgb() == color.toArgb(),
                                onClick = {
                                    onColorChange(color)
                                    customHex = color.toHexString()
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                Text(stringResource(R.string.color_picker_custom))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.inner_padding_of_container))
                ) {
                    // Display currently used color
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    StyledOutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = customHex,
                        placeholder = stringResource(R.string.color_picker_placer_holder),
                        labelText = stringResource(R.string.color_picker_label),
                        errorText = stringResource(R.string.color_picker_error),
                        onValueChange = {
                            customHex = it
                            val parsedColor = it.toColorOrNull()
                            if (parsedColor != null) {
                                // Preserve current alpha when parsing hex
                                onColorChange(parsedColor.copy(alpha = currentColor.alpha))
                            }
                        },
                        validateInput = {
                            customHex.toColorOrNull() != null
                        }
                    )
                }

                // Opacity Slider
                Text(
                    stringResource(R.string.color_picker_opacity) + String.format(
                        Locale.US,
                        "%.1f",
                        currentColor.alpha
                    ),
                )
                Slider(
                    value = currentColor.alpha,
                    onValueChange = { onColorChange(currentColor.copy(alpha = it)) },
                    valueRange = 0f..1f,
                    steps = 9, // 0, 0.1, 0.2, ..., 0.9, 1.0
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                        activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                        inactiveTickColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                            alpha = 0.5f
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun ColorOptionCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
    )
}

private fun Color.toHexString(): String {
    return String.format(Locale.US, "#%06X", (0xFFFFFF and this.toArgb()))
}

private fun String.toColorOrNull(): Color? {
    return try {
        val hex = if (startsWith("#")) substring(1) else this
        if (hex.length == 6) Color("#$hex".toColorInt())
        else null
    } catch (_: Exception) {
        null
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

@Preview(showBackground = true)
@Composable
private fun ColorPickerPreview() {
    ReaderTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ColorPicker(
                title = stringResource(R.string.reader_color_page),
                currentColor = Color(0xFFF5F5DC),
                onColorChange = {},
                onBackClick = {}
            )
        }
    }
}