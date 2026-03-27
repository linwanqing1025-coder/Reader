package io.lin.reader.ui.screen.setting.details

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import io.lin.reader.R
import io.lin.reader.ui.components.textfield.StyledOutlinedTextField
import io.lin.reader.ui.screen.setting.SettingSnippet
import java.util.Locale

data class ReaderColor(
    val background: Int = android.graphics.Color.BLACK,
    val page: Int = android.graphics.Color.WHITE,
    val filter: Int = 0x00000000
)

@Composable
fun ReaderColorDetails(
    modifier: Modifier = Modifier,
    readerColor: ReaderColor,
    onBackgroundColorChange: (Color) -> Unit,
    onPageColorChange: (Color) -> Unit,
    onFilterColorChange: (Color) -> Unit
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

    var onColorChangeLambda by remember { mutableStateOf<(Color) -> Unit>({}) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.inner_padding_of_container)),
        contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.inner_padding_of_container)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.inner_padding_of_container))
    ) {
        item {
            Text(
                text = stringResource(R.string.reader_color_label_preview),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = dimensionResource(R.dimen.half_inner_padding_of_container),
                    bottom = dimensionResource(R.dimen.half_inner_padding_of_container)
                )
            )
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
                    Column {
                        Text(
                            text = stringResource(R.string.reader_color_label_color),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(
                                start = dimensionResource(R.dimen.half_inner_padding_of_container),
                                bottom = dimensionResource(R.dimen.half_inner_padding_of_container)
                            )
                        )
                        SettingSnippet {
                            Column {
                                ColorItem(
                                    hint = background,
                                    color = Color(readerColor.background),
                                    onClick = {
                                        title = background
                                        onColorChangeLambda = onBackgroundColorChange
                                        isShowingColorSelector = true
                                    }
                                )
                                ColorItem(
                                    hint = page,
                                    color = Color(readerColor.page),
                                    onClick = {
                                        title = page
                                        onColorChangeLambda = onPageColorChange
                                        isShowingColorSelector = true
                                    }
                                )
                                ColorItem(
                                    hint = filter,
                                    color = Color(readerColor.filter),
                                    onClick = {
                                        title = filter
                                        onColorChangeLambda = onFilterColorChange
                                        isShowingColorSelector = true
                                    }
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
                            onColorChangeLambda(it)
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

    SettingSnippet(modifier = modifier) {
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
                LegendItem(color = pageColor, label = stringResource(R.string.reader_color_page))
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
fun ColorItem(
    modifier: Modifier = Modifier,
    hint: String = "Default Color Item",
    color: Color = Color.White,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                vertical = dimensionResource(R.dimen.half_inner_padding_of_container),
                horizontal = dimensionResource(R.dimen.inner_padding_of_container)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = hint,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(40.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

    val standardColors = listOf(
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
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.half_inner_padding_of_container))) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.color_picker_navigation),
                modifier = Modifier
                    .size(28.dp)
                    .padding(start = 8.dp)
                    .align(alignment = Alignment.CenterStart)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onBackClick)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(alignment = Alignment.Center)
            )
        }

        SettingSnippet {
            Column(
                modifier = Modifier.padding(
                    dimensionResource(R.dimen.inner_padding_of_container)
                ),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.inner_padding_of_container)
                )
            ) {
                Text(
                    text = stringResource(R.string.color_picker_standard),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

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

                Text(
                    text = stringResource(R.string.color_picker_custom),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                    text = stringResource(R.string.color_picker_opacity) + String.format(
                        Locale.US,
                        "%.1f",
                        currentColor.alpha
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
private fun ReaderColorDetailsPreview() {
    MaterialTheme {
        ReaderColorDetails(
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
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ColorPicker(
                title = "背景颜色选择",
                currentColor = Color(0xFFF5F5DC),
                onColorChange = {},
                onBackClick = {}
            )
        }
    }
}