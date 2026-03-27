package io.lin.reader.ui.viewmodel

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lin.reader.R
import io.lin.reader.data.booksrepository.BooksRepository
import io.lin.reader.data.preferences.AppPreferences
import io.lin.reader.data.preferences.HistoryPreferences
import io.lin.reader.data.preferences.ReaderPreferences
import io.lin.reader.data.preferences.SortPreference
import io.lin.reader.data.preferences.UserPreferencesRepository
import io.lin.reader.ui.screen.setting.details.AppLanguage
import io.lin.reader.ui.screen.setting.details.BookmarkSortMethod
import io.lin.reader.ui.screen.setting.details.DarkMode
import io.lin.reader.ui.screen.setting.details.PageAlignment
import io.lin.reader.ui.screen.setting.details.ReadingMode
import io.lin.reader.ui.screen.setting.details.SeriesSortMethod
import io.lin.reader.ui.screen.setting.details.ThemeColor
import io.lin.reader.ui.screen.setting.details.ThemeContrast
import io.lin.reader.ui.screen.setting.details.VolumeSortMethod
import io.lin.reader.utils.InteractionStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingScreenViewModel(
    private val booksRepository: BooksRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val app = AppManager()
    val seriesSort = SeriesSortManager()
    val volumeSort = VolumeSortManager()
    val bookmarkSort = BookmarkSortManager()
    val history = HistoryManager()
    val reading = ReadingManager()

    // 详情页管理
    private val _currentDetailType = MutableStateFlow<SettingDetailType?>(null)
    val currentDetailType: StateFlow<SettingDetailType?> = _currentDetailType.asStateFlow()

    fun showDetail(type: SettingDetailType) {
        _currentDetailType.value = type
    }

    fun closeDetail() {
        _currentDetailType.value = null
    }

    enum class SettingDetailType(@StringRes val labelRes: Int) {
        APP_APPEARANCE(R.string.setting_appearance),
        SORT(R.string.setting_sort),
        READER_COLOR(R.string.setting_reader_color),
        READING_MODE(R.string.setting_reading_mode),
        INTERACTION(R.string.setting_interaction_style),
        OTHER_READING_SETTING(R.string.setting_other_reading_settings)
    }

    fun resetAllSettings(onResult: () -> Unit = {}) {
        viewModelScope.launch {
            userPreferencesRepository.resetAllSettings()
            // Reset language to system if it was changed
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            onResult()
        }
    }

    inner class AppManager {
        val state: StateFlow<AppPreferences> = userPreferencesRepository.appPreferencesFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = AppPreferences()
            )

        fun updateDarkMode(theme: DarkMode) {
            viewModelScope.launch {
                userPreferencesRepository.updateAppTheme(theme)
            }
        }

        fun updateThemeColor(themeColor: ThemeColor) {
            viewModelScope.launch {
                userPreferencesRepository.updateAppThemeColor(themeColor)
            }
        }

        fun updateThemeContrast(themeContrast: ThemeContrast) {
            viewModelScope.launch {
                userPreferencesRepository.updateAppThemeContrast(themeContrast)
            }
        }

        fun updateLanguage(language: AppLanguage) {
            viewModelScope.launch {
                userPreferencesRepository.updateAppLanguage(language)

                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(
                    when (language) {
                        AppLanguage.ChineseSimplified -> "zh-CN"
                        AppLanguage.ChineseTraditional -> "zh-TW"
                        AppLanguage.English -> "en"
                        AppLanguage.Japanese -> "ja"
                        AppLanguage.System -> ""
                    }
                )
                AppCompatDelegate.setApplicationLocales(appLocale)
            }
        }
    }

    inner class SeriesSortManager {
        val state: StateFlow<SortPreference<SeriesSortMethod>> =
            userPreferencesRepository.seriesSortPreferencesFlow
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000L),
                    initialValue = SortPreference(SeriesSortMethod.Name, true)
                )

        fun updateMethod(sortMethod: SeriesSortMethod) {
            viewModelScope.launch {
                userPreferencesRepository.updateSeriesSortMethod(sortMethod)
            }
        }

        fun updateOrder(isAscending: Boolean) {
            viewModelScope.launch {
                userPreferencesRepository.updateSeriesSortAscending(isAscending)
            }
        }
    }

    inner class VolumeSortManager {
        val state: StateFlow<SortPreference<VolumeSortMethod>> =
            userPreferencesRepository.volumeSortPreferencesFlow
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = SortPreference(VolumeSortMethod.Name, true)
                )

        fun updateMethod(sortMethod: VolumeSortMethod) {
            viewModelScope.launch {
                userPreferencesRepository.updateVolumeSortMethod(sortMethod)
            }
        }

        fun updateOrder(isAscending: Boolean) {
            viewModelScope.launch {
                userPreferencesRepository.updateVolumeSortAscending(isAscending)
            }
        }
    }

    inner class BookmarkSortManager {
        val state: StateFlow<BookmarkSortMethod> =
            userPreferencesRepository.bookmarkSortMethodFlow
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000L),
                    initialValue = BookmarkSortMethod.LatestBookmarkTime
                )

        fun updateMethod(sortMethod: BookmarkSortMethod) {
            viewModelScope.launch {
                userPreferencesRepository.updateBookmarkSortMethod(sortMethod)
            }
        }
    }

    inner class HistoryManager {
        val state: StateFlow<HistoryPreferences> = userPreferencesRepository.historyPreferencesFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = HistoryPreferences(
                    showUnreadBooks = false,
                    unreadBooksExpanded = true
                )
            )

        fun clearAllHistory(onResult: (Int) -> Unit = {}) {
            viewModelScope.launch {
                val deletedCount = booksRepository.clearAllReadingHistory()
                onResult(deletedCount)
            }
        }

        fun toggleShowUnreadBooks() {
            viewModelScope.launch {
                val current = state.value.showUnreadBooks
                userPreferencesRepository.updateShowUnreadBooks(!current)
            }
        }

        fun toggleUnreadBooksExpanded() {
            viewModelScope.launch {
                val current = state.value.unreadBooksExpanded
                userPreferencesRepository.updateUnreadBooksExpanded(!current)
            }
        }
    }

    inner class ReadingManager {
        val state: StateFlow<ReaderPreferences> = userPreferencesRepository.readerPreferencesFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = ReaderPreferences()
            )

        fun toggleRtlMode() {
            viewModelScope.launch {
                val current = state.value.rtlMode
                userPreferencesRepository.updateRtlMode(!current)
            }
        }

        fun toggleRemoveGutter() {
            viewModelScope.launch {
                val current = state.value.removeGutter
                userPreferencesRepository.updateRemoveGutter(!current)
            }
        }

        fun updateReadingMode(mode: ReadingMode) {
            viewModelScope.launch {
                userPreferencesRepository.updateReadingMode(mode)
            }
        }

        fun updatePageAlignment(alignment: PageAlignment) {
            viewModelScope.launch {
                userPreferencesRepository.updatePageAlignment(alignment)
            }
        }

        fun updatePagePaddingRatio(paddingRatio: Float) {
            viewModelScope.launch {
                userPreferencesRepository.updatePagePaddingRatio(paddingRatio)
            }
        }

        fun toggleSeparateCover() {
            viewModelScope.launch {
                val current = state.value.separateCover
                userPreferencesRepository.updateSeparateCover(!current)
            }
        }

        fun toggleFixedPageIndicator() {
            viewModelScope.launch {
                val current = state.value.fixedPageIndicator
                userPreferencesRepository.updateFixedPageIndicator(!current)
            }
        }

        fun updateInteractionStyle(style: InteractionStyle) {
            viewModelScope.launch {
                userPreferencesRepository.updateInteractionStyle(style)
            }
        }

        fun updateBackgroundColor(color: Int) {
            viewModelScope.launch {
                userPreferencesRepository.updateBackgroundColor(color)
            }
        }

        fun updatePageColor(color: Int) {
            viewModelScope.launch {
                userPreferencesRepository.updatePageColor(color)
            }
        }

        fun updateFilterColor(color: Int) {
            viewModelScope.launch {
                userPreferencesRepository.updateFilterColor(color)
            }
        }
    }
}
