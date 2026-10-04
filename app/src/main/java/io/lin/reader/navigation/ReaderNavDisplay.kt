package io.lin.reader.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.lin.reader.ui.ViewModelProvider
import io.lin.reader.ui.screens.bookmark.BookmarkScreen
import io.lin.reader.ui.screens.favourite.FavouriteScreen
import io.lin.reader.ui.screens.history.HistoryScreen
import io.lin.reader.ui.screens.reading.ReadingScreen
import io.lin.reader.ui.screens.setting.SettingScreenViewModel
import io.lin.reader.ui.screens.setting.details.InteractionDetailsScreen
import io.lin.reader.ui.screens.setting.details.ReflowDetailsScreen
import io.lin.reader.ui.screens.setting.SettingScreen
import io.lin.reader.ui.screens.setting.details.AppAppearanceDetailsScreen
import io.lin.reader.ui.screens.setting.details.OtherReadingSettingDetailsScreen
import io.lin.reader.ui.screens.setting.details.ReaderColorDetailsScreen
import io.lin.reader.ui.screens.setting.details.ReadingModeDetailsScreen
import io.lin.reader.ui.screens.setting.details.SortDetailsScreen
import io.lin.reader.ui.screens.shelf.SeriesDetailScreen
import io.lin.reader.ui.screens.shelf.ShelfScreen

const val NAV_KEY_METADATA = "io.lin.reader.navigation.NAV_KEY"

/**
 * 将 NavKey 附加到 metadata 的辅助扩展函数
 */
private fun navKeyMetadata(key: NavKey): Map<String, Any> = mapOf(NAV_KEY_METADATA to key)

/**
 * 构建同时包含 NavKey、前进动画 (transitionSpec)、返回动画 (popTransitionSpec) 和 预测性返回 (predictivePopTransitionSpec) 的 metadata
 */
private fun navTransitionMetadata(
    key: NavKey,
    navSuiteType: NavigationSuiteType
): Map<String, Any> = navKeyMetadata(key) +
        NavDisplay.transitionSpec { calculateNavPushTransition(navSuiteType) } +
        NavDisplay.popTransitionSpec { calculateNavPopTransition(navSuiteType) } +
        NavDisplay.predictivePopTransitionSpec { calculateNavPopTransition(navSuiteType) }

@Composable
fun ReaderNavDisplay(
    backStack: List<NavKey>,
    navSuiteType: NavigationSuiteType,
    onNavigateUp: () -> Unit,
    onNavigate: (NavKey) -> Boolean,
    predictiveBackEnabled: Boolean = false // TODO: 预测性返回手势开关
) {
    val canPop = backStack.size > 1

    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberSharedViewModelStoreNavEntryDecorator()
        ),
        onBack = onNavigateUp,
        transitionSpec = { calculateNavPushTransition(navSuiteType) },
        popTransitionSpec = { calculateNavPopTransition(navSuiteType) },
        predictivePopTransitionSpec = { calculateNavPopTransition(navSuiteType) }
    ) { key: NavKey ->

        val transitionMetadata = navTransitionMetadata(key, navSuiteType)

        fun createEntry(
            extraMetadata: Map<String, Any> = emptyMap(),
            content: @Composable () -> Unit
        ): NavEntry<NavKey> {
            return NavEntry(
                key = key,
                metadata = transitionMetadata + extraMetadata
            ) {
                // 当开启关闭预测性返回手势时，拦截系统侧滑缩放，直接触发 onNavigateUp 播放你的平滑退场动画
                BackHandler(enabled = !predictiveBackEnabled && canPop) {
                    onNavigateUp()
                }
                content()
            }
        }

        when (key) {
            // 书架
            NavKey.Shelf -> createEntry {
                ShelfScreen(onNavigate = { onNavigate(it) })
            }

            // 收藏
            NavKey.Favourite -> createEntry {
                FavouriteScreen(onVolumeClick = { onNavigate(NavKey.Reading(it.id)) })
            }

            // 书签
            NavKey.Bookmark -> createEntry {
                BookmarkScreen(
                    onBookmarkClick = { volumeId, pageNumber ->
                        onNavigate(NavKey.Reading(volumeId, pageNumber))
                    }
                )
            }

            // 历史记录
            NavKey.History -> createEntry {
                HistoryScreen(entryVolumeReading = { onNavigate(NavKey.Reading(it.id)) })
            }

            // 设置主页
            NavKey.Setting -> createEntry {
                SettingScreen(onNavigateToDetail = { onNavigate(it) })
            }

            // 设置二级详情页
            is NavKey.SettingDetails -> createEntry(
                extraMetadata = SharedViewModelStoreNavEntryDecorator.parent(NavKey.Setting.toString())
            ) {
                val parentViewModel = viewModel<SettingScreenViewModel>(
                    viewModelStoreOwner = LocalSharedViewModelStoreOwner.current,
                    factory = ViewModelProvider.Factory
                )
                when (key) {
                    NavKey.AppAppearance -> AppAppearanceDetailsScreen(
                        onNavigateBack = onNavigateUp,
                        appPreferences = parentViewModel.appPreferences
                    )

                    NavKey.Sort -> SortDetailsScreen(
                        onNavigateBack = onNavigateUp,
                        shelfPreferences = parentViewModel.shelfPreferences,
                        bookmarkPreferences = parentViewModel.bookmarkPreferences
                    )

                    NavKey.ReaderColor -> ReaderColorDetailsScreen(
                        onNavigateBack = onNavigateUp,
                        readerColor = parentViewModel.readingPreferences.readerColor,
                        onPageColorChange = {
                            parentViewModel.readingPreferences.updatePageColor(it)
                        },
                        onBackgroundColorChange = {
                            parentViewModel.readingPreferences.updateBackgroundColor(it)
                        },
                        onFilterColorChange = {
                            parentViewModel.readingPreferences.updateFilterColor(it)
                        }
                    )

                    NavKey.ReadingMode -> ReadingModeDetailsScreen(
                        onNavigateBack = onNavigateUp,
                        readerPreferences = parentViewModel.readingPreferences
                    )

                    NavKey.Interaction -> InteractionDetailsScreen(
                        onNavigateBack = onNavigateUp,
                        interactionStyle = parentViewModel.readingPreferences.interactionStyle,
                        rtlMode = parentViewModel.readingPreferences.rtlMode,
                        updateInteractionStyle = parentViewModel.readingPreferences::updateInteractionStyle
                    )

                    NavKey.Reflow -> ReflowDetailsScreen(
                        onNavigateBack = onNavigateUp,
                        readerPreferences = parentViewModel.readingPreferences,
                        reflowPreferences = parentViewModel.reflowPreferences
                    )

                    NavKey.OtherReading -> OtherReadingSettingDetailsScreen(
                        onNavigateBack = onNavigateUp,
                        readerPreferences = parentViewModel.readingPreferences
                    )
                }
            }

            // 阅读界面
            is NavKey.Reading -> createEntry {
                ReadingScreen(
                    bookId = key.bookId,
                    pageNumber = key.pageNumber,
                    onNavigateUp = onNavigateUp
                )
            }

            // 系列详情页
            is NavKey.SeriesDetail -> createEntry {
                SeriesDetailScreen(
                    seriesId = key.seriesId,
                    onNavBack = onNavigateUp,
                    entryVolumeReading = { onNavigate(NavKey.Reading(it.id)) }
                )
            }
        }
    }
}
