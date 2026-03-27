package io.lin.reader.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log

object FileUtils {
    private const val TAG = "FileUtils"

    /**
     * 根据给定的 URI 获取文件名。
     * @param context The context.
     * @param uri The Uri to query.
     * @return 文件名，如果无法确定则返回 null。
     */
    fun getFileName(context: Context, uri: Uri): String? {
        // 检查是否是 content:// 类型的 URI
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            var cursor: Cursor? = null
            try {
                // 使用 OpenableColumns.DISPLAY_NAME 来查询文件名
                cursor = context.contentResolver.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    // 从查询结果中获取文件名
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get file name from content URI", e)
            } finally {
                cursor?.close()
            }
        }

        // 如果不是 content:// URI (例如 file://)，则直接从路径中获取
        // 注意：在 Android 10+ 上，直接访问 file:// URI 的路径可能会因为作用域存储而失败
        return uri.lastPathSegment
    }

    /**
     * 获取文件的总页数。
     * @param context 上下文，用于访问 ContentResolver。
     * @param fileUri 文件的 URI。可以是 file:// 或 content:// 类型。
     * @return 总页数。如果发生任何错误（如文件无效、权限不足等），则返回 0。
     */
    fun getFilePageCount(context: Context, fileUri: Uri): Int {
        return PdfUtils.getPdfPageCount(context, fileUri)
    }

    /**
     * 生成文件封面，返回生成的封面的Uri
     * */
    suspend fun getCover(context: Context, fileUri: Uri): String? {
        return PdfUtils.getPdfCover(context, fileUri)
    }
}