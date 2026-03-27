package io.lin.reader.ui.components.file

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

const val TAG = "FileSelector"

/**
 * 文件选择器的启动函数类型：
 * @param fileType 文件类型（如 "application/pdf"）
 * @param onFileSelected 选择文件后的回调（返回Uri字符串列表）
 */
typealias FileSelectorLauncher = (fileType: String, onFileSelected: (List<String>) -> Unit) -> Unit

/** 全局CompositionLocal，用于传递文件选择器 */
val LocalFileSelector = compositionLocalOf<FileSelectorLauncher> {
    error("未在根组件中提供FileSelector！")
}

/**
 * 懒加载文件选择器（语法合规+性能优化）：
 * 1. launcher在Composable上下文创建（仅一次，轻量）；
 * 2. 仅在用户首次点击时打印"首次使用"日志，后续复用；
 * 3. 完全符合Compose语法规范。
 */
@Composable
fun rememberLazyFileSelector(): FileSelectorLauncher {
    val context = LocalContext.current // 获取Context用于申请权限
    val lifecycleOwner = LocalLifecycleOwner.current // 获取当前生命周期所有者
    val currentFileType = remember { mutableStateOf("*/*") }
    val currentOnFileSelected = remember { mutableStateOf<((List<String>) -> Unit)?>(null) }
    // 标记是否是首次使用（区分"创建"和"使用"）
    val isFirstUse = remember { mutableStateOf(true) }

    val fileSelectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val selectedUris = mutableListOf<Uri>()
        if (result.resultCode == Activity.RESULT_OK) {
            // 处理多选
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    selectedUris.add(clipData.getItemAt(i).uri)
                }
            }
            // 处理单选
            result.data?.data?.let { uri ->
                selectedUris.add(uri)
            }

            // 关键步骤：为获取到的URI申请持久化读取权限
            if (selectedUris.isNotEmpty()) {
                Log.d(TAG, "Requesting persistent permissions for ${selectedUris.size} URIs.")
                selectedUris.forEach { uri ->
                    try {
                        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                        Log.d(TAG, "Successfully called takePersistableUriPermission for $uri")
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Failed to take persistable permission for $uri", e)
                    }
                }
            }

            // --- DIAGNOSTIC: Printing all persisted URI permissions ---
            val allPersistedUris = context.contentResolver.persistedUriPermissions
            if (allPersistedUris.isEmpty()) {
                Log.w(TAG, "DIAGNOSTIC: App holds NO persisted URI permissions at this moment.")
            } else {
                Log.i(
                    TAG,
                    "--- DIAGNOSTIC: Listing all ${allPersistedUris.size} persisted URI permissions held by the app ---"
                )
                allPersistedUris.forEachIndexed { index, uriPermission ->
                    Log.i(
                        TAG,
                        "  ${index + 1}: ${uriPermission.uri} (isRead: ${uriPermission.isReadPermission}, isWrite: ${uriPermission.isWritePermission})"
                    )
                }
                Log.i(TAG, "--- End of diagnostic list ---")
            }
            // --- END DIAGNOSTIC ---

            val uriStrings = selectedUris.map { it.toString() }
            Log.d(TAG, "选择了 ${uriStrings.size} 个文件")
            // 回调返回Uri列表
            currentOnFileSelected.value?.invoke(uriStrings)

        } else {
            Log.d(TAG, "未选择任何文件，返回空列表")
            // 用户取消选择时，也回调一个空列表
            currentOnFileSelected.value?.invoke(emptyList())
        }

        // 清理回调，防止内存泄漏
        currentOnFileSelected.value = null
    }

    // 2. 创建生命周期观察者（监听DESTROYED事件）
    val lifecycleObserver = remember {
        LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                // APP进程结束时：清理回调状态（避免内存泄漏）+ 打印日志
                Log.d(TAG, "=== APP进程结束，清理文件选择器回调状态 ===")
                currentOnFileSelected.value = null // 清空回调引用
                currentFileType.value = "*/*"      // 重置文件类型
            }
        }
    }

    // 3. 绑定生命周期监听（DisposableEffect确保重组/销毁时清理）
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        // Composable销毁时：移除观察者（避免内存泄漏）
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            Log.d(TAG, "=== DisposableEffect：移除文件选择器生命周期观察者 ===")
        }
    }

    // 4. 构建启动函数，实现"懒使用" ：首次使用时初始化，APP结束时才摧毁
    return remember {
        { fileType, onFileSelected ->
            // 首次使用时打印日志（用户真正关心的"首次触发"时机）
            if (isFirstUse.value) {
                Log.d(TAG, "=== 首次使用文件选择器（launcher已提前轻量创建） ===")
                isFirstUse.value = false
            } else {
                Log.d(TAG, "=== 复用文件选择器 ===")
            }

            // 更新文件类型和回调
            currentFileType.value = fileType
            currentOnFileSelected.value = onFileSelected

            // 唤起文件选择器（仅在用户点击时执行）
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = currentFileType.value
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                // 正确的授权是通过在回调中调用 takePersistableUriPermission() 完成的。
                // 此处只需申请临时的读取权限。
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            fileSelectLauncher.launch(intent)
        }
    }
}
