package io.lin.reader.data.preferences

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface HistoryPreferencesInterface {
    var showUnreadBooks: Boolean
    var unreadBooksExpanded: Boolean
    fun updateShowUnreadBooks(show: Boolean)
    fun updateUnreadBooksExpanded(isExpanded: Boolean)
}

data class HistoryPreferences(
    private val repo: UserPreferencesRepository,
    private val scope: CoroutineScope,
) : HistoryPreferencesInterface {
    override var showUnreadBooks by mutableStateOf(false)
    override var unreadBooksExpanded by mutableStateOf(false)

    init {
        scope.launch {
            repo.dataStore.data.collect { prefs ->
                showUnreadBooks = prefs[UserPreferencesRepository.SHOW_UNREAD_BOOKS] ?: false
                unreadBooksExpanded = prefs[UserPreferencesRepository.UNREAD_BOOKS_EXPANDED] ?: false
            }
        }
    }

    override fun updateShowUnreadBooks(show: Boolean) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.SHOW_UNREAD_BOOKS] = show
                if (!show) {
                    it[UserPreferencesRepository.UNREAD_BOOKS_EXPANDED] = false
                }
            }
        }
    }

    override fun updateUnreadBooksExpanded(isExpanded: Boolean) {
        scope.launch {
            if (showUnreadBooks) {
                repo.dataStore.edit {
                    it[UserPreferencesRepository.UNREAD_BOOKS_EXPANDED] = isExpanded
                }
            }
        }
    }
}