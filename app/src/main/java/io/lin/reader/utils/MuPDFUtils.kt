package io.lin.reader.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.WorkerThread
import com.artifex.mupdf.fitz.Archive
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.viewer.ContentInputStream
import com.artifex.mupdf.viewer.MuPDFCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import androidx.core.graphics.createBitmap
import io.lin.reader.utils.FileUtils.getMimeType
import java.lang.reflect.Field

/**
 * 文档工具类：使用 MuPDFCore 支持多种文档格式（PDF, EPUB, XPS, CBZ 等）
 */
object MuPDFUtils {
    private const val TAG = "MuPDFUtils.kt"
    private const val COVER_DIR_NAME = "covers"
    private const val MAX_COVER_SIDE_PX = 512
    private const val A4_ASPECT_RATIO = 1.4

    /**
     * 辅助方法：从 Uri 初始化 MuPDFCore。
     * 逻辑参考自 ReadingScreenViewModel.kt
     */
    private fun openCore(context: Context, uri: Uri): MuPDFCore? {
        val cr = context.contentResolver
        var fileSize: Long = -1
        val mimeType = getMimeType(context, uri) ?: "application/pdf"

        try {
            cr.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying URI metadata: $e")
        }

        if (fileSize == 0L) fileSize = -1

        var buf: ByteArray? = null
        try {
            val limit = 8 * 1024 * 1024
            val inputStream = cr.openInputStream(uri)
            if (fileSize < 0) {
                inputStream?.use { isStream ->
                    val tempBuf = ByteArray(limit)
                    val used = isStream.read(tempBuf)
                    val atEOF = isStream.read() == -1
                    if (used >= 0 && (used < limit || atEOF)) {
                        buf = if (tempBuf.size == used) tempBuf else tempBuf.copyOf(used)
                    }
                }
            } else if (fileSize <= limit) {
                inputStream?.use { isStream ->
                    val tempBuf = ByteArray(fileSize.toInt())
                    val used = isStream.read(tempBuf)
                    if (used >= 0 && used == fileSize.toInt()) {
                        buf = tempBuf
                    }
                }
            } else {
                inputStream?.close() // 不在内存加载，后续使用 stream
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading core buffer: $e")
        }

        return try {
            if (buf != null) {
                MuPDFCore(buf, mimeType)
            } else {
                MuPDFCore(ContentInputStream(cr, uri, fileSize), mimeType)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open MuPDFCore: $e")
            null
        }
    }

    /**
     * 获取文档的总页数。
     */
    @WorkerThread
    suspend fun getPageCount(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        var core: MuPDFCore? = null
        try {
            core = openCore(context, uri)
            return@withContext core?.countPages() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting page count: $e")
            0
        } finally {
            core?.onDestroy()
        }
    }

    /**
     * 将文档的特定页渲染为 Bitmap。
     * 外部需传入已打开的 MuPDFCore，本函数不负责销毁 Core。
     */
    fun renderPageToBitmap(
        core: MuPDFCore,
        pageIndex: Int,
        maxSidePx: Int
    ): Bitmap? {
        try {
            if (pageIndex < 0 || pageIndex >= core.countPages()) return null

            // 获取页面大小以计算比例
            val pageSize = core.getPageSize(pageIndex)
            val originalWidth = pageSize.x
            val originalHeight = pageSize.y
            val aspectRatio = if (originalHeight != 0f) originalWidth / originalHeight else 1f

            val targetWidth: Int
            val targetHeight: Int
            if (originalWidth > originalHeight) {
                targetWidth = maxSidePx
                targetHeight = (maxSidePx / aspectRatio).toInt()
            } else {
                targetHeight = maxSidePx
                targetWidth = (maxSidePx * aspectRatio).toInt()
            }

            if (targetWidth <= 0 || targetHeight <= 0) return null
            val bitmap = createBitmap(targetWidth, targetHeight)

            // 渲染指定页
            core.drawPage(
                bitmap,
                pageIndex,
                targetWidth,
                targetHeight,
                0,
                0,
                targetWidth,
                targetHeight,
                null
            )
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "renderPageToBitmap 失败 (Page $pageIndex): ${e.message}", e)
            return null
        }
    }

    /**
     * 智能提取封面图片：
     * 1. 优先尝试元数据标记的路径 (info:MetaCover)
     * 2. 若失败，遍历容器寻找包含 "cover" 关键字的图片
     * 3. 若仍失败，取容器中发现的第一张图片文件
     */
    private fun tryExtractEmbeddedCover(context: Context, uri: Uri, core: MuPDFCore): Bitmap? {
        var archive: Archive? = null
        try {
            // 通过反射获取 MuPDFCore 内部的 Document 对象
            val docField: Field =
                MuPDFCore::class.java.getDeclaredField("doc").apply { isAccessible = true }
            val doc = docField.get(core) as? Document ?: return null

            // 获取文件大小
            val cr = context.contentResolver
            var fileSize: Long = -1
            cr.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0) fileSize = cursor.getLong(idx)
                }
            }

