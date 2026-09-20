package io.lin.reader.ui.maintab.reading

import android.app.Application
import android.graphics.PointF
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.artifex.mupdf.fitz.SeekableInputStream
import com.artifex.mupdf.viewer.ContentInputStream
import com.artifex.mupdf.viewer.OutlineActivity
import io.lin.reader.data.booksrepository.BooksRepository
import io.lin.reader.data.database.Bookmark
import io.lin.reader.data.database.PageSetting
import io.lin.reader.data.database.Series
import io.lin.reader.data.database.Volume
import io.lin.reader.data.preferences.ReaderPreferences
import io.lin.reader.data.preferences.ReaderPreferencesForTest
import io.lin.reader.data.preferences.ReaderPreferencesInterface
import io.lin.reader.data.preferences.ReflowPreferences
import io.lin.reader.data.preferences.ReflowPreferencesInterface
import io.lin.reader.data.preferences.ReflowPreferencesForTest
import io.lin.reader.data.preferences.UserPreferencesRepository
import io.lin.reader.data.preferences.VolumeSortMethod
import io.lin.reader.mupdf.render.MuPDFCoreExtended
import io.lin.reader.mupdf.search.MuPDFSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

data class DocumentSearchUiState(
    val currentSearchQuery: String = "",
    val isSearching: Boolean = false,
    val searchProgress: Float = 0f,
)

data class ReadingUiState(
    val error: String? = null,
    val currentPage: Int = 0,
    val volume: Volume? = null,
    val series: Series? = null,
    val volumeList: List<Volume> = emptyList(),
    val isEdgeVolume: Pair<Boolean, Boolean> = false to false,
    val bookmarkList: List<Bookmark> = emptyList(),
    val outlineList: List<OutlineActivity.Item> = emptyList(),
    val pageSettings: Map<Int, PageSetting> = emptyMap(),
    val searchUiState: DocumentSearchUiState = DocumentSearchUiState(),
    val highlightLinks: Boolean = false,
)

private const val APP = "MuPDF"

interface ReadingScreenViewmodelInterface {
    val uiState: StateFlow<ReadingUiState>
    val mCore: StateFlow<MuPDFCoreExtended?>
    val muPDFSearch: StateFlow<MuPDFSearch?>
    val mPageSizes: Map<Int, PointF>
    val readerPreferences: ReaderPreferencesInterface
    val reflowPreferences: ReflowPreferencesInterface

    suspend fun loadVolume(bookId: Long, jumpToPage: Int? = null, lastVolumeId: Long? = null)
    fun clearResource()
    fun loadPageSize(pageIndex: Int)
    fun performLayout(width: Float, height: Float)
    fun goToPage(pageIndex: Int)
    fun toggleHighlightLinks()
    fun rotateCurrentPage(pageIndex: Int)
    fun switchVolume(targetVolumeId: Long)
    fun switchVolume(direction: Int)
    fun updateSearchQuery(query: String)
    suspend fun executeStepSearch(direction: Int, currentPageIndex: Int): Boolean
    suspend fun executeFullSearch(): Boolean
    fun clearSearch()
    fun toggleFavouriteState()
    fun isPageBookmarked(currentPage: Int, bookmarkList: List<Bookmark>): Boolean
    fun generateAutoBookmarkLabel(): String
    fun addBookmark(label: String, pageNumber: Int)
    fun deleteBookmark(pageNumber: Int)
}

