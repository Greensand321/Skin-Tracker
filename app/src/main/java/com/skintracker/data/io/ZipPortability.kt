package com.skintracker.data.io

import android.content.Context
import android.net.Uri
import com.skintracker.data.DayEntry
import com.skintracker.data.photo.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ZipImportResult(
    val entries: Map<String, DayEntry>,
    val photos: Map<String, ByteArray>, // filename → raw JPEG bytes
)

/**
 * Full-backup ZIP export/import: bundles `data.json` (all day entries in
 * DataPortability format) plus every stored JPEG in a `photos/` directory.
 * The native equivalent of manually combining JSON export + photo transfer.
 */
object ZipPortability {

    suspend fun export(
        context: Context,
        uri: Uri,
        entries: Map<String, DayEntry>,
        photoStore: PhotoStore,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val os = context.contentResolver.openOutputStream(uri) ?: error("no output stream")
            ZipOutputStream(os.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(DataPortability.encode(entries).toByteArray())
                zip.closeEntry()

                photoStore.listAllPhotoFiles().forEach { file ->
                    zip.putNextEntry(ZipEntry("photos/${file.name}"))
                    zip.write(file.readBytes())
                    zip.closeEntry()
                }
            }
        }.isSuccess
    }

    suspend fun import(context: Context, uri: Uri): ZipImportResult? = withContext(Dispatchers.IO) {
        runCatching {
            val ins = context.contentResolver.openInputStream(uri) ?: error("no input stream")
            var entries = emptyMap<String, DayEntry>()
            val photos = mutableMapOf<String, ByteArray>()

            ZipInputStream(ins.buffered()).use { zip ->
                var ze = zip.nextEntry
                while (ze != null) {
                    when {
                        ze.name == "data.json" ->
                            entries = DataPortability.decode(zip.readBytes().decodeToString())
                        ze.name.startsWith("photos/") && !ze.isDirectory -> {
                            val name = ze.name.removePrefix("photos/")
                            if (name.isNotEmpty()) photos[name] = zip.readBytes()
                        }
                    }
                    zip.closeEntry()
                    ze = zip.nextEntry
                }
            }
            ZipImportResult(entries, photos)
        }.getOrNull()
    }
}
