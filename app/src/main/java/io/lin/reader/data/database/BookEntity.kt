package io.lin.reader.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// 书籍系列表
@Entity(
    tableName = "series",
    indices = [Index(value = ["seriesName"], unique = true)]
)
data class Series(
    @PrimaryKey(autoGenerate = true) // 主键自增
    val id: Long = 0,
    val createTime: Long? = null, // 创建时间（用于排序）
    val seriesName: String = "Default Series Name", // 系列名（如“哈利波特”）
    val volumeCount: Int = 1 // 该系列的册数
)

// 单册书表，外键关联Series表
@Entity(
    tableName = "volumes",
    foreignKeys = [ForeignKey(
        entity = Series::class, // 关联的实体类
        parentColumns = ["id"], // 关联Series表的主键
        childColumns = ["seriesId"], // 本表格的外键字段
        onDelete = ForeignKey.CASCADE // 若系列被删除，对应的册书也自动删除
    )],
    indices = [
        // 为外键查询优化的索引
        Index(value = ["seriesId"]),
        // 复合唯一索引
        Index(value = ["seriesId", "volumeName"], unique = true)
    ]
)
data class Volume(
    @PrimaryKey(autoGenerate = true) // 主键自增
    val id: Long = 0,
    val createTime: Long? = null, // 创建时间（用于排序）
    val volumeName: String = "Default Volume Name", // 书名（如“哈利波特与死亡圣器”）
    val bookFileUri: String, // 书本体文件URI（存储Uri.toString()）
    val coverUri: String? = null, // 书封面URI（存储Uri.toString()）
    val totalPages: Int = 0, // 总页数
    val lastReadPage: Int = 0, // 上次阅读页数（暂不实现，默认0）
    val lastReadTime: Long? = null, // 上次阅读时间（初始化时为null）
    val seriesId: Long, // 外键：关联对应的书籍系列ID
    val isFavorite: Boolean = false // 收藏功能：默认为 false
)

// 书签表，外键关联Volumes表
@Entity(
    tableName = "bookmarks",
    foreignKeys = [ForeignKey(
        entity = Volume::class,
        parentColumns = ["id"],
        childColumns = ["volumeId"],
        onDelete = ForeignKey.CASCADE // 书籍被删除时，其关联的书签自动删除
    )],
    indices = [Index(value = ["volumeId"])]
)
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val volumeId: Long, // 外键：关联所属的书籍ID
    val label: String, // 书签名称
    val pageNumber: Int, // 记录的页数
    val addTime: Long // 添加书签的时间戳
)

/**
 * 一个用于封装查询结果的自定义数据类，它包含了完整的 Volume 信息以及其所属系列。
 * 这不是一个数据库实体。
 */
data class VolumeWithSeries(
    @Embedded
    val volume: Volume,
    @Relation(
        parentColumn = "seriesId",
        entityColumn = "id"
    )
    val series: Series
)

/**
 * 封装书籍及其所有书签的自定义数据类（一对多关系）。
 */
data class VolumeWithBookmarks(
    @Embedded
    val volume: Volume,
    @Relation(
        parentColumn = "id",
        entityColumn = "volumeId"
    )
    val bookmarks: List<Bookmark>
)
