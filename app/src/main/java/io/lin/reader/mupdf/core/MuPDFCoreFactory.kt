package io.lin.reader.mupdf.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.artifex.mupdf.fitz.SeekableInputStream
import com.artifex.mupdf.viewer.ContentInputStream
import io.lin.reader.utils.FileUtils.getMimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MuPDFCoreFactory {
    private const val TAG = "MuPDFCoreFactory"

    private fun openBuffer(buffer: ByteArray, magic: String): MuPDFCoreExtended? {
        return try {
            MuPDFCoreExtended(buffer, magic)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening document buffer: ${e.message}")
            null
        }
    }

    private fun openStream(stm: SeekableInputStream, magic: String): MuPDFCoreExtended? {
        return try {
            MuPDFCoreExtended(stm, magic)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening document stream: ${e.message}")
            null
        }
    }

    /**
     * 通过 Uri 打开 MuPDFCoreExtended。
     * 它会自动解析 FileSize，并且在小于 8MB 时优先加载进内存以提升性能。
     * 搬运自 DocumentActivity.java 的部分逻辑。
     */
    suspend fun openCore(context: Context, uri: Uri, mimeType: String? = null): MuPDFCoreExtended? =
        withContext(Dispatchers.IO) {
            val cr = context.contentResolver
            val actualMimeType = mimeType ?: getMimeType(context, uri) ?: "application/pdf"
            var fileSize: Long = -1

            // 获取文件大小
            try {
                cr.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if ((sizeIndex >= 0) && !cursor.isNull(sizeIndex)) {
                            fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying URI metadata: ${e.message}")
            }

            if (fileSize == 0L) fileSize = -1

            var buf: ByteArray? = null
            try {
                val limit = 8 * 1024 * 1024
                val inputStream = cr.openInputStream(uri)
                if (fileSize < 0) { // 大小未知
                    inputStream?.use { isStream ->
                        val tempBuf = ByteArray(limit)
                        val used = isStream.read(tempBuf)
                        val atEOF = isStream.read() == -1
                        if (used >= 0 && (used < limit || atEOF)) {
                            buf = if (tempBuf.size == used) tempBuf else tempBuf.copyOf(used)
                        }
                    }
                } else if (fileSize <= limit) { // 大小已知，且小于 8MB
                    inputStream?.use { isStream ->
                        val tempBuf = ByteArray(fileSize.toInt())
                        val used = isStream.read(tempBuf)
                        if (used >= 0 && used == fileSize.toInt()) {
                            buf = tempBuf
                        }
                    }
                } else {
                    inputStream?.close()
                }
            } catch (_: OutOfMemoryError) {
                buf = null
            } catch (e: Exception) {
                Log.e(TAG, "Error reading core buffer: ${e.message}")
                buf = null
            }

            val finalBuf = buf
            if (finalBuf != null) {
                Log.i(TAG, "Opening document from memory buffer of size ${finalBuf.size}")
                openBuffer(finalBuf, actualMimeType)
            } else {
                Log.i(TAG, "Opening document from stream")
                openStream(ContentInputStream(cr, uri, fileSize), actualMimeType)
            }
        }
}