            archive = Archive(ContentInputStream(cr, uri, fileSize))
            val entryCount = archive.countEntries()
            if (entryCount <= 0) return null

            // --- 策略 A: 尝试元数据路径 ---
            val metaCoverPath = doc.getMetaData("info:MetaCover")
            if (!metaCoverPath.isNullOrBlank() && archive.hasEntry(metaCoverPath)) {
                Log.d(TAG, "通过元数据提取封面: $metaCoverPath")
                return decodeArchiveEntry(archive, metaCoverPath)
            }

            // --- 策略 B & C: 盲搜容器内部文件 ---
            var firstImagePath: String? = null
            val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".webp")

            for (i in 0 until entryCount) {
                val entryPath = archive.listEntry(i)
                val lowerPath = entryPath.lowercase()

                // 只看图片文件
                if (imageExtensions.any { lowerPath.endsWith(it) }) {
                    // 记录第一张发现的图，作为最后的保底 (策略 C)
                    if (firstImagePath == null) firstImagePath = entryPath

                    // 策略 B: 包含关键字，大概率是封面
                    if (lowerPath.contains("cover") || lowerPath.contains("front") || lowerPath.contains(
                            "thumbnail"
                        )
                    ) {
                        Log.d(TAG, "盲搜命中封面关键字: $entryPath")
                        return decodeArchiveEntry(archive, entryPath)
                    }
                }
            }

            // --- 策略 C: 最终保底，返回发现的第一张图片 ---
            if (firstImagePath != null) {
                Log.d(TAG, "未发现封面关键字，返回第一张图片: $firstImagePath")
                return decodeArchiveEntry(archive, firstImagePath)
            }

            return null
        } catch (e: Exception) {
            Log.e(TAG, "提取嵌入封面异常: ${e.message}")
            return null
        } finally {
            archive?.destroy()
        }
    }

    /**
     * 辅助方法：从 Archive 条目中解码 Bitmap
     */
    private fun decodeArchiveEntry(archive: Archive, path: String): Bitmap? {
        return try {
            val buffer = archive.readEntry(path)
            val bytes = buffer.asByteArray()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "解码 Archive 条目失败: $path", e)
            null
        }
    }

    /**
     * 生成文档封面图。
     * 策略：优先提取嵌入图片 -> 普通渲染。
     */
    @WorkerThread
    suspend fun getCover(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        var core: MuPDFCore? = null
        var coverBitmap: Bitmap? = null

        try {
            core = openCore(context, uri) ?: return@withContext null

            // 一、对于原生重排文件（文档内没有页概念），尝试直接提取嵌入的封面文件，避免渲染文字
            if (core.isReflowable) {
                coverBitmap = tryExtractEmbeddedCover(context, uri, core)
            }

            // 二、对于非重排文件，或者上一步提取失败的文件，执行渲染逻辑
            if (coverBitmap == null) {
                Log.d(TAG, "无法直接提取图片，降级为渲染模式")
                if (core.isReflowable) {
                    // 如果是重排文档，使用 A4 纸比例进行保底排版渲染，否则直接按页渲染
                    val a4Height = (MAX_COVER_SIDE_PX * A4_ASPECT_RATIO).toInt()
                    core.layout(0, MAX_COVER_SIDE_PX, a4Height, 12)
                }
                coverBitmap = renderPageToBitmap(core, 0, MAX_COVER_SIDE_PX)
            }

            if (coverBitmap == null) return@withContext null

            // 保存 Bitmap 到文件
            val coverDir = File(context.filesDir, COVER_DIR_NAME)
            if (!coverDir.exists()) coverDir.mkdirs()

            val coverFileName = "cover_${UUID.randomUUID()}.png"
            val coverFile = File(coverDir, coverFileName)
            FileOutputStream(coverFile).use { outputStream ->
                coverBitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
            }

            val coverFileUri = Uri.fromFile(coverFile).toString()
            Log.d(TAG, "封面生成成功: $coverFileUri")
            return@withContext coverFileUri

        } catch (e: Exception) {
            Log.e(TAG, "生成封面失败：${e.message}", e)
            null
        } finally {
            coverBitmap?.recycle()
            core?.onDestroy()
        }
    }
}