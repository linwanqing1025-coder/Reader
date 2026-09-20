package io.lin.reader.ui.screens.shelf

import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import io.lin.reader.ReaderApplication
import io.lin.reader.data.booksrepository.BooksRepository
import io.lin.reader.data.database.Series
import io.lin.reader.data.database.Volume
import io.lin.reader.data.preferences.ShelfPreferences
import io.lin.reader.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SeriesDetailViewModel.kt"

class SeriesDetailScreenViewModel(
    application: ReaderApplication,
    private val booksRepository: BooksRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    // 使用 StateFlow 管理 ID，支持从 SavedStateHandle 恢复或从 UI 手动注入
    private val _seriesId = MutableStateFlow<Long?>(savedStateHandle.get<Long>("seriesId"))
    val seriesId: StateFlow<Long?> = _seriesId.asStateFlow()

    /**
     * 设置系列 ID（由 UI 调用）
     */
    fun setSeriesId(id: Long) {
        if (_seriesId.value != id) {
            _seriesId.value = id
            savedStateHandle["seriesId"] = id // 同步到 SavedStateHandle 以支持重建
        }
    }

    val shelfPreferences = ShelfPreferences(userPreferencesRepository, viewModelScope)
    val volumeImporter = VolumeImporter(application, booksRepository, viewModelScope)
    val volumesToImport get() = volumeImporter.volumesToImport

    /**
     * 响应式获取当前系列详情
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val seriesStream: StateFlow<Series?> = _seriesId
        .filterNotNull()
        .flatMapLatest { id ->
            booksRepository.getSeriesStreamById(id)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /**
     * 响应式获取书籍分页流
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val volumesPagingItemsStream: Flow<PagingData<Volume>> = _seriesId
        .filterNotNull()
        .flatMapLatest { id ->
            snapshotFlow {
                shelfPreferences.volumeSortMethod to shelfPreferences.volumeSortAscending
            }.flatMapLatest { (method, ascending) ->
                booksRepository.getVolumesStreamInSeries(id, method, ascending)
            }
        }.cachedIn(viewModelScope)

    private val _selectedVolumes = MutableStateFlow<Set<Long>>(emptySet())
    val selectedVolumes: StateFlow<Set<Long>> = _selectedVolumes.asStateFlow()

    fun toggleSelectedVolume(volumeId: Long) {
        val currentSet = _selectedVolumes.value
        _selectedVolumes.value = if (currentSet.contains(volumeId)) {
            currentSet - volumeId
        } else {
            currentSet + volumeId
        }
    }

    fun clearSelectedVolumes() {
        _selectedVolumes.value = emptySet()
    }

    fun toggleSelectAllVolumes() {
        viewModelScope.launch {
            val id = _seriesId.value ?: return@launch
            val series = seriesStream.value ?: return@launch
            val currentSelected = _selectedVolumes.value

            if (currentSelected.size >= series.volumeCount && series.volumeCount > 0) {
                clearSelectedVolumes()
            } else {
                val volumes = booksRepository.getVolumesInSeries(
                    seriesId = id,
                    order = shelfPreferences.volumeSortMethod,
                    isAscending = shelfPreferences.volumeSortAscending
                )
                _selectedVolumes.value = volumes.map { it.id }.toSet()
            }
        }
    }

    fun updateSeriesName(newName: String) {
        val id = _seriesId.value ?: return
        viewModelScope.launch {
            try {
                val series = seriesStream.value ?: return@launch
                if (newName != series.seriesName) {
                    booksRepository.updateSeries(series.copy(seriesName = newName))
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新系列名失败: $id", e)
            }
        }
    }

    fun deleteSelectedVolumes(onSeriesDeleted: () -> Unit = {}) {
        val id = _seriesId.value ?: return
        viewModelScope.launch {
            val selectedIds = _selectedVolumes.value.toList()
            if (selectedIds.isEmpty()) return@launch

            try {
                val series = seriesStream.value ?: return@launch
                val totalCount = series.volumeCount
                val selectedCount = selectedIds.size

                if (selectedCount >= totalCount) {
                    booksRepository.deleteSeriesById(id)
                    onSeriesDeleted() 
                } else {
                    booksRepository.deleteVolumesByIds(selectedIds)
                    val newCount = (totalCount - selectedCount).coerceAtLeast(0)
                    booksRepository.updateSeries(series.copy(volumeCount = newCount))
                }
            } catch (e: Exception) {
                Log.e(TAG, "批量删除操作失败", e)
            }
        }
    }

    fun saveVolumes() {
        val id = _seriesId.value ?: return
        viewModelScope.launch {
            try {
                volumeImporter.saveVolumeWithExistingSeries(id)
            } catch (e: Exception) {
                Log.e(TAG, "保存书籍失败", e)
            }
        }
    }
}
