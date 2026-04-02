package io.lin.reader.ui.screen.setting.details

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lin.reader.R
import io.lin.reader.data.preferences.AppPreferences
import io.lin.reader.ui.components.menu.StyledMenu
import io.lin.reader.ui.components.menu.StyledMenuItem
import io.lin.reader.ui.screen.setting.SettingSnippet
import io.lin.reader.ui.theme.AppThemePalette
import io.lin.reader.ui.theme.ReaderTheme
import io.lin.reader.ui.theme.color.BreezeTheme
import io.lin.reader.ui.theme.color.DefaultTheme
import io.lin.reader.ui.theme.color.HoneyTheme
import io.lin.reader.ui.theme.color.MapleTheme
import io.lin.reader.ui.theme.color.MeadowTheme

enum class ThemeColor(@get:StringRes val labelRes: Int) {
    Dynamic(R.string.theme_color_dynamic),
    Default(R.string.theme_color_default),
    Maple(R.string.theme_color_maple),
    Meadow(R.string.theme_color_meadow),
    Breeze(R.string.theme_color_breeze),
    Honey(R.string.theme_color_honey)
}

enum class ThemeContrast(@get:StringRes val labelRes: Int) {
    Light(R.string.contrast_light),
    Medium(R.string.contrast_medium),
    High(R.string.contrast_high)
}

enum class DarkMode {
    System, Light, Dark
}

enum class AppLanguage(@get:StringRes val labelRes: Int) {
    System(R.string.language_system),
    English(R.string.language_english),
    ChineseSimplified(R.string.language_chinese_simplified),
    ChineseTraditional(R.string.language_chinese_traditional),
    Japanese(R.string.language_japanese)
}

