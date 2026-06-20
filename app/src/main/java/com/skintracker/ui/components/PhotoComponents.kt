package com.skintracker.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.skintracker.data.Meal
import com.skintracker.data.photo.MAX_MEAL_PHOTOS
import com.skintracker.data.photo.MealPhoto
import com.skintracker.data.photo.CaptureTarget
import com.skintracker.data.photo.createCaptureTarget
import com.skintracker.ui.theme.KetoTheme
import java.io.File

/**
 * Photo area shown below a meal step's action row — the native counterpart of
 * the web app's `#photo-area` (CLAUDE.md "Photos" / index.html `loadMealPhoto`):
 * a thumbnail per stored photo (tap to view full-screen, ✕ to delete) plus an
 * "Add Photo" / "Add Another" capture button.
 *
 * Capture is delegated entirely to the system camera app via
 * `ActivityResultContracts.TakePicture()` — no CAMERA permission, no CameraX.
 */
@Composable
fun MealPhotoArea(
    meal: Meal,
    photos: List<MealPhoto>,
    onCaptured: (File) -> Unit,
    onView: (MealPhoto) -> Unit,
    onRemove: (MealPhoto) -> Unit,
) {
    val context = LocalContext.current
    var pendingCapture by remember { mutableStateOf<CaptureTarget?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val capture = pendingCapture
        pendingCapture = null
        if (capture != null) {
            // Either path consumes the temp file: a successful capture is
            // handed to PhotoStore (which deletes it once compressed), a
            // cancelled one has nothing to compress so we delete it directly.
            if (success) onCaptured(capture.file) else capture.file.delete()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        photos.forEach { photo ->
            PhotoThumb(photo = photo, onClick = { onView(photo) }, onDelete = { onRemove(photo) })
        }
        CameraButton(
            label = if (photos.isEmpty()) "📷 Add Photo" else "📷 Add Another",
            enabled = photos.size < MAX_MEAL_PHOTOS,
        ) {
            val target = createCaptureTarget(context)
            pendingCapture = target
            launcher.launch(target.uri)
        }
    }
}

@Composable
private fun PhotoThumb(photo: MealPhoto, onClick: () -> Unit, onDelete: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        AsyncImage(
            model = photo.file,
            contentDescription = "Meal photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .clip(RoundedCornerShape(13.dp))
                .clickable(onClick = onClick),
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            KText("✕", size = 13, color = Color.White)
        }
    }
}

@Composable
private fun CameraButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.surf2)
            .border(1.5.dp, c.bdI, RoundedCornerShape(13.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        KText(label, size = 14, color = if (enabled) c.txtM else c.txtD, weight = FontWeight.Medium)
    }
}

/**
 * Small "📷 2" badge shown next to a meal's label in the summary — mirrors the
 * web app's `loadSummaryPhotoIcons` (`#ph-ic-{meal}`). Tapping it opens the
 * first photo full-screen, same as on the web.
 */
@Composable
fun PhotoIndicator(count: Int, onClick: () -> Unit) {
    if (count == 0) return
    val c = KetoTheme.colors
    Box(
        Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(c.inp)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        KText("📷${if (count > 1) " $count" else ""}", size = 11, color = c.txtM)
    }
}

/**
 * Full-screen photo viewer — native counterpart of the web app's `#photoModal`
 * (tap anywhere to dismiss).
 */
@Composable
fun PhotoViewer(photo: MealPhoto, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = photo.file,
            contentDescription = "Meal photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .clip(RoundedCornerShape(13.dp)),
        )
    }
}
