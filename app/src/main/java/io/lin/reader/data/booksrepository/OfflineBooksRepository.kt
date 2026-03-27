package io.lin.reader.data.booksrepository

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.Transaction
import io.lin.reader.data.database.Bookmark
import io.lin.reader.data.database.BookmarkDao
import io.lin.reader.data.database.Series
import io.lin.reader.data.database.SeriesDao
import io.lin.reader.data.database.Volume
import io.lin.reader.data.database.VolumeDao
import io.lin.reader.data.database.VolumeWithBookmarks
import io.lin.reader.data.database.VolumeWithSeries
import io.lin.reader.ui.screen.setting.details.BookmarkSortMethod
import io.lin.reader.ui.screen.setting.details.SeriesSortMethod
import io.lin.reader.ui.screen.setting.details.VolumeSortMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.TreeMap

/**
 * 书籍仓库，作为应用唯一的真实数据源。
 */
class OfflineBooksRepository(
    private val seriesDao: SeriesDao,
    private val volumeDao: VolumeDao,
    private val bookmarkDao: BookmarkDao
) : BooksRepository {
    val tag = "OfflineBooksRepository"

    //增
    /**
     * 生成唯一的系列名称
     * @param originalName 原始系列名称
     * @param maxAttempts 最大重命名尝试次数
     * @return 可插入的唯一名称（null表示所有尝试失败）
     */
    private suspend fun getUniqueSeriesName(
        originalName: String,
        maxAttempts: Int = 99
    ): String? {
        // 一次性查询所有系列名称（避免反复插入抛异常，效率提升80%+）
        val existingNames = seriesDao.getSeriesNames()

        // 原始名称未重复，直接返回
        if (!existingNames.contains(originalName)) {
            return originalName
        }

        // 循环生成递增的Copy名称，直到找到唯一值
        for (i in 0..maxAttempts) {
            val newName = if (i == 0) {
                "$originalName-Copy"
            } else {
                "$originalName-Copy ($i)"
            }
            if (!existingNames.contains(newName)) {
                return newName
            }
        }

        // 达到最大尝试次数仍无唯一名称，返回null
        Log.e(tag, "为名称 '$originalName' 生成唯一名称失败，已达最大尝试次数 $maxAttempts")
        return null
    }

    /**
     * 辅助：生成批次内唯一的名称
     */
    private fun getUniqueNameInBatch(
        originalName: String,
        existingInDb: Set<String>,
        usedInBatch: MutableSet<String>,
        maxAttempts: Int = 99
    ): String? {
        if (!existingInDb.contains(originalName) && !usedInBatch.contains(originalName)) {
            usedInBatch.add(originalName)
            return originalName
        }
        for (i in 0..maxAttempts) {
            val newName = if (i == 0) "$originalName-Copy" else "$originalName-Copy ($i)"
            if (!existingInDb.contains(newName) && !usedInBatch.contains(newName)) {
                usedInBatch.add(newName)
                return newName
            }
        }
        return null
    }

    /**
     * 插入一个系列。如果名称重复，会自动重命名并尝试插入。
     * @param series 要插入的原始系列对象。
     * @return 返回最终插入或更新的系列的 ID。如果多次尝试失败，则返回 -1。
     */
    @Transaction
    override suspend fun upsertSeries(series: Series): Long {
        Log.d(tag, "enter booksRepository.upsertSeries().")
        Log.d(
            tag,
            "Series information:\n ID: ${series.id}\n SeriesName: ${series.seriesName}\n VolumeCount: ${series.volumeCount}\n"
        )
        // If the series has an ID, it's an update, so we don't need to check for unique name
        if (series.id != 0L) {
            seriesDao.updateSeries(series)
            return series.id
        }

        val originalName = series.seriesName
        val newName = getUniqueSeriesName(originalName)
        if (newName == null) {
            Log.d(tag, "系列名称重复，且无法生成新系列名")
            return -1
        } else {
            val newSeries =
                series.copy(seriesName = newName, createTime = System.currentTimeMillis())
            return seriesDao.upsertSeries(newSeries)
        }
    }

    /**
     * 批量在已有系列中添加新书
     * @param volumeList 要添加的新书列表, 其seriesId都已经正确指向一个已存在的系列
     * @return 成功插入的书籍数量（0表示未执行插入或插入失败）
     */
    @Transaction
    override suspend fun addVolumesInExistingSeries(volumeList: List<Volume>): Int {
        if (volumeList.isEmpty()) return 0
        val targetSeriesId = volumeList[0].seriesId

        try {
            val originalSeries = getSeriesById(targetSeriesId)
            val existingNamesInDb = volumeDao.getVolumeNamesBySeriesId(targetSeriesId).toSet()
            val usedInBatch = mutableSetOf<String>()

            val validVolumes = mutableListOf<Volume>()
            volumeList.forEach { volume ->
                val uniqueName = getUniqueNameInBatch(volume.volumeName, existingNamesInDb, usedInBatch)
                if (uniqueName != null) {
                    validVolumes.add(volume.copy(volumeName = uniqueName, createTime = System.currentTimeMillis()))
                }
            }

            if (validVolumes.isEmpty()) return 0

            val updatedSeries = originalSeries.copy(volumeCount = originalSeries.volumeCount + validVolumes.size)
            seriesDao.updateSeries(updatedSeries)
            val insertedIds = volumeDao.insertVolumes(validVolumes)
            return insertedIds.size
        } catch (e: Exception) {
            Log.e(tag, "批量插入到已有系列失败", e)
            return 0
        }
    }

    /**
     * 批量添加新书，并为这些书创建一个新系列（原子事务）。
     * @param volumeList 要添加的新书列表（seriesId 会被覆盖为新系列ID，无需提前设置）
     * @param newSeriesName 新系列的名称
     * @return 成功插入的书籍数量（0表示未执行插入或插入失败）
     */
    @Transaction
    override suspend fun addVolumesWithNewSeries(
        volumeList: List<Volume>,
        newSeriesName: String
    ): Int {
        if (volumeList.isEmpty()) return 0

        try {
            // 1. 插入新系列
            val newSeries = Series(seriesName = newSeriesName, volumeCount = volumeList.size)
            val newSeriesId = upsertSeries(newSeries)
            if (newSeriesId == -1L) return 0

            val usedInBatch = mutableSetOf<String>()
            val validVolumes = mutableListOf<Volume>()
            volumeList.forEach { volume ->
                // 在新系列中，库里肯定没名字，所以 existing 为空集
                val uniqueName = getUniqueNameInBatch(volume.volumeName, emptySet(), usedInBatch)
                if (uniqueName != null) {
                    validVolumes.add(volume.copy(seriesId = newSeriesId, volumeName = uniqueName, createTime = System.currentTimeMillis()))
                }
            }

            if (validVolumes.isEmpty()) {
                throw SQLiteConstraintException("No valid volumes to insert in new series")
            }

            val insertedIds = volumeDao.insertVolumes(validVolumes)

            // 修正册数
            if (insertedIds.size != newSeries.volumeCount) {
                seriesDao.updateSeries(Series(id = newSeriesId, seriesName = newSeriesName, volumeCount = insertedIds.size))
            }

            return insertedIds.size
        } catch (e: Exception) {
            Log.e(tag, "批量保存到新系列失败，触发事务回滚", e)
            throw e // 抛出异常以触发 @Transaction 回滚，防止空壳系列
        }
    }

    override suspend fun insertBookmark(bookmark: Bookmark): Long {
        return bookmarkDao.insertBookmark(bookmark)
    }

    override suspend fun insertBookmarks(bookmarks: List<Bookmark>) {
        bookmarkDao.insertBookmarks(bookmarks)
    }

    //删
    /**
     * 删除一本指定的书籍，并将其所属系列的册数减一。
     * 整个操作在一个事务中完成，确保数据一致性。
     * @param volumeId 要删除的书籍的ID。
     * @return 如果成功删除，返回 true；如果书籍不存在，返回 false。
     */
    @Transaction
    override suspend fun deleteVolumeWithUpdate(volumeId: Long): Boolean {
        // 1. 先根据ID获取要删除的书籍信息
        val volumeToDelete = volumeDao.getVolumeById(volumeId) ?: return false

        // 2. 删除书籍
        volumeDao.deleteVolume(volumeToDelete)

        // 3. 获取该书籍所属的系列
        val series = getSeriesById(volumeToDelete.seriesId)


        // 4. 更新系列的册数 (减一)
        val updatedSeries = series.copy(volumeCount = (series.volumeCount - 1).coerceAtLeast(0))
        seriesDao.updateSeries(updatedSeries)

        return true
    }

    /**
     * 批量删除书籍
     * @param volumeIds 待删除的书籍 ID 列表
     */
    override suspend fun deleteVolumesByIds(volumeIds: List<Long>) {
        // 判空：避免执行无意义的 SQL
        if (volumeIds.isEmpty()) return
        volumeDao.deleteVolumesByIds(volumeIds)
    }

    /**
     * 删除一个系列
     * @param seriesId 待删除的系列
     */
    override suspend fun deleteSeriesById(seriesId: Long): Int {
        return seriesDao.deleteSeriesById(seriesId)
    }

    override suspend fun deleteBookmark(bookmark: Bookmark) {
        bookmarkDao.deleteBookmark(bookmark)
    }

    override suspend fun clearAllBookmarks(): Int {
        return bookmarkDao.clearAllBookmarks()
    }


    //改
    /**
     * 清除所有书籍的阅读记录
     * */
    override suspend fun clearAllReadingHistory(): Int {
        return volumeDao.resetAllHistory()
    }

    /**
     * 更新单个系列信息
     * @param series 要修改的系列（一定要有已存在的系列Id）
     */
    override suspend fun updateSeries(series: Series) {
        // First, get the current series from the database
        val currentSeries = seriesDao.getSeriesById(series.id) ?: throw Exception("系列不存在")

        // Check if the name is actually being changed
        if (currentSeries.seriesName != series.seriesName) {
            // Name is being changed, so we need to ensure the new name is unique
            val uniqueName = getUniqueSeriesName(series.seriesName)
            if (uniqueName != null) {
                seriesDao.updateSeries(series.copy(seriesName = uniqueName))
            } else {
                Log.e(tag, "系列名更新失败，无法为 ${series.seriesName} 生成唯一名称")
                // Handle the case where a unique name could not be generated
            }
        } else {
            // The name has not changed, so just update the series.
            // This is for cases like updating the volumeCount.
            seriesDao.updateSeries(series)
        }
    }

    override suspend fun updateVolume(volume: Volume) {
        volumeDao.updateVolume(volume)
    }

    override suspend fun updateBookmark(bookmark: Bookmark) {
        bookmarkDao.updateBookmark(bookmark)
    }

    /**
     * 保存文件URI与封面URI的映射关系（更新所有匹配fileUri的Volume的coverUri）
     * @param fileUri 原PDF文件的URI（String）
     * @param coverUri 生成的封面图片URI（String）
     */
    @Transaction
    override suspend fun saveCoverUriMapping(fileUri: String, coverUri: String) {
        Log.d("BooksRepository", "saveCoverUriMapping entered")
        // 1. 查询所有fileUri匹配的Volume列表（核心修改：从查单个改为查列表）
        val existingVolumes = volumeDao.getVolumesByFileUri(fileUri)
        Log.d("BooksRepository", "volumeDao.getVolumesByFileUri(fileUri) functioned")

        if (existingVolumes.isNotEmpty()) {
            // 2. 遍历列表，批量更新所有匹配Volume的coverUri
            val updatedVolumes = existingVolumes.map { volume ->
                volume.copy(coverUri = coverUri)
            }
            // 批量更新（DAO需新增批量更新方法）
            volumeDao.updateVolumes(updatedVolumes)
            Log.d(
                "BooksRepository",
                "批量更新${existingVolumes.size}个Volume的封面URI：$fileUri → $coverUri"
            )

            // 核心：更新对应的系列，触发 PagingSource 失效，从而让书架刷新
            val seriesIds = existingVolumes.map { it.seriesId }.distinct()
            seriesIds.forEach { id ->
                seriesDao.getSeriesById(id)?.let { series ->
                    seriesDao.updateSeries(series)
                }
            }
        } else {
            Log.d("BooksRepository", "无匹配记录时，插入临时Volume（后续导入时可覆盖字段）")
            Log.d("BooksRepository", "无匹配Volume，插入新记录关联封面：$fileUri → $coverUri")
        }
    }

    //查
    /**
     * 获取所有系列，返回 PagingData<Series>，用于UI界面
     * @param order 排序方式
     * @param isAscending 是否升序
     * */
    override fun getAllSeriesStream(
        order: SeriesSortMethod,
        isAscending: Boolean
    ): Flow<PagingData<Series>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20, // A reasonable page size
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                when (order) {
                    SeriesSortMethod.Name -> {
                        if (isAscending) {
                            seriesDao.getAllSeriesInNameOrder()
                        } else {
                            seriesDao.getAllSeriesInNameOrderDesc()
                        }
                    }

                    SeriesSortMethod.CreateTime -> {
                        if (isAscending) {
                            seriesDao.getAllSeriesInCreateTimeOrder()
                        } else {
                            seriesDao.getAllSeriesInCreateTimeOrderDesc()
                        }
                    }
                }
            }
        ).flow
    }

    /**
     * 获取系列中的所有书，返回Flow<PagingData<Volume>>，用于UI界面
     * @param order 排序方式
     * */
    override fun getVolumesStreamInSeries(
        seriesId: Long,
        order: VolumeSortMethod,
        isAscending: Boolean
    ): Flow<PagingData<Volume>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20, // A reasonable page size
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                when (order) {
                    VolumeSortMethod.Name -> {
                        if (isAscending) volumeDao.getVolumesStreamFromSeriesInNameOrder(seriesId)
                        else volumeDao.getVolumesStreamFromSeriesInNameOrderDesc(seriesId)
                    }

                    VolumeSortMethod.CreateTime -> {
                        if (isAscending) volumeDao.getVolumesStreamFromSeriesInCreateTimeOrder(
                            seriesId
                        )
                        else volumeDao.getVolumesStreamFromSeriesInCreateTimeOrderDesc(seriesId)
                    }

                    VolumeSortMethod.LastReadTime -> {
                        if (isAscending) volumeDao.getVolumesStreamFromSeriesInLastReadTimeOrder(
                            seriesId
                        )
                        else volumeDao.getVolumesStreamFromSeriesInLastReadTimeOrderDesc(seriesId)
                    }
                }
            }
        ).flow
    }

    /**
     * 获取所有读过的书以展示历史阅读记录
     * */
    override fun getReadVolumesStream(): Flow<PagingData<VolumeWithSeries>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20, // A reasonable page size
                enablePlaceholders = false
            ),
            pagingSourceFactory = { volumeDao.getReadVolumes() }
        ).flow
    }

    /**
     * 获取所有未读过的书
     * */
    override fun getUnreadVolumesStream(): Flow<PagingData<VolumeWithSeries>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20, // A reasonable page size
                enablePlaceholders = false
            ),
            pagingSourceFactory = { volumeDao.getUnreadVolumes() }
        ).flow
    }

    /**
     * 获取一个系列，返回Series，用于更新数据库
     * */
    override suspend fun getSeriesById(seriesId: Long): Series {
        return seriesDao.getSeriesById(seriesId) ?: throw Exception("系列不存在")
    }

    /**
     * 获取一个系列，返回Flow<Series?>，用于UI界面
     * */
    override fun getSeriesStreamById(seriesId: Long): Flow<Series?> {
        Log.d(tag, "enter booksRepository.getSeriesStreamById(), seriesId: $seriesId")
        return seriesDao.getSeriesStreamById(seriesId)
    }

    /**
     * 获取单本书籍信息，返回Volume
     * */
    override suspend fun getVolumeById(volumeId: Long): Volume {
        return volumeDao.getVolumeById(volumeId) ?: throw Exception("书籍不存在")
    }

    /**
     * 获取系列中的所有书，返回List<Volume>，用于在系列内切换上下章节
     * @param order 排序方式
     * */
    override suspend fun getVolumesInSeries(
        seriesId: Long,
        order: VolumeSortMethod,
        isAscending: Boolean
    ): List<Volume> {
        return when (order) {
            VolumeSortMethod.Name -> {
                if (isAscending) volumeDao.getVolumesFromSeriesInNameOrder(seriesId)
                else volumeDao.getVolumesFromSeriesInNameOrderDesc(seriesId)
            }

            VolumeSortMethod.CreateTime -> {
                if (isAscending) volumeDao.getVolumesFromSeriesInCreateTimeOrder(seriesId)
                else volumeDao.getVolumesFromSeriesInCreateTimeOrderDesc(seriesId)
            }

            VolumeSortMethod.LastReadTime -> {
                if (isAscending) volumeDao.getVolumesFromSeriesInLastReadTimeOrder(seriesId)
                else volumeDao.getVolumesFromSeriesInLastReadTimeOrderDesc(seriesId)
            }
        }
    }

    /**
     * 按文件URI查询封面URI
     * */
    override suspend fun getCoverUriByFileUri(fileUri: String): String? {
        return volumeDao.getCoverUriByFileUri(fileUri)
    }

    override fun getTop3VolumesBySeriesIdOrderedByName(seriesId: Long): Flow<List<Volume>> {
        return volumeDao.getTop3VolumesBySeriesIdOrderedByName(seriesId)
    }

    override fun getVolumesWithBookmarksStream(order: BookmarkSortMethod): Flow<List<VolumeWithBookmarks>> {
        val flow = when (order) {
            BookmarkSortMethod.VolumeName -> volumeDao.getVolumesWithBookmarksSortedByName()
            BookmarkSortMethod.LastReadTime -> volumeDao.getVolumesWithBookmarksSortedByLastRead()
            BookmarkSortMethod.VolumeCreateTime -> volumeDao.getVolumesWithBookmarksSortedByCreateTime()
            BookmarkSortMethod.LatestBookmarkTime -> volumeDao.getVolumesWithBookmarksSortedByLatestBookmark()
        }
        return flow.map { map ->
            map.map { (volume, bookmarks) ->
                VolumeWithBookmarks(volume, bookmarks)
            }
        }
    }

    override fun getBookmarksByVolumeId(volumeId: Long): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksByVolumeId(volumeId)
    }

    override fun getFavoriteVolumesGroupedBySeriesStream(
        order: SeriesSortMethod,
        isAscending: Boolean
    ): Flow<Map<Series, List<Volume>>> {
        return volumeDao.getAllFavoriteVolumesWithSeries().map { list ->
            // 1. 定义比较器，用于 TreeMap 的 Key (Series) 排序
            val seriesComparator = when (order) {
                SeriesSortMethod.Name -> {
                    if (isAscending) compareBy<Series> { it.seriesName }
                    else compareByDescending { it.seriesName }
                }

                SeriesSortMethod.CreateTime -> {
                    if (isAscending) compareBy<Series> { it.createTime ?: 0L }
                    else compareByDescending { it.createTime ?: 0L }
                }
            }

            // 2. 使用 TreeMap 自动对 Series (Key) 进行排序
            val groupedMap = TreeMap<Series, MutableList<Volume>>(seriesComparator)

            // 3. 将原始列表分组填充到 Map 中
            list.forEach { volumeWithSeries ->
                val series = volumeWithSeries.series
                val volume = volumeWithSeries.volume
                if (!groupedMap.containsKey(series)) {
                    groupedMap[series] = mutableListOf()
                }
                groupedMap[series]?.add(volume)
            }

            // 4. 对每个分组内部的 Volume 按照名称排序（可选，为了美观通常会加）
            groupedMap.values.forEach { volumes ->
                volumes.sortBy { it.volumeName }
            }

            groupedMap
        }
    }

    override suspend fun clearAllFavorites(): Int {
        return volumeDao.clearAllFavorites()
    }

}
