package io.lin.reader.ui.screen.reading

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.util.Log
import io.lin.reader.utils.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap

/**
 * 专门处理 PDF 页面渲染的底层逻辑
 * @param renderer PDF 渲染器
 * @param pageIndex 要渲染的页索引（包括 0）
 * @param bitmapWidth 目标渲染宽度
 * @param mutex 渲染器互斥锁
 * @param removeGutter 是否自动去除订口（仅裁剪左右空白边缘）
 */
suspend fun renderPdfPage(
    renderer: PdfRenderer,
    pageIndex: Int,
    bitmapWidth: Int,
    mutex: Mutex,
    removeGutter: Boolean = false
): Bitmap? = withContext(Dispatchers.IO) {
    mutex.withLock {
        try {
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withLock null
            val page = renderer.openPage(pageIndex)
            val bitmapHeight = (page.height * bitmapWidth) / page.width
            var bitmap = createBitmap(bitmapWidth, bitmapHeight)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            if (removeGutter) {
                val cropped = BitmapUtils.removeHorizontalGutter(bitmap)
                if (cropped != bitmap) {
                    bitmap.recycle()
                    bitmap = cropped
                }
            }
            bitmap
        } catch (e: Exception) {
            Log.e("PdfRendererHelper", "Error rendering page $pageIndex", e)
            null
        }
    }
}
