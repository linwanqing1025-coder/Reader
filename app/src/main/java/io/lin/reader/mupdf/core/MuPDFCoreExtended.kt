package io.lin.reader.mupdf.core

import android.graphics.Bitmap
import android.util.Log
import com.artifex.mupdf.fitz.Cookie
import com.artifex.mupdf.fitz.DisplayList
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Font
import com.artifex.mupdf.fitz.Image
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.Point
import com.artifex.mupdf.fitz.Quad
import com.artifex.mupdf.fitz.Rect
import com.artifex.mupdf.fitz.RectI
import com.artifex.mupdf.fitz.SeekableInputStream
import com.artifex.mupdf.fitz.StructuredText
import com.artifex.mupdf.fitz.StructuredTextWalker
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import com.artifex.mupdf.viewer.MuPDFCore
import java.lang.reflect.Field
import java.lang.reflect.Method

private const val TAG = "MuPDFCoreExtended.kt"

/**
 * 扩展后的 MuPDFCore ：用反射机制实现新功能扩展，可以保证不修改源码。
 *
 * 新功能如下：
 * 1. 获取单页的 StructuredText
 * 2. 实现重排模式下的高度个性化自定义
 */
class MuPDFCoreExtended : MuPDFCore {
    // public MuPDFCore(byte[] buffer, String magic)
    constructor(buffer: ByteArray, magic: String) : super(buffer, magic)

    // public MuPDFCore(SeekableInputStream stm, String magic)
    constructor(stm: SeekableInputStream, magic: String) : super(stm, magic)

    // 缓存反射获得的数据
    // 需要在 [app/proguard-rules.pro] 声明开启代码混淆后的成员名保护
    companion object {
        private val resolutionField: Field by lazy {
            MuPDFCore::class.java.getDeclaredField("resolution").apply { isAccessible = true }
        }
        private val docField: Field by lazy {
            MuPDFCore::class.java.getDeclaredField("doc").apply { isAccessible = true }
        }
        private val pageField: Field by lazy {
            MuPDFCore::class.java.getDeclaredField("page").apply { isAccessible = true }
        }
        private val displayListField: Field by lazy {
            MuPDFCore::class.java.getDeclaredField("displayList").apply { isAccessible = true }
        }
        private val gotoPageMethod: Method by lazy {
            MuPDFCore::class.java.getDeclaredMethod("gotoPage", Int::class.javaPrimitiveType)
                .apply { isAccessible = true }
        }

        // TODO: Testing

    }

    /**
     * [保存时] 将当前的逻辑页码转换为物理锚点 Mark
     * 无论排版怎么变化，Mark 指向的内容不会变
     */
    @Synchronized
    fun createMarkFromPage(pageIndex: Int): Long {
        return try {
            val doc = docField.get(this) as? Document ?: return -1L
            val loc = doc.locationFromPageNumber(pageIndex)
            doc.makeBookmark(loc)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating mark from page: ${e.message}")
            -1L
        }
    }

    /**
     * [恢复时] 将物理锚点 Mark 重新计算为当前排版下的最新页码
     * 解决重排文档调整字体或屏幕后页码偏移的问题
     */
    @Synchronized
    fun pageNumberFromBookmark(mark: Long): Int {
        return try {
            val doc = docField.get(this) as? Document ?: return -1
            val loc = doc.findBookmark(mark)
            doc.pageNumberFromLocation(loc)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting page number from mark: ${e.message}")
            -1
        }
    }

    /**
     * 在传入的 Bitmap 的基础之上渲染单页内容。
     *
     * 逻辑直接抄 drawPage() 方法。
     *
     * 修改 AndroidDrawDevice 的构造方式以实现保持传入的 Bitmap 的原有内容以实现背景透明渲染。
     */
    @Synchronized
    fun drawPageTransparent(
        bm: Bitmap,
        pageNum: Int,
        pageW: Int,
        pageH: Int,
        patchX: Int,
        patchY: Int,
        patchW: Int,
        patchH: Int,
        cookie: Cookie?
    ) {
        // 先切换页面，这会更新父类的 page 和 displayList 字段
        gotoPageMethod.invoke(this, pageNum)

        // 通过反射获取父类私有字段的最新值
        val page = pageField.get(this) as? Page
        var displayList = displayListField.get(this) as? DisplayList
        val resolution = resolutionField.get(this) as Int

        // 如果 displayList 为空，尝试生成它并同步回父类字段
        if (displayList == null && page != null) {
            try {
                displayList = page.toDisplayList()
                displayListField.set(this, displayList)
            } catch (e: Exception) {
                Log.e(TAG, "drawPage error", e)
                displayList = null
            }
        }

        if (displayList != null && page != null) {
            val zoom = (resolution / 72).toFloat()
            val ctm = Matrix(zoom, zoom)
            val bbox = RectI(page.bounds.transform(ctm))
            val xscale = pageW.toFloat() / (bbox.x1 - bbox.x0).toFloat()
            val yscale = pageH.toFloat() / (bbox.y1 - bbox.y0).toFloat()
            ctm.scale(xscale, yscale)

            // 传入 false 参数，即可使渲染的内容保持透明且没有背景
            val dev = AndroidDrawDevice(bm, patchX, patchY, false)

            try {
                displayList.run(dev, ctm, cookie)
                dev.close()
            } finally {
                dev.destroy()
            }
        }
    }

