package io.lin.reader.ui.screen.reading

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 专门处理 PDF 页面渲染的底层逻辑
 * @param renderer PDF 渲染器
 * @param pageIndex 要渲染的页索引（包括 0）
 */
suspend fun renderPdfPage(
    renderer: PdfRenderer,
    pageIndex: Int,
    bitmapWidth: Int,
    mutex: Mutex
): Bitmap? = withContext(Dispatchers.IO) {
    mutex.withLock {
        try {
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withLock null
            val page = renderer.openPage(pageIndex)
            val bitmapHeight = (page.height * bitmapWidth) / page.width
            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        } catch (e: Exception) {
            Log.e("PdfRendererHelper", "Error rendering page $pageIndex", e)
            null
        }
    }
}
