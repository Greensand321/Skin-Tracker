package com.skintracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Two-column Room table: the primary key is the date string, and `data` holds
 * the full DayEntry serialised as JSON. This means adding new fields to
 * DayEntry never requires a Room migration — only the Kotlin class changes.
 */
@Entity(tableName = "day_entries")
data class DayEntryEntity(
    @PrimaryKey val date: String,
    val data: String,
)
