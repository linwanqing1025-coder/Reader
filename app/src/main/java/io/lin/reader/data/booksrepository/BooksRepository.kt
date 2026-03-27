package io.lin.reader.data.booksrepository

import androidx.paging.PagingData
import io.lin.reader.data.database.Bookmark
import io.lin.reader.data.database.Series
import io.lin.reader.data.database.Volume
import io.lin.reader.data.database.VolumeWithBookmarks
import io.lin.reader.data.database.VolumeWithSeries
import io.lin.reader.ui.screen.setting.details.BookmarkSortMethod
import io.lin.reader.ui.screen.setting.details.SeriesSortMethod
import io.lin.reader.ui.screen.setting.details.VolumeSortMethod
import kotlinx.coroutines.flow.Flow


interface BooksRepository {

    //增
    /**
     * 插入一个系列。如果名称重复，会自动重命名并尝试插入。
     * @param series 要插入的原始系列对象。
     * @return 返回最终插入或更新的系列的 ID。如果多次尝试失败，则返回 -1。
     */
    suspend fun upsertSeries(series: Series): Long

    /**
     * 批量在已有系列中添加新书
     * @param volumeList 要添加的新书列表, 其seriesId都已经正确指向一个已存在的系列
     * @return 成功插入的书籍数量（0表示未执行插入或插入失败）
     */
    suspend fun addVolumesInExistingSeries(volumeList: List<Volume>): Int

    /**
     * 批量添加新书，并为这些书创建一个新系列（原子事务）。
     * @param volumeList 要添加的新书列表（seriesId 会被覆盖为新系列ID，无需提前设置）
     * @param newSeriesName 新系列的名称
     * @return 成功插入的书籍数量（0表示未执行插入或插入失败）
     */
    suspend fun addVolumesWithNewSeries(volumeList: List<Volume>, newSeriesName: String): Int

    suspend fun insertBookmark(bookmark: Bookmark): Long
    suspend fun insertBookmarks(bookmarks: List<Bookmark>)

    //删
    /**
     * 删除一本指定的书籍，并将其所属系列的册数减一。
     * 整个操作在一个事务中完成，确保数据一致性。
     * @param volumeId 要删除的书籍的ID。
     * @return 如果成功删除，返回 true；如果书籍不存在，返回 false。
     */
    suspend fun deleteVolumeWithUpdate(volumeId: Long): Boolean

    /**
     * 批量删除书籍
     * @param volumeIds 待删除的书籍 ID 列表
     */
    suspend fun deleteVolumesByIds(volumeIds: List<Long>)

    /**
     * 删除一个系列
     * @param seriesId 待删除的系列
     */
    suspend fun deleteSeriesById(seriesId: Long): Int

    suspend fun deleteBookmark(bookmark: Bookmark)
    suspend fun clearAllBookmarks(): Int

    //改
    /**
     * 清除所有书籍的阅读记录
     * */
    suspend fun clearAllReadingHistory(): Int
    suspend fun updateSeries(series: Series)

    /**
     * 保存文件URI与封面URI的映射关系（更新所有匹配fileUri的Volume的coverUri）
     * @param fileUri 原PDF文件的URI（String）
     * @param coverUri 生成的封面图片URI（String）
     */
    suspend fun saveCoverUriMapping(fileUri: String, coverUri: String)
    suspend fun updateVolume(volume: Volume)

    suspend fun updateBookmark(bookmark: Bookmark)


    //查
    /**
     * 获取所有读过的书以展示历史阅读记录
     * */
    fun getReadVolumesStream(): Flow<PagingData<VolumeWithSeries>>

    /**
     * 获取所有未读过的书
     * */
    fun getUnreadVolumesStream(): Flow<PagingData<VolumeWithSeries>>

    /**
     * 获取系列中的所有书，返回Flow<PagingData<Volume>>，用于UI界面
     * @param order 排序方式
     * @param isAscending 是否升序
     * */
    fun getVolumesStreamInSeries(
        seriesId: Long,
        order: VolumeSortMethod,
        isAscending: Boolean
    ): Flow<PagingData<Volume>>

    /**
     * 获取所有系列，返回 PagingData<Series>，用于UI界面
     * @param order 排序方式
     * @param isAscending 是否升序
     * */
    fun getAllSeriesStream(order: SeriesSortMethod, isAscending: Boolean): Flow<PagingData<Series>>

    /**
     * 获取一个系列，返回Series，用于更新数据库
     * */
    suspend fun getSeriesById(seriesId: Long): Series

    /**
     * 获取一个系列，返回Flow<Series?>，用于UI界面
     * */
    fun getSeriesStreamById(seriesId: Long): Flow<Series?>

    /**
     * 获取单本书籍，返回Volume
     * */
    suspend fun getVolumeById(volumeId: Long): Volume

    /**
     * 获取系列中的所有书，返回List<Volume>，用于在系列内切换上下章节
     * @param order 排序方式
     * */
    suspend fun getVolumesInSeries(
        seriesId: Long,
        order: VolumeSortMethod,
        isAscending: Boolean
    ): List<Volume>

    /**
     * 按文件URI查询封面URI
     * */
    suspend fun getCoverUriByFileUri(fileUri: String): String?
    fun getTop3VolumesBySeriesIdOrderedByName(seriesId: Long): Flow<List<Volume>>

    /**
     * 获取带有书签的书籍流，并按指定方式排序。
     * @param order 书签排序方式
     */
    fun getVolumesWithBookmarksStream(order: BookmarkSortMethod): Flow<List<VolumeWithBookmarks>>

    fun getBookmarksByVolumeId(volumeId: Long): Flow<List<Bookmark>>

    /**
     * 获取所有收藏的书籍，并按系列分组。
     * @param order 系列的排序方式
     * @param isAscending 是否升序
     */
    fun getFavoriteVolumesGroupedBySeriesStream(
        order: SeriesSortMethod,
        isAscending: Boolean
    ): Flow<Map<Series, List<Volume>>>

    /**
     * 清除所有收藏。
     */
    suspend fun clearAllFavorites(): Int

}
