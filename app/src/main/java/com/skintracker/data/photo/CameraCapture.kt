package com.skintracker.data.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** A freshly-created temp file plus the content:// Uri the camera app writes into it through. */
data class CaptureTarget(val file: File, val uri: Uri)

/**
 * Creates a fresh content:// target in the app's cache dir for the system
 * camera app to write a full-resolution JPEG into (handed to
 * `ActivityResultContracts.TakePicture()`). [PhotoStore.addFromCapture] reads
 * and compresses [CaptureTarget.file] into a separate, much smaller copy and
 * always deletes it afterwards — see [clearStaleCaptures] for the startup
 * safety net that sweeps up anything left behind by a crash or killed process
 * mid-capture.
 */
fun createCaptureTarget(context: Context): CaptureTarget {
    val file = File(capturesDir(context), "capture_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return CaptureTarget(file, uri)
}

/**
 * Deletes any temp capture files left over from a previous session — e.g. the
 * process was killed between the camera writing the file and
 * [PhotoStore.addFromCapture] deleting it. Safe to call on every launch: a
 * normal session always leaves this directory empty.
 */
fun clearStaleCaptures(context: Context) {
    capturesDir(context).listFiles()?.forEach { it.delete() }
}

private fun capturesDir(context: Context): File =
    File(context.cacheDir, "captures").apply { mkdirs() }
