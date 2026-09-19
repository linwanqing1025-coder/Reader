package io.lin.reader.data.preferences

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.edit
import io.lin.reader.ui.feature.reading.feature.font.PRESET_FONTS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface ReflowPreferencesInterface {
    val usePublisherStyle: Boolean
    val pageBreakBefore: String
    val pageBreakAfter: String
    val fontFamily: String
    val fontSize: Int
    val fontBold: Boolean
    val fontItalic: Boolean
    val lineHeight: Float
    val textColor: Int
    val backgroundColor: Int
    val horizontalMargin: Float
    val verticalMargin: Float
    val horizontalPadding: Float
    val verticalPadding: Float
    val listStyleType: String
    val listStylePosition: String
    val textAlign: String
    val textIndent: Float
    val paragraphSpacing: Float
    val hyphenation: Boolean

    fun toggleUsePublisherStyle()
    fun updatePageBreakBefore(value: String)
    fun updatePageBreakAfter(value: String)
    fun updateFontFamily(value: String)
    fun updateFontSize(value: Int)
    fun updateFontBold(value: Boolean)
    fun updateFontItalic(value: Boolean)
    fun updateLineHeight(value: Float)
    fun updateTextColor(value: Color)
    fun updateBackgroundColor(value: Color)
    fun updateMargins(horizontal: Float, vertical: Float)
    fun updatePaddings(horizontal: Float, vertical: Float)
    fun updateListStyleType(value: String)
    fun updateListStylePosition(value: String)
    fun updateTextAlign(value: String)
    fun updateTextIndent(value: Float)
    fun updateParagraphSpacing(value: Float)
    fun toggleHyphenation()
    fun generateCss(): String
}

