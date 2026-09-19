package io.lin.reader.ui.feature.shelf

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
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
import io.lin.reader.utils.FileUtils
import io.lin.reader.utils.MuPDFUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ShelfScreenViewModel.kt"

data class BookDetails(
    val fileUri: String = "",
    val volumeName: String = "Default Volume Name",
    val mimeType: String = "application/pdf", // 默认设为 PDF
    val pageCount: Int = 0,
    val seriesId: Long = 0, //seriesId=0 表示这是本新书（默认）
    val coverUri: String? = null // 记录已存在的封面
)

/**
 * seriesId = 0时新建一个系列
 * */
fun BookDetails.toVolume(): Volume = Volume(
    volumeName = volumeName,
    bookFileUri = fileUri,
    mimeType = mimeType, // 同步 MIME 类型
    coverUri = coverUri, // 复用已有的封面字段
    totalPages = pageCount,
    seriesId = seriesId
)

/**
 * Use when importing or showing books is needed
 * */
data class VolumesToImport(
    val booksDetails: List<BookDetails> = emptyList(),
    val isEntryValid: Boolean = false
)

/**
 * Use with ShelfUiState
 * */
data class ShelfItem(
    val series: Series,
    val top3Volumes: List<Volume> = emptyList() // 核心改动：保存 Volume 列表以支持自发渲染
)

/**
 * ShelfScreen使用的ViewModel
 * */