class ReadingScreenViewModel(
    application: Application,
    userPreferencesRepository: UserPreferencesRepository,
    private val booksRepository: BooksRepository
) : AndroidViewModel(application), ReadingScreenViewmodelInterface {
    override val readerPreferences =
        ReaderPreferences(userPreferencesRepository, viewModelScope)
    override val reflowPreferences =
        ReflowPreferences(userPreferencesRepository, viewModelScope)

    private val _uiState = MutableStateFlow(ReadingUiState())
    override val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()

    private val _mCore = MutableStateFlow<MuPDFCoreExtended?>(null)
    override val mCore: StateFlow<MuPDFCoreExtended?> = _mCore.asStateFlow()

    private val _muPDFSearch = MutableStateFlow<MuPDFSearch?>(null)
    override val muPDFSearch: StateFlow<MuPDFSearch?> = _muPDFSearch.asStateFlow()

    override val mPageSizes = mutableStateMapOf<Int, PointF>()

    private var searchJob: Job? = null
    private var bookmarksJob: Job? = null
    private var pageSettingsJob: Job? = null
    private var historySyncJob: Job? = null
    private val rendererMutex = Mutex()

    /**
     * 将上面的成员变量清理成默认状态
     */
    override fun clearResource() {
        _mCore.value?.onDestroy()
        _mCore.value = null
        clearSearch()
        bookmarksJob?.cancel()
        pageSettingsJob?.cancel()
        historySyncJob?.cancel()
        mPageSizes.clear()
    }

    /**
     * 防止 MuPDFCore 造成内存泄漏以及搜素未结束造成的性能浪费
     */
    override fun onCleared() {
        syncHistory()
        clearResource()
    }

    private fun openBuffer(buffer: ByteArray, magic: String): MuPDFCoreExtended? {
        return try {
            MuPDFCoreExtended(buffer, magic)
        } catch (e: Exception) {
            Log.e(APP, "Error opening document buffer: $e")
            null
        }
    }

    private fun openStream(stm: SeekableInputStream, magic: String): MuPDFCoreExtended? {
        return try {
            val core = MuPDFCoreExtended(stm, magic)
            core
        } catch (e: Exception) {
            Log.e(APP, "Error opening document stream: $e")
            null
        }
    }

    private suspend fun openCore(uri: Uri, size: Long, mimetype: String): MuPDFCoreExtended? =
        withContext(Dispatchers.IO) {
            val cr = getApplication<Application>().contentResolver
            var buf: ByteArray? = null

            try {
                val limit = 8 * 1024 * 1024
                if (size < 0) { // size is unknown
                    cr.openInputStream(uri)?.use { isStream ->
                        val tempBuf = ByteArray(limit)
                        val used = isStream.read(tempBuf)
                        val atEOF = isStream.read() == -1
                        if (used >= 0 && (used < limit || atEOF)) {
                            buf = if (tempBuf.size == used) tempBuf else tempBuf.copyOf(used)
                        }
                    }
                } else if (size <= limit) { // size is known and below limit
                    cr.openInputStream(uri)?.use { isStream ->
                        val tempBuf = ByteArray(size.toInt())
                        val used = isStream.read(tempBuf)
                        if (used >= 0 && used == size.toInt()) {
                            buf = tempBuf
                        }
                    }
                }
            } catch (_: OutOfMemoryError) {
                buf = null
            } catch (e: Exception) {
                Log.e(APP, "Error reading core: $e")
                buf = null
            }

            val buffer = buf
            if (buffer != null) {
                Log.i(APP, "  Opening document from memory buffer of size " + buffer.size)
                openBuffer(buffer, mimetype)
            } else {
                Log.i(APP, "  Opening document from stream")
                openStream(ContentInputStream(cr, uri, size), mimetype)
            }
        }

    /**
     * 搬运 DocumentActivity.java 的 onCreate() 方法中的部分逻辑。
     * 实现解析 URI 、解析文件大小和类型，并将结果传递给 openCore() 以初始化 MuPDFCore 。
     */
    private suspend fun createMuPDFDocumentActivity(volume: Volume) {
        val uri = volume.bookFileUri.toUri()
        val cr = getApplication<Application>().contentResolver

        var fileSize: Long = -1

        // Query metadata
        try {
            cr.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(APP, "Error querying URI metadata: $e")
        }

        if (fileSize == 0L) fileSize = -1

        val core = openCore(uri, fileSize, volume.mimeType)
        if (core != null) {
            _mCore.value = core
            _muPDFSearch.value = MuPDFSearch()
        }
    }

    /**
     * 加载单册：始终使用 Book ID 从数据库进行单册查询，确保数据时效性以及数据源唯一
     * @param bookId 目标单册
     * @param jumpToPage 是否顺便跳转至指定页，不传默认按历史记录
     * @param lastVolumeId 如果是在阅读器内执行单册切换，传递切换前的单册 ID
     */
    override suspend fun loadVolume(
        bookId: Long,
        jumpToPage: Int?,
        lastVolumeId: Long?
    ) {
        if (_mCore.value != null && bookId == lastVolumeId) return

        // 清理旧资源
        clearResource()

        // 获取新数据
        val volume = booksRepository.getVolumeById(bookId)
        val series = booksRepository.getSeriesById(volume.seriesId)
        val volumeList = booksRepository.getVolumesInSeries(
            seriesId = volume.seriesId,
            order = VolumeSortMethod.Name,
            isAscending = true
        )
        // 更新 UI
        _uiState.update {
            it.copy(
                volume = volume,
                series = series,
                volumeList = volumeList,
                currentPage = jumpToPage ?: volume.lastReadPage,
                isEdgeVolume = calculateEdgeVolume(volume, volumeList),
                error = null,
                highlightLinks = false,
            )
        }

        // 加载 MuPDFCore
        createMuPDFDocumentActivity(volume)

        // 获取目录
        val core = _mCore.value
        if (core != null && core.hasOutline()) {
            val outline = core.outline
            if (outline != null) {
                _uiState.update { it.copy(outlineList = outline, error = null) }
            }
        }

        // 获取书签和页面设置
        observeBookmarks(bookId)
        observePageSettings(bookId)

        // 页面跳转逻辑 (0-based)
        val startPage = jumpToPage ?: (if (volume.lastReadPage >= 0) volume.lastReadPage else 0)
        goToPage(startPage)
        updateLastRead(startPage)

        // 启动定时历史记录同步
        startHistorySync()
    }

    /**
     * 获取单页大小，结果会存进 mPageSizes
     */
    override fun loadPageSize(pageIndex: Int) {
        val core = _mCore.value ?: return
        if (mPageSizes.containsKey(pageIndex)) return

        viewModelScope.launch(Dispatchers.IO) {
            rendererMutex.withLock {
                try {
                    val size = core.getPageSize(pageIndex)
                    withContext(Dispatchers.Main) {
                        mPageSizes[pageIndex] = size
                    }
                } catch (e: Exception) {
                    Log.e("ReadingScreenViewModel", "Error getting page size for $pageIndex", e)
                }
            }
        }
    }

    /**
     * 执行重排逻辑：
     *  1. 容器大小变化
     */
    override fun performLayout(width: Float, height: Float) {
        val core = _mCore.value ?: return
        if (!core.isReflowable) return

        viewModelScope.launch(Dispatchers.IO) {
            rendererMutex.withLock {
                val currentP = _uiState.value.currentPage

                // 注入自定义 CSS
                Log.d("FontCheck","在干活吗？")
                core.customReflowStyle(
                    publisherCss = true,
                    userCss = if (reflowPreferences.usePublisherStyle) "" else reflowPreferences.generateCss()
                )
                Log.d("FontCheck","在干活。")

                // 执行排版: layout(int oldPage, int w, int h, int em)
                val newPage = core.layout(
                    currentP,
                    width.roundToInt(),
                    height.roundToInt(),
                    reflowPreferences.fontSize
                )

                // 更新状态
                val newTotalPages = core.countPages()
                withContext(Dispatchers.Main) {
                    mPageSizes.clear()
                    _uiState.update { state ->
                        state.copy(
                            volume = state.volume?.copy(totalPages = newTotalPages),
                            currentPage = newPage
                        )
                    }
                }
            }
        }
    }

    /**
     * 页面跳转
     */
    override fun goToPage(pageIndex: Int) {
        val totalCount = _uiState.value.volume?.totalPages ?: _mCore.value?.countPages() ?: 0
        if (totalCount <= 0) return
        // 增加边界检查：确保跳转页码在 0 到 (总页数-1)之间 (0-based)
        val safeIndex = pageIndex.coerceIn(0, totalCount - 1)
        _uiState.update { it.copy(currentPage = safeIndex) }
    }

    /**
     * 是否显示链接高亮
     */
    override fun toggleHighlightLinks() {
        _uiState.update { it.copy(highlightLinks = !it.highlightLinks) }
    }

    // 单页特殊设置
    private fun observePageSettings(bookId: Long) {
        pageSettingsJob?.cancel()
        pageSettingsJob = viewModelScope.launch {
            booksRepository.getPageSettingsByVolumeId(bookId).collectLatest { settings ->
                val settingsMap = settings.associateBy { it.pageIndex }
                _uiState.update { it.copy(pageSettings = settingsMap) }
            }
        }
    }

    /**
     * 响应操作按钮旋转单页
     */
    override fun rotateCurrentPage(pageIndex: Int) {
        val volumeId = _uiState.value.volume?.id ?: return
        val currentSetting = _uiState.value.pageSettings[pageIndex]
        val currentRotation = currentSetting?.rotation ?: 0
        val newRotation = (currentRotation + 90) % 360

        viewModelScope.launch {
            booksRepository.upsertPageSetting(
                PageSetting(
                    volumeId = volumeId,
                    pageIndex = pageIndex,
                    rotation = newRotation
                )
            )
        }
    }

    // 单册切换
    /**
     * 切换单册：接受 Volume Id ，直接跳转
     */
    override fun switchVolume(targetVolumeId: Long) {
        val lastVolumeId = _uiState.value.volume?.id
        if (lastVolumeId == targetVolumeId) return
        viewModelScope.launch {
            this@ReadingScreenViewModel.loadVolume(targetVolumeId)
        }
    }

    /**
     * 切换单册：接受一个方向偏移量。
     */
    override fun switchVolume(direction: Int) {
        val list = _uiState.value.volumeList
        val currentVolumeId = _uiState.value.volume?.id ?: return

        val currentIndex = list.indexOfFirst { it.id == currentVolumeId }

        if (currentIndex != -1) {
            val targetIndex = currentIndex + direction

            // 边界检查
            if (targetIndex in list.indices) {
                viewModelScope.launch {
                    this@ReadingScreenViewModel.loadVolume(list[targetIndex].id)
                }
            } else {
                Log.w("ReadingViewModel", "Target volume index $targetIndex is out of bounds.")
            }
        }
    }

    /**
     * 计算当前阅读的单册是否是系列的首尾册
     */
    private fun calculateEdgeVolume(v: Volume?, list: List<Volume>): Pair<Boolean, Boolean> {
        if (v == null || list.isEmpty()) return false to false
        return (list.first().id == v.id) to (list.last().id == v.id)
    }

    // 搜索
    /**
     * 更新搜索文本
     */
    override fun updateSearchQuery(query: String) {
        _uiState.update {
            it.copy(
                searchUiState = DocumentSearchUiState(query)
            )
        }
    }

    /**
     * 单步搜索
     */
    override suspend fun executeStepSearch(direction: Int, currentPageIndex: Int): Boolean {
        val searcher = _muPDFSearch.value ?: return false
        val core = _mCore.value ?: return false
        val query = _uiState.value.searchUiState.currentSearchQuery
        if (query.isBlank()) return false

        searchJob?.cancel() // 停止之前的搜索
        searchJob = currentCoroutineContext()[Job] // 捕获当前任务引用，以便手动停止

        // Loading
        _uiState.update { it.copy(searchUiState = it.searchUiState.copy(isSearching = true)) }

        return try {
            // IO 线程执行并等待结果
            val result = withContext(Dispatchers.IO) {
                searcher.findNextHit(
                    core = core,
                    text = query,
                    direction = direction,
                    displayPage = currentPageIndex,
                    onProgress = { p: Int ->
                        val totalPage = _uiState.value.volume?.totalPages ?: core.countPages()
                        val progress =
                            if (direction > 0) (p - currentPageIndex).toFloat() / (totalPage - currentPageIndex - 1)
                            else (currentPageIndex - p).toFloat() / (currentPageIndex + 1)
                        _uiState.update {
                            it.copy(
                                searchUiState = it.searchUiState.copy(
                                    searchProgress = progress
                                )
                            )
                        }
                    }
                )
            }

            // 搜到了就跳转翻页
            if (result != null) {
                goToPage(result.pageIndex)
                true
            } else {
                false
            }
        } finally {
            _uiState.update { it.copy(searchUiState = it.searchUiState.copy(isSearching = false)) }
            searchJob = null
        }
    }

    /**
     * 全文档搜索
     */
    override suspend fun executeFullSearch(): Boolean {
        val searcher = _muPDFSearch.value ?: return false
        val core = _mCore.value ?: return false
        val query = _uiState.value.searchUiState.currentSearchQuery
        if (query.isBlank()) return false

        searchJob?.cancel()
        searchJob = currentCoroutineContext()[Job] // 捕获当前任务引用

        _uiState.update { it.copy(searchUiState = it.searchUiState.copy(isSearching = true)) }

        return try {
            // IO 线程执行并等待结果
            val results = withContext(Dispatchers.IO) {
                searcher.searchDocument(
                    core = core,
                    text = query,
                    onProgress = { p ->
                        val totalPage = _uiState.value.volume?.totalPages ?: core.countPages()
                        val progress = if (totalPage > 0) (p + 1).toFloat() / totalPage else 0f
                        _uiState.update {
                            it.copy(
                                searchUiState = it.searchUiState.copy(
                                    searchProgress = progress
                                )
                            )
                        }
                    }
                )
            }
            results.isNotEmpty()
        } finally {
            _uiState.update { it.copy(searchUiState = it.searchUiState.copy(isSearching = false)) }
            searchJob = null
        }
    }

    /**
     * 清理搜索，包括 searchJob 、 UI State 、 searcher
     */
    override fun clearSearch() {
        searchJob?.cancel()
        muPDFSearch.value?.clear()
        _uiState.update {
            it.copy(
                searchUiState = DocumentSearchUiState()
            )
        }
    }

    // 收藏
    override fun toggleFavouriteState() {
        val currentVolume = _uiState.value.volume ?: return
        viewModelScope.launch {
            val updatedVolume = currentVolume.copy(isFavorite = !currentVolume.isFavorite)
            booksRepository.updateVolume(updatedVolume)
            _uiState.update { it.copy(volume = updatedVolume) }
        }
    }

    // 书签
    private fun observeBookmarks(bookId: Long) {
        bookmarksJob?.cancel()
        bookmarksJob = viewModelScope.launch {
            booksRepository.getBookmarksByVolumeId(bookId).collectLatest { bookmarks ->
                _uiState.update { it.copy(bookmarkList = bookmarks) }
            }
        }
    }

    override fun isPageBookmarked(currentPage: Int, bookmarkList: List<Bookmark>): Boolean =
        bookmarkList.any { it.pageNumber == currentPage }

    override fun generateAutoBookmarkLabel(): String {
        val bookmarkList = _uiState.value.bookmarkList
        val regex = Regex("^Bookmark (\\d{2})$")
        val existingNumbers = bookmarkList.mapNotNull { bookmark ->
            regex.find(bookmark.label)?.groupValues?.get(1)?.toIntOrNull()
        }.toSet()
        for (i in 1..99) if (i !in existingNumbers) return "Bookmark ${
            i.toString().padStart(2, '0')
        }"
        return "Bookmark 99+"
    }

    override fun addBookmark(label: String, pageNumber: Int) {
        val volumeId = _uiState.value.volume?.id ?: return
        viewModelScope.launch {
            booksRepository.insertBookmark(
                Bookmark(
                    volumeId = volumeId,
                    label = label,
                    pageNumber = pageNumber,
                    addTime = System.currentTimeMillis()
                )
            )
        }
    }

    override fun deleteBookmark(pageNumber: Int) {
        _uiState.value.bookmarkList.find { it.pageNumber == pageNumber }?.let {
            viewModelScope.launch { booksRepository.deleteBookmark(it) }
        }
    }


    // 历史记录
    /**
     * 手动更新记录，传递目标页数
     */
    private fun updateLastRead(page: Int) {
        val currentVolume = _uiState.value.volume ?: return
        viewModelScope.launch {
            val updatedVolume = currentVolume.copy(
                lastReadPage = page,
                lastReadTime = System.currentTimeMillis()
            )
            booksRepository.updateVolume(updatedVolume)
            _uiState.update { it.copy(volume = updatedVolume) }
        }
    }

    /**
     * 同步记录
     */
    private fun syncHistory() {
        val currentPage = _uiState.value.currentPage
        updateLastRead(currentPage)
    }

    /**
     * 启动自动同步
     */
    private fun startHistorySync() {
        historySyncJob?.cancel()
        historySyncJob = viewModelScope.launch {
            while (isActive) {
                delay(1.minutes)
                syncHistory()
            }
        }
    }
}

