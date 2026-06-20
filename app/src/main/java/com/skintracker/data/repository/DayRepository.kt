package com.skintracker.data.repository

import android.util.Log
import com.skintracker.data.DayEntry
import com.skintracker.data.DayEntrySurrogate
import com.skintracker.data.toSurrogate
import com.skintracker.data.db.DayEntryDao
import com.skintracker.data.db.DayEntryEntity
import kotlinx.serialization.json.Json

private const val TAG = "DayRepository"

class DayRepository(private val dao: DayEntryDao) : IDayRepository {

    private val json = Json {
        ignoreUnknownKeys = true  // safe to add new DayEntry fields without breaking old records
        encodeDefaults = true     // write default-valued fields so records are self-contained
    }

    override suspend fun load(date: String): DayEntry {
        val entity = dao.get(date) ?: return DayEntry(date = date)
        return decode(entity) ?: DayEntry(date = date)
    }

    override suspend fun loadAll(): List<DayEntry> = dao.getAll().mapNotNull(::decode)

    override suspend fun save(entry: DayEntry) {
        dao.upsert(DayEntryEntity(date = entry.date, data = json.encodeToString(DayEntrySurrogate.serializer(), entry.toSurrogate())))
    }

    override suspend fun saveAll(entries: List<DayEntry>) {
        dao.upsertAll(entries.map { DayEntryEntity(date = it.date, data = json.encodeToString(DayEntrySurrogate.serializer(), it.toSurrogate())) })
    }

    override suspend fun delete(key: String) = dao.deleteByDate(key)
    override suspend fun deleteAll() = dao.deleteAll()

    // A decode failure here means a row exists in SQLite but its JSON no longer
    // matches DayEntrySurrogate (e.g. an incompatible field type after a schema
    // change). Falling back to a blank DayEntry is intentional — a single bad
    // row shouldn't crash the whole app — but it's logged loudly because the
    // caller can no longer tell "no data" from "undecodable data", and saving
    // that blank entry back would silently overwrite the original row for good.
    private fun decode(entity: DayEntryEntity): DayEntry? =
        runCatching { json.decodeFromString(DayEntrySurrogate.serializer(), entity.data).toDomain() }
            .onFailure { e -> Log.e(TAG, "Failed to decode entry for ${entity.date}: ${entity.data}", e) }
            .getOrNull()
}
