package io.lin.reader.ui.screens.reading.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lin.reader.R
import io.lin.reader.data.preferences.CropMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomControlBar(
    visible: Boolean,
    currentPageIndex: Int,
    pageCount: Int,
    onPageChange: (Int) -> Unit,
    fixedPageIndicator: Boolean,
    isEdgeVolume: Pair<Boolean, Boolean>,
    onPreviousVolume: () -> Unit,
    onNextVolume: () -> Unit,
    isScreenRotated: Boolean,
    onRotateIconClick: () -> Unit,
    rtlMode: Boolean,
    onRtlIconClick: () -> Unit,
    cropMode: CropMode,
    onCropIconClick: () -> Unit,
    onSettingIconClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var indicatorPageIndex by remember(currentPageIndex) { mutableIntStateOf(currentPageIndex) }
    val animatedColor by animateColorAsState(targetValue = Color(0xFFE0E0E0), label = "color")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(//拦截手势
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 固定页码显示器
        if (fixedPageIndicator) {
            PageNumberIndicator(
                text = "${indicatorPageIndex + 1} / $pageCount",
                color = animatedColor,
                modifier = Modifier
                    .padding(bottom = dimensionResource(R.dimen.half_inner_padding_of_container))
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + expandVertically() + slideInVertically { it },
            exit = fadeOut() + shrinkVertically() + slideOutVertically { it }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 未固定页码显示器，一起随动画消失
                if (!fixedPageIndicator) {
                    PageNumberIndicator(
                        text = "${indicatorPageIndex + 1} / $pageCount",
                        color = animatedColor,
                        modifier = Modifier
                            .padding(bottom = dimensionResource(R.dimen.half_inner_padding_of_container))
                    )
                }

                // 进度条
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 上一册
                    IconButton(
                        onClick = onPreviousVolume,
                        enabled = !isEdgeVolume.first
                    ) { Icon(Icons.Filled.SkipPrevious, "Prev") }

                    // 进度条
                    val sliderState = rememberSliderState(
                        value = currentPageIndex.toFloat(),
                        steps = (pageCount - 2).coerceAtLeast(0),
                        trackRange = 0f..(pageCount - 1).toFloat().coerceAtLeast(0f)
                    )
                    val direction = if (rtlMode) LayoutDirection.Rtl else LayoutDirection.Ltr
                    CompositionLocalProvider(LocalLayoutDirection provides direction) {
                        Slider(
                            state = sliderState,
                            modifier = Modifier.weight(1f),
                            enabled = true,
                            onValueChange = {
                                sliderState.value = it
                                indicatorPageIndex = it.toInt()
                            },
                            onValueChangeFinished = { onPageChange(sliderState.value.toInt()) },
                            colors = SliderDefaults.colors(),
                            interactionSource = remember { MutableInteractionSource() }
                        )
                    }

                    // 下一册
                    IconButton(
                        onClick = onNextVolume,
                        enabled = !isEdgeVolume.second
                    ) { Icon(Icons.Filled.SkipNext, "Next") }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 声明各 Icon 的说明文字变量
                val screenDirectionDescription = stringResource(R.string.reading_screen_direction)
                val rtlDescription = stringResource(R.string.other_reading_settings_rtl)
                val cropDescription = stringResource(R.string.other_reading_settings_remove_gutter)
                val settingDescription = stringResource(R.string.setting_other_reading_settings)

                // 底部 Icon 栏使用 BottomAppBar 重构
                BottomAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. 阅读器屏幕方向，不持久化
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                            tooltip = {
                                PlainTooltip {
                                    Text(screenDirectionDescription)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = onRotateIconClick) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = screenDirectionDescription,
                                    tint = if (isScreenRotated) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                        }

                        // 2. 从右向左翻页模式 (RTL)
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                            tooltip = {
                                PlainTooltip {
                                    Text(rtlDescription)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = onRtlIconClick) {
                                Icon(
                                    imageVector = if (rtlMode) Icons.Filled.TurnLeft else Icons.Filled.TurnRight,
                                    contentDescription = rtlDescription,
                                    modifier = Modifier.size(32.dp),
                                    tint = if (rtlMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                        }

                        // 3. 裁剪/切边模式 (Crop)
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                            tooltip = {
                                PlainTooltip {
                                    Text(cropDescription)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = onCropIconClick) {
                                Icon(
                                    imageVector = cropMode.icon,
                                    contentDescription = cropDescription,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (cropMode != CropMode.None) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                        }

                        // 4. 其他阅读设置
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                            tooltip = {
                                PlainTooltip {
                                    Text(settingDescription)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = onSettingIconClick) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = settingDescription
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PageNumberIndicator(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                drawStyle = Stroke(miter = 10f, width = 4f, join = StrokeJoin.Round)
            ),
            color = Color.Black.copy(alpha = 0.7f)
        )
        Text(
            text = text,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = color
        )
    }
}