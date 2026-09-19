package io.lin.reader.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** 
 * 书籍系列表 
 * @property id 主键（自增）
 * @property createTime 创建时间（用于排序）
 * @property seriesName 系列名（如《哈利波特》）
 * @property volumeCount 该系列的册数
 */
@Entity(
    tableName = "series",
    indices = [Index(value = ["seriesName"], unique = true)]
)
data class Series(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createTime: Long? = null,
    val seriesName: String = "Default Series Name",
    val volumeCount: Int = 1
)

/** 
 * 单册书表，外键关联Series表
 * @property id 主键（自增）
 * @property volumeName 书名（如《哈利波特与死亡圣器》）
 * @property mimeType 记录文件类型，默认 "application/pdf"
 * @property bookFileUri 书本体文件 URI(Uri.toString())
 * @property coverUri 书封面 URI(Uri.toString())
 * @property totalPages 总页数
 * @property createTime 创建时间（用于排序）
 * @property lastReadPage 上次阅读页索引（0-based），-1 代表未读
 * @property lastReadTime 上次阅读时间， null 表示未读
 * @property isFavorite 收藏功能：默认为 false
 * @property seriesId 外键：关联对应的书籍系列 ID
 */
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
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val volumeName: String = "Default Volume Name",
    val mimeType: String = "application/pdf",
    val bookFileUri: String,
    val coverUri: String? = null,
    val totalPages: Int = 0,
    val createTime: Long? = null,
    val lastReadPage: Int = -1,
    val lastReadTime: Long? = null,
    val isFavorite: Boolean = false,
    val seriesId: Long
)

/** 
 * 书签表，外键关联 Volumes 表 
 * @property id 主键（自增）
 * @property volumeId 外键：关联所属的书籍ID
 * @property label 书签名称
 * @property pageNumber 记录的页数
 * @property addTime 添加书签的时间戳
 */
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
    val volumeId: Long,
    val label: String,
    val pageNumber: Int,
    val addTime: Long
)

/** 
 * 页面设置表，用于存储特定页面的旋转等信息 
 * @property volumeId 所属书籍 ID
 * @property pageIndex 页面索引
 * @property rotation 旋转角度：0, 90, 180, 270
 */
@Entity(
    tableName = "page_settings",
    primaryKeys = ["volumeId", "pageIndex"],
    foreignKeys = [ForeignKey(
        entity = Volume::class,
        parentColumns = ["id"],
        childColumns = ["volumeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["volumeId"])]
)
data class PageSetting(
    val volumeId: Long,
    val pageIndex: Int,
    val rotation: Int = 0
)

/**
 * 一个用于封装查询结果的自定义数据类，它包含了完整的 Volume 信息以及其所属系列。
 * 这不是一个数据库实体。
 * @property volume 书籍信息
 * @property series 所属系列信息
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
 * @property volume 书籍信息
 * @property bookmarks 书籍关联的所有书签列表
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
