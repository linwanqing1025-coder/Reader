package io.lin.reader.mupdf.search

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.artifex.mupdf.fitz.Quad
import com.artifex.mupdf.fitz.StructuredText
import io.lin.reader.mupdf.core.MuPDFCoreExtended
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private const val TAG = "MuPDFSearch.kt"

/**
 * 单页搜索结果
 */
data class PageSearchResult(
    val pageIndex: Int,
    val searchBoxes: Array<Array<Quad>>?
)

private const val SEARCH_STYLE =
        StructuredText.SEARCH_IGNORE_CASE or
        StructuredText.SEARCH_REGEXP

/**
 * 重写的 SearchTask.java 与 SearchTaskResult.java
 */
class MuPDFSearch {
    // 使用 mutableStateOf 便于更新 UI ，同时保证与官方架构相似
    var txt by mutableStateOf<String?>(null)
    var focusedItem by mutableStateOf<PageSearchResult?>(null)
    var allItems by mutableStateOf<List<PageSearchResult>?>(null)


    fun clear() {
        txt = null
        focusedItem = null
        allItems = null
    }

    /**
     * 单步搜索，若 allItems 中有结果，则从里面找
     * @param onProgress 进度反馈回调函数，参数为正在检索的页索引
     */
    suspend fun findNextHit(
        core: MuPDFCoreExtended,
        text: String,
        direction: Int,
        displayPage: Int,
        onProgress: (Int) -> Unit = {}
    ): PageSearchResult? = withContext(Dispatchers.IO) {
        if (text.isEmpty() || direction == 0) return@withContext null

        // 检查 allItems
        if (txt == text && !allItems.isNullOrEmpty()) {
            val all = allItems!!
            val next = if (direction > 0) {
                all.firstOrNull { it.pageIndex > displayPage } ?: all.first()
            } else {
                all.lastOrNull { it.pageIndex < displayPage } ?: all.last()
            }
            withContext(Dispatchers.Main) { focusedItem = next }
            return@withContext next
        }

        // 词变了则重置
        withContext(Dispatchers.Main) {
            if (text != txt) clear()
            txt = text
        }

        // 逐页检索
        val start = focusedItem?.pageIndex ?: -1
        var index = if (start == -1) displayPage else start + direction
        val pageCount = core.countPages()

        // 生成正则表达式文以支持断行文本搜索
        val pattern = text.map { ch ->
            if (ch in "\\.[]{}()*+?^$|") "\\$ch" else ch.toString()
        }.joinToString("\\s*")

        while (index in 0 until pageCount && isActive) {
            withContext(Dispatchers.Main) { onProgress(index) }

            val searchHits = try {
                // TODO: 测试断行词搜索
                Log.d(TAG, "pattern: $pattern")
                core.searchPage(index, pattern, SEARCH_STYLE)
            } catch (e: Exception) {
                null
            }

            if (!searchHits.isNullOrEmpty()) {
                val result = PageSearchResult(index, searchHits)
                withContext(Dispatchers.Main) {
                    focusedItem = result
                }
                return@withContext result
            }
            index += direction
        }
        return@withContext null
    }

    /**
     * 全文档搜索
     * @param onProgress 进度反馈回调函数，参数为正在检索的页索引
     */
    suspend fun searchDocument(
        core: MuPDFCoreExtended,
        text: String,
        onProgress: (Int) -> Unit = {}
    ): List<PageSearchResult> = withContext(Dispatchers.IO) {
        if (text.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<PageSearchResult>()
        val pageCount = core.countPages()

        for (i in 0 until pageCount) {
            if (!isActive) break
            withContext(Dispatchers.Main) { onProgress(i) }

            val searchHits = try {
                core.searchPage(i, text, SEARCH_STYLE)
            } catch (e: Exception) {
                null
            }
            if (!searchHits.isNullOrEmpty()) {
                results.add(PageSearchResult(i, searchHits))
            }
        }

        withContext(Dispatchers.Main) {
            txt = text
            allItems = results
        }
        return@withContext results
    }
}
