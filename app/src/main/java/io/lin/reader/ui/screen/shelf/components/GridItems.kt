package io.lin.reader.ui.screen.shelf.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import io.lin.reader.R
import io.lin.reader.ui.components.cover.VolumeCover
import io.lin.reader.ui.components.menu.StyledMenu
import io.lin.reader.ui.components.menu.StyledMenuIcon
import io.lin.reader.ui.components.menu.StyledMenuItem


@Preview(showBackground = true)
@Composable
fun SeriesActionPopupPreview() {
    MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(300.dp, 300.dp)
            .padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Box {
            SeriesActionPopup(
                expanded = true,
                onDismissRequest = {},
                offset = DpOffset(0.dp, 0.dp),
                onEdit = {},
                onDelete = {},
            )
        }
    }
}

@Composable
private fun SeriesActionPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    StyledMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        properties = PopupProperties(clippingEnabled = true),
    ) {
        StyledMenuItem(
            text = stringResource(R.string.shelf_menu_edit_name),
            onClick = {
                onEdit()
                onDismissRequest()
            },
            leadingIcon = {
                StyledMenuIcon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.shelf_menu_edit_name)
                )
            }
        )
        StyledMenuItem(
            text = stringResource(R.string.shelf_menu_delete),
            onClick = {
                onDelete()
                onDismissRequest()
            },
            leadingIcon = {
                StyledMenuIcon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.shelf_menu_delete)
                )
            }
        )
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Preview
@Composable
fun SeriesBox(
    seriesName: String = "Default Series Name",
    volumeCount: Int = 3,
    coverUriList: List<String?> = listOf(null, null, null),
    enterSeries: () -> Unit = {},
    editSeriesName: () -> Unit = {},
    deleteSeries: () -> Unit = {},
) {
    if (volumeCount == 0) {
        return
    }

    var showMenu by remember { mutableStateOf(false) }
    var itemHeight by remember { mutableIntStateOf(0) }
    var itemPosition by remember { mutableStateOf(Offset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }

    // 确定显示的封面数量：取 coverUriList 实际大小（最多3个），若为空则至少显示 1 个以展示占位图
    val displayCount = coverUriList.size.coerceIn(1, 3)

    val bookWidth = 100.dp
    val bookHeight = 140.dp
    val offsetStep = 4.dp
    val halfOffsetStep = 2.dp

    Column(
        modifier = Modifier.padding(
            top = dimensionResource(R.dimen.inner_padding_of_container),
            start = dimensionResource(R.dimen.padding_unit),
            end = dimensionResource(R.dimen.padding_unit)
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = bookWidth + offsetStep * 2,
                    height = bookHeight + offsetStep * 2
                )
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = enterSeries,
                    onLongClick = { showMenu = true }
                )
        ) {
            val verticalStartPoint = halfOffsetStep * (3 - displayCount)
            val horizontalStartPoint = offsetStep + halfOffsetStep * (displayCount - 1)
            for (i in 0 until displayCount) {
                val currentOffset = offsetStep * i
                VolumeCover(
                    width = bookWidth,
                    height = bookHeight,
                    coverUri = coverUriList.getOrNull(displayCount - 1 - i),
                    modifier = Modifier.offset(
                        x = horizontalStartPoint - currentOffset,
                        y = verticalStartPoint + currentOffset
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = seriesName,
            modifier = Modifier.width(bookWidth + offsetStep * 2),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )

        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
        val itemHeightDp = with(density) { itemHeight.toDp() }
        val showAbove = (itemPosition.y + itemHeight / 2) > (screenHeightPx / 2)

        SeriesActionPopup(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(0.dp, if (showAbove) -itemHeightDp else 0.dp),
            onEdit = editSeriesName,
            onDelete = deleteSeries
        )
    }
}

/**
 * 可点击、可选中的单本书籍图标（MD3 重塑版，带阴影）。
 */
@Preview
@Composable
fun VolumeBox(
    volumeName: String = "Default Volume Name",
    isSelected: Boolean = false,
    isSelectingVolume: Boolean = false,
    coverUri: String? = null,
    changeSelectedState: () -> Unit = {},
    enterVolume: () -> Unit = {},
    activateSelecting: () -> Unit = {},
) {
    val bookWidth = 100.dp
    val bookHeight = 140.dp
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = Modifier.padding(
            top = dimensionResource(R.dimen.inner_padding_of_container),
            start = dimensionResource(R.dimen.padding_unit),
            end = dimensionResource(R.dimen.padding_unit)
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(bookWidth, bookHeight)
                // 添加阴影：放在 clip 之前以确保阴影不被自身裁切
                .shadow(elevation = 4.dp, shape = shape)
                .background(MaterialTheme.colorScheme.surface, shape)
                .clip(shape)
                .combinedClickable(
                    onClick = {
                        if (isSelectingVolume) changeSelectedState()
                        else enterVolume()
                    },
                    onLongClick = {
                        if (!isSelectingVolume) {
                            activateSelecting()
                            changeSelectedState()
                        }
                    }
                )
                .then(
                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                    else Modifier
                )
        ) {
            VolumeCover(
                coverUri = coverUri,
                width = bookWidth,
                height = bookHeight
            )

            if (isSelectingVolume) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Black.copy(alpha = 0.2f)
                        )
                )

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                            .zIndex(2f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = volumeName,
            modifier = Modifier.width(bookWidth),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
    }
}
