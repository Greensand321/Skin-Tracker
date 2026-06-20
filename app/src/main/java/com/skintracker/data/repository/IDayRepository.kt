package com.skintracker.data.repository

import com.skintracker.data.DayEntry

interface IDayRepository {
    suspend fun load(date: String): DayEntry
    suspend fun loadAll(): List<DayEntry>
    suspend fun save(entry: DayEntry)

    /** Bulk upsert — used by Import (and, eventually, Snapshot restore) to write many days in one go. */
    suspend fun saveAll(entries: List<DayEntry>)

    suspend fun delete(key: String)
    suspend fun deleteAll()
}