data class ReflowPreferences(
    private val repo: UserPreferencesRepository,
    private val scope: CoroutineScope
) : ReflowPreferencesInterface {
    override var usePublisherStyle by mutableStateOf(false)
    override var pageBreakBefore by mutableStateOf("auto")
    override var pageBreakAfter by mutableStateOf("auto")
    override var fontFamily by mutableStateOf("sans-serif")
    override var fontSize by mutableIntStateOf(18)
    override var fontBold by mutableStateOf(false)
    override var fontItalic by mutableStateOf(false)
    override var lineHeight by mutableFloatStateOf(1.5f)
    override var textColor by mutableIntStateOf(android.graphics.Color.BLACK)
    override var backgroundColor by mutableIntStateOf(android.graphics.Color.WHITE)
    override var horizontalMargin by mutableFloatStateOf(0f)
    override var verticalMargin by mutableFloatStateOf(0f)
    override var horizontalPadding by mutableFloatStateOf(0f)
    override var verticalPadding by mutableFloatStateOf(0f)
    override var listStyleType by mutableStateOf("disc")
    override var listStylePosition by mutableStateOf("outside")
    override var textAlign by mutableStateOf("justify")
    override var textIndent by mutableFloatStateOf(24f)
    override var paragraphSpacing by mutableFloatStateOf(12f)
    override var hyphenation by mutableStateOf(true)

    init {
        scope.launch {
            repo.dataStore.data.collect { prefs ->
                prefs[UserPreferencesRepository.REFLOW_PAGE_BREAK_BEFORE]?.let {
                    pageBreakBefore = it
                }
                prefs[UserPreferencesRepository.REFLOW_PAGE_BREAK_AFTER]?.let {
                    pageBreakAfter = it
                }
                prefs[UserPreferencesRepository.REFLOW_FONT_FAMILY]?.let { fontFamily = it }
                prefs[UserPreferencesRepository.REFLOW_FONT_SIZE]?.let { fontSize = it }
                prefs[UserPreferencesRepository.REFLOW_FONT_BOLD]?.let { fontBold = it }
                prefs[UserPreferencesRepository.REFLOW_FONT_ITALIC]?.let { fontItalic = it }
                prefs[UserPreferencesRepository.REFLOW_LINE_HEIGHT]?.let { lineHeight = it }
                prefs[UserPreferencesRepository.REFLOW_TEXT_COLOR]?.let { textColor = it }
                prefs[UserPreferencesRepository.REFLOW_BG_COLOR]?.let { backgroundColor = it }
                prefs[UserPreferencesRepository.REFLOW_HORIZONTAL_MARGIN]?.let {
                    horizontalMargin = it
                }
                prefs[UserPreferencesRepository.REFLOW_VERTICAL_MARGIN]?.let { verticalMargin = it }
                prefs[UserPreferencesRepository.REFLOW_HORIZONTAL_PADDING]?.let {
                    horizontalPadding = it
                }
                prefs[UserPreferencesRepository.REFLOW_VERTICAL_PADDING]?.let {
                    verticalPadding = it
                }
                prefs[UserPreferencesRepository.REFLOW_LIST_STYLE_TYPE]?.let { listStyleType = it }
                prefs[UserPreferencesRepository.REFLOW_LIST_STYLE_POSITION]?.let {
                    listStylePosition = it
                }
                prefs[UserPreferencesRepository.REFLOW_TEXT_ALIGN]?.let { textAlign = it }
                prefs[UserPreferencesRepository.REFLOW_TEXT_INDENT]?.let { textIndent = it }
                prefs[UserPreferencesRepository.REFLOW_PARAGRAPH_SPACING]?.let {
                    paragraphSpacing = it
                }
                prefs[UserPreferencesRepository.REFLOW_HYPHENATION]?.let { hyphenation = it }
                prefs[UserPreferencesRepository.REFLOW_USE_PUBLISHER_STYLE]?.let {
                    usePublisherStyle = it
                }
            }
        }
    }

    override fun toggleUsePublisherStyle() {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_USE_PUBLISHER_STYLE] = !usePublisherStyle
            }
        }
    }

    override fun updatePageBreakBefore(value: String) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_PAGE_BREAK_BEFORE] = value
            }
        }
    }

    override fun updatePageBreakAfter(value: String) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_PAGE_BREAK_AFTER] = value
            }
        }
    }

    override fun updateFontFamily(value: String) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_FONT_FAMILY] = value
            }
        }
    }

    override fun updateFontSize(value: Int) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_FONT_SIZE] = value
            }
        }
    }

    override fun updateFontBold(value: Boolean) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_FONT_BOLD] = value
            }
        }
    }

    override fun updateFontItalic(value: Boolean) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_FONT_ITALIC] = value
            }
        }
    }

    override fun updateLineHeight(value: Float) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_LINE_HEIGHT] = value
            }
        }
    }

    override fun updateTextColor(value: Color) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_TEXT_COLOR] = value.toArgb()
            }
        }
    }

    override fun updateBackgroundColor(value: Color) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_BG_COLOR] = value.toArgb()
            }
        }
    }

    override fun updateMargins(horizontal: Float, vertical: Float) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_HORIZONTAL_MARGIN] = horizontal
                it[UserPreferencesRepository.REFLOW_VERTICAL_MARGIN] = vertical
            }
        }
    }

    override fun updatePaddings(horizontal: Float, vertical: Float) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_HORIZONTAL_PADDING] = horizontal
                it[UserPreferencesRepository.REFLOW_VERTICAL_PADDING] = vertical
            }
        }
    }

    override fun updateListStyleType(value: String) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_LIST_STYLE_TYPE] = value
            }
        }
    }

    override fun updateListStylePosition(value: String) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_LIST_STYLE_POSITION] = value
            }
        }
    }

    override fun updateTextAlign(value: String) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_TEXT_ALIGN] = value
            }
        }
    }

    override fun updateTextIndent(value: Float) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_TEXT_INDENT] = value
            }
        }
    }

    override fun updateParagraphSpacing(value: Float) {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_PARAGRAPH_SPACING] = value
            }
        }
    }

    override fun toggleHyphenation() {
        scope.launch {
            repo.dataStore.edit {
                it[UserPreferencesRepository.REFLOW_HYPHENATION] = !hyphenation
            }
        }
    }

    // TODO: 验证 bundled 字体加载是否成功
    override fun generateCss(): String {
        val textColorHex = String.format("#%06X", 0xFFFFFF and textColor)
        val bgColorHex = String.format("#%06X", 0xFFFFFF and backgroundColor)

        // 字体信息
        val fontInfo = PRESET_FONTS.firstOrNull { it.family == fontFamily }
        val fontFace = fontInfo?.cssFontFace() ?: ""// TODO: 非项目内置字体
        val fontWeight = if (fontBold) "bold" else "normal"
        val fontStyle = if (fontItalic) "italic" else "normal"

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
                font-family: "$fontFamily" !important;  
            }  
            body {  
                font-weight: $fontWeight;  
                font-style: $fontStyle;  
                line-height: $lineHeight;  
                color: $textColorHex;  
                text-align: $textAlign;  
                text-indent: ${textIndent}pt;  
                margin: ${verticalMargin}pt ${horizontalMargin}pt !important;  
                padding: ${verticalPadding}pt ${horizontalPadding}pt !important;  
                hyphens: ${if (hyphenation) "auto" else "none"};  
            }  
            ul, ol {  
                list-style-type: $listStyleType;  
                list-style-position: $listStylePosition;  
            }  
            p {  
                margin-top: 0;  
                margin-bottom: ${paragraphSpacing}pt;  
            }  
            """.trimIndent()

        Log.d("FontCheck", "css: $res")
        return res
    }
}

class ReflowPreferencesForTest : ReflowPreferencesInterface {
    override var usePublisherStyle = false
    override var pageBreakBefore = "auto"
    override var pageBreakAfter = "auto"
    override var fontFamily = "sans-serif"
    override var fontSize = 18
    override var fontBold = false
    override var fontItalic = false
    override var lineHeight = 1.5f
    override var textColor = android.graphics.Color.BLACK
    override var backgroundColor = android.graphics.Color.WHITE
    override var horizontalMargin = 16f
    override var verticalMargin = 16f
    override var horizontalPadding = 0f
    override var verticalPadding = 0f
    override var listStyleType = "disc"
    override var listStylePosition = "outside"
    override var textAlign = "justify"
    override var textIndent = 24f
    override var paragraphSpacing = 12f
    override var hyphenation = true

    override fun toggleUsePublisherStyle() {}
    override fun updatePageBreakBefore(value: String) {}
    override fun updatePageBreakAfter(value: String) {}
    override fun updateFontFamily(value: String) {}
    override fun updateFontSize(value: Int) {}
    override fun updateFontBold(value: Boolean) {}
    override fun updateFontItalic(value: Boolean) {}
    override fun updateLineHeight(value: Float) {}
    override fun updateTextColor(value: Color) {}
    override fun updateBackgroundColor(value: Color) {}
    override fun updateMargins(horizontal: Float, vertical: Float) {}
    override fun updatePaddings(horizontal: Float, vertical: Float) {}
    override fun updateListStyleType(value: String) {}
    override fun updateListStylePosition(value: String) {}
    override fun updateTextAlign(value: String) {}
    override fun updateTextIndent(value: Float) {}
    override fun updateParagraphSpacing(value: Float) {}
    override fun toggleHyphenation() {}
    override fun generateCss(): String = ""
}
