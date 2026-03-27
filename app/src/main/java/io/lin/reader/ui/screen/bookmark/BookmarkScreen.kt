package io.lin.reader.ui.screen.bookmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.lin.reader.R
import io.lin.reader.data.database.Bookmark
import io.lin.reader.data.database.VolumeWithBookmarks
import io.lin.reader.navigation.NavigationDestination
import io.lin.reader.navigation.ScreenRoute
import io.lin.reader.ui.components.blankscreen.BlankScreenContent
import io.lin.reader.ui.components.dialog.NotificationDialog
import io.lin.reader.ui.components.menu.StyledMenu
import io.lin.reader.ui.components.menu.StyledMenuIcon
import io.lin.reader.ui.components.menu.StyledMenuItem
import io.lin.reader.ui.components.screenbar.StyledBarIconButton
import io.lin.reader.ui.components.screenbar.StyledScreenBar
import io.lin.reader.ui.components.snackbar.StyledSnackbarHost
import io.lin.reader.ui.screen.setting.details.BookmarkSortMethod
import io.lin.reader.ui.viewmodel.AppViewModelProvider
import io.lin.reader.ui.viewmodel.BookmarkScreenViewModel
import kotlinx.coroutines.launch

object BookmarkScreenDestination : NavigationDestination {
    override val route = ScreenRoute.BookmarkScreen.name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(
    modifier: Modifier = Modifier,
    viewModel: BookmarkScreenViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onBookmarkClick: (Long, Int) -> Unit, // 跳转到书籍的特定页
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { StyledSnackbarHost(snackbarHostState) },
        topBar = {
            BookmarkScreenTopBar(
                scrollBehavior = scrollBehavior,
                sortPreference = uiState.sortMethod,
                onSortMethodChange = { viewModel.updateSortMethod(it) },
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
                onBookmarkClick(bookmark.volumeId, bookmark.pageNumber)
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
    scrollBehavior: TopAppBarScrollBehavior? = null,
    sortPreference: BookmarkSortMethod,
    onSortMethodChange: (BookmarkSortMethod) -> Unit,
    clearAllBookmarks: () -> Unit,
) {
    val isClearingAllBookmarks = remember { mutableStateOf(false) }
    StyledScreenBar(
        modifier = modifier,
        title = stringResource(R.string.navigation_label_bookmark),
        iconButtonTools = {
            Box {
                var sortSubMenuExpanded by remember { mutableStateOf(false) }
                StyledBarIconButton(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(R.string.sort_bookmark_sort),
                    onClick = { sortSubMenuExpanded = true }
                )
                StyledMenu(
                    expanded = sortSubMenuExpanded,
                    onDismissRequest = { sortSubMenuExpanded = false }
                ) {
                    BookmarkSortMethod.entries.forEachIndexed { index, method ->
                        StyledMenuItem(
                            text = when (method) {
                                BookmarkSortMethod.VolumeName -> stringResource(R.string.bookmark_sort_volume_name)
                                BookmarkSortMethod.LastReadTime -> stringResource(R.string.bookmark_sort_last_read_time)
                                BookmarkSortMethod.VolumeCreateTime -> stringResource(R.string.bookmark_sort_volume_create_time)
                                BookmarkSortMethod.LatestBookmarkTime -> stringResource(R.string.bookmark_sort_latest_bookmark_time)
                            },
                            onClick = {
                                onSortMethodChange(method)
                                sortSubMenuExpanded = false
                            },
                            leadingIcon = {
                                if (sortPreference == method) {
                                    StyledMenuIcon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.bookmark_top_bar_sort_selected)
                                    )
                                } else {
                                    Spacer(Modifier.size(22.dp))
                                }
                            }
                        )
                    }
                }
            }
            StyledBarIconButton(
                imageVector = Icons.Filled.DeleteSweep,
                contentDescription = stringResource(R.string.bookmark_top_bar_clear_bookmark),
                onClick = { isClearingAllBookmarks.value = true },
            )
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