class ShelfScreenViewModel(
    private val booksRepository: BooksRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val application: ReaderApplication
) : ViewModel() {

    val shelfPreferences = ShelfPreferences(userPreferencesRepository, viewModelScope)

    /**
     * 需要导入的书籍
     * */
    var volumesToImport by mutableStateOf(VolumesToImport())
        private set

    /**
     * 验证书籍输入的合法性
     * @param books 待验证的书籍列表，默认取当前UI状态中的书籍列表
     * @return 验证结果：true=全部合法，false=不满足规则
     */
    private fun validateInput(books: List<BookDetails> = volumesToImport.booksDetails): Boolean {
        // 书籍列表不能为空
        if (books.isEmpty()) return false

        // 遍历每一本书，验证 fileUri和 volumeName 非空
        return books.all { book ->
            book.fileUri.isNotBlank() && book.volumeName.isNotBlank()
        }
    }

    /**
     * 重置 volumesToImport
     */
    fun resetVolumesToImport() {
        volumesToImport = VolumesToImport()
    }

    /**
     * 用文件选择器选择的文件 URI列表 更新 BookUiState
     * @param fileUriStrList 选择的文件URI字符串列表（文件选择器返回的结果）
     */
    fun updateVolumesToImportByUriList(fileUriStrList: List<String>) {
        // 1. 空列表直接返回，避免无效操作
        if (fileUriStrList.isEmpty()) {
            Log.d(TAG, "URI列表为空，无需更新BookUiState")
            return
        }

        // 启动一个协程来处理所有数据，确保 UI 状态更新在数据处理完之后再执行
        viewModelScope.launch {
            // 2. 批量处理URI列表：解析+获取文件信息
            val results = fileUriStrList.mapIndexed { index, uriStr ->
                // 2.1 解析URI字符串
                val uri = try {
                    uriStr.toUri()
                } catch (e: Exception) {
                    Log.e(TAG, "第${index + 1}个URI解析失败：$uriStr", e)
                    null
                }

                if (uri == null) {
                    null
                } else {
                    // 2.2 在 IO 线程获取文件名、PDF页数和 MIME 类型（耗时操作，使用 withContext 挂起）
                    val (baseInfo, resolvedMimeType) = withContext(Dispatchers.IO) {
                        val name =
                            FileUtils.getFileName(application, uri) ?: "Unnamed Book ${index + 1}"
                        val count = MuPDFUtils.getPageCount(application, uri)
                        val cover = booksRepository.getCoverUriByFileUri(uriStr) // 提前查询库中是否已有该文件的封面
                        
                        // 2.2.1 还原官方解析策略
                        var type = application.contentResolver.getType(uri)
                        if (type == null || type == "application/octet-stream") {
                            type = name // 官方降级策略：使用文件名作为 magic
                        }
                        
                        Triple(name, count, cover) to type
                    }

                    val (fileName, pageCount, existingCover) = baseInfo

                    // 2.3 生成 BookDetails
                    BookDetails(
                        fileUri = uriStr,
                        volumeName = fileName,
                        mimeType = resolvedMimeType ?: "application/pdf",
                        pageCount = pageCount,
                        seriesId = 0,
                        coverUri = existingCover
                    ).also {
                        Log.d(TAG, "第${index + 1}本书记载完成：$fileName, 类型: ${resolvedMimeType ?: "application/pdf"}, 封面状态: ${if(existingCover!=null) "已有" else "无"}")
                    }
                }
            }.filterNotNull()

            // 3. 数据处理完毕后，一次性更新 UI 状态，避免竞态条件
            volumesToImport = VolumesToImport(
                booksDetails = results,
                isEntryValid = validateInput(results)
            )

            // 4. 批量日志输出
            Log.d(
                TAG,
                "BookUiState批量更新完成：\n" +
                        "总URI数：${fileUriStrList.size}\n" +
                        "有效书籍数：${results.size}\n" +
                        "输入验证结果：${volumesToImport.isEntryValid}"
            )
        }
    }

    /**
     * 批量保存书籍到【新系列】
     * 优化点：先快速入库，让 UI 显示，然后后台异步生成封面。
     * @param newSeriesName 新建系列的名称
     */
    fun saveVolumesWithNewSeries(newSeriesName: String) {
        val books = volumesToImport.booksDetails
        if (!validateInput(books)) {
            Log.d(TAG, "输入验证失败，取消保存")
            return
        }

        viewModelScope.launch {
            try {
                // 1. 立即转换并入库（如果 coverUri 在 updateVolumesToImportByUriList 中已查到，则带上）
                val volumeList = books.map { it.copy(seriesId = 0).toVolume() }
                
                Log.d(TAG, "触发快速入库到新系列：${volumeList.size} 本书")
                booksRepository.addVolumesWithNewSeries(volumeList, newSeriesName)
            } catch (e: Exception) {
                Log.e(TAG, "批量保存到新系列失败", e)
            }
        }
    }

    /**
     * 批量保存书籍到【指定的已有系列】
     * @param seriesId 目标已有系列的ID
     */
    fun saveVolumeWithExistingSeries(seriesId: Long) {
        if (seriesId <= 0) return
        val books = volumesToImport.booksDetails
        if (!validateInput(books)) return

        viewModelScope.launch {
            try {
                // 1. 立即入库
                val volumeList = books.map { it.toVolume().copy(seriesId = seriesId) }
                
                Log.d(TAG, "触发快速入库到已有系列：$seriesId")
                booksRepository.addVolumesInExistingSeries(volumeList)
            } catch (e: Exception) {
                Log.e(TAG, "批量保存到已有系列失败：$seriesId", e)
            }
        }
    }

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

    /**
     * 当前选中的系列 ID (0 表示未选中)
     */
    private val _currentSeriesId = MutableStateFlow(0L)
    val currentSeriesId: StateFlow<Long> = _currentSeriesId.asStateFlow()

    /**
     * 自动获取当前选中的系列详情 (对应原 seriesStream)
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSeries: StateFlow<Series?> = _currentSeriesId
        .flatMapLatest { id ->
            if (id == 0L) flowOf(null)
            else booksRepository.getSeriesStreamById(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /**
     * 当前选定系列下的书籍分页数据流
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSeriesVolumesPagingItems: Flow<PagingData<Volume>> = combine(
        _currentSeriesId,
        snapshotFlow { shelfPreferences.volumeSortMethod to shelfPreferences.volumeSortAscending }
    ) { id, sortInfo ->
        id to sortInfo
    }.flatMapLatest { (id, sortInfo) ->
        if (id == 0L) {
            flowOf(PagingData.empty())
        } else {
            booksRepository.getVolumesStreamInSeries(
                seriesId = id,
                order = sortInfo.first,
                isAscending = sortInfo.second
            )
        }
    }.cachedIn(viewModelScope)

    /**
     * 导航至指定系列
     */
    fun updateSeriesId(seriesId: Long) {
        _currentSeriesId.value = seriesId
    }

    /**
     * 返回书架
     */
    fun resetSeriesId() {
        _currentSeriesId.value = 0L
        _isSelectingVolume.value = false // 重置选择模式
        clearSelectedVolume()
    }

    /**
     * 私有可变 StateFlow
     * */
    private val _selectedVolume = MutableStateFlow<Set<Long>>(emptySet())
    private val _isSelectingVolume = MutableStateFlow(false)

    /**
     * 对外暴露不可变的 StateFlow（防止外部直接修改）
     * */
    val selectedVolume: StateFlow<Set<Long>> = _selectedVolume.asStateFlow()
    val isSelectingVolume: StateFlow<Boolean> = _isSelectingVolume.asStateFlow()

    fun setSelectingVolume(selecting: Boolean) {
        _isSelectingVolume.value = selecting
    }

    /**
     * 快速添加单个 volumeId（已存在则忽略）
     */
    fun addSelectedVolume(volumeId: Long) {
        _selectedVolume.value += volumeId
    }

    /**
     * 快速删除单个 volumeId（不存在则忽略）
     */
    fun removeSelectedVolume(volumeId: Long) {
        _selectedVolume.value -= volumeId
    }

    /**
     * 切换选中状态（存在则删除，不存在则添加）
     */
    fun toggleSelectedVolume(volumeId: Long) {
        val currentSet = _selectedVolume.value
        _selectedVolume.value = if (currentSet.contains(volumeId)) {
            currentSet - volumeId
        } else {
            currentSet + volumeId
        }
    }

    /**
     * 清空所有选中的 volumeId
     */
    fun clearSelectedVolume() {
        _selectedVolume.value = emptySet()
    }

    /**
     * 判断某个 volumeId 是否被选中
     */
    fun isVolumeSelected(volumeId: Long): Boolean {
        return _selectedVolume.value.contains(volumeId)
    }

    /**
     * 获取选中的 volumeId 数量
     */
    fun getSelectedVolumeCount(): Int {
        return _selectedVolume.value.size
    }

    /**
     * 切换全选/全不选：未全选则全选，已全选则全不选
     */
    fun toggleSelectAllVolumes() {
        viewModelScope.launch {
            val seriesId = _currentSeriesId.value
            if (seriesId == 0L) return@launch

            val series = currentSeries.value ?: return@launch
            val currentSelected = _selectedVolume.value

            // 判断逻辑：如果选中数量等于总数，说明已经全选，则执行清空
            if (currentSelected.size >= series.volumeCount && series.volumeCount > 0) {
                clearSelectedVolume()
            } else {
                // 否则从数据库加载该系列所有书籍 ID 并全选
                val volumes = booksRepository.getVolumesInSeries(
                    seriesId = seriesId,
                    order = shelfPreferences.volumeSortMethod,
                    isAscending = shelfPreferences.volumeSortAscending
                )
                _selectedVolume.value = volumes.map { it.id }.toSet()
                Log.d(TAG, "全选系列中书籍：共选中 ${_selectedVolume.value.size} 本")
            }
        }
    }

    /**
     * 删除选中的所有书籍（批量操作）
     */
    fun deleteSelectedVolumes() {
        viewModelScope.launch {
            val selectedVolumeIds = _selectedVolume.value.toList()
            if (selectedVolumeIds.isEmpty()) return@launch

            try {
                val seriesId = _currentSeriesId.value
                val series = currentSeries.value ?: return@launch
                val totalVolumeCount = series.volumeCount
                val selectedCount = selectedVolumeIds.size

                // 如果选中的数量等于总数，直接删除整个系列
                if (selectedCount >= totalVolumeCount) {
                    booksRepository.deleteSeriesById(seriesId)
                    Log.d(TAG, "全选删除：直接删除系列 $seriesId")
                    resetSeriesId() // 返回书架
                } else {
                    // 仅删除选中书籍
                    booksRepository.deleteVolumesByIds(selectedVolumeIds)
                    // 同步更新系列的书籍数量（volumeCount）
                    val newCount = (totalVolumeCount - selectedCount).coerceAtLeast(0)
                    booksRepository.updateSeries(series.copy(volumeCount = newCount))
                    Log.d(TAG, "部分删除：删除了 $selectedCount 本，剩余 $newCount 本")
                }
                clearSelectedVolume()
            } catch (e: Exception) {
                Log.e(TAG, "批量删除操作失败", e)
            }
        }
    }
}
