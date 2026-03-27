package io.lin.reader

import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.lin.reader.navigation.NavigationItems
import io.lin.reader.navigation.ReaderNavHost
import io.lin.reader.navigation.ScreenRoute
import io.lin.reader.ui.components.file.LocalFileSelector
import io.lin.reader.ui.components.file.rememberLazyFileSelector
import io.lin.reader.ui.theme.ReaderTheme


@Composable
fun ReaderApp(
    navController: NavHostController = rememberNavController()
) {
    // 获取设备建议的导航套件类型
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val navSuiteType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)

    // 全局文件选择器
    val lazyFileSelector = rememberLazyFileSelector()

    // 获取当前路由状态
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 判断是否在阅读界面
    val isReadingScreen =
        currentDestination?.route?.substringBefore('/') == ScreenRoute.ReadingScreen.name


    val currentLayoutType = if (isReadingScreen) NavigationSuiteType.None else navSuiteType
    CompositionLocalProvider(
        LocalFileSelector provides lazyFileSelector
    ) {
        NavigationSuiteScaffold(
            layoutType = currentLayoutType,
            navigationSuiteItems = {
                // 仅在非阅读界面填充导航项
                if (!isReadingScreen) {
                    NavigationItems.entries.forEach { item ->
                        val route = item.route
                        item(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = stringResource(item.contentDescription)
                                )
                            },
                            label = { Text(stringResource(item.label)) },
                            selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) {
            ReaderNavHost(
                // 传递原始 navSuiteType 以便动画逻辑识别设备模式（底部栏 vs 侧边栏）
                navSuiteType = navSuiteType,
                navController = navController
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReaderAppCompactPreview() {
    ReaderTheme {
        Surface {
            ReaderApp()
        }
    }
}