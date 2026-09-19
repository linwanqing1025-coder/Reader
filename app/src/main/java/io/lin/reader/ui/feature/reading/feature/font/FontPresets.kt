package io.lin.reader.ui.feature.reading.feature.font

import android.annotation.SuppressLint

/*
  解析 CSS 时，font-family 字段首先被作为字体名使用，优先匹配库内自带字体名称。
  包括：
  "Courier", "Courier-Oblique", "Courier-Bold", "Courier-BoldOblique",
  "Helvetica", "Helvetica-Oblique", "Helvetica-Bold", "Helvetica-BoldOblique",
  "Times-Roman", "Times-Italic", "Times-Bold", "Times-BoldItalic",
  "Symbol",
  "ZapfDingbats".
  每种字体只传名称即可，css-apply.c会自动根据注入的 CSS 规则拼接字体文件名并加载。
  对于非内置字体，需要在注入的 CSS 规则中添加可访问的 URL 路径：
  font-family: "Font Name"; src: url(<File Path>);
*/

/**
 * MuPDF 库内置字体
 */
val MUPDF_BUILTIN_FONTS = listOf(
    MuPDFFontInfo(FontType.MUPDF, "Courier"),
    MuPDFFontInfo(FontType.MUPDF, "Helvetica"),
    MuPDFFontInfo(FontType.MUPDF, "Times-Roman"),
    MuPDFFontInfo(FontType.MUPDF, "Symbol"),
    MuPDFFontInfo(FontType.MUPDF, "ZapfDingbats")
)

// TODO: 先搁置，需要添加应用联网功能
/**
 * 开发者打包的字体，需要执行 CSS 注入路径
 * 路径起点为 assets 目录
 */
@SuppressLint("SdCardPath")
val BUNDLED_FONTS = listOf(
    // 霞鹜文楷
    MuPDFFontInfo(
        type = FontType.BUNDLED,
        family = "霞鹜文楷",
        regularPath = "fonts/LXGWWenKai-Regular.ttf",
        boldPath = "fonts/LXGWWenKai-Medium.ttf",
    )
)

val PRESET_FONTS = MUPDF_BUILTIN_FONTS + BUNDLED_FONTS