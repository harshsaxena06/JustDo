package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CategoryDao
import com.example.data.dao.FocusSessionDao
import com.example.data.dao.ReminderDao
import com.example.data.dao.SubtaskDao
import com.example.data.dao.TaskDao
import com.example.data.entity.CategoryEntity
import com.example.data.entity.FocusSessionEntity
import com.example.data.entity.ReminderEntity
import com.example.data.entity.SubtaskEntity
import com.example.data.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        ReminderEntity::class,
        CategoryEntity::class,
        SubtaskEntity::class,
        FocusSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        const val DATABASE_NAME = "justdo_offline.db"
        const val DEFAULT_OTHER_CATEGORY_ID = 7L

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(id = 1L, name = "Study", color = 0xFF4F46E5.toInt(), isDefault = true),
            CategoryEntity(id = 2L, name = "Work", color = 0xFF7C3AED.toInt(), isDefault = true),
            CategoryEntity(id = 3L, name = "Personal", color = 0xFF0D9488.toInt(), isDefault = true),
            CategoryEntity(id = 4L, name = "Health", color = 0xFF16A34A.toInt(), isDefault = true),
            CategoryEntity(id = 5L, name = "Coding", color = 0xFF2563EB.toInt(), isDefault = true),
            CategoryEntity(id = 6L, name = "Exercise", color = 0xFFEA580C.toInt(), isDefault = true),
            CategoryEntity(id = 7L, name = "Other", color = 0xFF64748B.toInt(), isDefault = true)
        )

        // Prepared explicit migration template for future schema upgrades without destructive fallback
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Schema v1 -> v2 placeholder
            }
        }

        fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedDefaultCategories(db)
                    }
                })
                .build()
        }

        fun seedDefaultCategories(db: SupportSQLiteDatabase) {
            DEFAULT_CATEGORIES.forEach { cat ->
                db.execSQL(
                    "INSERT OR IGNORE INTO categories (id, name, color, isDefault) VALUES (?, ?, ?, ?)",
                    arrayOf(cat.id, cat.name, cat.color, if (cat.isDefault) 1 else 0)
                )
            }
        }
    }
}
