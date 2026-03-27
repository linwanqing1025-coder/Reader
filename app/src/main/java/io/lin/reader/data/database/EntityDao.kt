package io.lin.reader.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {
    /**
     * 1. 增：
     * 插入或更新一个系列。
     * - 如果系列ID为0损害不存在，则执行插入。
     * - 如果系列ID已存在，则执行更新。
     * 用于“创建新系列”（不能用来更新系列名，会抛出SQLiteConstraintException异常导致闪退）。
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun upsertSeries(series: Series): Long

    /**
     * 2. 删：
     * 根据ID删除一个系列。
     * 由于外键设置了CASCADE，所有与此系列关联的书籍将被自动删除。
     */
    @Delete
    suspend fun deleteSeries(series: Series)

    //删
    /**
     * 按系列ID删除单个系列（推荐）
     * 无需先查询Series对象，直接传ID即可删除，更高效灵活。
     * 外键CASCADE生效：关联书籍会自动删除。
     * @param seriesId 要删除的系列ID
     * @return 受影响的行数（成功删除返回1，无此ID返回0）
     */
    @Query("DELETE FROM series WHERE id = :seriesId")
    suspend fun deleteSeriesById(seriesId: Long): Int

    /**
     * 批量删除指定 ID 的系列（核心批量删除函数）
     * @param seriesIds 待删除的系列 ID 列表（非空）
     * @return 受影响的行数（即成功删除的系列数量，可用于判断删除是否生效）
     * 特性：触发外键 CASCADE，自动删除每个系列关联的所有书籍
     */
    @Query("DELETE FROM series WHERE id IN (:seriesIds)")
    suspend fun deleteSeriesByIds(seriesIds: List<Long>): Int

    /**
     * 3. 改：
     * 更新一个已存在的系列。
     * （如果不需要“插入或更新”的灵灵活度，可以使用此方法）
     */
    @Update
    suspend fun updateSeries(series: Series)

    /**
     * 4. 查
     */
    /**
     * 根据Id获取书的系列，返回一个Series，用于与其他函数互操作。
     * */
    @Query("SELECT * FROM series WHERE id = :seriesId")
    suspend fun getSeriesById(seriesId: Long): Series?

    /**
     * 根据Id获取书的系列，返回一个Flow<Series?>，用于UI界面。
     * */
    @Query("SELECT * FROM series WHERE id = :seriesId")
    fun getSeriesStreamById(seriesId: Long): Flow<Series?>

    @Query("SELECT * FROM series")
    suspend fun getAllSeriesList(): List<Series>

    @Query("SELECT seriesName FROM series")
    suspend fun getSeriesNames(): List<String>

    /**
     * 获取所有系列，按系列名称升序排列趋势。
     */
    @Query("SELECT * FROM series ORDER BY seriesName ASC")
    fun getAllSeriesInNameOrder(): PagingSource<Int, Series>

    /**
     * 获取所有系列，按系列名称降序排列。
     */
    @Query("SELECT * FROM series ORDER BY seriesName DESC")
    fun getAllSeriesInNameOrderDesc(): PagingSource<Int, Series>

    /**
     * 获取所有系列，按创建时间升序排列趋势。
     * NULL 值会排在最后面趋势。
     */
    @Query("SELECT * FROM series ORDER BY CASE WHEN createTime IS NULL THEN 1 ELSE 0 END, createTime ASC")
    fun getAllSeriesInCreateTimeOrder(): PagingSource<Int, Series>

    /**
     * 获取所有系列，按创建时间降序排列。
     * NULL 值会排在最后面。
     */
    @Query("SELECT * FROM series ORDER BY CASE WHEN createTime IS NULL THEN 1 ELSE 0 END, createTime DESC")
    fun getAllSeriesInCreateTimeOrderDesc(): PagingSource<Int, Series>
}

@Dao
interface VolumeDao {
    // 1. 增：
    /**
     * 批量插入书籍（核心批量新增函数）
     * 调用前必须确保所有Volume的seriesId指向已存在的系列或者为0！
     * @param volumes 待插入的书籍列表（非空）
     * @return List<Long> 每本书插入后的自增ID列表（顺序与传入volumes一致）
     * 注意：若某本书的seriesId无效，会触发Room约束异常，需上层捕获处理
     */
    @Insert
    suspend fun insertVolumes(volumes: List<Volume>): List<Long>

    // 2. 删：
    /**
     * 根据ID删除一本书。
     */
    @Delete
    suspend fun deleteVolume(volume: Volume)

    /**
     * 批量删除指定 ID 的书籍
     * @param volumeIds 待删除的书籍 ID 列表
     */
    @Query("DELETE FROM volumes WHERE id IN (:volumeIds)")
    suspend fun deleteVolumesByIds(volumeIds: List<Long>)


    // 3. 改：
    /**
     * 更新一本已存在的书籍。
     * 用于“改变书籍名称、封面等信息”。
     */
    @Update
    suspend fun updateVolume(volume: Volume)

    /**
     * 批量更新Volume
     * @param volumes 待更新的Volume列表
     */
    @Update
    suspend fun updateVolumes(volumes: List<Volume>)