@Composable
fun AppAppearanceDetails(
    modifier: Modifier = Modifier,
    appPreferences: AppPreferences,
    updateAppDarkMode: (DarkMode) -> Unit,
    updateAppThemeColor: (ThemeColor) -> Unit,
    updateAppThemeContrast: (ThemeContrast) -> Unit,
    updateAppLanguage: (AppLanguage) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(dimensionResource(R.dimen.inner_padding_of_container)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.inner_padding_of_container))
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.half_inner_padding_of_container))) {
                Text(
                    text = stringResource(R.string.appearance_label_theme),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        start = dimensionResource(R.dimen.half_inner_padding_of_container),
                        bottom = dimensionResource(R.dimen.half_inner_padding_of_container)
                    )
                )
                SettingSnippet {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(R.dimen.inner_padding_of_container))
                    ) {
                        Text(
                            text = stringResource(R.string.appearance_theme_color),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(ThemeColor.entries) { color ->
                                ThemePreviewItem(
                                    darkMode = appPreferences.theme,
                                    themeColor = color,
                                    themeContrast = appPreferences.themeContrast,
                                    isSelected = appPreferences.themeColor == color,
                                    onClick = { updateAppThemeColor(color) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.appearance_theme_contrast),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(50)
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ThemeContrast.entries.forEachIndexed { index, contrast ->
                                val isSelected = appPreferences.themeContrast == contrast
                                AppearanceOption(
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(contrast.labelRes),
                                    isSelected = isSelected,
                                    onClick = { updateAppThemeContrast(contrast) }
                                )
                                if (index < ThemeContrast.entries.size - 1) {
                                    VerticalDivider(
                                        modifier = Modifier.height(32.dp),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.appearance_theme_dark),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(50)
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DarkMode.entries.forEachIndexed { index, theme ->
                                val isSelected = appPreferences.theme == theme
                                AppearanceOption(
                                    modifier = Modifier.weight(1f),
                                    text = when (theme) {
                                        DarkMode.System -> stringResource(R.string.dark_mode_system)
                                        DarkMode.Light -> stringResource(R.string.dark_mode_light)
                                        DarkMode.Dark -> stringResource(R.string.dark_mode_dark)
                                    },
                                    icon = when (theme) {
                                        DarkMode.System -> Icons.Default.SettingsSuggest
                                        DarkMode.Light -> Icons.Default.LightMode
                                        DarkMode.Dark -> Icons.Default.DarkMode
                                    },
                                    isSelected = isSelected,
                                    onClick = { updateAppDarkMode(theme) }
                                )
                                if (index < DarkMode.entries.size - 1) {
                                    VerticalDivider(
                                        modifier = Modifier.height(32.dp),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.half_inner_padding_of_container))) {
                Text(
                    text = stringResource(R.string.appearance_label_language),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        start = dimensionResource(R.dimen.half_inner_padding_of_container),
                        bottom = dimensionResource(R.dimen.half_inner_padding_of_container)
                    )
                )
                SettingSnippet {
                    var expanded by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { expanded = true })
                                .padding(
                                    vertical = dimensionResource(R.dimen.half_inner_padding_of_container),
                                    horizontal = dimensionResource(R.dimen.inner_padding_of_container)
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.inner_padding_of_container)))
                                Text(
                                    text = stringResource(R.string.appearance_label_language),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(appPreferences.language.labelRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box {
                            StyledMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                AppLanguage.entries.forEach { lang ->
                                    StyledMenuItem(
                                        text = stringResource(lang.labelRes),
                                        onClick = {
                                            updateAppLanguage(lang)
                                            expanded = false
                                        },
                                        trailingIcon = if (appPreferences.language == lang) {
                                            {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewItem(
    darkMode: DarkMode,
    themeColor: ThemeColor,
    themeContrast: ThemeContrast,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val darkTheme = when (darkMode) {
        DarkMode.System -> isSystemInDarkTheme()
        DarkMode.Light -> false
        DarkMode.Dark -> true
    }
    val context = LocalContext.current
    
    // 修正动态颜色判断逻辑
    val scheme: ColorScheme = if (themeColor == ThemeColor.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val palette: AppThemePalette = when (themeColor) {
            ThemeColor.Default, ThemeColor.Dynamic -> DefaultTheme
            ThemeColor.Maple -> MapleTheme
            ThemeColor.Meadow -> MeadowTheme
            ThemeColor.Breeze -> BreezeTheme
            ThemeColor.Honey -> HoneyTheme
        }
        if (darkTheme) {
            when (themeContrast) {
                ThemeContrast.Light -> palette.darkScheme
                ThemeContrast.Medium -> palette.mediumContrastDarkColorScheme
                ThemeContrast.High -> palette.highContrastDarkColorScheme
            }
        } else {
            when (themeContrast) {
                ThemeContrast.Light -> palette.lightScheme
                ThemeContrast.Medium -> palette.mediumContrastLightColorScheme
                ThemeContrast.High -> palette.highContrastLightColorScheme
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(104.dp)
    ) {
        Box(
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surface)
                .clickable(onClick = onClick)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) scheme.primary else scheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 模拟 App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(scheme.primaryContainer)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(scheme.primary))
                        Box(modifier = Modifier.width(36.dp).height(6.dp).clip(CircleShape).background(scheme.onPrimaryContainer.copy(alpha = 0.5f)))
                    }
                }

                // 模拟主体内容
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 模拟书籍/卡片项
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(scheme.secondary)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.width(40.dp).height(6.dp).clip(CircleShape).background(scheme.onSurface))
                            Box(modifier = Modifier.width(24.dp).height(4.dp).clip(CircleShape).background(scheme.onSurfaceVariant))
                        }
                    }

                    // 模拟阅读状态 - 标签/分类
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(scheme.secondaryContainer)
                            )
                        }
                    }

                    // 模拟进度条
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.width(30.dp).height(4.dp).clip(CircleShape).background(scheme.onSurfaceVariant.copy(alpha = 0.4f)))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(scheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(scheme.primary)
                            )
                        }
                    }

                    // 底部装饰
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(scheme.tertiaryContainer)
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(scheme.error)
                        )
                    }
                }
            }

            // 选中指示器
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.primary.copy(alpha = 0.08f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .background(scheme.primary, CircleShape)
                        .border(2.dp, scheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(themeColor.labelRes),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) scheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppearanceOption(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor =
        if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(dimensionResource(R.dimen.half_inner_padding_of_container)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.padding(2.dp))
        }
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppAppearanceDetailsPreview() {
    ReaderTheme {
        Surface {
            AppAppearanceDetails(
                appPreferences = AppPreferences(),
                updateAppDarkMode = {},
                updateAppThemeColor = {},
                updateAppThemeContrast = {},
                updateAppLanguage = {}
            )
        }
    }
}
