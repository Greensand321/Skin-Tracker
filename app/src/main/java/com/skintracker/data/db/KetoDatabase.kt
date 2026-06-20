package com.skintracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** File name on disk — also used by `StorageStats` to size the database file directly. */
const val KETO_DB_NAME = "keto_tracker.db"

@Database(entities = [DayEntryEntity::class], version = 1, exportSchema = false)
abstract class KetoDatabase : RoomDatabase() {

    abstract fun dayEntryDao(): DayEntryDao

    companion object {
        @Volatile private var INSTANCE: KetoDatabase? = null

        fun get(context: Context): KetoDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                KetoDatabase::class.java,
                KETO_DB_NAME,
            ).build().also { INSTANCE = it }
        }
    }
}
