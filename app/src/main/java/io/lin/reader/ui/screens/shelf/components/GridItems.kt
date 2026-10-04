package io.lin.reader.ui.screens.shelf.components

import android.annotation.SuppressLint
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import io.lin.reader.R
import io.lin.reader.data.database.Volume
import io.lin.reader.ui.components.cover.VolumeCover
import io.lin.reader.ui.components.menu.StyledDropdownMenu
import io.lin.reader.ui.components.menu.StyledDropdownMenuItem
import io.lin.reader.ui.components.selection.SelectableBox

@Composable
private fun SeriesActionPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    StyledDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        properties = PopupProperties(clippingEnabled = true),
    ) {
        StyledDropdownMenuItem(
            text = { Text(stringResource(R.string.shelf_menu_edit_name)) },
            onClick = {
                onEdit()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.shelf_menu_edit_name)
                )
            }
        )
        StyledDropdownMenuItem(
            text = { Text(stringResource(R.string.shelf_menu_delete)) },
            onClick = {
                onDelete()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.shelf_menu_delete)
                )
            }
        )
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun SeriesBox(
    seriesName: String = "Default Series Name",
    volumeCount: Int = 3,
    volumes: List<Volume> = emptyList(), // 接受 Volume 列表
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

    // 确定显示的封面数量：取 volumes 实际大小（最多3个），若为空则至少显示 1 个以展示占位图
    val displayCount = volumes.size.coerceIn(1, 3)

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
                val volume = volumes.getOrNull(displayCount - 1 - i)
                if (volume != null) {
                    VolumeCover(
                        volume = volume,
                        width = bookWidth,
                        height = bookHeight,
                        modifier = Modifier.offset(
                            x = horizontalStartPoint - currentOffset,
                            y = verticalStartPoint + currentOffset
                        )
                    )
                }
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
 *
 * selected == true : 单击响应 onSelectedChange;
 *
 * selected == false : 单击响应 onClick.
 */
@Composable
fun VolumeBox(
    volume: Volume,
    isSelected: Boolean = false,
    isSelectingVolume: Boolean = false,
    onSelectedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val volumeName = volume.volumeName
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
        SelectableBox(
            modifier = Modifier.size(width = bookWidth, height = bookHeight),
            selected = isSelected,
            onSelectedChange = onSelectedChange,
            isSelectionMode = isSelectingVolume,
            onClick = onClick,
            onLongClick = onLongClick,
            shape = shape
        ) {
            VolumeCover(
                volume = volume,
                width = bookWidth,
                height = bookHeight,
            )
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