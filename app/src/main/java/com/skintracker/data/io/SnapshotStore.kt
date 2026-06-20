package com.skintracker.data.io

import android.content.Context
import android.net.Uri
import com.skintracker.data.DayEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * File-based storage for full snapshot data — the native counterpart to the
 * web app's inline `data` map inside each snapshot record (CLAUDE.md "Snapshot Schema").
 *
 * Metadata (id, name, ts, days) is stored separately in DataStore by [PrefsStore].
 * Full entry data lives here as individual JSON files: `filesDir/snapshots/snap_{id}.json`.
 * Keeping them separate avoids ballooning the DataStore key for users with large datasets.
 */
class SnapshotStore(context: Context) {

    private val dir = File(context.filesDir, "snapshots").apply { mkdirs() }

    private fun file(id: Long) = File(dir, "snap_$id.json")

    /** Encodes [entries] and writes to `snap_{id}.json`. Returns true on success. */
    suspend fun save(id: Long, entries: Map<String, DayEntry>): Boolean = withContext(Dispatchers.IO) {
        runCatching { file(id).writeText(DataPortability.encode(entries)) }.isSuccess
    }

    /** Decodes and returns the entry map for [id], or null if the file is missing or corrupt. */
    suspend fun load(id: Long): Map<String, DayEntry>? = withContext(Dispatchers.IO) {
        val f = file(id)
        if (!f.exists()) return@withContext null
        runCatching { DataPortability.decode(f.readText()) }.getOrNull()
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        file(id).delete()
    }

    /** Writes the snapshot JSON to [uri] via SAF — used by "Export Snapshot". Returns true on success. */
    suspend fun writeTo(context: Context, id: Long, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val f = file(id)
        if (!f.exists()) return@withContext false
        DataPortability.write(context, uri, f.readText())
    }
}
