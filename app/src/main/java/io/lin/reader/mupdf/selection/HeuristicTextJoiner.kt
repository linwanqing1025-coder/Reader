package io.lin.reader.mupdf.selection

/**
 * 启发式文本拼接器 (方案 A)
 * 
 * 用于处理原生 [com.artifex.mupdf.fitz.StructuredText.copy] 提取文本时，
 * 无法区分“排版换行”和“真实段落换行”的问题。
 * 此方案通过分析字符串前后的字符、连字符和标点，采用启发式的规则来决定如何抹平或保留换行。
 */
object HeuristicTextJoiner {

    private fun isCJK(c: Char): Boolean = c.code in 0x4E00..0x9FFF ||
            c.code in 0x3400..0x4DBF || c.code in 0x3000..0x303F ||
            c.code in 0xFF00..0xFFEF || c.code in 0x3040..0x30FF

    private fun isLatin(c: Char): Boolean = c.code in 0x20..0x24F || c.code in 0x1E00..0x1EFF

    private fun skipRun(raw: String, startIndex: Int): Int {
        var i = startIndex
        while (i < raw.length && (raw[i] == '\n' || raw[i] == '\r')) {
            i++
        }
        return i - 1 // 因为外部循环还有 i++
    }

    private fun isPunctuation(c: Char): Boolean {
        return c.code in 0x2000..0x206F || // 一般标点
                c.code in 0x3000..0x303F || // CJK 符号和标点
                c.code in 0xFF00..0xFFEF || // 全角 ASCII 变体（包含。，！？）
                c == '”' || c == '’' || c == '」' || c == '』' || c == '》' || c == '）' || c == '】' ||
                c == '.' || c == '!' || c == '?' || c == ';' || c == ':'
    }

    /**
     * 将包含生硬换行符的原始文本进行启发式清洗和拼接。
     *
     * @param raw 通过 sText.copy() 得到的未经处理的字符串
     * @return 尝试恢复语义连贯的字符串
     */
    fun joinCopiedText(raw: String): String {
        val sb = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '\n' || c == '\r') {
                // 跳过 \r\n 的 \r
                val prev = sb.lastOrNull()
                val next = raw.subSequence(i + 1, raw.length).firstOrNull { it != '\r' && it != '\n' }
                // 连续空行 -> 段落分隔，保留一个换行
                val isParaBreak = raw.subSequence(i, raw.length)
                    .takeWhile { it == '\r' || it == '\n' }.count() >= 2
                when {
                    prev == null || next == null -> {}           // 首尾换行丢弃
                    isParaBreak -> {
                        sb.append('\n')
                        i = skipRun(raw, i)
                    }
                    prev == '-' && isLatin(next) -> sb.deleteCharAt(sb.length - 1)  // 连字符断词：去掉 '-' 拼接
                    isPunctuation(prev) -> {
                        // 如果上一行以标点符号结尾，很大概率是一个完整的句子结束，应该保留换行，而不是强行和下一行拼接
                        sb.append('\n')
                    }
                    isCJK(prev) && isCJK(next) -> {}             // CJK 断行：直接拼接，不加空格
                    else -> sb.append(' ')                       // 拉丁/混排：换成空格
                }
            } else {
                sb.append(c)
            }
            i++
        }
        return sb.toString()
    }
}