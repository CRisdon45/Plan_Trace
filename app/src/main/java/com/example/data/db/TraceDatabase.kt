package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ProjectEntity::class], version = 2, exportSchema = false)
abstract class TraceDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN pageDrawingsJson TEXT NOT NULL DEFAULT '{}'")
            }
        }
        @Volatile
        private var INSTANCE: TraceDatabase? = null

        fun getInstance(context: Context): TraceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TraceDatabase::class.java,
                    "plantrace_db"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
