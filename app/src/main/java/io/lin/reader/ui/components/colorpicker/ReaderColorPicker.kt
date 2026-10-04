package io.lin.reader.ui.components.colorpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.AlphaTile
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import io.lin.reader.R
import io.lin.reader.ui.components.textfield.StyledOutlinedTextField
import io.lin.reader.ui.theme.ReaderTheme
import java.util.Locale

@Composable
fun ReaderColorPicker(
    modifier: Modifier = Modifier,
    currentColor: Color = Color.White,
    onColorChange: (Color) -> Unit = {}
) {
    val controller = rememberColorPickerController()

    val standardColors = remember {
        listOf(
            // 背景推荐 5 个
            Color(0xFFF8F9FA), // 极简纯白 / 明亮底色
            Color(0xFFEFECE6), // 温和暖白 / 纸张周边
            Color(0xFFE2D8C3), // 复古羊皮纸背景
            Color(0xFF2C2C2E), // 极简暗灰 / 夜间过渡
            Color(0xFF121212), // Material 深色首选

            // 书页推荐 5 个
            Color(0xFFFFFFFF), // 纯白 - 经典高对比度
            Color(0xFFFFFBE8), // 奶白/暖香槟 - 降低刺眼感
            Color(0xFFF5F5DC), // 羊皮纸/米黄 - 经典纸张质感
            Color(0xFFCCE8CF), // 豆沙绿 - 经典护眼色
            Color(0xFF1C2526), // 墨绿暗色 - 舒适夜间

            // 滤镜推荐 5 个
            Color(0x33FF9800), // 20% 暖琥珀 - 轻度夜间去蓝光
            Color(0x4DFF9800), // 30% 暖橘 - 深度夜间去蓝光
            Color(0x264CAF50), // 15% 护眼绿 - 减轻视觉疲劳
            Color(0x33000000), // 20% 灰滤镜 - 适度降低屏幕全局亮度
            Color(0x66000000)  // 40% 深灰 - 极暗环境压制刺眼强光
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.color_picker_standard))

        // 15 Standard color blocks in 3 rows
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            standardColors.chunked(5).forEach { rowColors ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowColors.forEach { color ->
                        ColorOptionCircle(
                            color = color,
                            isSelected = currentColor.toArgb() == color.toArgb(),
                            onClick = {
                                onColorChange(color)
                                controller.selectByColor(color, fromUser = false)
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        Text(stringResource(R.string.color_picker_custom))

        // 1. HSV 彩虹色盘取色器
        HsvColorPicker(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            controller = controller,
            initialColor = currentColor,
            onColorChanged = { colorEnvelope ->
                if (colorEnvelope.fromUser) {
                    onColorChange(colorEnvelope.color)
                }
            }
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.inner_padding_of_container))
        ) {
            // 支持带透明度（棋盘格底纹）的实时颜色展示
            AlphaTile(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                controller = controller
            )
            var customHex by remember(currentColor) { mutableStateOf(currentColor.toHexString()) }
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
                        val finalColor = parsedColor.copy(alpha = currentColor.alpha)
                        onColorChange(finalColor)
                        controller.selectByColor(finalColor, fromUser = false)
                    }
                },
                validateInput = {
                    customHex.toColorOrNull() != null
                }
            )
        }

        // Alpha Slider
        Text(
            stringResource(R.string.color_picker_alpha) + ": " + String.format(
                Locale.US,
                "%.1f",
                currentColor.alpha
            )
        )
        AlphaSlider(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(12.dp)),
            controller = controller,
            initialColor = currentColor
        )

        // Brightness Slider
        Text(stringResource(R.string.color_picker_brightness))
        BrightnessSlider(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(12.dp)),
            controller = controller,
            initialColor = currentColor
        )
    }
}

/**
 * 包含 ReaderColorPicker 的弹窗组件 (Dialog)
 */
@Composable
fun ReaderColorPickerDialog(
    visible: Boolean,
    title: String,
    currentColor: Color,
    onColorChange: (Color) -> Unit,
    onDismissRequest: () -> Unit
) {
    if (!visible) return

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier.padding(
                    vertical = 8.dp,
                    horizontal = 24.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 固定顶部的标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // 2. 中间可滚动的取色器区域
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    ReaderColorPicker(
                        currentColor = currentColor,
                        onColorChange = onColorChange
                    )
                }

                // 3. 固定底部的确定按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = stringResource(R.string.confirm),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
private fun ReaderColorPickerPreview() {
    var currentColor by remember { mutableStateOf(Color.White) }
    var visible by remember { mutableStateOf(true) }
    ReaderTheme {
        ReaderColorPickerDialog(
            visible = visible,
            title = stringResource(R.string.reader_color_page),
            currentColor = currentColor,
            onColorChange = { currentColor = it },
            onDismissRequest = { visible = false }
        )
    }
}
