package io.lin.reader.ui.screens.bookmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import io.lin.reader.R
import io.lin.reader.data.database.Bookmark
import io.lin.reader.data.database.VolumeWithBookmarks
import io.lin.reader.data.preferences.BookmarkSortMethod
import io.lin.reader.ui.ViewModelProvider
import io.lin.reader.ui.components.blankscreen.BlankScreenContent
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.menu.StyledDropdownMenu
import io.lin.reader.ui.components.menu.StyledDropdownMenuItem
import io.lin.reader.ui.components.snackbar.StyledSnackbarHost
import kotlinx.coroutines.launch

@Composable
fun BookmarkScreen(
    modifier: Modifier = Modifier,
    viewModel: BookmarkScreenViewModel = viewModel(factory = ViewModelProvider.Factory),
    onBookmarkClick: (Long, Int) -> Unit, // 跳转到书籍的特定页
) {
    val uiState by viewModel.uiState.collectAsState()
    val bookmarkPreferences = viewModel.bookmarkPreferences

    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { StyledSnackbarHost(snackbarHostState) },
        topBar = {
            BookmarkScreenTopBar(
                scrollBehavior = scrollBehavior,
                sortMethod = uiState.sortMethod,
                updateSortMethod = { bookmarkPreferences.updateBookmarkSortMethod(it) },
                clearAllBookmarks = {
                    val appContext = context.applicationContext
                    if (uiState.volumesWithBookmarks.isEmpty()) {
                        val text = appContext.getString(R.string.bookmark_no_bookmark_to_clear)
                        scope.launch {
                            snackbarHostState.showSnackbar(text)
                        }
                    } else viewModel.clearAllBookmarks(
                        onResult = { count ->
                            val text =
                                appContext.getString(R.string.bookmark_bookmark_cleared, count)
                            scope.launch {
                                snackbarHostState.showSnackbar(text)
                            }
                        }
                    )
                }
            )
        },
    ) { innerPadding ->
        BookmarkScreenContent(
            modifier = Modifier.padding(innerPadding),
            volumeWithBookmarks = uiState.volumesWithBookmarks,
            onBookmarkClick = { bookmark ->
                onBookmarkClick(bookmark.volumeId, bookmark.history.pageIndex ?: 0) // TODO: 针对重排文档书签跳转的适配
            },
            onBookmarkDelete = { bookmark ->
                viewModel.deleteBookmark(bookmark)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkScreenTopBar(
    modifier: Modifier = Modifier,
    sortMethod: BookmarkSortMethod,
    updateSortMethod: (BookmarkSortMethod) -> Unit,
    clearAllBookmarks: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val isClearingAllBookmarks = remember { mutableStateOf(false) }

    val sortIconStr = stringResource(R.string.sort_bookmark_sort)
    val clearStr = stringResource(R.string.bookmark_top_bar_clear_bookmark)
    val onClearClick = { isClearingAllBookmarks.value = true }

    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                stringResource(R.string.navigation_label_bookmark),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            // Sort
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Below
                    ),
                tooltip = {
                    PlainTooltip(
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                            paneTitle = sortIconStr
                        }
                    ) {
                        Text(sortIconStr)
                    }
                },
                state = rememberTooltipState(),
            )
            {
                var isMainMenuExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { isMainMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = sortIconStr,
                    )
                }

                StyledDropdownMenu(
                    expanded = isMainMenuExpanded,
                    onDismissRequest = { isMainMenuExpanded = false },
                ) {
                    BookmarkSortMethod.entries.forEach { method ->
                        StyledDropdownMenuItem(
                            text = {
                                Text(
                                    when (method) {
                                        BookmarkSortMethod.VolumeName -> stringResource(R.string.bookmark_sort_volume_name)
                                        BookmarkSortMethod.LastReadTime -> stringResource(R.string.bookmark_sort_last_read_time)
                                        BookmarkSortMethod.VolumeCreateTime -> stringResource(R.string.bookmark_sort_volume_create_time)
                                        BookmarkSortMethod.LatestBookmarkTime -> stringResource(R.string.bookmark_sort_latest_bookmark_time)
                                    }
                                )
                            },
                            onClick = { updateSortMethod(method) },
                            leadingIcon = if (sortMethod == method) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        stringResource(R.string.shelf_top_bar_sort_selected)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            // Clear All Bookmarks
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Below
                    ),
                tooltip = {
                    PlainTooltip(
                        modifier =
                            Modifier.semantics {
                                liveRegion = LiveRegionMode.Assertive
                                paneTitle = clearStr
                            }
                    ) {
                        Text(clearStr)
                    }
                },
                state = rememberTooltipState(),
            ) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = clearStr,
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior
    )

    if (isClearingAllBookmarks.value) {
        val notificationText = stringResource(R.string.bookmark_clear_bookmark_notification)
        NotificationDialog(
            title = stringResource(R.string.bookmark_dialog_title_clear),
            notification = notificationText,
            onDismiss = { isClearingAllBookmarks.value = false },
            onConfirm = {
                clearAllBookmarks()
                isClearingAllBookmarks.value = false
            }
        )
    }
}

@Composable
private fun BookmarkScreenContent(
    modifier: Modifier = Modifier,
    volumeWithBookmarks: List<VolumeWithBookmarks>,
    onBookmarkClick: (Bookmark) -> Unit,
    onBookmarkDelete: (Bookmark) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (volumeWithBookmarks.isEmpty()) {
            BlankScreenContent(Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(R.dimen.inner_padding_of_container)),
                contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.inner_padding_of_container)),
            ) {
                items(
                    items = volumeWithBookmarks,
                    key = { it.volume.id } // 使用 volume id 作为 key 优化性能
                ) { item ->
                    VolumeBookmarksGroup(
                        volumeWithBookmarks = item,
                        onBookmarkClick = onBookmarkClick,
                        onBookmarkDelete = onBookmarkDelete
                    )
                }
            }
        }
    }
}