package com.skintracker.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

/** Mirrors the web app's MAX_MEAL_PHOTOS (see CLAUDE.md / index.html). */
const val MAX_MEAL_PHOTOS = 5

private const val MAX_DIMENSION_PX = 900
private const val JPEG_QUALITY = 75

/** One stored photo for a meal. [file] is a stable, immutable on-disk JPEG. */
data class MealPhoto(val id: String, val file: File)

enum class PhotoSaveResult { SAVED, LIMIT_REACHED, FAILED }

/**
 * On-device JPEG photo storage for meal logs — the native counterpart to the
 * web app's `keto-photos` IndexedDB store (see CLAUDE.md "IndexedDB (photo
 * store)"). Photos live in app-private storage as compressed JPEGs named
 * `{date}_{meal}_{timestamp}.jpg`; the timestamp makes every filename unique
 * forever, so a deleted slot is never reused — which keeps Coil's file-path
 * image cache from ever serving stale bytes for a recycled name.
 */
class PhotoStore(context: Context) {

    private val dir = File(context.filesDir, "photos").apply { mkdirs() }

    private fun prefix(date: String, meal: String) = "${date}_${meal}_"

    /** All photos for a meal, oldest first (filenames sort chronologically). */
    fun listPhotos(date: String, meal: String): List<MealPhoto> {
        val p = prefix(date, meal)
        return (dir.listFiles { f -> f.isFile && f.name.startsWith(p) && f.extension == "jpg" } ?: emptyArray())
            .sortedBy { it.name }
            .map { MealPhoto(id = it.name, file = it) }
    }

    /**
     * Compresses a freshly-captured photo ([CaptureTarget.file]) and stores it
     * as a new slot for [date]/[meal], same as the web app's `addMealPhoto` →
     * `compressImage` pipeline (max 900px, ~0.75 quality — see CLAUDE.md
     * "IndexedDB (photo store)"). Always deletes [capture] when done — it's a
     * full-resolution temp file that's no longer needed either way (see
     * `CameraCapture.kt` for why this matters).
     */
    suspend fun addFromCapture(date: String, meal: String, capture: File): PhotoSaveResult =
        withContext(Dispatchers.IO) {
            try {
                if (listPhotos(date, meal).size >= MAX_MEAL_PHOTOS) return@withContext PhotoSaveResult.LIMIT_REACHED
                val bytes = compress(capture) ?: return@withContext PhotoSaveResult.FAILED
                val file = File(dir, "${prefix(date, meal)}${System.currentTimeMillis()}.jpg")
                runCatching { file.writeBytes(bytes) }
                    .fold({ PhotoSaveResult.SAVED }, { PhotoSaveResult.FAILED })
            } finally {
                capture.delete()
            }
        }

    suspend fun delete(photo: MealPhoto) = withContext(Dispatchers.IO) {
        photo.file.delete()
    }

    /** Total on-disk size (bytes) and count of every stored photo — for the Settings storage stats. */
    suspend fun usage(): Pair<Long, Int> = withContext(Dispatchers.IO) {
        val files = dir.listFiles { f -> f.isFile && f.extension == "jpg" } ?: emptyArray()
        files.sumOf { it.length() } to files.size
    }

    /** All photo files — used by ZIP export to bundle every stored JPEG. */
    fun listAllPhotoFiles(): List<File> =
        (dir.listFiles { f -> f.isFile && f.extension == "jpg" } ?: emptyArray()).toList()

    /** Writes [bytes] to the photos directory under [filename] — used by ZIP restore.
     * Skips existing files to protect photos the user has taken since the backup was made. */
    suspend fun restorePhoto(filename: String, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val target = File(dir, filename)
        if (target.exists()) return@withContext false
        runCatching { target.writeBytes(bytes) }.isSuccess
    }

    // ── Compression: decode → EXIF-correct → downscale → JPEG-encode ─────────

    private fun compress(file: File): ByteArray? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        file.inputStream().use { BitmapFactory.decodeStream(it, null, bounds) }

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_DIMENSION_PX && bounds.outHeight / (sample * 2) >= MAX_DIMENSION_PX) {
            sample *= 2
        }
        val decoded = file.inputStream().use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val oriented = rotateToUpright(file.inputStream().use { ExifInterface(it) }, decoded)
        val scaled = scaleDown(oriented, MAX_DIMENSION_PX)

        ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }.getOrNull()

    private fun rotateToUpright(exif: ExifInterface?, bitmap: Bitmap): Bitmap {
        val degrees = when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDimension && h <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * scale).roundToInt(), (h * scale).roundToInt(), true)
    }
}
