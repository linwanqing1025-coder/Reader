package io.lin.reader.ui.feature.favourite

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lin.reader.data.booksrepository.BooksRepository
import io.lin.reader.data.database.Series
import io.lin.reader.data.database.Volume
import io.lin.reader.data.preferences.SeriesSortMethod
import io.lin.reader.data.preferences.ShelfPreferences
import io.lin.reader.data.preferences.SortPreference
import io.lin.reader.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * FavouriteScreen 的 UI 状态封装
 */
data class FavouriteUiState(
    val groupedFavorites: Map<Series, List<Volume>> = emptyMap(),
    val sortPreference: SortPreference<SeriesSortMethod> = SortPreference(
        SeriesSortMethod.Name,
        true
    )
)

/**
 * FavouriteScreen 的 ViewModel
 */
class FavouriteScreenViewModel(
    private val booksRepository: BooksRepository,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val shelfPreferences = ShelfPreferences(userPreferencesRepository, viewModelScope)

    /**
     * 获取收藏界面的 UI 状态
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<FavouriteUiState> = snapshotFlow {
        shelfPreferences.seriesSortMethod to shelfPreferences.seriesSortAscending
    }.flatMapLatest { (method, ascending) ->
        booksRepository.getFavoriteVolumesGroupedBySeriesStream(
            order = method,
            isAscending = ascending
        ).map { groupedMap ->
            FavouriteUiState(
                groupedFavorites = groupedMap,
                sortPreference = SortPreference(method, ascending)
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FavouriteUiState()
    )

    /**
     * 将单本书籍移出收藏
     */
    fun removeFromFavorite(volume: Volume) {
        viewModelScope.launch {
            booksRepository.updateVolume(volume.copy(isFavorite = false))
        }
    }

    /**
     * 清除所有收藏
     */
    fun clearAllFavorites(onResult: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val deletedCount = booksRepository.clearAllFavorites()
            onResult(deletedCount)
        }
    }
}
