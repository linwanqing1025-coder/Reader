package io.lin.reader.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * PDF工具类：使用 Android 官方的 PdfRenderer 库
 */
object PdfUtils {
    private const val TAG = "PdfUtils"

    // 封面存储目录（应用内部私有存储）
    private const val COVER_DIR_NAME = "covers"

    // 封面最大参考边长（px），确保图片不会太大占用空间，同时保持清晰
    private const val MAX_COVER_SIDE_PX = 512

    /**
     * 获取 PDF 文件的总页数。
     */
    fun getPdfPageCount(context: Context, pdfUri: Uri): Int {
        val parcelFileDescriptor =
            context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return 0

        return try {
            PdfRenderer(parcelFileDescriptor).use { renderer ->
                renderer.pageCount
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * 生成PDF第一页封面图片，保存到应用内部存储并返回图片Uri（String）
     * 优化：按原始比例生成
     */
    @WorkerThread
    suspend fun getPdfCover(context: Context, pdfUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            var parcelFileDescriptor: ParcelFileDescriptor? = null
            var pdfRenderer: PdfRenderer? = null
            var coverFileUri: String? = null

            try {
                parcelFileDescriptor = context.contentResolver.openFileDescriptor(pdfUri, "r")
                    ?: return@withContext null

                pdfRenderer = PdfRenderer(parcelFileDescriptor)
                if (pdfRenderer.pageCount <= 0) return@withContext null

                // 获取第一页
                val page = pdfRenderer.openPage(0)
                
                // --- 核心优化：计算比例尺寸 ---
                val originalWidth = page.width
                val originalHeight = page.height
                val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()

                // 根据最大边长限制，按比例计算目标宽高
                val targetWidth: Int
                val targetHeight: Int
                if (originalWidth > originalHeight) {
                    targetWidth = MAX_COVER_SIDE_PX
                    targetHeight = (MAX_COVER_SIDE_PX / aspectRatio).toInt()
                } else {
                    targetHeight = MAX_COVER_SIDE_PX
                    targetWidth = (MAX_COVER_SIDE_PX * aspectRatio).toInt()
                }

                val coverBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                
                // 渲染PDF页面到Bitmap
                page.render(
                    coverBitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )
                page.close()

                val coverDir = File(context.filesDir, COVER_DIR_NAME)
                if (!coverDir.exists()) coverDir.mkdirs()

                val coverFileName = "cover_${UUID.randomUUID()}.png"
                val coverFile = File(coverDir, coverFileName)
                FileOutputStream(coverFile).use { outputStream ->
                    coverBitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                }
                coverBitmap.recycle()

                coverFileUri = Uri.fromFile(coverFile).toString()
                Log.d(TAG, "封面生成成功(比例 $aspectRatio)：$coverFileUri")

            } catch (e: Exception) {
                Log.e(TAG, "生成PDF封面失败：${e.message}", e)
            } finally {
                pdfRenderer?.close()
                parcelFileDescriptor?.close()
            }

            return@withContext coverFileUri
        }
    }
}
