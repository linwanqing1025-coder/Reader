package io.lin.reader.ui

import androidx.compose.animation.togetherWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.lin.reader.navigation.NavKey
import io.lin.reader.navigation.NavigationItems
import io.lin.reader.navigation.getEnterAnimation
import io.lin.reader.navigation.getExitAnimation
import io.lin.reader.navigation.rememberSharedViewModelStoreNavEntryDecorator
import io.lin.reader.ui.feature.bookmark.BookmarkScreen
import io.lin.reader.ui.feature.favourite.FavouriteScreen
import io.lin.reader.ui.feature.history.HistoryScreen
import io.lin.reader.ui.feature.setting.SettingScreen
import io.lin.reader.ui.feature.setting.SettingScreenViewModel
import io.lin.reader.ui.feature.shelf.ShelfScreen
import kotlinx.serialization.json.Json

@Composable
fun MainTabsScreen(
    activeRootKey: NavKey.Root,
    onNavigate: (NavKey) -> Boolean,
    onNavigateUp: () -> Unit
) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val navSuiteType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
    val floatingNavigationBar = LocalAppViewModel.current.appPreferences.floatingNavigationBar

    // 子导航栈，仅用于管理 5 个 Tab 的切换
    val tabBackStackSaver = Saver<MutableList<NavKey.Root>, String>(
        save = { Json.encodeToString(it.toList()) },
        restore = { Json.decodeFromString<List<NavKey.Root>>(it).toMutableStateList() }
    )
    val tabBackStack = rememberSaveable(saver = tabBackStackSaver) {
        mutableStateListOf(activeRootKey)
    }

    // 当父级的 activeRootKey 变化时（例如通过某种方式外部跳转），同步子导航栈
    LaunchedEffect(activeRootKey) {
        if (tabBackStack.last() != activeRootKey) {
            tabBackStack.clear()
            tabBackStack.add(activeRootKey)
        }
    }

    val appContent: @Composable (() -> Unit) = {
        // 抓取外层的 ViewModelStoreOwner ，再传递给 RootTabNavDisplay
        val outerOwner = LocalViewModelStoreOwner.current!!
        RootTabNavDisplay(
            backStack = tabBackStack,
            onNavigate = onNavigate,
            onNavigateUp = onNavigateUp,
            sharedViewModelStoreOwner = outerOwner,
            navSuiteType = navSuiteType
        )
    }

    if (floatingNavigationBar) {
        return appContent()
    } else {
        return NavigationSuiteScaffold(
            layoutType = navSuiteType,
            navigationSuiteItems = {
                NavigationItems.entries.forEach { item ->
                    val key = item.key as NavKey.Root
                    item(
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = stringResource(item.contentDescription)
                            )
                        },
                        label = { Text(stringResource(item.label)) },
                        selected = tabBackStack.last() == key,
                        onClick = {
                            if (tabBackStack.last() != key) {
                                // 通知父级栈切换 Root，父级更新后会通过 activeRootKey 同步回子级
                                onNavigate(key)
                            }
                        }
                    )
                }
            }
        ) {
            appContent()
        }
    }
}

@Composable
private fun RootTabNavDisplay(
    backStack: List<NavKey>,
    onNavigateUp: () -> Unit,
    onNavigate: (NavKey) -> Boolean,
    sharedViewModelStoreOwner: ViewModelStoreOwner,
    navSuiteType: NavigationSuiteType
) {
    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberSharedViewModelStoreNavEntryDecorator()
        ),
        onBack = onNavigateUp
    ) { key: NavKey ->
        when (key) {
            // 书架
            NavKey.Root.Shelf -> NavEntry(
                key = key,
                metadata = NavDisplay.transitionSpec {
                    val enter = getEnterAnimation(navSuiteType)
                    val exit = getExitAnimation(navSuiteType)
                    enter togetherWith exit
                }
            ) {
                ShelfScreen(
                    entryVolumeReading = { onNavigate(NavKey.Reading(it.id)) }
                )
            }

            // 收藏
            NavKey.Root.Favourite -> NavEntry(
                key = key,
                metadata = NavDisplay.transitionSpec {
                    val enter = getEnterAnimation(navSuiteType)
                    val exit = getExitAnimation(navSuiteType)
                    enter togetherWith exit
                }
            ) {
                FavouriteScreen(
                    onVolumeClick = { onNavigate(NavKey.Reading(it.id)) }
                )
            }

            // 书签
            NavKey.Root.Bookmark -> NavEntry(
                key = key,
                metadata = NavDisplay.transitionSpec {
                    val enter = getEnterAnimation(navSuiteType)
                    val exit = getExitAnimation(navSuiteType)
                    enter togetherWith exit
                }
            ) {
                BookmarkScreen(
                    onBookmarkClick = { volumeId, pageNumber ->
                        onNavigate(NavKey.Reading(volumeId, pageNumber))
                    }
                )
            }

            // 历史记录
            NavKey.Root.History -> NavEntry(
                key = key,
                metadata = NavDisplay.transitionSpec {
                    val enter = getEnterAnimation(navSuiteType)
                    val exit = getExitAnimation(navSuiteType)
                    enter togetherWith exit
                }
            ) {
                HistoryScreen(
                    entryVolumeReading = { onNavigate(NavKey.Reading(it.id)) }
                )
            }

            // 设置
            NavKey.Root.Setting -> NavEntry(
                key = key,
                metadata = NavDisplay.transitionSpec {
                    val enter = getEnterAnimation(navSuiteType)
                    val exit = getExitAnimation(navSuiteType)
                    enter togetherWith exit
                }
            ) {
                // 直接使用从外层穿透进来的共享 Owner
                val viewModel = viewModel<SettingScreenViewModel>(
                    viewModelStoreOwner = sharedViewModelStoreOwner,
                    factory = ViewModelProvider.Factory
                )
                SettingScreen(
                    onNavigateToDetail = { onNavigate(it) },
                    viewModel = viewModel
                )
            }

            else -> NavEntry(key = key) { /* 不应该发生 */ }
        }
    }
}

// 辅助扩展方法
private fun <T> List<T>.toMutableStateList() =
    mutableStateListOf<T>().apply { addAll(this@toMutableStateList) }
