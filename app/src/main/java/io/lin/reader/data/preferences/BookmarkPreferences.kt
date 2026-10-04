package io.lin.reader.data.preferences

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import io.lin.reader.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "BookmarkPreferences.kt"

/**
 * 书签页面的书籍排序方式
 */
enum class BookmarkSortMethod(
    @StringRes val displayName: Int
) {
    VolumeName(R.string.bookmark_sort_volume_name),         // 按 Volume 名称
    LastReadTime(R.string.bookmark_sort_last_read_time),       // 按 Volume 上次阅读时间
    VolumeCreateTime(R.string.bookmark_sort_volume_create_time),   // 按 Volume 创建时间
    LatestBookmarkTime(R.string.bookmark_sort_latest_bookmark_time)  // 按最新书签的添加时间
}

interface BookmarkPreferencesInterface {
    val bookmarkSortMethod: BookmarkSortMethod
    fun updateBookmarkSortMethod(method: BookmarkSortMethod)
}

data class BookmarkPreferences(
    private val repo: UserPreferencesRepository,
    private val scope: CoroutineScope
) : BookmarkPreferencesInterface {
    override var bookmarkSortMethod by mutableStateOf(BookmarkSortMethod.LatestBookmarkTime)

    init {
        scope.launch {
            repo.dataStore.data.collect { preferences ->
                val bookmarkMethodName = preferences[UserPreferencesRepository.BOOKMARK_SORT_METHOD]
                    ?: BookmarkSortMethod.LatestBookmarkTime.name
                bookmarkSortMethod = try {
                    BookmarkSortMethod.valueOf(bookmarkMethodName)
                } catch (e: Exception) {
                    e.message?.let { Log.e(TAG, it) }
                    BookmarkSortMethod.LatestBookmarkTime
                }
            }
        }
    }

    override fun updateBookmarkSortMethod(method: BookmarkSortMethod) {
        scope.launch {
            repo.dataStore.edit { it[UserPreferencesRepository.BOOKMARK_SORT_METHOD] = method.name }
        }
    }
}

class BookmarkPreferencesForTest : BookmarkPreferencesInterface {
    override var bookmarkSortMethod = BookmarkSortMethod.VolumeName
    override fun updateBookmarkSortMethod(method: BookmarkSortMethod) {}
}