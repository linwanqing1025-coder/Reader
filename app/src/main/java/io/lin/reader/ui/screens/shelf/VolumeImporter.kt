package io.lin.reader.ui.screens.shelf

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import io.lin.reader.ReaderApplication
import io.lin.reader.data.booksrepository.BooksRepository
import io.lin.reader.data.database.Volume
import io.lin.reader.utils.FileUtils
import io.lin.reader.utils.MuPDFUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "VolumeImporter.kt"

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

class VolumeImporter(
    private val application: ReaderApplication,
    private val booksRepository: BooksRepository,
    private val viewModelScope: CoroutineScope
) {
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
                        mimeType = resolvedMimeType,
                        pageCount = pageCount,
                        seriesId = 0,
                        coverUri = existingCover
                    ).also {
                        Log.d(TAG, "第${index + 1}本书记载完成：$fileName, 类型: ${resolvedMimeType}, 封面状态: ${if(existingCover!=null) "已有" else "无"}")
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
}