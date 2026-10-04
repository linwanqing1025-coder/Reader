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
 * @property seriesName 系列名（如《哈利波特》）
 * @property volumeCount 该系列的册数
 * @property createTime 创建时间（用于排序）
 */
@Entity(
    tableName = "series",
    indices = [Index(value = ["seriesName"], unique = true)]
)
data class Series(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val seriesName: String,
    val volumeCount: Int,
    val createTime: Long,
)

/** 
 * 单册书表，外键关联 Series 表
 * @property id 主键（自增）
 * @property seriesId 外键：关联对应的书籍系列 ID
 * @property volumeName 书名（如《哈利波特与死亡圣器》）
 * @property mimeType 记录文件类型，默认 "application/pdf"
 * @property createTime 创建时间（用于排序）
 * @property bookFileUri 书本体文件 URI(Uri.toString())
 * @property coverUri 书封面 URI(Uri.toString())
 * @property totalPages 总页数。若为 null，说明该文档为重排文档（如 EPUB），页数会动态变化；若非 null，代表该文档为固定版式（如 PDF）。
 * @property isFavorite 收藏功能：默认为 false
 * @property history 历史阅读记录。若为 null，表示该书从未被阅读过。
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
    // 键
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val seriesId: Long,

    // 基本信息
    val volumeName: String,
    val mimeType: String,
    val createTime: Long,
    val bookFileUri: String,
    val coverUri: String? = null,
    val totalPages: Int? = 0,

    // 收藏
    val isFavorite: Boolean = false,

    // 历史记录（组合 ReadingHistory）
    @Embedded(prefix = "history_")
    val history: ReadingHistory? = null,
)

/** 
 * 书签表，外键关联 Volumes 表 
 * @property id 主键（自增）
 * @property volumeId 外键：关联所属的书籍ID
 * @property label 书签名称（用户自定义的备注或默认名称）
 * @property addTime 添加书签的时间戳
 * @property history 组合的阅读位置信息，不能为空，记录书签具体的定位数据。
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

    // 书签具体的定位数据，不能为空
    @Embedded(prefix = "pos_")
    val history: ReadingHistory
)

/**
 * 阅读历史记录
 * @property mupdfMark MuPDF 底层的物理锚点。解决重排文档由于排版变化导致逻辑页码 `pageIndex` 失效的问题。
 * @property time 上次阅读时间
 * @property readProgress 阅读进度百分比 [0.0f ~ 1.0f]。作为重排文档书签 UI 展示的补充进度。
 * @property pageIndex 记录的逻辑页索引。固定版式(PDF)有绝对页码；重排文档(EPUB)可为 null，因页码随排版改变。
 * @property chapterInfo 从 Outline 获取的章节信息。设为 null 则说明该文档没有 Outline 。
 */
data class ReadingHistory(
    val mupdfMark: Long,
    val time: Long,
    val readProgress: Float = 0.0f,
    val pageIndex: Int? = null,
    val chapterInfo: String? = null,
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