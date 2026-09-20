package io.lin.reader.ui.maintab.setting.details

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.lin.reader.R
import io.lin.reader.data.preferences.AppLanguage
import io.lin.reader.data.preferences.AppPreferencesForTest
import io.lin.reader.data.preferences.AppPreferencesInterface
import io.lin.reader.data.preferences.DarkMode
import io.lin.reader.data.preferences.ThemeColor
import io.lin.reader.data.preferences.ThemeContrast
import io.lin.reader.ui.components.menu.StyledMenu
import io.lin.reader.ui.components.menu.StyledMenuItem
import io.lin.reader.ui.components.screenbar.DynamicTopAppBar
import io.lin.reader.ui.maintab.setting.SettingList
import io.lin.reader.ui.maintab.setting.SettingListItem
import io.lin.reader.ui.theme.AppThemePalette
import io.lin.reader.ui.theme.ReaderTheme
import io.lin.reader.ui.components.selection.SelectableBox
import io.lin.reader.ui.theme.color.BreezeTheme
import io.lin.reader.ui.theme.color.DefaultTheme
import io.lin.reader.ui.theme.color.HoneyTheme
import io.lin.reader.ui.theme.color.MapleTheme
import io.lin.reader.ui.theme.color.MeadowTheme

@Composable
fun AppAppearanceDetailsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    appPreferences: AppPreferencesInterface
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
                title = stringResource(R.string.setting_appearance),
                scrollFraction = scrollFraction,
                onNavigationClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        AppAppearanceDetails(
            modifier = Modifier.padding(innerPadding),
            listState = listState,
            appPreferences = appPreferences
        )
    }
}

@Composable
fun AppAppearanceDetails(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    appPreferences: AppPreferencesInterface
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    )
    {
        item {
            SettingList(
                title = { Text(stringResource(R.string.appearance_label_theme)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.inner_padding_of_container))
                ) {
                    Text(stringResource(R.string.appearance_theme_color))
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
                                onClick = { appPreferences.updateThemeColor(color) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = stringResource(R.string.appearance_theme_contrast))
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeContrast.entries.forEachIndexed { index, contrast ->
                            val isSelected = appPreferences.themeContrast == contrast
                            SegmentedButton(
                                modifier = Modifier.weight(1f),
                                selected = isSelected,
                                onClick = { appPreferences.updateThemeContrast(contrast) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ThemeContrast.entries.size
                                ),
                                icon = { SegmentedButtonDefaults.Icon(active = isSelected) },
                                label = {
                                    Text(
                                        text = stringResource(contrast.labelRes),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(stringResource(R.string.appearance_theme_dark))
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        DarkMode.entries.forEachIndexed { index, theme ->
                            val isSelected = appPreferences.theme == theme
                            SegmentedButton(
                                modifier = Modifier.weight(1f),
                                selected = isSelected,
                                onClick = { appPreferences.updateTheme(theme) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = DarkMode.entries.size
                                ),
                                icon = {
                                    SegmentedButtonDefaults.Icon(
                                        active = isSelected,
                                        activeContent = {
                                            Icon(
                                                imageVector = when (theme) {
                                                    DarkMode.System -> Icons.Default.SettingsSuggest
                                                    DarkMode.Light -> Icons.Default.LightMode
                                                    DarkMode.Dark -> Icons.Default.DarkMode
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                            )
                                        }
                                    )
                                },
                                label = {
                                    Text(
                                        text = when (theme) {
                                            DarkMode.System -> stringResource(R.string.dark_mode_system)
                                            DarkMode.Light -> stringResource(R.string.dark_mode_light)
                                            DarkMode.Dark -> stringResource(R.string.dark_mode_dark)
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingList(
                title = { Text("导航") }
            ) {
                SettingListItem(
                    headlineContent = { Text("悬浮导航栏") },
                    onClick = { appPreferences.toggleFloatingNavigationBar() },
                    leadingIcon = Icons.Default.Layers
                ) {
                    Switch(
                        checked = appPreferences.floatingNavigationBar,
                        onCheckedChange = { appPreferences.toggleFloatingNavigationBar() }
                    )
                }
            }
        }

        item {
            SettingList(
                title = { Text(stringResource(R.string.appearance_label_language)) }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.appearance_label_language)) },
                        onClick = { expanded = !expanded },
                        leadingIcon = Icons.Default.Language
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(appPreferences.language.labelRes),
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
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
                                        appPreferences.updateLanguage(lang)
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
    val scheme: ColorScheme =
        if (themeColor == ThemeColor.Dynamic) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            val palette: AppThemePalette = when (themeColor) {
                ThemeColor.Default -> DefaultTheme
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
        SelectableBox(
            selected = isSelected,
            onSelectedChange = { onClick() },
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            activeColor = scheme.primary,
            checkColor = scheme.onPrimary
        ) {
            Column(modifier = Modifier
                .fillMaxSize()
                .background(scheme.surface)) {
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
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(scheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(scheme.onPrimaryContainer.copy(alpha = 0.5f))
                        )
                    }
                }

                // 模拟主体内容
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
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
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(scheme.onSurface)
                            )
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(scheme.onSurfaceVariant)
                            )
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
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(scheme.onSurfaceVariant.copy(alpha = 0.4f))
                        )
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

@Preview(showBackground = true)
@Composable
private fun AppAppearanceDetailsScreenPreview() {
    ReaderTheme {
        AppAppearanceDetailsScreen(
            appPreferences = AppPreferencesForTest()
        )
    }
}