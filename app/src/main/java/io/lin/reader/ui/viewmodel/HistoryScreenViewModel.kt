package io.lin.reader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import io.lin.reader.data.booksrepository.BooksRepository
import io.lin.reader.data.database.Volume
import io.lin.reader.data.database.VolumeWithSeries
import io.lin.reader.data.preferences.HistoryPreferences
import io.lin.reader.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryScreenUiState(
    val historyPreferences: HistoryPreferences = HistoryPreferences(
        showUnreadBooks = false,
        unreadBooksExpanded = false
    )
)

class HistoryScreenViewModel(
    private val booksRepository: BooksRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }

    val readVolumesUiState: Flow<PagingData<VolumeWithSeries>> =
        booksRepository.getReadVolumesStream()
            .cachedIn(viewModelScope)
    val unreadVolumesUiState: Flow<PagingData<VolumeWithSeries>> =
        booksRepository.getUnreadVolumesStream()
            .cachedIn(viewModelScope)

    fun clearHistoryInSingleVolume(volume: Volume) {
        val newVolume = volume.copy(lastReadTime = null, lastReadPage = 0)
        viewModelScope.launch {
            booksRepository.updateVolume(newVolume)
        }
    }

    fun clearAllHistory(onResult: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val deletedCount = booksRepository.clearAllReadingHistory()
            onResult(deletedCount)
        }
    }

    val historyScreenUiState: StateFlow<HistoryScreenUiState> =
        userPreferencesRepository.historyPreferencesFlow.map { historyPreferences ->
            HistoryScreenUiState(historyPreferences)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = HistoryScreenUiState()
        )

    fun updateShowUnreadBooks(show: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateShowUnreadBooks(show)
        }
    }

    fun updateUnreadBooksExpanded(isExpanded: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateUnreadBooksExpanded(isExpanded)
        }
    }
}
