package io.lin.reader.ui.screens.shelf

import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import io.lin.reader.ReaderApplication
import io.lin.reader.data.booksrepository.BooksRepository
import io.lin.reader.data.database.Series
import io.lin.reader.data.database.Volume
import io.lin.reader.data.preferences.ShelfPreferences
import io.lin.reader.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "ShelfScreenViewModel.kt"

/**
 * Use with ShelfUiState
 * */
data class ShelfItem(
    val series: Series,
    val top3Volumes: List<Volume> = emptyList()
)

/**
 * ShelfScreen使用的ViewModel
 * */
class ShelfScreenViewModel(
    application: ReaderApplication,
    private val booksRepository: BooksRepository,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val shelfPreferences = ShelfPreferences(userPreferencesRepository, viewModelScope)

    // 实例化新的导入器
    val volumeImporter = VolumeImporter(application, booksRepository, viewModelScope)

    // 为了兼容现有的 UI 代码 (ShelfScreen.kt 还在访问 viewModel.volumesToImport)
    val volumesToImport get() = volumeImporter.volumesToImport

    /**
     * 更改系列名
     * */
    fun updateSeriesName(newSeriesName: String, seriesId: Long) {
        if (seriesId == 0L) return
        viewModelScope.launch {
            try {
                val series = booksRepository.getSeriesById(seriesId)
                if (newSeriesName != series.seriesName) {
                    booksRepository.updateSeries(series.copy(seriesName = newSeriesName))
                    Log.d(TAG, "更新系列名成功: $newSeriesName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新系列名失败: $seriesId", e)
            }
        }
    }

    /**
     * 删除系列
     * */
    fun deleteSeries(seriesId: Long) {
        viewModelScope.launch {
            try {
                val deleteRes = booksRepository.deleteSeriesById(seriesId)
                if (deleteRes >= 1) {
                    Log.d(TAG, "删除系列成功：$seriesId")
                } else {
                    Log.d(TAG, "删除失败：系列 $seriesId 可能不存在")
                }
            } catch (e: Exception) {
                Log.e(TAG, "删除系列异常", e)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val seriesPagingItems: Flow<PagingData<ShelfItem>> = snapshotFlow { 
        shelfPreferences.seriesSortMethod to shelfPreferences.seriesSortAscending 
    }.flatMapLatest { (method, ascending) ->
        booksRepository.getAllSeriesStream(method, ascending)
    }.map { pagingData ->
        pagingData.map { series ->
            val top3Volumes =
                booksRepository.getTop3VolumesBySeriesIdOrderedByName(series.id).first()
            ShelfItem(series, top3Volumes)
        }
    }.cachedIn(viewModelScope)
}
