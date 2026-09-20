package io.lin.reader.navigation

import kotlinx.serialization.Serializable

/**
 * 导航键：定义应用中所有可导航的目标。
 * 使用 [Serializable] 以支持进程死亡后的状态恢复。
 */
@Serializable
sealed class NavKey {
    // 导航栏的五个 Tab
    @Serializable
    sealed class Root : NavKey()

    @Serializable
    data object Shelf : Root()

    @Serializable
    data object Favourite : Root()

    @Serializable
    data object Bookmark : Root()

    @Serializable
    data object History : Root()

    @Serializable
    data object Setting : Root()

    // 各设置详情页
    @Serializable
    sealed class SettingDetails : NavKey()
    
    // 系列详情页
    @Serializable
    data class SeriesDetail(val seriesId: Long) : NavKey()

    @Serializable
    data object AppAppearance : SettingDetails()

    @Serializable
    data object Sort : SettingDetails()

    @Serializable
    data object ReaderColor : SettingDetails()

    @Serializable
    data object ReadingMode : SettingDetails()

    @Serializable
    data object Interaction : SettingDetails()

    @Serializable
    data object Reflow : SettingDetails()

    @Serializable
    data object OtherReading : SettingDetails()

    // 阅读界面
    @Serializable
    data class Reading(
        val bookId: Long,
        val pageNumber: Int = -1
    ) : NavKey()
}
