package io.lin.reader.ui.screens.reading.control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.BottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scrim
import androidx.compose.material3.ScrimDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.lin.reader.R
import io.lin.reader.data.preferences.CropMode
import io.lin.reader.data.preferences.InteractionStyle
import io.lin.reader.data.preferences.PageAlignment
import io.lin.reader.data.preferences.ReaderPreferencesForTest
import io.lin.reader.data.preferences.ReaderPreferencesInterface
import io.lin.reader.data.preferences.ReadingMode
import io.lin.reader.ui.components.colorpicker.ReaderColorPickerDialog
import io.lin.reader.ui.theme.ReaderTheme

private enum class ColorSettingType { Background, Page, Filter }

/**
 * 底部阅读设置抽屉 (BottomSettingSheet)
 * 参照 ChipSamples 与 BottomSheetSamples 编写，使用 FilterChip 与 FlowRow
 * 包含 ReaderPreferencesInterface 中的全部设置项并配备预览
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BottomSettingSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    readerPreferences: ReaderPreferencesInterface,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    var activeColorDialogType by remember { mutableStateOf<ColorSettingType?>(null) }
    // Handle sheet animations
    LaunchedEffect(visible) {
        if (visible) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    if (visible) {
        Scrim(
            "scrim",
            onClick = onDismissRequest
        )
        BottomSheet(
            modifier = modifier.statusBarsPadding(), // 1. 避开顶部状态栏
            state = sheetState,
            onDismissRequest = onDismissRequest,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.setting_other_reading_settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                // 1. 阅读模式 (ReadingMode)
                SettingSection(title = stringResource(R.string.setting_reading_mode)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReadingMode.entries.forEach { mode ->
                            val selected = readerPreferences.readingMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { readerPreferences.updateReadingMode(mode) },
                                label = { Text(stringResource(mode.displayName)) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }

                // 2. 页面对齐方式 (PageAlignment)
                SettingSection(title = stringResource(R.string.page_alignment_vertical)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PageAlignment.entries.forEach { alignment ->
                            val selected = readerPreferences.pageAlignment == alignment
                            FilterChip(
                                selected = selected,
                                onClick = { readerPreferences.updatePageAlignment(alignment) },
                                label = { Text(stringResource(alignment.displayName)) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }

                // 3. 页面间距留白 (pagePaddingRatio)
                SettingSection(
                    title = stringResource(R.string.reading_mode_label_padding),
                    trailingContent = {
                        Text(
                            text = "${(readerPreferences.pagePaddingRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    val sliderState = rememberSliderState(
                        value = readerPreferences.pagePaddingRatio,
                        steps = 9,
                        trackRange = 0f..0.2f
                    )
                    Slider(
                        state = sliderState,
                        modifier = Modifier,
                        enabled = true,
                        onValueChange = {
                            readerPreferences.updatePagePaddingRatio(it)
                            sliderState.value = it
                        },
                        onValueChangeFinished = null,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        interactionSource = remember { MutableInteractionSource() }
                    )
                }

                // 4. 触控交互样式 (InteractionStyle)
                SettingSection(title = stringResource(R.string.setting_interaction_style)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InteractionStyle.entries.forEach { style ->
                            val selected = readerPreferences.interactionStyle == style
                            FilterChip(
                                selected = selected,
                                onClick = { readerPreferences.updateInteractionStyle(style) },
                                label = { Text(stringResource(style.displayName)) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }

                // 5. 裁剪/切边模式 (CropMode)
                SettingSection(title = stringResource(R.string.other_reading_settings_remove_gutter)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CropMode.entries.forEach { mode ->
                            val selected = readerPreferences.cropMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { readerPreferences.updateCropMode(mode) },
                                label = { Text(stringResource(mode.displayName)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (selected) Icons.Default.Done else mode.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            )
                        }
                    }
                }

                // 6. 其他功能开关选项 (RTL, Separate Cover, Fixed Indicator, Reflow)
                SettingSection(title = stringResource(R.string.other_reading_settings_label)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 从右向左翻页 (RTL)
                        FilterChip(
                            selected = readerPreferences.rtlMode,
                            onClick = { readerPreferences.toggleRtlMode() },
                            label = { Text(stringResource(R.string.other_reading_settings_rtl)) },
                            leadingIcon = if (readerPreferences.rtlMode) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null
                        )

                        // 独立封面 (Separate Cover)
                        FilterChip(
                            selected = readerPreferences.separateCover,
                            onClick = { readerPreferences.toggleSeparateCover() },
                            label = { Text(stringResource(R.string.other_reading_settings_separate_cover)) },
                            leadingIcon = if (readerPreferences.separateCover) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null
                        )

                        // 固定页码指示器 (Fixed Page Indicator)
                        FilterChip(
                            selected = readerPreferences.fixedPageIndicator,
                            onClick = { readerPreferences.toggleFixedPageIndicator() },
                            label = { Text(stringResource(R.string.other_reading_settings_fixed_page_indicator)) },
                            leadingIcon = if (readerPreferences.fixedPageIndicator) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null
                        )

                        // 文本重排模式 (Reflow)
                        FilterChip(
                            selected = readerPreferences.isReflow,
                            onClick = { readerPreferences.toggleReflowMode() },
                            label = { Text(stringResource(R.string.setting_reflow)) },
                            leadingIcon = if (readerPreferences.isReflow) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null
                        )
                    }
                }

                // 7. 阅读器颜色状态 (ReaderColor) 与调色弹窗
                SettingSection(title = stringResource(R.string.setting_reader_color)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ColorPreviewChip(
                            label = stringResource(R.string.reader_color_background),
                            color = Color(readerPreferences.readerColor.background),
                            onClick = { activeColorDialogType = ColorSettingType.Background }
                        )
                        ColorPreviewChip(
                            label = stringResource(R.string.reader_color_page),
                            color = Color(readerPreferences.readerColor.page),
                            onClick = { activeColorDialogType = ColorSettingType.Page }
                        )
                        ColorPreviewChip(
                            label = stringResource(R.string.reader_color_filter),
                            color = Color(readerPreferences.readerColor.filter),
                            onClick = { activeColorDialogType = ColorSettingType.Filter }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // 显示对应的 ReaderColorPickerDialog 弹窗
    activeColorDialogType?.let { dialogType ->
        val (dialogTitle, currentColor, updateColor) = when (dialogType) {
            ColorSettingType.Background -> Triple(
                stringResource(R.string.reader_color_background),
                Color(readerPreferences.readerColor.background)
            ) { color: Color -> readerPreferences.updateBackgroundColor(color.toArgb()) }

            ColorSettingType.Page -> Triple(
                stringResource(R.string.reader_color_page),
                Color(readerPreferences.readerColor.page)
            ) { color: Color -> readerPreferences.updatePageColor(color.toArgb()) }

            ColorSettingType.Filter -> Triple(
                stringResource(R.string.reader_color_filter),
                Color(readerPreferences.readerColor.filter)
            ) { color: Color -> readerPreferences.updateFilterColor(color.toArgb()) }
        }

        ReaderColorPickerDialog(
            visible = true,
            title = dialogTitle,
            currentColor = currentColor,
            onColorChange = updateColor,
            onDismissRequest = { activeColorDialogType = null }
        )
    }
}

@Composable
private fun SettingSection(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            trailingContent?.invoke()
        }
        content()
    }
}

@Composable
private fun ColorPreviewChip(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview
@Composable
private fun BottomSettingSheetPreview() {
    ReaderTheme {
        BottomSettingSheet(
            visible = true,
            onDismissRequest = {},
            readerPreferences = ReaderPreferencesForTest()
        )
    }
}
