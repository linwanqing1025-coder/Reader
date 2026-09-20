package io.lin.reader.navigation

import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.lin.reader.ui.MainTabsScreen
import io.lin.reader.ui.maintab.reading.ReadingScreen
import io.lin.reader.ui.maintab.setting.SettingScreenViewModel
import io.lin.reader.ui.screens.shelf.SeriesDetailScreen
import io.lin.reader.ui.maintab.setting.details.AppAppearanceDetailsScreen
import io.lin.reader.ui.maintab.setting.details.InteractionDetailsScreen
import io.lin.reader.ui.maintab.setting.details.OtherReadingSettingDetailsScreen
import io.lin.reader.ui.maintab.setting.details.ReaderColorDetailsScreen
import io.lin.reader.ui.maintab.setting.details.ReadingModeDetailsScreen
import io.lin.reader.ui.maintab.setting.details.ReflowDetailsScreen
import io.lin.reader.ui.maintab.setting.details.SortDetailsScreen

@Composable
fun ReaderNavDisplay(
    backStack: List<NavKey>,
    onNavigateUp: () -> Unit,
    onNavigate: (NavKey) -> Boolean
) {
    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberSharedViewModelStoreNavEntryDecorator()
        ),
        onBack = onNavigateUp
    ) { key: NavKey ->
        when(key){
            is NavKey.Root -> NavEntry(
                key = key,
                contentKey = key.toString(),
                metadata = NavDisplay.transitionSpec {
                    val enter = getEnterAnimation()
                    val exit = getExitAnimation()
                    enter togetherWith exit
                }
            ) {
                MainTabsScreen(
                    activeRootKey = key,
                    onNavigate = onNavigate,
                    onNavigateUp = onNavigateUp
                )
            }

            is NavKey.SettingDetails -> NavEntry(
                key = key,
                metadata = NavDisplay.transitionSpec {
                    val enter = getEnterAnimation()
                    val exit = getExitAnimation()
                    enter togetherWith exit
                } + SharedViewModelStoreNavEntryDecorator.parent(NavKey.Setting.toString())
            ) {
                val parentViewModel = viewModel<SettingScreenViewModel>(
                    viewModelStoreOwner = LocalSharedViewModelStoreOwner.current
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
                            parentViewModel.readingPreferences.updatePageColor(
                                it
                            )
                        },
                        onBackgroundColorChange = {
                            parentViewModel.readingPreferences.updateBackgroundColor(
                                it
                            )
                        },
                        onFilterColorChange = {
                            parentViewModel.readingPreferences.updateFilterColor(
                                it
                            )
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

            is NavKey.Reading -> NavEntry(
                key = key,
                metadata = NavDisplay.transitionSpec {
                    val enter = getEnterAnimation()
                    val exit = getExitAnimation()
                    enter togetherWith exit
                }
            ) {
                ReadingScreen(
                    bookId = key.bookId,
                    pageNumber = key.pageNumber,
                    onNavigateUp = onNavigateUp
                )
            }

            is NavKey.SeriesDetail -> NavEntry(
                key = key,
                metadata = NavDisplay.transitionSpec {
                    val enter = getEnterAnimation()
                    val exit = getExitAnimation()
                    enter togetherWith exit
                }
            ) {
                SeriesDetailScreen(
                    seriesId = key.seriesId,
                    onNavBack = onNavigateUp,
                    entryVolumeReading = { onNavigate(NavKey.Reading(it.id)) }
                )
            }
        }
    }
}