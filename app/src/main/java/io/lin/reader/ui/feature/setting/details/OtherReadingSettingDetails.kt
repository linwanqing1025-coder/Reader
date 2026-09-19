package io.lin.reader.ui.feature.setting.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.lin.reader.R
import io.lin.reader.data.preferences.ReaderPreferencesForTest
import io.lin.reader.data.preferences.ReaderPreferencesInterface
import io.lin.reader.ui.components.menu.StyledMenu
import io.lin.reader.ui.components.menu.StyledMenuItem
import io.lin.reader.ui.components.screenbar.DynamicTopAppBar
import io.lin.reader.ui.feature.setting.SettingList
import io.lin.reader.ui.feature.setting.SettingListItem
import io.lin.reader.ui.theme.ReaderTheme

enum class CropMode {
    None,           // 不裁剪
    Horizontal,     // 仅左右 (消除书沟)
    Vertical,       // 仅上下 (去除页眉页脚)
    All             // 四周全部裁剪 (自动切边)
}

@Composable
fun OtherReadingSettingDetailsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    readerPreferences: ReaderPreferencesInterface
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
                title = stringResource(R.string.other_reading_settings_label),
                scrollFraction = scrollFraction,
                onNavigationClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        OtherReadingSettingDetails(
            modifier = Modifier.padding(innerPadding),
            listState = listState,
            readerPreferences = readerPreferences
        )
    }
}

@Composable
fun OtherReadingSettingDetails(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    readerPreferences: ReaderPreferencesInterface
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
                title = { Text(stringResource(R.string.other_reading_settings_label)) }
            ) {
                SettingListItem(
                    headlineContent = { Text(stringResource(R.string.other_reading_settings_rtl)) },
                    leadingIcon = Icons.Default.SwapHoriz
                ) {
                    Switch(
                        checked = readerPreferences.rtlMode,
                        onCheckedChange = { readerPreferences.toggleRtlMode() }
                    )
                }
                SettingListItem(
                    headlineContent = { Text(stringResource(R.string.other_reading_settings_separate_cover)) },
                    leadingIcon = Icons.Default.Collections
                ) {
                    Switch(
                        checked = readerPreferences.separateCover,
                        onCheckedChange = { readerPreferences.toggleSeparateCover() }
                    )
                }

                // 新增裁剪模式选择
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    SettingListItem(
                        headlineContent = { Text(stringResource(R.string.other_reading_settings_remove_gutter)) },
                        leadingIcon = Icons.Default.ContentCut
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = readerPreferences.cropMode.name,
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
                    // 裁剪选择菜单
                    Box {
                        StyledMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            CropMode.entries.forEach { it ->
                                StyledMenuItem(
                                    text = it.name,
                                    onClick = {
                                        readerPreferences.updateCropMode(it)
                                        expanded = false
                                    },
                                    trailingIcon = if (readerPreferences.cropMode == it) {
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

                SettingListItem(
                    headlineContent = { Text(stringResource(R.string.other_reading_settings_fixed_page_indicator)) },
                    leadingIcon = Icons.Filled.Pin
                ) {
                    Switch(
                        checked = readerPreferences.fixedPageIndicator,
                        onCheckedChange = { readerPreferences.toggleFixedPageIndicator() }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun OtherReadingSettingDetailsScreenPreview() {
    ReaderTheme {
        OtherReadingSettingDetailsScreen(
            readerPreferences = ReaderPreferencesForTest()
        )
    }
}