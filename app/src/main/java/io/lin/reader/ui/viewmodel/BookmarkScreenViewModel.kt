package io.lin.reader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lin.reader.data.booksrepository.BooksRepository
import io.lin.reader.data.database.Bookmark
import io.lin.reader.data.database.VolumeWithBookmarks
import io.lin.reader.data.preferences.UserPreferencesRepository
import io.lin.reader.ui.screen.setting.details.BookmarkSortMethod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * BookmarkScreen 的 UI 状态封装
 */
data class BookmarkUiState(
    val volumesWithBookmarks: List<VolumeWithBookmarks> = emptyList(),
    val sortMethod: BookmarkSortMethod = BookmarkSortMethod.LatestBookmarkTime
)

/**
 * BookmarkScreen 的 ViewModel
 */
class BookmarkScreenViewModel(
    private val booksRepository: BooksRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    /**
     * 获取书签界面的 UI 状态
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BookmarkUiState> = userPreferencesRepository.bookmarkSortMethodFlow
        .flatMapLatest { sortMethod ->
            booksRepository.getVolumesWithBookmarksStream(sortMethod).map { volumes ->
                BookmarkUiState(
                    volumesWithBookmarks = volumes,
                    sortMethod = sortMethod
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BookmarkUiState()
        )

    /**
     * 更新书签排序方式
     */
    fun updateSortMethod(sortMethod: BookmarkSortMethod) {
        viewModelScope.launch {
            userPreferencesRepository.updateBookmarkSortMethod(sortMethod)
        }
    }

    /**
     * 删除单个书签
     */
    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            booksRepository.deleteBookmark(bookmark)
        }
    }

    /**
     * 清空所有书签
     */
    fun clearAllBookmarks(onResult: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val deletedCount = booksRepository.clearAllBookmarks()
            onResult(deletedCount)
        }
    }
}