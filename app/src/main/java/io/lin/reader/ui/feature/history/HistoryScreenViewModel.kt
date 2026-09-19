package io.lin.reader.ui.feature.history

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
import kotlinx.coroutines.launch

class HistoryScreenViewModel(
    private val booksRepository: BooksRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val historyPreferences = HistoryPreferences(userPreferencesRepository, viewModelScope)

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
}