package com.skintracker.data.io

import android.content.Context
import com.skintracker.data.db.KETO_DB_NAME
import com.skintracker.data.photo.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 512 MB — a fixed display ceiling for the storage bar. App-private storage
 * (Room + on-disk JPEGs) isn't quota-limited the way browser localStorage/
 * IndexedDB are, so there's no real OS limit to measure against; the web
 * app's own native (Capacitor) code path makes the same choice for the same
 * reason (see `Storage.stats()` / CLAUDE.md "Storage usage stats"). */
private const val DISPLAY_CEILING_KB = 512 * 1024

/** Snapshot of on-device storage usage shown in Settings — native counterpart
 * of the web app's `getStorageStats()` (`{ usedKB, pct, days, snaps }`). */
data class StorageStats(
    val days: Int,
    val dbKB: Int,
    val photoKB: Int,
    val photoCount: Int,
    val totalKB: Int,
    val quotaKB: Int,
    /** Fraction in `0f..1f`, ready for `Modifier.fillMaxWidth(pct)`. */
    val pct: Float,
)

object StorageUsage {

    /** Sizes the Room database file directly and sums every stored JPEG. */
    suspend fun compute(context: Context, photoStore: PhotoStore, dayCount: Int): StorageStats =
        withContext(Dispatchers.IO) {
            val dbFile = context.getDatabasePath(KETO_DB_NAME)
            val dbKB = (if (dbFile.exists()) dbFile.length() else 0L).kb()
            val (photoBytes, photoCount) = photoStore.usage()
            val photoKB = photoBytes.kb()
            val totalKB = dbKB + photoKB
            StorageStats(
                days = dayCount,
                dbKB = dbKB,
                photoKB = photoKB,
                photoCount = photoCount,
                totalKB = totalKB,
                quotaKB = DISPLAY_CEILING_KB,
                pct = (totalKB.toFloat() / DISPLAY_CEILING_KB).coerceIn(0f, 1f),
            )
        }

    private fun Long.kb(): Int = (this / 1024).toInt()
}
