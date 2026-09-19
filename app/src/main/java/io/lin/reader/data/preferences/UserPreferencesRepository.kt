package io.lin.reader.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

class UserPreferencesRepository(
    internal val dataStore: DataStore<Preferences>
) {
    internal companion object {
        const val TAG = "UserPreferencesRepo"

        // App
        val APP_THEME = stringPreferencesKey("app_theme")
        val APP_THEME_COLOR = stringPreferencesKey("app_theme_color")
        val APP_THEME_CONTRAST = stringPreferencesKey("app_theme_contrast")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val APP_FLOATING_NAVIGATION_BAR = booleanPreferencesKey("app_floating_navigation_bar")

        // Shelf
        val SERIES_SORT_METHOD = stringPreferencesKey("series_sort_method")
        val SERIES_SORT_ASCENDING = booleanPreferencesKey("series_sort_ascending")

        // Series
        val VOLUME_SORT_METHOD = stringPreferencesKey("volume_sort_method")
        val VOLUME_SORT_ASCENDING = booleanPreferencesKey("volume_sort_ascending")

        // Bookmark
        val BOOKMARK_SORT_METHOD = stringPreferencesKey("bookmark_sort_method")

        // History
        val SHOW_UNREAD_BOOKS = booleanPreferencesKey("show_unread_books")
        val UNREAD_BOOKS_EXPANDED = booleanPreferencesKey("unread_books_expanded")

        // Reader
        val READING_MODE = stringPreferencesKey("reading_mode")
        val PAGE_ALIGNMENT = stringPreferencesKey("page_alignment")
        val PAGE_PADDING_RATIO = floatPreferencesKey("page_padding_ratio")
        val RTL_MODE = booleanPreferencesKey("rtl_mode")
        val CROP_MODE = stringPreferencesKey("crop_mode")
        val SEPARATE_COVER = booleanPreferencesKey("separate_cover")
        val FIXED_PAGE_INDICATOR = booleanPreferencesKey("fixed_page_indicator")
        val INTERACTION_STYLE = stringPreferencesKey("interaction_style")
        val BACKGROUND_COLOR = intPreferencesKey("background_color")
        val PAGE_COLOR = intPreferencesKey("page_color")
        val FILTER_COLOR = intPreferencesKey("filter_color")
        val INTERACTION_STYLE_HINT_PENDING = booleanPreferencesKey("interaction_style_hint_pending")
        val IS_REFLOW = booleanPreferencesKey("is_reflow")

        // Reflow Specific
        val REFLOW_USE_PUBLISHER_STYLE = booleanPreferencesKey("reflow_use_publisher_style")
        val REFLOW_PAGE_BREAK_BEFORE = stringPreferencesKey("reflow_page_break_before")
        val REFLOW_PAGE_BREAK_AFTER = stringPreferencesKey("reflow_page_break_after")
        val REFLOW_FONT_FAMILY = stringPreferencesKey("reflow_font_family")
        val REFLOW_FONT_SIZE = intPreferencesKey("reflow_font_size")
        val REFLOW_FONT_BOLD = booleanPreferencesKey("reflow_font_bold")
        val REFLOW_FONT_ITALIC = booleanPreferencesKey("reflow_font_italic")
        val REFLOW_LINE_HEIGHT = floatPreferencesKey("reflow_line_height")
        val REFLOW_TEXT_COLOR = intPreferencesKey("reflow_text_color")
        val REFLOW_BG_COLOR = intPreferencesKey("reflow_bg_color")
        val REFLOW_HORIZONTAL_MARGIN = floatPreferencesKey("reflow_horizontal_margin")
        val REFLOW_VERTICAL_MARGIN = floatPreferencesKey("reflow_vertical_margin")
        val REFLOW_HORIZONTAL_PADDING = floatPreferencesKey("reflow_horizontal_padding")
        val REFLOW_VERTICAL_PADDING = floatPreferencesKey("reflow_vertical_padding")
        val REFLOW_LIST_STYLE_TYPE = stringPreferencesKey("reflow_list_style_type")
        val REFLOW_LIST_STYLE_POSITION = stringPreferencesKey("reflow_list_style_position")
        val REFLOW_TEXT_ALIGN = stringPreferencesKey("reflow_text_align")
        val REFLOW_TEXT_INDENT = floatPreferencesKey("reflow_text_indent")
        val REFLOW_PARAGRAPH_SPACING = floatPreferencesKey("reflow_paragraph_spacing")
        val REFLOW_HYPHENATION = booleanPreferencesKey("reflow_hyphenation")
    }

    suspend fun resetAllSettings() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}