package io.lin.reader.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import android.webkit.MimeTypeMap

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
     * 获取文件的 MIME 类型。
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        return if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            context.contentResolver.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
        }
    }

    /**
     * 如果 URI 是本地文件协议 (file://)，则尝试从磁盘删除该文件。
     * @param uriString 文件的 URI 字符串
     * @return 是否成功删除
     */
    fun deleteFileIfUriIsLocal(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    val result = file.delete()
                    Log.d(TAG, "Deleted local file: $uriString, success: $result")
                    result
                } else {
                    Log.d(TAG, "File does not exist: $uriString")
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete file: $uriString", e)
            false
        }
    }
}
