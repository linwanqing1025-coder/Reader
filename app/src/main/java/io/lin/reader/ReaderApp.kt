package io.lin.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import io.lin.reader.navigation.FloatingNavigationBar
import io.lin.reader.navigation.NavKey
import io.lin.reader.navigation.NavigationItems
import io.lin.reader.navigation.ReaderNavDisplay
import io.lin.reader.ui.AppViewModel
import io.lin.reader.ui.LocalAppViewModel
import io.lin.reader.ui.ViewModelProvider
import io.lin.reader.ui.components.file.LocalFileSelector
import io.lin.reader.ui.components.file.rememberLazyFileSelector
import io.lin.reader.ui.theme.ReaderTheme
import kotlinx.serialization.json.Json

@Composable
fun ReaderApp() {
    // APP ViewModel and Preferences
    val viewModel: AppViewModel = viewModel(factory = ViewModelProvider.Factory)
    val appPreferences = viewModel.appPreferences

    // 全局文件选择器
    val lazyFileSelector = rememberLazyFileSelector()

    // Navigation 3 Back Stack
    val navKeyListSaver = Saver<MutableList<NavKey>, String>(
        save = { Json.encodeToString(it.toList()) },
        restore = { Json.decodeFromString<List<NavKey>>(it).toMutableStateList() }
    )
    val backStack = rememberSaveable(saver = navKeyListSaver) {
        mutableStateListOf(NavKey.Root.Shelf)
    }
    val onNavigateUp = {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }
    val onNavigate: (NavKey) -> Boolean = { newKey: NavKey ->
        if (newKey is NavKey.Root) {
            // 切换顶级 Tab 时，清空并重新开始（标准 Bottom Nav 行为）
            backStack.clear()
            backStack.add(newKey)
        } else {
            backStack.add(newKey)
        }
    }
    val currentKey = backStack.lastOrNull()
    val isRootPage = NavigationItems.entries.any {
        it.key == currentKey
    }

    ReaderTheme(
        darkMode = appPreferences.theme,
        themeColor = appPreferences.themeColor,
        themeContrast = appPreferences.themeContrast
    ) {
        val navigationBarScrollBehavior =
            FloatingToolbarDefaults.exitAlwaysScrollBehavior(
                exitDirection = Bottom
            )
        val scrollModifier =
            if (isRootPage) Modifier.nestedScroll(navigationBarScrollBehavior)
            else Modifier

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(scrollModifier),
        ) {
            CompositionLocalProvider(
                LocalFileSelector provides lazyFileSelector,
                LocalAppViewModel provides viewModel
            ) {
                ReaderNavDisplay(
                    backStack = backStack,
                    onNavigateUp = onNavigateUp,
                    onNavigate = onNavigate
                )
                FloatingNavigationBar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(1f),
                    scrollBehavior = navigationBarScrollBehavior,
                    isRootPage = isRootPage,
                    currentKey = currentKey,
                    navigationItemsList = NavigationItems.entries, // TODO: 用户自己选择 Tab 栏
                    onNavigate = onNavigate
                )
            }
        }
    }
}

// 辅助扩展方法
private fun <T> List<T>.toMutableStateList() =
    mutableStateListOf<T>().apply { addAll(this@toMutableStateList) }