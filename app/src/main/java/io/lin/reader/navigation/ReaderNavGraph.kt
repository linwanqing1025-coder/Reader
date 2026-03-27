/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.lin.reader.navigation

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.lin.reader.ui.screen.bookmark.BookmarkScreen
import io.lin.reader.ui.screen.bookmark.BookmarkScreenDestination
import io.lin.reader.ui.screen.favourite.FavouriteScreen
import io.lin.reader.ui.screen.favourite.FavouriteScreenDestination
import io.lin.reader.ui.screen.history.HistoryScreen
import io.lin.reader.ui.screen.history.HistoryScreenDestination
import io.lin.reader.ui.screen.reading.ReadingScreen
import io.lin.reader.ui.screen.reading.ReadingScreenDestination
import io.lin.reader.ui.screen.setting.SettingScreen
import io.lin.reader.ui.screen.setting.SettingScreenDestination
import io.lin.reader.ui.screen.shelf.ShelfScreen
import io.lin.reader.ui.screen.shelf.ShelfScreenDestination


/**
 * Provides Navigation graph for the application.
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@Composable
fun ReaderNavHost(
    navSuiteType: NavigationSuiteType,
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = ShelfScreenDestination.route,
        modifier = modifier,
        enterTransition = {
            getEnterAnimation(
                initialState,
                targetState,
                navSuiteType
            )
        },
        exitTransition = {
            getExitAnimation(
                initialState,
                targetState,
                navSuiteType
            )
        },
        popEnterTransition = {
            getEnterAnimation(
                initialState,
                targetState,
                navSuiteType
            )
        },
        popExitTransition = {
            getExitAnimation(
                initialState,
                targetState,
                navSuiteType
            )
        }
    ) {
        composable(route = ShelfScreenDestination.route) {
            ShelfScreen(
                modifier = modifier,
                entryVolumeReading = { navController.navigate("${ReadingScreenDestination.route}/${it.id}") }
            )
        }

        composable(route = FavouriteScreenDestination.route) {
            FavouriteScreen(
                modifier = modifier,
                onVolumeClick = { navController.navigate("${ReadingScreenDestination.route}/${it.id}") }
            )
        }

        composable(route = BookmarkScreenDestination.route) {
            BookmarkScreen(
                modifier = modifier,
                onBookmarkClick = { volumeId, pageNumber ->
                    val route =
                        "${ReadingScreenDestination.route}/${volumeId}?${ReadingScreenDestination.pageNumberArg}=${pageNumber}"
                    navController.navigate(route)
                }
            )
        }

        composable(route = HistoryScreenDestination.route) {
            HistoryScreen(
                modifier = modifier,
                entryVolumeReading = { navController.navigate("${ReadingScreenDestination.route}/${it.id}") }
            )
        }

        composable(route = SettingScreenDestination.route) {
            SettingScreen(
                modifier = modifier,
            )
        }

        composable(
            route = ReadingScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(ReadingScreenDestination.bookIdArg) { type = NavType.LongType },
                navArgument(ReadingScreenDestination.pageNumberArg) {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) {
            ReadingScreen(
                modifier = modifier,
                onNavigateUp = { navController.navigateUp() },
            )
        }
    }
}

private fun getNavigationDirection(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
): Int? {
    val initialRoute = initialState.destination.route?.substringBefore('/')
    val targetRoute = targetState.destination.route?.substringBefore('/')

    val initialIndex = NavigationItems.entries.indexOfFirst { it.route == initialRoute }
    val targetIndex = NavigationItems.entries.indexOfFirst { it.route == targetRoute }

    if (initialIndex == -1 || targetIndex == -1 || initialIndex == targetIndex) {
        return null
    }

    return if (targetIndex > initialIndex) 1 else -1
}

private fun getEnterAnimation(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry,
    navSuiteType: NavigationSuiteType
): EnterTransition {
    val initialRoute = initialState.destination.route?.substringBefore('/')
    val targetRoute = targetState.destination.route?.substringBefore('/')

    // Case 1: ReadingScreen 涉及的导航（始终水平）
    if (targetRoute == ScreenRoute.ReadingScreen.name) {
        return slideInHorizontally(animationSpec = tween(300), initialOffsetX = { it })
    }
    if (initialRoute == ScreenRoute.ReadingScreen.name) {
        return slideInHorizontally(animationSpec = tween(300), initialOffsetX = { -it })
    }

    // Case 2: 导航栏项目之间的导航
    val direction = getNavigationDirection(initialState, targetState)
    if (direction != null) {
        return if (navSuiteType == NavigationSuiteType.NavigationBar) {
            slideInHorizontally(animationSpec = tween(300), initialOffsetX = { it * direction })
        } else {
            slideInVertically(animationSpec = tween(300), initialOffsetY = { it * direction })
        }
    }

    return fadeIn(animationSpec = tween(300))
}

private fun getExitAnimation(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry,
    navSuiteType: NavigationSuiteType
): ExitTransition {
    val initialRoute = initialState.destination.route?.substringBefore('/')
    val targetRoute = targetState.destination.route?.substringBefore('/')

    // Case 1: ReadingScreen 涉及的导航（始终水平）
    if (targetRoute == ScreenRoute.ReadingScreen.name) {
        return slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { -it })
    }
    if (initialRoute == ScreenRoute.ReadingScreen.name) {
        return slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { it })
    }

    // Case 2: 导航栏项目之间的导航
    val direction = getNavigationDirection(initialState, targetState)
    if (direction != null) {
        return if (navSuiteType == NavigationSuiteType.NavigationBar) {
            slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { -it * direction })
        } else {
            slideOutVertically(animationSpec = tween(300), targetOffsetY = { -it * direction })
        }
    }

    return fadeOut(animationSpec = tween(300))
}