    /**
     * 高效地将所有书籍的上次阅读时间重置为 NULL，并将上次阅读页数重置为 0。
     * @return 返回受影响的行数。
     */
    @Query("UPDATE volumes SET lastReadTime = NULL, lastReadPage = 0 WHERE lastReadTime IS NOT NULL OR lastReadPage != 0")
    suspend fun resetAllHistory(): Int

    /**
     * 将所有收藏的书籍移出收藏。
     */
    @Query("UPDATE volumes SET isFavorite = 0 WHERE isFavorite = 1")
    suspend fun clearAllFavorites(): Int


    // 4. 查：

    /**
     * 根据主键ID获取书籍信息。
     * */
    @Query("SELECT * FROM volumes WHERE id = :volumeId")
    suspend fun getVolumeById(volumeId: Long): Volume?

    /**
     * 根据Id获取单本书籍，返回一个Flow<Volume?>，用于UI界面。
     * */
    @Query("SELECT * FROM volumes WHERE id = :volumeId")
    fun getVolumeStreamById(volumeId: Long): Flow<Volume?>

    /**
     * 根据Id获取单本书籍及其系列信息。
     * @param volumeId 书籍ID
     * @return 一个包含书籍信息的Flow，包含Volume and Series。
     */
    @Transaction
    @Query("SELECT * FROM volumes WHERE id = :volumeId")
    fun getVolumeWithSeries(volumeId: Long): Flow<VolumeWithSeries?>

    /**
     * 获取所有收藏的书籍及其系列。
     */
    @Transaction
    @Query("SELECT * FROM volumes WHERE isFavorite = 1 ORDER BY lastReadTime DESC, createTime DESC")
    fun getFavoriteVolumes(): PagingSource<Int, VolumeWithSeries>

    /**
     * 获取所有收藏的书籍及其系列信息，不分页。
     */
    @Transaction
    @Query("SELECT * FROM volumes WHERE isFavorite = 1")
    fun getAllFavoriteVolumesWithSeries(): Flow<List<VolumeWithSeries>>

    /**
     * 高效获取所有已读过的书籍及其系列。
     * 仅返回 lastReadTime 不为 NULL 的书籍，并按上次阅读时间降序排列趋势。
     */
    @Transaction
    @Query(
        """
        SELECT * FROM volumes
        WHERE lastReadTime IS NOT NULL
        ORDER BY lastReadTime DESC
    """
    )
    fun getReadVolumes(): PagingSource<Int, VolumeWithSeries>

    /**
     * 高效获取所有未读过的书籍及其系列。
     * 仅返回 lastReadTime 为 NULL 的书籍，按创建时间降序排列。
     */
    @Transaction
    @Query(
        """
        SELECT * FROM volumes
        WHERE lastReadTime IS NULL
        ORDER BY CASE WHEN createTime IS NULL THEN 1 ELSE 0 END, createTime DESC
    """
    )
    fun getUnreadVolumes(): PagingSource<Int, VolumeWithSeries>

    /**
     * 查询指定系列下按书籍名称排序的前三本书
     * @param seriesId 目标系列ID
     * @return 按volumeName升序排列的前三本Volume，无数据则返回空列表
     */
    @Query(
        """
        SELECT * FROM volumes
        WHERE seriesId = :seriesId
        ORDER BY volumeName COLLATE NOCASE ASC
        LIMIT 3
    """
    )
    fun getTop3VolumesBySeriesIdOrderedByName(seriesId: Long): Flow<List<Volume>>

    @Query("SELECT coverUri FROM volumes WHERE bookFileUri = :fileUri LIMIT 1")
    suspend fun getCoverUriByFileUri(fileUri: String): String?

    @Query("SELECT * FROM volumes WHERE bookFileUri = :fileUri")
    suspend fun getVolumesByFileUri(fileUri: String): List<Volume>

    @Query("SELECT volumeName FROM volumes WHERE seriesId = :seriesId")
    suspend fun getVolumeNamesBySeriesId(seriesId: Long): List<String>

    // --- 获取系列内书籍的排序函数 ---
    @Query("SELECT * FROM volumes WHERE seriesId = :seriesId ORDER BY volumeName ASC")
    suspend fun getVolumesFromSeriesInNameOrder(seriesId: Long): List<Volume>

    @Query("SELECT * FROM volumes WHERE seriesId = :seriesId ORDER BY volumeName DESC")
    suspend fun getVolumesFromSeriesInNameOrderDesc(seriesId: Long): List<Volume>

    @Query("SELECT * FROM volumes WHERE seriesId = :seriesId ORDER BY CASE WHEN createTime IS NULL THEN 1 ELSE 0 END, createTime ASC")
    suspend fun getVolumesFromSeriesInCreateTimeOrder(seriesId: Long): List<Volume>

    @Query("SELECT * FROM volumes WHERE seriesId = :seriesId ORDER BY CASE WHEN createTime IS NULL THEN 1 ELSE 0 END, createTime DESC")
    suspend fun getVolumesFromSeriesInCreateTimeOrderDesc(seriesId: Long): List<Volume>

