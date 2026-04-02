package io.lin.reader.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.lin.reader.data.booksrepository.BooksRepository
import io.lin.reader.data.database.Bookmark
import io.lin.reader.data.database.Series
import io.lin.reader.data.database.Volume
import io.lin.reader.data.preferences.ReaderPreferences
import io.lin.reader.data.preferences.UserPreferencesRepository
import io.lin.reader.ui.screen.reading.ReadingScreenDestination
import io.lin.reader.ui.screen.reading.renderPdfPage
import io.lin.reader.ui.screen.setting.details.PageAlignment
import io.lin.reader.ui.screen.setting.details.ReadingMode
import io.lin.reader.ui.screen.setting.details.VolumeSortMethod
import io.lin.reader.utils.InteractionStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext


data class ReadingUiState(
    val volume: Volume? = null,
    val series: Series? = null,
    val volumeList: List<Volume> = emptyList(),
    val bookmarkList: List<Bookmark> = emptyList(),
    val initialPage: Int? = null,
    val isEdgeVolume: Pair<Boolean, Boolean> = false to false,
    val error: String? = null
)

class ReadingScreenViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val booksRepository: BooksRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ReadingUiState())
    val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()

    // 提升渲染器和缓存到 ViewModel 以在配置更改（旋转屏幕）后保持
    private val _pdfRenderer = MutableStateFlow<PdfRenderer?>(null)
    val pdfRenderer = _pdfRenderer.asStateFlow()

    private val _pageCount = MutableStateFlow(0)
    val pageCount = _pageCount.asStateFlow()

    // 缓存 Bitmap，key 为 pageIndex
    val cachedBitmaps = mutableStateMapOf<Int, Bitmap>()
    private val rendererMutex = Mutex()

    // 记录上一次渲染时书籍ID和渲染宽度，用于判断是否真的需要重新渲染
    private var lastVolumeId: Long = -1
    private var lastRenderedWidth = -1

    init {
        Log.d("ReadingScreenViewModel", "ViewModel initialized")
        viewModelScope.launch {
            try {
                val bookId: Long? = savedStateHandle.get<Long>(ReadingScreenDestination.bookIdArg)
                val pageNumber: Int? =
                    savedStateHandle.get<Int>(ReadingScreenDestination.pageNumberArg)

                Log.d("ReadingScreenViewModel", "Book id: $bookId, Page number: $pageNumber")
                if (bookId != null) {
                    loadVolume(bookId, pageNumber)
                    observeBookmarks(bookId)
                } else {
                    _uiState.update {
                        it.copy(error = "Book ID not found in navigation arguments.")
                    }
                }
            } catch (e: Exception) {
                Log.e("ReadingScreenViewModel", "Error initializing ViewModel", e)
                _uiState.update {
                    it.copy(error = e.message)
                }
            }
        }
    }

    private suspend fun loadVolume(bookId: Long, jumpToPage: Int? = null) {
        val volume = booksRepository.getVolumeById(bookId)
        val series = booksRepository.getSeriesById(volume.seriesId)
        val volumeList = booksRepository.getVolumesInSeries(
            seriesId = volume.seriesId,
            order = VolumeSortMethod.Name,
            isAscending = true
        )

        val edgeStatus = calculateEdgeVolume(volume, volumeList)

        _uiState.update {
            it.copy(
                volume = volume,
                series = series,
                volumeList = volumeList,
                initialPage = if (jumpToPage != null && jumpToPage > 0) jumpToPage else null,
                isEdgeVolume = edgeStatus,
                error = null
            )
        }

        initializePdfRenderer(volume)
        
        // 初始进入时，通过 updateLastReadPage 同时更新页码和时间戳，解决历史记录中显示第0页的问题
        val startPage = if (jumpToPage != null && jumpToPage > 0) jumpToPage 
                        else if (volume.lastReadPage > 0) volume.lastReadPage 
                        else 1
        updateLastRead(startPage)
    }

    private fun initializePdfRenderer(volume: Volume) {
        viewModelScope.launch {
            try {
                // 如果是同一本书且渲染器已就绪，不处理
                if (_pdfRenderer.value != null && lastVolumeId == volume.id) {
                    return@launch
                }

                // 彻底清理旧书籍状态
                _pdfRenderer.value?.close()
                _pdfRenderer.value = null
                cachedBitmaps.clear()
                lastVolumeId = volume.id
                lastRenderedWidth = -1

                val uri = volume.bookFileUri.toUri()
                val renderer = withContext(Dispatchers.IO) {
                    val pfd =
                        getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")
                    if (pfd != null) PdfRenderer(pfd) else null
                }

                if (renderer != null) {
                    _pdfRenderer.value = renderer
                    _pageCount.value = renderer.pageCount
                }
            } catch (e: Exception) {
                Log.e("ReadingScreenViewModel", "Error initializing PdfRenderer", e)
                onPdfError(e)
            }
        }
    }

    /**
     * 预加载页面
     * @param targetPages 目标页码列表 (0-based)
     * @param bitmapWidth 渲染宽度
     * @param removeGutter 是否自动去除订口
     */
    fun prefetchPages(
        targetPages: List<Int>,
        bitmapWidth: Int,
        removeGutter: Boolean
    ) {
        val renderer = _pdfRenderer.value ?: return

        // 如果宽度发生重大变化（比如从竖屏转横屏且布局导致宽度变化），可能需要重新渲染
        if (lastRenderedWidth == -1) {
            lastRenderedWidth = bitmapWidth
        }

        // 清理远离当前视口的缓存
        val margin = 10
        val minPage = targetPages.minOrNull() ?: 0
        val maxPage = targetPages.maxOrNull() ?: 0
        val keysToRemove =
            cachedBitmaps.keys.filter { it < minPage - margin || it > maxPage + margin }
        keysToRemove.forEach { cachedBitmaps.remove(it) }

        viewModelScope.launch(Dispatchers.IO) {
            targetPages.forEach { pageIndex ->
                if (!cachedBitmaps.containsKey(pageIndex)) {
                    val bitmap =
                        renderPdfPage(renderer, pageIndex, lastRenderedWidth, rendererMutex, removeGutter)
                    if (bitmap != null) {
                        withContext(Dispatchers.Main) {
                            cachedBitmaps[pageIndex] = bitmap
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _pdfRenderer.value?.close()
        cachedBitmaps.clear()
    }

    fun toggleFavouriteState() {
        val currentVolume = _uiState.value.volume ?: return
        viewModelScope.launch {
            try {
                val updatedVolume = currentVolume.copy(isFavorite = !currentVolume.isFavorite)
                booksRepository.updateVolume(updatedVolume)
                _uiState.update { state ->
                    state.copy(
                        volume = updatedVolume,
                        volumeList = state.volumeList.map {
                            if (it.id == updatedVolume.id) updatedVolume else it
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("ReadingScreenViewModel", "Error toggling favourite state", e)
            }
        }
    }

    private fun observeBookmarks(bookId: Long) {
        viewModelScope.launch {
            booksRepository.getBookmarksByVolumeId(bookId).collectLatest { bookmarks ->
                _uiState.update { it.copy(bookmarkList = bookmarks) }
            }
        }
    }

    fun generateAutoBookmarkLabel(): String {
        val bookmarkList = _uiState.value.bookmarkList
        val regex = Regex("^Bookmark (\\d{2})$")
        val existingNumbers = bookmarkList.mapNotNull { bookmark ->
            regex.find(bookmark.label)?.groupValues?.get(1)?.toIntOrNull()
        }.toSet()
        for (i in 1..99) {
            if (i !in existingNumbers) {
                return "Bookmark ${i.toString().padStart(2, '0')}"
            }
        }
        return "Bookmark 99+"
    }

    fun addBookmark(label: String, pageNumber: Int) {
        val volumeId = _uiState.value.volume?.id ?: return
        viewModelScope.launch {
            val bookmark = Bookmark(
                volumeId = volumeId,
                label = label,
                pageNumber = pageNumber,
                addTime = System.currentTimeMillis()
            )
            booksRepository.insertBookmark(bookmark)
        }
    }

    fun deleteBookmark(pageNumber: Int) {
        val bookmark = _uiState.value.bookmarkList.find { it.pageNumber == pageNumber }
        if (bookmark != null) {
            viewModelScope.launch {
                booksRepository.deleteBookmark(bookmark)
            }
        }
    }

    fun switchToNextVolume() {
        val currentState = _uiState.value
        val currentVolume = currentState.volume ?: return
        val list = currentState.volumeList
        val currentIndex = list.indexOfFirst { it.id == currentVolume.id }
        if (currentIndex != -1 && currentIndex < list.size - 1) {
            viewModelScope.launch {
                val nextVolumeId = list[currentIndex + 1].id
                loadVolume(nextVolumeId)
                observeBookmarks(nextVolumeId)
            }
        }
    }

    fun switchToPreviousVolume() {
        val currentState = _uiState.value
        val currentVolume = currentState.volume ?: return
        val list = currentState.volumeList
        val currentIndex = list.indexOfFirst { it.id == currentVolume.id }
        if (currentIndex > 0) {
            viewModelScope.launch {
                val prevVolumeId = list[currentIndex - 1].id
                loadVolume(prevVolumeId)
                observeBookmarks(prevVolumeId)
            }
        }
    }

    fun onVolumeSelected(volumeId: Long) {
        val currentState = _uiState.value
        if (currentState.volume?.id != volumeId) {
            viewModelScope.launch {
                loadVolume(volumeId)
                observeBookmarks(volumeId)
            }
        }
    }

    private fun calculateEdgeVolume(
        currentVolume: Volume?,
        volumeList: List<Volume>
    ): Pair<Boolean, Boolean> {
        if (currentVolume == null || volumeList.isEmpty()) return false to false
        val isFirst = volumeList.first().id == currentVolume.id
        val isLast = volumeList.last().id == currentVolume.id
        return isFirst to isLast
    }

    fun onPdfError(error: Throwable) {
        _uiState.update {
            it.copy(error = error.message)
        }
    }

    fun updateLastRead(page: Int) {
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

    fun isPageBookmarked(currentPage: Int, bookmarkList: List<Bookmark>): Boolean {
        return bookmarkList.any { it.pageNumber == currentPage }
    }

    val readingSetting = ReadingManager()

    inner class ReadingManager {
        val state: StateFlow<ReaderPreferences> = userPreferencesRepository.readerPreferencesFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = ReaderPreferences()
            )

        fun toggleRtlMode() {
            viewModelScope.launch {
                val current = state.value.rtlMode
                userPreferencesRepository.updateRtlMode(!current)
            }
        }

        fun toggleRemoveGutter() {
            viewModelScope.launch {
                val current = state.value.removeGutter
                userPreferencesRepository.updateRemoveGutter(!current)
                // 清理缓存以重新渲染
                cachedBitmaps.clear()
            }
        }

        fun updateReadingMode(mode: ReadingMode) {
            viewModelScope.launch {
                userPreferencesRepository.updateReadingMode(mode)
            }
        }

        fun updatePageAlignment(alignment: PageAlignment) {
            viewModelScope.launch {
                userPreferencesRepository.updatePageAlignment(alignment)
            }
        }

        fun updatePagePaddingRatio(paddingRatio: Float) {
            viewModelScope.launch {
                userPreferencesRepository.updatePagePaddingRatio(paddingRatio)
            }
        }

        fun toggleSeparateCover() {
            viewModelScope.launch {
                val current = state.value.separateCover
                userPreferencesRepository.updateSeparateCover(!current)
            }
        }

        fun toggleFixedPageIndicator() {
            viewModelScope.launch {
                val current = state.value.fixedPageIndicator
                userPreferencesRepository.updateFixedPageIndicator(!current)
            }
        }

        fun updateInteractionStyle(style: InteractionStyle) {
            viewModelScope.launch {
                userPreferencesRepository.updateInteractionStyle(style)
            }
        }

        fun clearInteractionStyleHint() {
            viewModelScope.launch {
                userPreferencesRepository.clearInteractionStyleHint()
            }
        }

        fun updateBackgroundColor(color: Int) {
            viewModelScope.launch {
                userPreferencesRepository.updateBackgroundColor(color)
            }
        }

        fun updatePageColor(color: Int) {
            viewModelScope.launch {
                userPreferencesRepository.updatePageColor(color)
            }
        }

        fun updateFilterColor(color: Int) {
            viewModelScope.launch {
                userPreferencesRepository.updateFilterColor(color)
            }
        }
    }
}
