package io.lin.reader.ui.maintab.setting.details

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.lin.reader.R
import io.lin.reader.data.preferences.InteractionStyle
import io.lin.reader.ui.components.screenbar.DynamicTopAppBar
import io.lin.reader.ui.maintab.setting.SettingList
import io.lin.reader.ui.theme.ReaderTheme
import io.lin.reader.ui.components.selection.SelectableBox
import io.lin.reader.ui.maintab.reading.control.InteractionStyle0Preview
import io.lin.reader.ui.maintab.reading.control.InteractionStyle1Preview
import io.lin.reader.ui.maintab.reading.control.InteractionStyle1RtlPreview
import io.lin.reader.ui.maintab.reading.control.InteractionStyle2Preview
import io.lin.reader.ui.maintab.reading.control.InteractionStyle2RtlPreview
import io.lin.reader.ui.maintab.reading.control.InteractionStyle3Preview
import io.lin.reader.ui.maintab.reading.control.InteractionStyle3RtlPreview

@Composable
fun InteractionDetailsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    interactionStyle: InteractionStyle = InteractionStyle.Style0,
    rtlMode: Boolean = false,
    updateInteractionStyle: (InteractionStyle) -> Unit = {}
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
                title = stringResource(R.string.setting_interaction_style),
                scrollFraction = scrollFraction,
                onNavigationClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        InteractionDetails(
            modifier = Modifier.padding(innerPadding),
            listState = listState,
            interactionStyle = interactionStyle,
            rtlMode = rtlMode,
            updateInteractionStyle = updateInteractionStyle
        )
    }
}

@Composable
fun InteractionDetails(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    interactionStyle: InteractionStyle = InteractionStyle.Style0,
    rtlMode: Boolean = false,
    updateInteractionStyle: (InteractionStyle) -> Unit = {}
){
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    )
    {
        item {
            SettingList(
                title = { Text(stringResource(R.string.setting_interaction_style)) },
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorIndicator(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(InteractionStyle.entries.toTypedArray()) { style ->
                            InteractionStyleOption(
                                interactionStyle = style,
                                isSelected = interactionStyle == style,
                                isRtl = rtlMode,
                                onClick = { updateInteractionStyle(style) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorIndicatorItem(
            color = MaterialTheme.colorScheme.surfaceVariant,
            label = stringResource(R.string.interaction_hint_previous)
        )
        ColorIndicatorItem(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            label = stringResource(R.string.interaction_hint_menu)
        )
        ColorIndicatorItem(
            color = MaterialTheme.colorScheme.primaryContainer,
            label = stringResource(R.string.interaction_hint_next)
        )
    }
}

@Composable
private fun ColorIndicatorItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InteractionStyleOption(
    modifier: Modifier = Modifier,
    interactionStyle: InteractionStyle,
    isSelected: Boolean,
    isRtl: Boolean,
    onClick: () -> Unit
) {
    val previewWidth = 100.dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SelectableBox(
            selected = isSelected,
            onSelectedChange = { onClick() },
            modifier = Modifier.width(previewWidth),
            shape = RoundedCornerShape(16.dp)
        ) {
            when (interactionStyle) {
                InteractionStyle.Style0 -> InteractionStyle0Preview(previewWidth)
                InteractionStyle.Style1 -> if (isRtl) InteractionStyle1RtlPreview(previewWidth) else InteractionStyle1Preview(
                    previewWidth
                )

                InteractionStyle.Style2 -> if (isRtl) InteractionStyle2RtlPreview(previewWidth) else InteractionStyle2Preview(
                    previewWidth
                )

                InteractionStyle.Style3 -> if (isRtl) InteractionStyle3RtlPreview(previewWidth) else InteractionStyle3Preview(
                    previewWidth
                )
            }
        }

        val label = when (interactionStyle) {
            InteractionStyle.Style0 -> stringResource(R.string.interaction_style_vertical)
            InteractionStyle.Style1 -> stringResource(R.string.interaction_style_horizontal)
            InteractionStyle.Style2 -> stringResource(R.string.interaction_style_L)
            InteractionStyle.Style3 -> stringResource(R.string.interaction_style_Mixed)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun InteractionDetailsScreenPreview() {
    ReaderTheme {
        InteractionDetailsScreen()
    }
}