package io.lin.reader.ui.feature.reading.feature.render

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

    // TODO: new features
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
                displayList = null
            }
        }

        if (displayList != null && page != null) {
            val zoom = (resolution / 72).toFloat()
            val ctm = Matrix(zoom, zoom)
            val bbox = RectI(page.getBounds().transform(ctm))
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
     * 获取单页 StructuredText，用于实现单页文本选择
     */
    fun getStructuredText(pageNum: Int): StructuredText? {
        return synchronized(this) {
            try {
                gotoPageMethod.invoke(this, pageNum)
                val page = pageField.get(this) as? Page
                page?.toStructuredText()
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