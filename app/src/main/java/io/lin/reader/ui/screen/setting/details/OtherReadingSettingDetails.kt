package io.lin.reader.ui.screen.setting.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderVertical
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.lin.reader.R
import io.lin.reader.ui.screen.setting.SettingItem
import io.lin.reader.ui.screen.setting.SettingSnippet

@Preview
@Composable
fun OtherReadingSettingDetails(
    modifier: Modifier = Modifier,
    rtlMode: Boolean = false,
    toggleRtlMode: () -> Unit = {},
    separateCover: Boolean = false,
    toggleSeparateCover: () -> Unit = { },
    removeGutter: Boolean = false,
    toggleRemoveGutter: () -> Unit = { },
    fixedPageIndicator: Boolean = false,
    toggleFixedPageIndicator: () -> Unit = { }
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.inner_padding_of_container)),
        contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.inner_padding_of_container)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.inner_padding_of_container))
    ) {
        item {
            Text(
                text = stringResource(R.string.other_reading_settings_label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = dimensionResource(R.dimen.half_inner_padding_of_container),
                    bottom = dimensionResource(R.dimen.half_inner_padding_of_container)
                )
            )
            SettingSnippet {
                SettingItem(
                    imageVector = Icons.Default.SwapHoriz,
                    leadingIconSize = 32.dp,
                    hint = stringResource(R.string.other_reading_settings_rtl),
                    onClick = toggleRtlMode,
                    isSwitchOption = true,
                    switchChecked = rtlMode,
                    onSwitchChange = { toggleRtlMode() }
                )
                SettingItem(
                    imageVector = Icons.Default.Collections,
                    hint = stringResource(R.string.other_reading_settings_separate_cover),
                    onClick = { toggleSeparateCover() },
                    isSwitchOption = true,
                    switchChecked = separateCover,
                    onSwitchChange = { toggleSeparateCover() }
                )
                SettingItem(
                    imageVector = Icons.Filled.BorderVertical,
                    hint = stringResource(R.string.other_reading_settings_remove_gutter),
                    onClick = { toggleRemoveGutter() },
                    isSwitchOption = true,
                    switchChecked = removeGutter,
                    onSwitchChange = { toggleRemoveGutter() }
                )
                SettingItem(
                    imageVector = Icons.Filled.Pin,
                    hint = stringResource(R.string.other_reading_settings_fixed_page_indicator),
                    onClick = { toggleFixedPageIndicator() },
                    isSwitchOption = true,
                    switchChecked = fixedPageIndicator,
                    onSwitchChange = { toggleFixedPageIndicator() }
                )
            }
        }
    }
}
