package io.lin.reader.mupdf.reflow

import android.util.Log
import io.lin.reader.data.preferences.ReflowPreferencesInterface
import io.lin.reader.mupdf.font.PRESET_FONTS

object ReflowCssGenerator {
    // TODO: 验证 bundled 字体加载是否成功
    fun generateCss(prefs: ReflowPreferencesInterface): String {
        val textColorHex = String.format("#%06X", 0xFFFFFF and prefs.textColor)
        val bgColorHex = String.format("#%06X", 0xFFFFFF and prefs.backgroundColor)

        // 字体信息
        val fontInfo = PRESET_FONTS.firstOrNull { it.family == prefs.fontFamily }
        val fontFace = fontInfo?.cssFontFace() ?: ""// TODO: 非项目内置字体
        val fontWeight = if (prefs.fontBold) "bold" else "normal"
        val fontStyle = if (prefs.fontItalic) "italic" else "normal"

        val res = """  
            $fontFace  
        
            html, body {  
                margin: 0 !important;  
                padding: 0 !important;  
                background-color: $bgColorHex;  
            }  
            @page {  
                margin: 0 !important;  
            }  
            * {  
                font-family: "${prefs.fontFamily}" !important;  
            }  
            body {  
                font-weight: $fontWeight;  
                font-style: $fontStyle;  
                line-height: ${prefs.lineHeight};  
                color: $textColorHex;  
                text-align: ${prefs.textAlign};  
                text-indent: ${prefs.textIndent}pt;  
                margin: ${prefs.verticalMargin}pt ${prefs.horizontalMargin}pt !important;  
                padding: ${prefs.verticalPadding}pt ${prefs.horizontalPadding}pt !important;  
                hyphens: ${if (prefs.hyphenation) "auto" else "none"};  
            }  
            ul, ol {  
                list-style-type: ${prefs.listStyleType};  
                list-style-position: ${prefs.listStylePosition};  
            }  
            p {  
                margin-top: 0;  
                margin-bottom: ${prefs.paragraphSpacing}pt;  
            }  
            """.trimIndent()

        Log.d("FontCheck", "css: $res")
        return res
    }
}
