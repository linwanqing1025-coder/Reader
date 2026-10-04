package io.lin.reader.mupdf.selection

/**
 * StructuredText 提取选项，对应 fz_stext_options 位标志。
 * 用法：page.toStructuredText("选项1,选项2,...")，逗号分隔。
 * 权威清单见 docs/reference/common/stext-options.md
 */
object ToStructuredTextOptions {
    /** 保留图像 block（FZ_STEXT_PRESERVE_IMAGES）。不加则丢弃所有图片，
     *  onImageBlock 回调不会触发。"点按查看图片"功能必须加。 */
    const val PRESERVE_IMAGES = "preserve-images"

    /** 保留连字原形，不展开（FZ_STEXT_PRESERVE_LIGATURES）。
     *  不加则 "ffi" 连字拆成 f、f、i 三个字符。 */
    const val PRESERVE_LIGATURES = "preserve-ligatures"

    /** 不合并同一行上的 span（FZ_STEXT_PRESERVE_SPANS）。
     *  需要区分同一段内的字体样式变化时使用。 */
    const val PRESERVE_SPANS = "preserve-spans"

    /** 保留原始空白字符（FZ_STEXT_PRESERVE_WHITESPACE）。
     *  不加则所有制表符/水平空白统一替换为普通空格。 */
    const val PRESERVE_WHITESPACE = "preserve-whitespace"

    /** 不在字符间大间隙处自动补空格（FZ_STEXT_INHIBIT_SPACES）。
     *  PDF 通常不显式存储空格，默认由 MuPDF 按间隙推断插入。 */
    const val INHIBIT_SPACES = "inhibit-spaces"

    /** 在"疑似段落边界"处切开 TextBlock（FZ_STEXT_PARAGRAPH_BREAK）。
     *  仅适用于从左到右、从上到下排版，配合 segment 效果最好。
     *  用途：使 block 边界 ≈ 段落边界，分辨语义断行。 */
    const val PARAGRAPH_BREAK = "paragraph-break"

    /** 尝试拼接行尾连字符断词（FZ_STEXT_DEHYPHENATE）。
     *  行尾连字符标记为软连字符，拍平时 "-\n" 直接拼接；
     *  "exam-\nple" 可被搜索/复制为 "example"。 */
    const val DEHYPHENATE = "dehyphenate"

    /** 不应用 PDF ActualText 替换（FZ_STEXT_IGNORE_ACTUALTEXT）。 */
    const val IGNORE_ACTUALTEXT = "ignore-actualtext"

    /** unicode 映射失败时用字符码(CID)代替（FZ_STEXT_USE_CID_FOR_UNKNOWN_UNICODE）。
     *  字符会带 CHAR_FLAGS_UNICODE_IS_CID 标志。 */
    const val USE_CID_FOR_UNKNOWN_UNICODE = "use-cid-for-unknown-unicode"

    /** unicode 映射失败时用字形索引(GID)代替（FZ_STEXT_USE_GID_FOR_UNKNOWN_UNICODE）。
     *  与上面互斥，字符带 CHAR_FLAGS_UNICODE_IS_GID 标志。 */
    const val USE_GID_FOR_UNKNOWN_UNICODE = "use-gid-for-unknown-unicode"

    /** 从字形轮廓计算精确字符 bbox（FZ_STEXT_ACCURATE_BBOXES）。 */
    const val ACCURATE_BBOXES = "accurate-bboxes"

    /** 从字体字形计算升降部（FZ_STEXT_ACCURATE_ASCENDERS）。 */
    const val ACCURATE_ASCENDERS = "accurate-ascenders"

    /** 扩展字符 bbox 以完整包含字形宽度（FZ_STEXT_ACCURATE_SIDE_BEARINGS）。 */
    const val ACCURATE_SIDE_BEARINGS = "accurate-side-bearings"

    /** 检测文本样式：伪粗体、删除线、下划线等（FZ_STEXT_COLLECT_STYLES）。
     *  填充 CHAR_FLAGS_BOLD/STRIKEOUT/UNDERLINE 等标志。 */
    const val COLLECT_STYLES = "collect-styles"

    /** 跳过完全在裁剪区域外的文字（FZ_STEXT_CLIP）。 */
    const val CLIP = "clip"

    /** 收集结构树（FZ_STEXT_COLLECT_STRUCTURE）。
     *  生成 FZ_STEXT_BLOCK_STRUCT 节点，StructuredTextWalker 的
     *  beginStruct/endStruct 回调才有效。EPUB/tagged PDF 可用。 */
    const val STRUCTURED = "structured"

    /** 收集矢量图形 bbox（FZ_STEXT_COLLECT_VECTORS）。
     *  产生 vector block，onVector 回调有效，可获取下划线/表格线等图形。 */
    const val VECTORS = "vectors"

    /** 延迟矢量提取，避免矢量把文本行切开（FZ_STEXT_LAZY_VECTORS）。 */
    const val LAZY_VECTORS = "lazy-vectors"

    /** 合并相邻的水平/垂直矢量（FZ_STEXT_FUZZY_VECTORS）。
     *  对重建表格网格有帮助。 */
    const val FUZZY_VECTORS = "fuzzy-vectors"

    /** 对页面做区域分割（FZ_STEXT_SEGMENT）。
     *  按版面把内容切成逻辑区域，是 paragraph-break/table-hunt 的前置。 */
    const val SEGMENT = "segment"

    /** 在分割后的页面上探测表格（FZ_STEXT_TABLE_HUNT）。
     *  需要先启用 segment。 */
    const val TABLE_HUNT = "table-hunt"

    /**
     * 指定内容收集的裁剪矩形（FZ_STEXT_CLIP_RECT），带参数：
     * 用法："clip-rect=x0:y0:x1:y1"，只有矩形内的内容会被提取。
     * 适合只提取页面某一块区域的场景，可配合 clip 使用。
     */
    fun clipRect(x0: Float, y0: Float, x1: Float, y1: Float) =
        "clip-rect=$x0:$y0:$x1:$y1"
}