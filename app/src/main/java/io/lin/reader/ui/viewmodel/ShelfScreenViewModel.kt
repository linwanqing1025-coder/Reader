package io.lin.reader.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import io.lin.reader.data.preferences.SortPreference
import io.lin.reader.data.preferences.UserPreferencesRepository
import io.lin.reader.ui.screen.setting.details.SeriesSortMethod
import io.lin.reader.ui.screen.setting.details.VolumeSortMethod
import io.lin.reader.utils.FileUtils
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

data class BookDetails(
    val fileUri: String = "",
    val volumeName: String = "Default Volume Name",
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
    val top3CoverUris: List<String?> = emptyList() // 该系列前三本书的封面Uri
)

/**
 * ShelfScreen使用的ViewModel
 * */
class ShelfScreenViewModel(
    private val booksRepository: BooksRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val application: ReaderApplication
) : ViewModel() {

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
            Log.d("LuoTest", "URI列表为空，无需更新BookUiState")
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
                    Log.e("LuoTest", "第${index + 1}个URI解析失败：$uriStr", e)
                    null
                }

                if (uri == null) {
                    null
                } else {
                    // 2.2 在 IO 线程获取文件名和PDF页数（耗时操作，使用 withContext 挂起）
                    val (fileName, pageCount, existingCover) = withContext(Dispatchers.IO) {
                        val name =
                            FileUtils.getFileName(application, uri) ?: "Unnamed Book ${index + 1}"
                        val count = FileUtils.getFilePageCount(application, uri)
                        val cover = booksRepository.getCoverUriByFileUri(uriStr) // 提前查询库中是否已有该文件的封面
                        Triple(name, count, cover)
                    }

                    // 2.3 生成 BookDetails
                    BookDetails(
                        fileUri = uriStr,
                        volumeName = fileName,
                        pageCount = pageCount,
                        seriesId = 0,
                        coverUri = existingCover
                    ).also {
                        Log.d("LuoTest", "第${index + 1}本书记载完成：$fileName, 封面状态: ${if(existingCover!=null) "已有" else "无"}")
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
                "LuoTest",
                "BookUiState批量更新完成：\n" +
                        "总URI数：${fileUriStrList.size}\n" +
                        "有效书籍数：${results.size}\n" +
                        "输入验证结果：${volumesToImport.isEntryValid}"
            )
        }
    }

    /**
     * 获取单个文件的封面并异步存入数据库。
     * 这是一个挂起函数，但它内部处理封面生成并调用 repository 进行映射保存。
     * @param fileUriStr 文件URI字符串
     */
    private suspend fun processAndSaveCover(fileUriStr: String) {
        val uri = try {
            fileUriStr.toUri()
        } catch (e: Exception) {
            Log.e("LuoTest", "URI 解析失败：$fileUriStr", e)
            return
        }

        // 1. 尝试从数据库获取已有的封面记录
        var coverUriStr: String? = booksRepository.getCoverUriByFileUri(fileUriStr)

        if (coverUriStr == null) {
            Log.d("LuoTest", "文件[$fileUriStr]无封面，开始生成...")
            // 2. 在 IO 线程执行耗时的封面生成
            coverUriStr = withContext(Dispatchers.IO) {
                FileUtils.getCover(application, uri)
            }

            // 3. 生成成功则存入数据库，并触发 Repository 内的刷新逻辑
            if (coverUriStr != null) {
                Log.d("LuoTest", "文件[$fileUriStr]封面生成成功：$coverUriStr，存入数据库映射")
                booksRepository.saveCoverUriMapping(fileUriStr, coverUriStr)
            } else {
                Log.e("LuoTest", "文件[$fileUriStr]封面生成失败")
            }
        } else {
            // 补丁：即使找到了封面，也要确保本次导入中那些 coverUri 还是 null 的记录被补全
            // 这解决了“导入已存在文件的复刻本时封面不显示”的问题
            Log.d("LuoTest", "文件[$fileUriStr]已有封面：$coverUriStr, 确保同步到所有记录")
            booksRepository.saveCoverUriMapping(fileUriStr, coverUriStr)
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
            Log.d("LuoTest", "输入验证失败，取消保存")
            return
        }

        viewModelScope.launch {
            try {
                // 1. 立即转换并入库（如果 coverUri 在 updateVolumesToImportByUriList 中已查到，则带上）
                val volumeList = books.map { it.copy(seriesId = 0).toVolume() }
                
                Log.d("LuoTest", "触发快速入库到新系列：${volumeList.size} 本书")
                val insertedCount = booksRepository.addVolumesWithNewSeries(volumeList, newSeriesName)

                if (insertedCount > 0) {
                    Log.d("LuoTest", "新系列入库成功，开始后台生成封面...")
                    // 2. 后台并发处理封面：对 fileUri 进行去重，避免同一文件重复启动生成任务
                    books.map { it.fileUri }.distinct().forEach { fileUri ->
                        launch(Dispatchers.Default) {
                            processAndSaveCover(fileUri)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LuoTest", "批量保存到新系列失败", e)
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
                
                Log.d("LuoTest", "触发快速入库到已有系列：$seriesId")
                val insertedCount = booksRepository.addVolumesInExistingSeries(volumeList)

                if (insertedCount > 0) {
                    Log.d("LuoTest", "已有系列入库成功，开始后台生成封面...")
                    // 2. 后台并发处理封面：去重 fileUri
                    books.map { it.fileUri }.distinct().forEach { fileUri ->
                        launch(Dispatchers.Default) {
                            processAndSaveCover(fileUri)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LuoTest", "批量保存到已有系列失败：$seriesId", e)
            }
        }
    }

    /**
     * Series排序方式和正序倒序
     * */
    val seriesSortPreference: StateFlow<SortPreference<SeriesSortMethod>> =
        userPreferencesRepository.seriesSortPreferencesFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SortPreference(SeriesSortMethod.Name, true) // 提供一个初始值
            )

    /**
     * 更新Series排序方法的函数
     */
    fun updateSeriesSortMethod(sortMethod: SeriesSortMethod) {
        viewModelScope.launch {
            userPreferencesRepository.updateSeriesSortMethod(sortMethod)
        }
    }

    /**
     * 切换Series正/倒序的函数
     */
    fun toggleSeriesSortAscending() {
        viewModelScope.launch {
            val currentAscending = seriesSortPreference.value.isAscending
            userPreferencesRepository.updateSeriesSortAscending(!currentAscending)
        }
    }

    /**
     * 系列方格数据流
     * */
    @OptIn(ExperimentalCoroutinesApi::class)
    val seriesPagingItems: Flow<PagingData<ShelfItem>> = seriesSortPreference
        .flatMapLatest { sortPreference ->
            booksRepository.getAllSeriesStream(
                sortPreference.sortMethod,
                sortPreference.isAscending
            )
        }
        .map { pagingData ->
            pagingData.map { series ->
                val top3Volumes =
                    booksRepository.getTop3VolumesBySeriesIdOrderedByName(series.id).first()
                val top3CoverUris = top3Volumes.map { it.coverUri }
                ShelfItem(series, top3CoverUris)
            }
        }
        .cachedIn(viewModelScope)

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
                    Log.d("LuoTest", "更新系列名成功: $newSeriesName")
                }
            } catch (e: Exception) {
                Log.e("LuoTest", "更新系列名失败: $seriesId", e)
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
                    Log.d("LuoTest", "删除系列成功：$seriesId")
                } else {
                    Log.d("LuoTest", "删除失败：系列 $seriesId 可能不存在")
                }
            } catch (e: Exception) {
                Log.e("LuoTest", "删除系列异常", e)
            }
        }
    }

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
     * Volume排序方式和正序倒序
     */
    val volumeSortPreference: StateFlow<SortPreference<VolumeSortMethod>> =
        userPreferencesRepository.volumeSortPreferencesFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SortPreference(VolumeSortMethod.Name, true)
            )

    /**
     * 更新 Volume 排序方法
     */
    fun updateVolumeSortMethod(sortMethod: VolumeSortMethod) {
        viewModelScope.launch {
            userPreferencesRepository.updateVolumeSortMethod(sortMethod)
        }
    }

    /**
     * 切换 Volume 正/倒序
     */
    fun toggleVolumeSortAscending() {
        viewModelScope.launch {
            val currentAscending = volumeSortPreference.value.isAscending
            userPreferencesRepository.updateVolumeSortAscending(!currentAscending)
        }
    }

    /**
     * 当前选定系列下的书籍分页数据流
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSeriesVolumesPagingItems: Flow<PagingData<Volume>> = combine(
        _currentSeriesId,
        volumeSortPreference
    ) { id, sortPref ->
        id to sortPref
    }.flatMapLatest { (id, sortPref) ->
        if (id == 0L) {
            flowOf(PagingData.empty())
        } else {
            booksRepository.getVolumesStreamInSeries(
                seriesId = id,
                order = sortPref.sortMethod,
                isAscending = sortPref.isAscending
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
                val sortPref = volumeSortPreference.value
                val volumes = booksRepository.getVolumesInSeries(
                    seriesId = seriesId,
                    order = sortPref.sortMethod,
                    isAscending = sortPref.isAscending
                )
                _selectedVolume.value = volumes.map { it.id }.toSet()
                Log.d("LuoTest", "全选系列中书籍：共选中 ${_selectedVolume.value.size} 本")
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
                    Log.d("LuoTest", "全选删除：直接删除系列 $seriesId")
                    resetSeriesId() // 返回书架
                } else {
                    // 仅删除选中书籍
                    booksRepository.deleteVolumesByIds(selectedVolumeIds)
                    // 同步更新系列的书籍数量（volumeCount）
                    val newCount = (totalVolumeCount - selectedCount).coerceAtLeast(0)
                    booksRepository.updateSeries(series.copy(volumeCount = newCount))
                    Log.d("LuoTest", "部分删除：删除了 $selectedCount 本，剩余 $newCount 本")
                }
                clearSelectedVolume()
            } catch (e: Exception) {
                Log.e("LuoTest", "批量删除操作失败", e)
            }
        }
    }
}
