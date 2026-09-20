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

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import io.lin.reader.R

/**
 * 导航项枚举：定义底部导航栏的所有项目及其对应的 [NavKey.Root]。
 */
enum class NavigationItems(
    @StringRes val label: Int,
    val icon: ImageVector,
    @StringRes val contentDescription: Int,
    val key: NavKey
) {
    SHELF(
        R.string.navigation_label_shelf,
        Icons.AutoMirrored.Default.LibraryBooks,
        R.string.navigation_label_shelf,
        NavKey.Shelf
    ),
    FAVOURITE(
        R.string.navigation_label_favourite,
        Icons.Default.Star,
        R.string.navigation_label_favourite,
        NavKey.Favourite
    ),
    BOOKMARK(
        R.string.navigation_label_bookmark,
        Icons.Default.Bookmarks,
        R.string.navigation_label_bookmark,
        NavKey.Bookmark
    ),
    HISTORY(
        R.string.navigation_label_history,
        Icons.Default.History,
        R.string.navigation_label_history,
        NavKey.History
    ),
    SETTING(
        R.string.navigation_label_setting,
        Icons.Default.Settings,
        R.string.navigation_label_setting,
        NavKey.Setting
    ),
}