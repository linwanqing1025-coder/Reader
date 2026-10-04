package io.lin.reader.navigation

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.scene.Scene

private const val TAG = "NavigationAnimation.kt"

private fun NavKey.isRoot(): Boolean = this is NavKey.Root

private fun extractNavKey(state: Any?): NavKey? {
    return when (state) {
        is NavKey -> state
        is Scene<*> -> state.metadata[NAV_KEY_METADATA] as? NavKey
        else -> null
    }
}

/**
 * 专用于计算 前进(Push/Navigate) 时的动画
 */
fun <T> AnimatedContentTransitionScope<T>.calculateNavPushTransition(
    navSuiteType: NavigationSuiteType
): ContentTransform {
    val defaultTransition = fadeIn(tween(300)) togetherWith fadeOut(tween(300))
    val fromKey = extractNavKey(initialState)
    val toKey = extractNavKey(targetState)

    if (fromKey == null || toKey == null || fromKey == toKey) {
        Log.d(TAG, "PushTransition fallback: fromKey=$fromKey, toKey=$toKey")
        return defaultTransition
    }

    // 只有在 isRoot 的 key 之间导航时，才计算平移；否则一律使用瓦片推入动画
    return if (fromKey.isRoot() && toKey.isRoot()) {
        calculateRootToRootTransition(fromKey, toKey, navSuiteType, defaultTransition)
    } else {
        tilePushTransition()
    }
}

/**
 * 专用于计算 返回(Pop/Back) 时的动画
 */
fun <T> AnimatedContentTransitionScope<T>.calculateNavPopTransition(
    navSuiteType: NavigationSuiteType
): ContentTransform {
    val defaultTransition = fadeIn(tween(300)) togetherWith fadeOut(tween(300))
    val fromKey = extractNavKey(initialState)
    val toKey = extractNavKey(targetState)

    if (fromKey == null || toKey == null || fromKey == toKey) {
        Log.d(TAG, "PopTransition fallback: fromKey=$fromKey, toKey=$toKey")
        return defaultTransition
    }

    // 只有在 isRoot 的 key 之间导航时，才计算平移；否则一律使用瓦片退出动画
    return if (fromKey.isRoot() && toKey.isRoot()) {
        calculateRootToRootTransition(fromKey, toKey, navSuiteType, defaultTransition)
    } else {
        tilePopTransition()
    }
}

/**
 * 提取出的 Root 之间互相切换的平移计算逻辑
 */
private fun <T> AnimatedContentTransitionScope<T>.calculateRootToRootTransition(
    fromKey: NavKey,
    toKey: NavKey,
    navSuiteType: NavigationSuiteType,
    defaultTransition: ContentTransform
): ContentTransform {
    val animSpec = tween<IntOffset>(300)
    val fadeSpec = tween<Float>(300)

    val fromIndex = NavigationItems.entries.indexOfFirst { it.key == fromKey }
    val toIndex = NavigationItems.entries.indexOfFirst { it.key == toKey }
    val isForward = toIndex > fromIndex

    return when (navSuiteType) {
        NavigationSuiteType.NavigationBar -> {
            if (isForward) {
                (slideInHorizontally(animSpec) { it } + fadeIn(fadeSpec)) togetherWith
                        (slideOutHorizontally(animSpec) { -it } + fadeOut(fadeSpec))
            } else {
                (slideInHorizontally(animSpec) { -it } + fadeIn(fadeSpec)) togetherWith
                        (slideOutHorizontally(animSpec) { it } + fadeOut(fadeSpec))
            }
        }
        NavigationSuiteType.NavigationRail,
        NavigationSuiteType.NavigationDrawer -> {
            if (isForward) {
                (slideInVertically(animSpec) { it } + fadeIn(fadeSpec)) togetherWith
                        (slideOutVertically(animSpec) { -it } + fadeOut(fadeSpec))
            } else {
                (slideInVertically(animSpec) { -it } + fadeIn(fadeSpec)) togetherWith
                        (slideOutVertically(animSpec) { it } + fadeOut(fadeSpec))
            }
        }
        else -> defaultTransition
    }
}

private fun tilePushTransition(duration: Int = 350): ContentTransform {
    val animSpec = tween<IntOffset>(duration)
    val fadeSpec = tween<Float>(duration)
    return (slideInHorizontally(animSpec) { fullWidth -> fullWidth } togetherWith fadeOut(
        fadeSpec,
        targetAlpha = 0.3f
    )).apply { targetContentZIndex = 1f }
}

private fun tilePopTransition(duration: Int = 350): ContentTransform {
    val animSpec = tween<IntOffset>(duration)
    val fadeSpec = tween<Float>(duration)
    return (fadeIn(
        fadeSpec,
        initialAlpha = 0.3f
    ) togetherWith slideOutHorizontally(animSpec) { fullWidth -> fullWidth })
        .apply { targetContentZIndex = -1f }
}