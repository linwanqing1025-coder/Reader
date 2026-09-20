package io.lin.reader.mupdf.font


enum class FontType {
    MUPDF,  // MuPDF 内置字体
    BUNDLED,  // APK assets 打包字体
    USER,      // 用户本地字体文件
    SYSTEM,   // Android 系统自带字体
}

/**
 * 字体信息
 * @property type 字体来源种类，为 MuPDF 内置字体时，后面四个路径都不需要传入
 * @property family 字体名 来自字体文件：<字体名>-<粗斜体>.ttf/otf
 * @property regularPath 一般体路径
 * @property boldPath 粗体路径
 * @property italicPath 斜体路径
 * @property boldItalicPath 粗斜体路径
 */
data class MuPDFFontInfo(
    val type: FontType,
    val family: String,
    val regularPath: String? = null,
    val boldPath: String? = null,
    val italicPath: String? = null,
    val boldItalicPath: String? = null
) {
    // 获取字体路径
    fun getPath(bold: Boolean, italic: Boolean): String? {
        return if (bold && italic) boldItalicPath
        else if (bold) boldPath
        else if (italic) italicPath
        else regularPath
    }

    fun cssFontFace(): String {
        // MuPDF 内置字体不需要 @font-face，直接用 family 名走 fz_lookup_builtin_font
        if (type == FontType.MUPDF) return ""

        val faces = mutableListOf<String>()

        fun appendFace(path: String?, weight: String, style: String) {
            if (path.isNullOrBlank()) return
            faces += """  
            @font-face {  
                font-family: "$family";  
                font-weight: $weight;  
                font-style: $style;  
                src: url($path);  
            }  
        """.trimIndent()
        }

        appendFace(regularPath, "normal", "normal")
        appendFace(boldPath, "bold", "normal")
        appendFace(italicPath, "normal", "italic")
        appendFace(boldItalicPath, "bold", "italic")

        return faces.joinToString("\n")
    }
}