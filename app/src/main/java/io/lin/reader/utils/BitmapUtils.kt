package io.lin.reader.utils

import android.graphics.Bitmap
import kotlin.math.abs
import androidx.core.graphics.get

object BitmapUtils {

    /**
     * 自动裁剪 Bitmap 左右边缘空白区域（去除订口）
     * 采取跳行扫描方案以平衡性能和准确度
     * 限制：单边裁剪宽度不超过原图宽度的 1/8
     */
    fun removeHorizontalGutter(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        // 太小的图片不处理
        if (width < 50 || height < 50) return bitmap

        // 性能优化：跳过扫描步长
        val skip = 4
        
        // 采样背景色：通常 PDF 背景是纯色，取左上角像素
        val bgColor = bitmap[0, 0]
        val r = (bgColor shr 16) and 0xFF
        val g = (bgColor shr 8) and 0xFF
        val b = bgColor and 0xFF
        
        // 判断是否为内容的本地函数
        fun isContent(x: Int, y: Int): Boolean {
            val color = bitmap[x, y]
            if (color == bgColor) return false
            
            val cr = (color shr 16) and 0xFF
            val cg = (color shr 8) and 0xFF
            val cb = color and 0xFF
            
            // 计算与背景色的色差，增加一定的容差处理扫描件噪点
            return abs(cr - r) + abs(cg - g) + abs(cb - b) > 20
        }

        var left = 0
        var right = width - 1

        // 1. 从左向右扫，寻找左内容边界
        leftLoop@ for (x in 0 until width step skip) {
            for (y in 0 until height step skip) {
                if (isContent(x, y)) {
                    left = x
                    break@leftLoop
                }
            }
        }

        // 2. 从右向左扫，寻找右内容边界
        rightLoop@ for (x in width - 1 downTo left step skip) {
            for (y in 0 until height step skip) {
                if (isContent(x, y)) {
                    right = x
                    break@rightLoop
                }
            }
        }

        // 3. 计算裁剪限制 (1/8 宽度)
        val maxCropPerSide = width / 8
        val padding = 4

        // 计算最终左侧裁剪量（内容边界减去内边距，且不超过 1/8 限制）
        val cropLeft = (left - padding).coerceIn(0, maxCropPerSide)
        // 计算最终右侧裁剪量
        val cropRight = (width - 1 - right - padding).coerceIn(0, maxCropPerSide)

        // 如果两边都不需要裁剪，则直接返回
        if (cropLeft <= 0 && cropRight <= 0) {
            return bitmap
        }

        val finalLeft = cropLeft
        val finalRight = (width - 1) - cropRight
        val newWidth = finalRight - finalLeft + 1

        return if (newWidth in 1..<width) {
            try {
                Bitmap.createBitmap(bitmap, finalLeft, 0, newWidth, height)
            } catch (e: Exception) {
                bitmap
            }
        } else {
            bitmap
        }
    }
}
