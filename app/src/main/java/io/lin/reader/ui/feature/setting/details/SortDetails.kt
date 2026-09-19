package io.lin.reader.ui.feature.setting.details

import  androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.outlined.AddToPhotos
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lin.reader.R
import io.lin.reader.data.preferences.BookmarkPreferencesInterface
import io.lin.reader.data.preferences.BookmarkSortMethod
import io.lin.reader.data.preferences.SeriesSortMethod
import io.lin.reader.data.preferences.ShelfPreferencesInterface
import io.lin.reader.data.preferences.VolumeSortMethod
import io.lin.reader.ui.components.screenbar.DynamicTopAppBar
import io.lin.reader.ui.feature.setting.SettingList
import io.lin.reader.ui.theme.ReaderTheme

@Composable
fun SortDetailsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    shelfPreferences: ShelfPreferencesInterface,
    bookmarkPreferences: BookmarkPreferencesInterface
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
                title = stringResource(R.string.setting_sort),
                scrollFraction = scrollFraction,
                onNavigationClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        SortDetails(
            modifier = Modifier.padding(innerPadding),
            shelfPreferences = shelfPreferences,
            bookmarkPreferences = bookmarkPreferences,
            listState = listState
        )
    }
}

@Composable
fun SortDetails(
    modifier: Modifier = Modifier,
    shelfPreferences: ShelfPreferencesInterface,
    bookmarkPreferences: BookmarkPreferencesInterface,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState()
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
                title = { Text(stringResource(R.string.sort_series_sort)) }
            ) {
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
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SeriesSortMethod.entries.forEachIndexed { index, method ->
                            val isSelected = shelfPreferences.seriesSortMethod == method
                            SegmentedButton(
                                modifier = Modifier.weight(1f),
                                selected = isSelected,
                                onClick = { shelfPreferences.updateSeriesSortMethod(method) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = SeriesSortMethod.entries.size
                                ),
                                icon = {
                                    SegmentedButtonDefaults.Icon(
                                        active = isSelected,
                                        activeContent = {
                                            Icon(
                                                imageVector = when (method) {
                                                    SeriesSortMethod.Name -> Icons.Default.SortByAlpha
                                                    SeriesSortMethod.CreateTime -> Icons.Outlined.AddToPhotos
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                },
                                label = {
                                    Text(
                                        text = when (method) {
                                            SeriesSortMethod.Name -> stringResource(R.string.series_sort_name)
                                            SeriesSortMethod.CreateTime -> stringResource(R.string.series_sort_create_time)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = 0.5f
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.sort_order),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val options = listOf(true, false)
                        options.forEachIndexed { index, isAscending ->
                            val isSelected = shelfPreferences.seriesSortAscending == isAscending
                            SegmentedButton(
                                modifier = Modifier.weight(1f),
                                selected = isSelected,
                                onClick = {
                                    if (shelfPreferences.seriesSortAscending != isAscending) {
                                        shelfPreferences.toggleSeriesSortAscending()
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                                ),
                                icon = {
                                    SegmentedButtonDefaults.Icon(
                                        active = isSelected,
                                        activeContent = {
                                            Icon(
                                                imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                },
                                label = {
                                    Text(
                                        text = if (isAscending) stringResource(R.string.sort_ascending) else stringResource(
                                            R.string.sort_descending
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
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
                title = { Text(stringResource(R.string.sort_volume_sort)) }
            ) {
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
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VolumeSortMethod.entries.forEachIndexed { index, method ->
                            val isSelected = shelfPreferences.volumeSortMethod == method
                            SegmentedButton(
                                modifier = Modifier.weight(1f),
                                selected = isSelected,
                                onClick = { shelfPreferences.updateVolumeSortMethod(method) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = VolumeSortMethod.entries.size
                                ),
                                icon = {
                                    SegmentedButtonDefaults.Icon(
                                        active = isSelected,
                                        activeContent = {
                                            Icon(
                                                imageVector = when (method) {
                                                    VolumeSortMethod.Name -> Icons.Default.SortByAlpha
                                                    VolumeSortMethod.CreateTime -> Icons.Outlined.AddToPhotos
                                                    VolumeSortMethod.LastReadTime -> Icons.Default.History
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                },
                                label = {
                                    Text(
                                        text = when (method) {
                                            VolumeSortMethod.Name -> stringResource(R.string.volume_sort_name)
                                            VolumeSortMethod.CreateTime -> stringResource(R.string.volume_sort_create_time)
                                            VolumeSortMethod.LastReadTime -> stringResource(R.string.volume_sort_last_read_time)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = 0.5f
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.sort_order),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val options = listOf(true, false)
                        options.forEachIndexed { index, isAscending ->
                            val isSelected = shelfPreferences.volumeSortAscending == isAscending
                            SegmentedButton(
                                modifier = Modifier.weight(1f),
                                selected = isSelected,
                                onClick = {
                                    if (shelfPreferences.volumeSortAscending != isAscending) {
                                        shelfPreferences.toggleVolumeSortAscending()
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                                ),
                                icon = {
                                    SegmentedButtonDefaults.Icon(
                                        active = isSelected,
                                        activeContent = {
                                            Icon(
                                                imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                },
                                label = {
                                    Text(
                                        text = if (isAscending) stringResource(R.string.sort_ascending) else stringResource(
                                            R.string.sort_descending
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
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
                title = { Text(stringResource(R.string.sort_bookmark_sort)) }
            ) {
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
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        BookmarkSortMethod.entries.forEachIndexed { index, method ->
                            val isSelected = bookmarkPreferences.bookmarkSortMethod == method
                            SegmentedButton(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                selected = isSelected,
                                onClick = { bookmarkPreferences.updateBookmarkSortMethod(method) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = BookmarkSortMethod.entries.size
                                ),
                                icon = {},
                                label = {
                                    Text(
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
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 12.sp
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SortDetailsScreenPreview() {
    ReaderTheme {
        SortDetailsScreen(
            shelfPreferences = io.lin.reader.data.preferences.ShelfPreferencesForTest(),
            bookmarkPreferences = io.lin.reader.data.preferences.BookmarkPreferencesForTest()
        )
    }
}