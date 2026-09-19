package io.lin.reader.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lin.reader.data.booksrepository.BooksRepository
import io.lin.reader.data.preferences.AppPreferences
import io.lin.reader.data.preferences.UserPreferencesRepository
import io.lin.reader.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(
    private val booksRepository: BooksRepository,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val appPreferences = AppPreferences(userPreferencesRepository, viewModelScope)

    /**
     * 全局封面保存逻辑：更新数据库中文件 URI 与封面图片 URI 的映射。
     */
    fun saveCoverMapping(fileUri: String, coverUri: String) {
        viewModelScope.launch {
            booksRepository.saveCoverUriMapping(fileUri, coverUri)
        }
    }

    /**
     * 处理封面损坏逻辑：物理删除坏文件，并重置数据库映射。
     * @param fileUri 文档文件自身的 URI
     * @param brokenCoverUri 损坏的封面图片的 URI
     */
    fun handleCoverError(fileUri: String, brokenCoverUri: String) {
        viewModelScope.launch {
            // 1. 物理删除（IO 线程）
            withContext(Dispatchers.IO) {
                FileUtils.deleteFileIfUriIsLocal(brokenCoverUri)
            }
            // 2. 逻辑重置：清除数据库中的坏路径，触发 Flow 刷新
            booksRepository.saveCoverUriMapping(fileUri, "")
        }
    }
}

/**
 * CompositionLocal 用于在全局范围内共享 AppViewModel，避免层层传递。
 */
val LocalAppViewModel = staticCompositionLocalOf<AppViewModel> {
    error("No AppViewModel provided")
}