    @Query("SELECT * FROM volumes WHERE seriesId = :seriesId ORDER BY CASE WHEN lastReadTime IS NULL THEN 1 ELSE 0 END, lastReadTime ASC")
    suspend fun getVolumesFromSeriesInLastReadTimeOrder(seriesId: Long): List<Volume>

    @Query("SELECT * FROM volumes WHERE seriesId = :seriesId ORDER BY CASE WHEN lastReadTime IS NULL THEN 1 ELSE 0 END, lastReadTime DESC")
    suspend fun getVolumesFromSeriesInLastReadTimeOrderDesc(seriesId: Long): List<Volume>

    @Query("SELECT * FROM volumes WHERE seriesId = :seriesId ORDER BY volumeName ASC")
    fun getVolumesStreamFromSeriesInNameOrder(seriesId: Long): PagingSource<Int, Volume>

    @Query("SELECT * FROM volumes WHERE seriesId = :seriesId ORDER BY volumeName DESC")
    fun getVolumesStreamFromSeriesInNameOrderDesc(seriesId: Long): PagingSource<Int, Volume>

    @Query("SELECT * FROM volumes WHERE seriesId = :seriesId ORDER BY CASE WHEN createTime IS NULL THEN 1 ELSE 0 END, createTime ASC")
    fun getVolumesStreamFromSeriesInCreateTimeOrder(seriesId: Long): PagingSource<Int, Volume>

    @Query("SELECT * FROM volumes WHERE seriesId = :seriesId ORDER BY CASE WHEN createTime IS NULL THEN 1 ELSE 0 END, createTime DESC")
    fun getVolumesStreamFromSeriesInCreateTimeOrderDesc(seriesId: Long): PagingSource<Int, Volume>

    @Query("SELECT * FROM volumes WHERE seriesId = :seriesId ORDER BY CASE WHEN lastReadTime IS NULL THEN 1 ELSE 0 END, lastReadTime ASC")
    fun getVolumesStreamFromSeriesInLastReadTimeOrder(seriesId: Long): PagingSource<Int, Volume>

    @Query("SELECT * FROM volumes WHERE seriesId = :seriesId ORDER BY CASE WHEN lastReadTime IS NULL THEN 1 ELSE 0 END, lastReadTime DESC")
    fun getVolumesStreamFromSeriesInLastReadTimeOrderDesc(seriesId: Long): PagingSource<Int, Volume>

    // --- 书签相关的 Volume 查询 ---

    /**
     * 获取含有书签的书籍，并按 Volume 名称排序趋势。
     * 内部书签按 pageNumber 升序排序趋势。
     */
    @Transaction
    @Query(
        """
        SELECT * FROM volumes 
        INNER JOIN bookmarks ON volumes.id = bookmarks.volumeId 
        ORDER BY volumes.volumeName ASC, bookmarks.pageNumber ASC
    """
    )
    fun getVolumesWithBookmarksSortedByName(): Flow<Map<Volume, List<Bookmark>>>

    /**
     * 获取含有书签的书籍，并按 Volume 上次阅读时间排序（倒序）。
     */
    @Transaction
    @Query(
        """
        SELECT * FROM volumes 
        INNER JOIN bookmarks ON volumes.id = bookmarks.volumeId 
        ORDER BY volumes.lastReadTime DESC, bookmarks.pageNumber ASC
    """
    )
    fun getVolumesWithBookmarksSortedByLastRead(): Flow<Map<Volume, List<Bookmark>>>

    /**
     * 获取含有书签的书籍，并按 Volume 创建时间排序（倒序）。
     */
    @Transaction
    @Query(
        """
        SELECT * FROM volumes 
        INNER JOIN bookmarks ON volumes.id = bookmarks.volumeId 
        ORDER BY volumes.createTime DESC, bookmarks.pageNumber ASC
    """
    )
    fun getVolumesWithBookmarksSortedByCreateTime(): Flow<Map<Volume, List<Bookmark>>>

    /**
     * 获取含有书签的书籍，并按最新书签的添加时间排序（倒序）。
     */
    @Transaction
    @Query(
        """
        SELECT * FROM volumes 
        INNER JOIN bookmarks ON volumes.id = bookmarks.volumeId 
        ORDER BY (SELECT MAX(addTime) FROM bookmarks WHERE volumeId = volumes.id) DESC, bookmarks.pageNumber ASC
    """
    )
    fun getVolumesWithBookmarksSortedByLatestBookmark(): Flow<Map<Volume, List<Bookmark>>>
}

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmark(bookmark: Bookmark): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmarks(bookmarks: List<Bookmark>)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Update
    suspend fun updateBookmark(bookmark: Bookmark)

    @Query("SELECT * FROM bookmarks WHERE volumeId = :volumeId ORDER BY pageNumber ASC")
    fun getBookmarksByVolumeId(volumeId: Long): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE id = :bookmarkId")
    suspend fun getBookmarkById(bookmarkId: Long): Bookmark?

    @Query("DELETE FROM bookmarks")
    suspend fun clearAllBookmarks(): Int
}
