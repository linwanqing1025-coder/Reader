package io.lin.reader.data.preferences

import android.graphics.Color
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.lin.reader.ui.screen.setting.details.AppLanguage
import io.lin.reader.ui.screen.setting.details.BookmarkSortMethod
import io.lin.reader.ui.screen.setting.details.DarkMode
import io.lin.reader.ui.screen.setting.details.PageAlignment
import io.lin.reader.ui.screen.setting.details.ReaderColor
import io.lin.reader.ui.screen.setting.details.ReadingMode
import io.lin.reader.ui.screen.setting.details.SeriesSortMethod
import io.lin.reader.ui.screen.setting.details.ThemeColor
import io.lin.reader.ui.screen.setting.details.ThemeContrast
import io.lin.reader.ui.screen.setting.details.VolumeSortMethod
import io.lin.reader.utils.InteractionStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException


class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        const val TAG = "UserPreferencesRepo"

        // App
        val APP_THEME = stringPreferencesKey("app_theme")
        val APP_THEME_COLOR = stringPreferencesKey("app_theme_color")
        val APP_THEME_CONTRAST = stringPreferencesKey("app_theme_contrast")
        val APP_LANGUAGE = stringPreferencesKey("app_language")

        // Shelf
        val SERIES_SORT_METHOD = stringPreferencesKey("series_sort_method")
        val SERIES_SORT_ASCENDING = booleanPreferencesKey("series_sort_ascending")

        // Reader
        val READING_MODE = stringPreferencesKey("reading_mode")
        val PAGE_ALIGNMENT = stringPreferencesKey("page_alignment")
        val PAGE_PADDING_RATIO = floatPreferencesKey("page_padding_ratio")
        val RTL_MODE = booleanPreferencesKey("rtl_mode")
        val REMOVE_GUTTER = booleanPreferencesKey("remove_gutter")
        val SEPARATE_COVER = booleanPreferencesKey("separate_cover")
        val FIXED_PAGE_INDICATOR = booleanPreferencesKey("fixed_page_indicator")
        val INTERACTION_STYLE = stringPreferencesKey("interaction_style")
        val BACKGROUND_COLOR = intPreferencesKey("background_color")
        val PAGE_COLOR = intPreferencesKey("page_color")
        val FILTER_COLOR = intPreferencesKey("filter_color")
        val INTERACTION_STYLE_HINT_PENDING = booleanPreferencesKey("interaction_style_hint_pending")

        // Series
        val VOLUME_SORT_METHOD = stringPreferencesKey("volume_sort_method")
        val VOLUME_SORT_ASCENDING = booleanPreferencesKey("volume_sort_ascending")

        // History
        val SHOW_UNREAD_BOOKS = booleanPreferencesKey("show_unread_books")
        val UNREAD_BOOKS_EXPANDED = booleanPreferencesKey("unread_books_expanded")

        // Bookmark
        val BOOKMARK_SORT_METHOD = stringPreferencesKey("bookmark_sort_method")
    }

    // App
    val appPreferencesFlow: Flow<AppPreferences> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading app preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            val theme = try {
                DarkMode.valueOf(preferences[APP_THEME] ?: DarkMode.System.name)
            } catch (e: Exception) {
                DarkMode.System
            }
            val themeColor = try {
                ThemeColor.valueOf(preferences[APP_THEME_COLOR] ?: ThemeColor.Default.name)
            } catch (e: Exception) {
                ThemeColor.Default
            }
            val themeContrast = try {
                ThemeContrast.valueOf(preferences[APP_THEME_CONTRAST] ?: ThemeContrast.Light.name)
            } catch (e: Exception) {
                ThemeContrast.Light
            }
            val language = try {
                AppLanguage.valueOf(preferences[APP_LANGUAGE] ?: AppLanguage.System.name)
            } catch (e: Exception) {
                AppLanguage.System
            }
            AppPreferences(theme, language, themeColor, themeContrast)
        }

    suspend fun updateAppTheme(theme: DarkMode) {
        dataStore.edit { preferences ->
            preferences[APP_THEME] = theme.name
        }
    }

    suspend fun updateAppThemeColor(themeColor: ThemeColor) {
        dataStore.edit { preferences ->
            preferences[APP_THEME_COLOR] = themeColor.name
        }
    }

    suspend fun updateAppThemeContrast(themeContrast: ThemeContrast) {
        dataStore.edit { preferences ->
            preferences[APP_THEME_CONTRAST] = themeContrast.name
        }
    }

    suspend fun updateAppLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = language.name
        }
    }


    // Shelf
    val seriesSortPreferencesFlow: Flow<SortPreference<SeriesSortMethod>> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading series sort preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            val sortMethod = try {
                SeriesSortMethod.valueOf(
                    preferences[SERIES_SORT_METHOD] ?: SeriesSortMethod.Name.name
                )
            } catch (e: Exception) {
                SeriesSortMethod.Name
            }
            val isAscending = preferences[SERIES_SORT_ASCENDING] ?: true
            SortPreference(sortMethod, isAscending)
        }

    suspend fun updateSeriesSortMethod(sortMethod: SeriesSortMethod) {
        dataStore.edit { preferences ->
            preferences[SERIES_SORT_METHOD] = sortMethod.name
        }
    }

    suspend fun updateSeriesSortAscending(isAscending: Boolean) {
        dataStore.edit { preferences ->
            preferences[SERIES_SORT_ASCENDING] = isAscending
        }
    }


    // Series
    val volumeSortPreferencesFlow: Flow<SortPreference<VolumeSortMethod>> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading volume sort preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            val sortMethod = try {
                VolumeSortMethod.valueOf(
                    preferences[VOLUME_SORT_METHOD] ?: VolumeSortMethod.Name.name
                )
            } catch (e: Exception) {
                VolumeSortMethod.Name
            }
            val isAscending = preferences[VOLUME_SORT_ASCENDING] ?: true
            SortPreference(sortMethod, isAscending)
        }

    suspend fun updateVolumeSortMethod(sortMethod: VolumeSortMethod) {
        dataStore.edit { preferences ->
            preferences[VOLUME_SORT_METHOD] = sortMethod.name
        }
    }

    suspend fun updateVolumeSortAscending(isAscending: Boolean) {
        dataStore.edit { preferences ->
            preferences[VOLUME_SORT_ASCENDING] = isAscending
        }
    }


    // Bookmark
    val bookmarkSortMethodFlow: Flow<BookmarkSortMethod> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading bookmark sort preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            try {
                BookmarkSortMethod.valueOf(
                    preferences[BOOKMARK_SORT_METHOD] ?: BookmarkSortMethod.LatestBookmarkTime.name
                )
            } catch (e: Exception) {
                BookmarkSortMethod.LatestBookmarkTime
            }
        }

    suspend fun updateBookmarkSortMethod(sortMethod: BookmarkSortMethod) {
        dataStore.edit { preferences ->
            preferences[BOOKMARK_SORT_METHOD] = sortMethod.name
        }
    }


    // History
    val historyPreferencesFlow: Flow<HistoryPreferences> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading history preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            val showUnreadBooks = preferences[SHOW_UNREAD_BOOKS] ?: false
            val unreadBooksExpanded = preferences[UNREAD_BOOKS_EXPANDED] ?: false
            HistoryPreferences(showUnreadBooks, unreadBooksExpanded)
        }

    suspend fun updateShowUnreadBooks(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_UNREAD_BOOKS] = show
            if (!show) {
                preferences[UNREAD_BOOKS_EXPANDED] = false
            }
        }
    }

    suspend fun updateUnreadBooksExpanded(isExpanded: Boolean) {
        dataStore.edit { preferences ->
            val showUnreadBooks = preferences[SHOW_UNREAD_BOOKS] ?: false
            if (showUnreadBooks) {
                preferences[UNREAD_BOOKS_EXPANDED] = isExpanded
            }
        }
    }


    // Reading
    val readerPreferencesFlow: Flow<ReaderPreferences> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading reader preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            val readingModeName = preferences[READING_MODE] ?: ReadingMode.Single.name
            val readingMode = try {
                ReadingMode.valueOf(readingModeName)
            } catch (e: Exception) {
                ReadingMode.Single
            }

            val alignmentName = preferences[PAGE_ALIGNMENT] ?: PageAlignment.Vertical.name
            val pageAlignment = try {
                PageAlignment.valueOf(alignmentName)
            } catch (e: Exception) {
                PageAlignment.Vertical
            }

            val pagePaddingRatio = preferences[PAGE_PADDING_RATIO] ?: 0f
            val rtlMode = preferences[RTL_MODE] ?: false
            val removeGutter = preferences[REMOVE_GUTTER] ?: false
            val separateCover = preferences[SEPARATE_COVER] ?: false
            val fixedPageIndicator = preferences[FIXED_PAGE_INDICATOR] ?: false
            val interactionStyleName =
                preferences[INTERACTION_STYLE] ?: InteractionStyle.Style0.name
            val interactionStyle = try {
                InteractionStyle.valueOf(interactionStyleName)
            } catch (e: Exception) {
                InteractionStyle.Style0
            }

            val colors = ReaderColor(
                background = preferences[BACKGROUND_COLOR] ?: Color.BLACK,
                page = preferences[PAGE_COLOR] ?: Color.WHITE,
                filter = preferences[FILTER_COLOR] ?: 0x00000000 // Transparent
            )

            val isInteractionHintPending = preferences[INTERACTION_STYLE_HINT_PENDING] ?: true

            ReaderPreferences(
                readingMode = readingMode,
                pageAlignment = pageAlignment,
                pagePaddingRatio = pagePaddingRatio,
                separateCover = separateCover,
                rtlMode = rtlMode,
                removeGutter = removeGutter,
                fixedPageIndicator = fixedPageIndicator,
                interactionStyle = interactionStyle,
                colors = colors,
                isInteractionHintPending = isInteractionHintPending
            )
        }

    suspend fun updateReadingMode(readingMode: ReadingMode) {
        dataStore.edit { preferences ->
            preferences[READING_MODE] = readingMode.name
        }
    }

    suspend fun updatePageAlignment(alignment: PageAlignment) {
        dataStore.edit { preferences ->
            preferences[PAGE_ALIGNMENT] = alignment.name
        }
    }

    suspend fun updatePagePaddingRatio(ratio: Float) {
        dataStore.edit { preferences ->
            preferences[PAGE_PADDING_RATIO] = ratio
        }
    }

    suspend fun updateRtlMode(rtlMode: Boolean) {
        dataStore.edit { preferences ->
            preferences[RTL_MODE] = rtlMode
        }
    }

    suspend fun updateRemoveGutter(removeGutter: Boolean) {
        dataStore.edit { preferences ->
            preferences[REMOVE_GUTTER] = removeGutter
        }
    }

    suspend fun updateSeparateCover(separateCover: Boolean) {
        dataStore.edit { preferences ->
            preferences[SEPARATE_COVER] = separateCover
        }
    }

    suspend fun updateFixedPageIndicator(fixed: Boolean) {
        dataStore.edit { preferences ->
            preferences[FIXED_PAGE_INDICATOR] = fixed
        }
    }

    suspend fun updateInteractionStyle(interactionStyle: InteractionStyle) {
        dataStore.edit { preferences ->
            preferences[INTERACTION_STYLE] = interactionStyle.name
            // 切换样式时，重置待提示状态为 true
            preferences[INTERACTION_STYLE_HINT_PENDING] = true
        }
    }

    suspend fun clearInteractionStyleHint() {
        dataStore.edit { preferences ->
            preferences[INTERACTION_STYLE_HINT_PENDING] = false
        }
    }

    suspend fun updateBackgroundColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[BACKGROUND_COLOR] = color
        }
    }

    suspend fun updatePageColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[PAGE_COLOR] = color
        }
    }

    suspend fun updateFilterColor(color: Int) {
        dataStore.edit { preferences ->
            preferences[FILTER_COLOR] = color
        }
    }

    suspend fun resetAllSettings() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

data class AppPreferences(
    val theme: DarkMode = DarkMode.System,
    val language: AppLanguage = AppLanguage.System,
    val themeColor: ThemeColor = ThemeColor.Default,
    val themeContrast: ThemeContrast = ThemeContrast.Light
)

data class SortPreference<T>(val sortMethod: T, val isAscending: Boolean)

data class HistoryPreferences(val showUnreadBooks: Boolean, val unreadBooksExpanded: Boolean)

data class ReaderPreferences(
    val readingMode: ReadingMode = ReadingMode.Single,
    val pageAlignment: PageAlignment = PageAlignment.Vertical,
    val pagePaddingRatio: Float = 0f,
    val separateCover: Boolean = false,
    val rtlMode: Boolean = false,
    val removeGutter: Boolean = false,
    val fixedPageIndicator: Boolean = false,
    val interactionStyle: InteractionStyle = InteractionStyle.Style0,
    val colors: ReaderColor = ReaderColor(),
    val isInteractionHintPending: Boolean = true
)
