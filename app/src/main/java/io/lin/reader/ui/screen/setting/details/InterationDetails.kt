package io.lin.reader.ui.screen.setting.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.lin.reader.R
import io.lin.reader.ui.screen.setting.SettingSnippet
import io.lin.reader.utils.InteractionStyle
import io.lin.reader.utils.InteractionStyle0Preview
import io.lin.reader.utils.InteractionStyle1Preview
import io.lin.reader.utils.InteractionStyle1RtlPreview
import io.lin.reader.utils.InteractionStyle2Preview
import io.lin.reader.utils.InteractionStyle2RtlPreview
import io.lin.reader.utils.InteractionStyle3Preview
import io.lin.reader.utils.InteractionStyle3RtlPreview

@Preview(showBackground = true)
@Composable
fun InteractionDetails(
    modifier: Modifier = Modifier,
    interactionStyle: InteractionStyle = InteractionStyle.Style0,
    rtlMode: Boolean = false,
    updateInteractionStyle: (InteractionStyle) -> Unit = {}
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
                text = stringResource(R.string.interaction_label),
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
                    modifier = Modifier.padding(
                        vertical = dimensionResource(R.dimen.inner_padding_of_container)
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.half_inner_padding_of_container)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.setting_interaction_style),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.inner_padding_of_container))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ColorIndicator(
                        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.inner_padding_of_container))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.inner_padding_of_container)),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(InteractionStyle.values()) { style ->
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
        Box(
            modifier = Modifier
                .width(previewWidth)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(16.dp)
                )
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

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = Color.Transparent,
                            shape = CircleShape
                        )
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