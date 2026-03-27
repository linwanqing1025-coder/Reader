package io.lin.reader.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 数据库主类，定义了数据库的结构和版本。
 */
@Database(
    entities = [Series::class, Volume::class, Bookmark::class], // 添加了 Bookmark 实体
    version = 1,
    exportSchema = false
)
abstract class ReaderDatabase : RoomDatabase() {

    // 提供 DAO 接口的访问方法
    abstract fun seriesDao(): SeriesDao
    abstract fun volumeDao(): VolumeDao
    abstract fun bookmarkDao(): BookmarkDao // 添加了 BookmarkDao

    /**
     * 单例模式，确保整个应用中只有一个数据库实例。
     */
    companion object {
        @Volatile
        private var Instance: ReaderDatabase? = null

        /**
         * 获取数据库实例的公共方法。
         */
        fun getDatabase(context: Context): ReaderDatabase {
            return Instance ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReaderDatabase::class.java,
                    "book_database"
                )
                    // 由于不增加版本号但修改了结构，建议在开发阶段添加此项，
                    // 否则若手机已有旧库会报错。清除应用数据亦可。
                    .fallbackToDestructiveMigration()//TODO
                    .build()
                Instance = instance
                instance
            }
        }
    }
}
