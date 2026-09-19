package io.lin.reader.ui.feature.setting

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lin.reader.R
import io.lin.reader.data.booksrepository.BooksRepository
import io.lin.reader.data.preferences.AppPreferences
import io.lin.reader.data.preferences.BookmarkPreferences
import io.lin.reader.data.preferences.HistoryPreferences
import io.lin.reader.data.preferences.ReaderPreferences
import io.lin.reader.data.preferences.ReflowPreferences
import io.lin.reader.data.preferences.ShelfPreferences
import io.lin.reader.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingScreenViewModel(
    private val booksRepository: BooksRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val appPreferences = AppPreferences(userPreferencesRepository, viewModelScope)
    val shelfPreferences = ShelfPreferences(userPreferencesRepository, viewModelScope)
    val bookmarkPreferences = BookmarkPreferences(userPreferencesRepository, viewModelScope)
    val historyPreferences = HistoryPreferences(userPreferencesRepository, viewModelScope)
    val readingPreferences = ReaderPreferences(userPreferencesRepository, viewModelScope)
    val reflowPreferences = ReflowPreferences(userPreferencesRepository, viewModelScope)

    fun clearAllHistory() {
        viewModelScope.launch {
            booksRepository.clearAllReadingHistory()
        }
    }

    fun resetAllSettings(onResult: () -> Unit = {}) {
        viewModelScope.launch {
            userPreferencesRepository.resetAllSettings()
            // Reset language to system if it was changed
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            onResult()
        }
    }
}