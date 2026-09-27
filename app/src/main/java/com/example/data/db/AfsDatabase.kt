package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CommandHistoryEntity
import com.example.data.model.ShortcutEntity

@Database(
    entities = [ShortcutEntity::class, CommandHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AfsDatabase : RoomDatabase() {
    abstract fun shortcutDao(): ShortcutDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AfsDatabase? = null

        fun getDatabase(context: Context): AfsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AfsDatabase::class.java,
                    "afs_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