class ReadingScreenViewModelForTest : ReadingScreenViewmodelInterface {
    override val uiState: StateFlow<ReadingUiState> =
        MutableStateFlow(ReadingUiState()).asStateFlow()
    override val mCore: StateFlow<MuPDFCoreExtended?> =
        MutableStateFlow<MuPDFCoreExtended?>(null).asStateFlow()
    override val muPDFSearch: StateFlow<MuPDFSearch?> =
        MutableStateFlow<MuPDFSearch?>(null).asStateFlow()
    override val mPageSizes: Map<Int, PointF> = emptyMap()
    override val readerPreferences: ReaderPreferencesInterface = ReaderPreferencesForTest()
    override val reflowPreferences: ReflowPreferencesInterface = ReflowPreferencesForTest()

    override suspend fun loadVolume(bookId: Long, jumpToPage: Int?, lastVolumeId: Long?) {}
    override fun clearResource() {}
    override fun loadPageSize(pageIndex: Int) {}
    override fun goToPage(pageIndex: Int) {}
    override fun toggleHighlightLinks() {}
    override fun rotateCurrentPage(pageIndex: Int) {}
    override fun switchVolume(targetVolumeId: Long) {}
    override fun switchVolume(direction: Int) {}
    override fun updateSearchQuery(query: String) {}
    override suspend fun executeStepSearch(direction: Int, currentPageIndex: Int): Boolean = false
    override suspend fun executeFullSearch(): Boolean = false
    override fun clearSearch() {}
    override fun toggleFavouriteState() {}
    override fun isPageBookmarked(currentPage: Int, bookmarkList: List<Bookmark>): Boolean = false
    override fun generateAutoBookmarkLabel(): String = ""
    override fun addBookmark(label: String, pageNumber: Int) {}
    override fun deleteBookmark(pageNumber: Int) {}
    override fun performLayout(width: Float, height: Float) {}
}