    /**
     * 单页搜索（带有自定义选项的扩展版本）。
     * 
     * 在官方的 [MuPDFCore] 中，`searchPage` 方法默认使用了 [StructuredText.SEARCH_IGNORE_CASE] 标志，
     * 这会导致跨段落或包含连字符的搜索在重排模式下出现断词问题。
     * 
     * 该扩展方法通过反射获取当前 `Page` 对象，直接调用原生的 `search(needle, style)`，
     * 允许调用方传入诸如 [StructuredText.SEARCH_KEEP_PARAGRAPHS] 或 [StructuredText.SEARCH_KEEP_HYPHENS] 
     * 的组合标志（Flags）以优化重排模式下的搜索行为。
     *
     * @param pageNum 搜索的页码。
     * @param text 要搜索的关键字。
     * @param style 搜索选项标志（例如：StructuredText.SEARCH_IGNORE_CASE | StructuredText.SEARCH_KEEP_PARAGRAPHS）。
     * @return 匹配的包围框数组，搜索不到或出错时返回 null。
     */
    @Synchronized
    fun searchPage(pageNum: Int, text: String?, style: Int): Array<Array<Quad>>? {
        return synchronized(this) {
            try {
                // 1. 先切换页面
                gotoPageMethod.invoke(this, pageNum)
                // 2. 拿到 Page 对象
                val page = pageField.get(this) as? Page
                // 3. 直接调用 Page 原生暴露的带 style 的 search 方法
                page?.search(text, style)
            } catch (e: Exception) {
                Log.e(TAG, "searchPage error", e)
                null
            }
        }
    }

    /**
     * 获取单页 StructuredText，用于实现单页文本选择
     * @param options StructuredText 的提取选项，例如 "segment,paragraph-break"
     */
    fun getStructuredText(pageNum: Int, options: String? = null): StructuredText? {
        return synchronized(this) {
            try {
                gotoPageMethod.invoke(this, pageNum)
                val page = pageField.get(this) as? Page
                if (options != null) {
                    page?.toStructuredText(options)
                } else {
                    page?.toStructuredText()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * 自定义重排 CSS 样式，字号由父类的 layout() 方法实现。
     *
     * 调用后必须再次调用父类的 layout() 方法才能成功显示自定义重排。
     */
    fun customReflowStyle(publisherCss: Boolean = true, userCss: String) {
        synchronized(this) {
            try {
                val doc = docField.get(this) as? Document
                doc?.style(publisherCss, userCss)
                doc?.countPages()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // TODO：测试用：打印字体信息
    fun printFontInformation() {
        val page = pageField.get(this) as? Page
        val stext = page?.toStructuredText()
        stext?.walk(
            object : StructuredTextWalker {
                override fun onChar(
                    c: Int,
                    origin: Point,
                    font: Font,
                    size: Float,
                    q: Quad,
                    argb: Int,
                    flags: Int,
                    bidi: Int
                ) {
                    Log.d(
                        "FontCheck",
                        "char='${c.toChar()}' font=${font.name} bold=${font.isBold} italic=${font.isItalic}"
                    )
                }

                // 其余回调可留空实现
                override fun onImageBlock(bbox: Rect, transform: Matrix, image: Image) {}
                override fun beginTextBlock(bbox: Rect, flags: Int) {}
                override fun endTextBlock() {}
                override fun beginLine(bbox: Rect, wmode: Int, dir: Point) {}
                override fun endLine() {}
                override fun beginStruct(standard: String?, raw: String?, index: Int) {}
                override fun endStruct() {}
                override fun onVector(
                    bbox: Rect,
                    info: StructuredTextWalker.VectorInfo,
                    argb: Int
                ) {
                }
            }
        )
    }
}