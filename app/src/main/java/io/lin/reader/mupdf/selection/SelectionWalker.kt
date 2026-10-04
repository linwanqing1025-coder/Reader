package io.lin.reader.mupdf.selection

import com.artifex.mupdf.fitz.Font
import com.artifex.mupdf.fitz.Image
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Point
import com.artifex.mupdf.fitz.Quad
import com.artifex.mupdf.fitz.Rect
import com.artifex.mupdf.fitz.StructuredTextWalker
import java.lang.StringBuilder

/**
 * 页面上的图片实体，记录了图片在页面上的坐标和图像对象
 */
data class PageImage(
    val bbox: Rect,
    val image: Image
)

/**
 * 专用于提取选区文本的 Walker。
 *
 * 通过配合 [ToStructuredTextOptions.SEGMENT] 和 [ToStructuredTextOptions.PARAGRAPH_BREAK] 选项，
 * 可以精确地区分“段落内换行”和“真实段落边界”，从而实现完美的选文复制拼接。
 */
class SelectionWalker(
    private val highlightQuads: Array<Quad>? = null // 当前页的所有高亮框，设为 nullable 以支持仅提取图片的情况
) : StructuredTextWalker {
    val resultBuilder = StringBuilder()
    val extractedImages = mutableListOf<PageImage>()
    
    private var inSelectedBlock = false
    private var lineCountInBlock = 0
    private var lastChar: Char? = null

    /**
     * 判断当前字的中心点是否落在任何一个高亮框 (Quad) 的内部。
     */
    private fun isCharSelected(q: Quad): Boolean {
        if (highlightQuads == null) return false
        
        // 计算当前字的中心坐标
        val centerX = (q.ul_x + q.ur_x + q.ll_x + q.lr_x) / 4f
        val centerY = (q.ul_y + q.ur_y + q.ll_y + q.lr_y) / 4f

        return highlightQuads.any { hq ->
            // 简单的 AABB 矩形包含测试
            val minX = minOf(hq.ll_x, hq.ul_x)
            val maxX = maxOf(hq.lr_x, hq.ur_x)
            val minY = minOf(hq.ul_y, hq.ur_y)
            val maxY = maxOf(hq.ll_y, hq.lr_y)

            centerX in minX..maxX && centerY in minY..maxY
        }
    }

    private fun isCJK(c: Char): Boolean = c.code in 0x4E00..0x9FFF ||
            c.code in 0x3400..0x4DBF || c.code in 0x3000..0x303F ||
            c.code in 0xFF00..0xFFEF || c.code in 0x3040..0x30FF

    override fun beginTextBlock(bbox: Rect?, flags: Int) {
        // 遇到新的段落
        inSelectedBlock = false
        lineCountInBlock = 0
    }

    override fun beginLine(bbox: Rect?, wmode: Int, dir: Point?) {
        lineCountInBlock++
        // 如果这不是本段落的第一行，且我们在上一行有选中文本
        // 说明这是段落内的自动换行（排版换行），需要进行行内拼接处理
        if (lineCountInBlock > 1 && inSelectedBlock) {
            val prev = lastChar
            if (prev != null) {
                if (prev == '-') {
                    // 连词符断词（例如 "exam-\nple"），回退一个字符删掉连字符，且不加空格
                    resultBuilder.deleteCharAt(resultBuilder.length - 1)
                } else if (isCJK(prev)) {
                    // 中文断行，直接接上下一行，不加任何分隔符
                } else {
                    // 英文/西文词间断行，补一个空格防止两个单词粘连
                    resultBuilder.append(' ')
                }
            }
        }
    }

    override fun onChar(
        c: Int,
        origin: Point?,
        font: Font?,
        size: Float,
        q: Quad,
        argb: Int,
        flags: Int,
        bidi: Int
    ) {
        if (isCharSelected(q)) {
            inSelectedBlock = true
            val ch = c.toChar()
            
            // 跳过一些脏数据，例如 ASCII < 32 的非打印字符（回车、换行已经在生命周期里处理了）
            // 除非是空格
            if (ch.code >= 32 || ch == '\t') {
                resultBuilder.append(ch)
                lastChar = ch
            }
        }
    }

    override fun endLine() {
        // 单行结束时不做任何处理，真正的换行由 endTextBlock 负责
    }

    override fun endTextBlock() {
        // 段落结束。如果这个段落里有我们选中的字，说明该换段落了
        if (inSelectedBlock) {
            resultBuilder.append('\n')
            lastChar = '\n'
        }
    }

    // ----------------------------------------------------
    // 获取页面图片
    // ----------------------------------------------------
    
    override fun onImageBlock(bbox: Rect?, transform: Matrix?, image: Image?) {
        // 保存图片的边界和 Image 实例以供后续的点击检测和提取
        if (bbox != null && image != null) {
            extractedImages.add(PageImage(bbox, image))
        }
    }
    
    override fun beginStruct(standard: String?, raw: String?, index: Int) {}
    
    override fun endStruct() {}
    
    override fun onVector(bbox: Rect?, info: StructuredTextWalker.VectorInfo?, argb: Int) {}
}