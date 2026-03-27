package io.lin.reader.ui.screen.setting.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.outlined.AddToPhotos
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.lin.reader.R
import io.lin.reader.data.preferences.SortPreference
import io.lin.reader.ui.screen.setting.SettingSnippet

enum class SeriesSortMethod {
    Name,
    CreateTime
}

enum class VolumeSortMethod {
    Name,
    CreateTime,
    LastReadTime
}

/**
 * 书签页面的书籍排序方式
 */
enum class BookmarkSortMethod {
    VolumeName,         // 按 Volume 名称
    LastReadTime,       // 按 Volume 上次阅读时间
    VolumeCreateTime,   // 按 Volume 创建时间
    LatestBookmarkTime  // 按最新书签的添加时间
}

@Preview
@Composable
fun SortDetails(
    modifier: Modifier = Modifier,
    seriesSortPreferences: SortPreference<SeriesSortMethod> = SortPreference(
        SeriesSortMethod.Name,
        true
    ),
    volumeSortPreferences: SortPreference<VolumeSortMethod> = SortPreference(
        VolumeSortMethod.Name,
        true
    ),
    bookmarkSortMethod: BookmarkSortMethod = BookmarkSortMethod.VolumeName,
    updateSeriesSortMethod: (SeriesSortMethod) -> Unit = {},
    updateSeriesOrder: (Boolean) -> Unit = {},
    updateVolumeSortMethod: (VolumeSortMethod) -> Unit = {},
    updateVolumeOrder: (Boolean) -> Unit = {},
    updateBookmarkSortMethod: (BookmarkSortMethod) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.inner_padding_of_container)),
        contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.inner_padding_of_container)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.inner_padding_of_container))
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.half_inner_padding_of_container))) {
                Text(
                    text = stringResource(R.string.sort_series_sort),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
                SettingSnippet {
                    Column(
                        modifier = Modifier.padding(
                            dimensionResource(R.dimen.inner_padding_of_container)
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            dimensionResource(R.dimen.half_inner_padding_of_container)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.sort_by),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
                            SeriesSortMethod.entries.forEachIndexed { index, method ->
                                val isSelected = seriesSortPreferences.sortMethod == method
                                SortOption(
                                    modifier = Modifier.weight(1f),
                                    text = when (method) {
                                        SeriesSortMethod.Name -> stringResource(R.string.series_sort_name)
                                        SeriesSortMethod.CreateTime -> stringResource(R.string.series_sort_create_time)
                                    },
                                    icon = when (method) {
                                        SeriesSortMethod.Name -> Icons.Default.SortByAlpha
                                        SeriesSortMethod.CreateTime -> Icons.Outlined.AddToPhotos
                                    },
                                    isSelected = isSelected,
                                    onClick = { if (!isSelected) updateSeriesSortMethod(method) }
                                )
                                if (index < SeriesSortMethod.entries.size - 1) {
                                    VerticalDivider(
                                        modifier = Modifier.height(32.dp),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.sort_order),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(50)
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SortOption(
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.sort_ascending),
                                icon = Icons.Default.ArrowUpward,
                                isSelected = seriesSortPreferences.isAscending,
                                onClick = { updateSeriesOrder(true) }
                            )
                            VerticalDivider(
                                modifier = Modifier.height(32.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            SortOption(
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.sort_descending),
                                icon = Icons.Default.ArrowDownward,
                                isSelected = !seriesSortPreferences.isAscending,
                                onClick = { updateSeriesOrder(false) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.half_inner_padding_of_container)
                )
            ) {
                Text(
                    text = stringResource(R.string.sort_volume_sort),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
                SettingSnippet {
                    Column(
                        modifier = Modifier.padding(
                            dimensionResource(R.dimen.inner_padding_of_container)
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            dimensionResource(R.dimen.half_inner_padding_of_container)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.sort_by),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(50)
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VolumeSortMethod.entries.forEachIndexed { index, method ->
                                val isSelected = volumeSortPreferences.sortMethod == method
                                SortOption(
                                    modifier = Modifier.weight(
                                        weight = when (method) {
                                            VolumeSortMethod.Name -> 4f
                                            VolumeSortMethod.CreateTime -> 5f
                                            VolumeSortMethod.LastReadTime -> 6f
                                        }
                                    ),
                                    text = when (method) {
                                        VolumeSortMethod.Name -> stringResource(R.string.volume_sort_name)
                                        VolumeSortMethod.CreateTime -> stringResource(R.string.volume_sort_create_time)
                                        VolumeSortMethod.LastReadTime -> stringResource(R.string.volume_sort_last_read_time)
                                    },
                                    icon = when (method) {
                                        VolumeSortMethod.Name -> Icons.Default.SortByAlpha
                                        VolumeSortMethod.CreateTime -> Icons.Outlined.AddToPhotos
                                        VolumeSortMethod.LastReadTime -> Icons.Default.History
                                    },
                                    isSelected = isSelected,
                                    onClick = { if (!isSelected) updateVolumeSortMethod(method) }
                                )
                                if (index < VolumeSortMethod.entries.size - 1) {
                                    VerticalDivider(
                                        modifier = Modifier.height(32.dp),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.sort_order),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(50)
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SortOption(
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.sort_ascending),
                                icon = Icons.Default.ArrowUpward,
                                isSelected = volumeSortPreferences.isAscending,
                                onClick = { updateVolumeOrder(true) }
                            )
                            VerticalDivider(
                                modifier = Modifier.height(32.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            SortOption(
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.sort_descending),
                                icon = Icons.Default.ArrowDownward,
                                isSelected = !volumeSortPreferences.isAscending,
                                onClick = { updateVolumeOrder(false) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.half_inner_padding_of_container))) {
                Text(
                    text = stringResource(R.string.sort_bookmark_sort),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
                SettingSnippet {
                    Column(
                        modifier = Modifier.padding(
                            dimensionResource(R.dimen.inner_padding_of_container)
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            dimensionResource(R.dimen.half_inner_padding_of_container)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.sort_by),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BookmarkSortMethod.entries.chunked(2).forEach { rowMethods ->
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
                                    rowMethods.forEachIndexed { index, method ->
                                        val isSelected = bookmarkSortMethod == method
                                        SortOption(
                                            modifier = Modifier.weight(1f),
                                            text = when (method) {
                                                BookmarkSortMethod.VolumeName -> stringResource(R.string.bookmark_sort_volume_name)
                                                BookmarkSortMethod.LastReadTime -> stringResource(R.string.bookmark_sort_last_read_time)
                                                BookmarkSortMethod.VolumeCreateTime -> stringResource(
                                                    R.string.bookmark_sort_volume_create_time
                                                )

                                                BookmarkSortMethod.LatestBookmarkTime -> stringResource(
                                                    R.string.bookmark_sort_latest_bookmark_time
                                                )
                                            },
                                            icon = when (method) {
                                                BookmarkSortMethod.VolumeName -> Icons.Default.SortByAlpha
                                                BookmarkSortMethod.LastReadTime -> Icons.Default.History
                                                BookmarkSortMethod.VolumeCreateTime -> Icons.Outlined.AddToPhotos
                                                BookmarkSortMethod.LatestBookmarkTime -> Icons.Default.BookmarkBorder
                                            },
                                            isSelected = isSelected,
                                            onClick = {
                                                if (!isSelected) updateBookmarkSortMethod(
                                                    method
                                                )
                                            }
                                        )
                                        if (index < rowMethods.size - 1) {
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
            }
        }
    }
}

@Composable
private fun SortOption(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor =
        if (isSelected) MaterialTheme.colorScheme.secondaryContainer
        else Color.Transparent
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .clickable(
                onClick = {
                    if (!isSelected) onClick()
                }
            )
            .background(backgroundColor)
            .padding(
                dimensionResource(R.dimen.half_inner_padding_of_container)
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.padding(2.dp))
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
