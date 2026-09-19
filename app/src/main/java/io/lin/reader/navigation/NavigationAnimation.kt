package io.lin.reader.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.navigation3.scene.Scene

/**
 * 获取顶级 Tab 的索引位置
 */
private fun getRootTabIndex(key: Any?): Int {
    return NavigationItems.entries.indexOfFirst { it.key == key }
}

/**
 * 计算进入动画
 */
fun AnimatedContentTransitionScope<Scene<*>>.getEnterAnimation(
    navSuiteType: NavigationSuiteType = NavigationSuiteType.NavigationBar
): EnterTransition {
    val from = initialState.key
    val to = targetState.key
    
    val fromIndex = getRootTabIndex(from)
    val toIndex = getRootTabIndex(to)

    return when {
        // 1. 若是在五个顶级 Tab 之间导航：依据导航栏 Tab 摆放顺序水平平移
        // TODO: 考虑导航栏种类
        fromIndex != -1 && toIndex != -1 -> {
            if (toIndex > fromIndex) {
                // 目标索引大，从右向左滑入
                slideInHorizontally(animationSpec = tween(300)) { it }
            } else {
                // 目标索引小，从左向右滑入
                slideInHorizontally(animationSpec = tween(300)) { -it }
            }
        }

        // 2. 若是在顶级 Tab 与其他界面（如 Reading、SettingDetails）之间导航
        (from is NavKey.Root && to !is NavKey.Root) || (from !is NavKey.Root && to is NavKey.Root) -> {
            fadeIn(animationSpec = tween(300))
        }

        // 3. 若是在顶级 Tab 以外的其他界面之间导航（预留分支，后续可修改）
        else -> {
            fadeIn(animationSpec = tween(300))
        }
    }
}

/**
 * 计算退出动画
 */
fun AnimatedContentTransitionScope<Scene<*>>.getExitAnimation(
    navSuiteType: NavigationSuiteType = NavigationSuiteType.NavigationBar
): ExitTransition {
    val from = initialState.key
    val to = targetState.key

    val fromIndex = getRootTabIndex(from)
    val toIndex = getRootTabIndex(to)

    return when {
        // 1. 若是在五个顶级 Tab 之间导航
        fromIndex != -1 && toIndex != -1 -> {
            if (toIndex > fromIndex) {
                // 目标在右边，当前页向左滑出
                slideOutHorizontally(animationSpec = tween(300)) { -it }
            } else {
                // 目标在左边，当前页向右滑出
                slideOutHorizontally(animationSpec = tween(300)) { it }
            }
        }

        // 2. 若是在顶级 Tab 与其他界面之间导航
        (from is NavKey.Root && to !is NavKey.Root) || (from !is NavKey.Root && to is NavKey.Root) -> {
            fadeOut(animationSpec = tween(300))
        }

        // 3. 若是在顶级 Tab 以外的其他界面之间导航（预留分支，后续可修改）
        else -> {
            fadeOut(animationSpec = tween(300))
        }
    }
}
