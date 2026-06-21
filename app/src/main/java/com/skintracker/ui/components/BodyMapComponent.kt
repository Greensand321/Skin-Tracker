package com.skintracker.ui.components

import android.graphics.Region as AndroidRegion
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.skintracker.data.BodyZone
import com.skintracker.data.nextSeverity
import com.skintracker.ui.theme.KetoTheme

// ── Virtual canvas ────────────────────────────────────────────────────────────
// All zone paths are defined in this coordinate space then scaled to the
// actual composable size at draw time. Coordinates derived from a ~2.4:1
// tall body reference (head centre ≈ y=42, feet bottom ≈ y=572).
private const val VW = 240f
private const val VH = 572f

// ── Fixed body-map colours (skin-diagram style, theme-independent) ────────────
private val SKIN_FILL   = Color(0xFFF5C5A3)  // peach base
private val SKIN_BORDER = Color(0xFFBE8C6A)  // darker edge for outline
private val SEV_MILD     = Color(0xFFFFE566)
private val SEV_MODERATE = Color(0xFFFF9933)
private val SEV_SEVERE   = Color(0xFFFF3838)

private fun Int?.toFillColor() = when (this) {
    1    -> SEV_MILD
    2    -> SEV_MODERATE
    3    -> SEV_SEVERE
    else -> SKIN_FILL
}

private fun Int.toTextColor() = when (this) {
    1    -> Color(0xFFA07800)
    2    -> Color(0xFFB85000)
    else -> Color(0xFFBB1111)
}

// ── Zone path definitions (virtual 240 × 572 space) ──────────────────────────
//
// Anatomical layout (patient-facing front view, no screen mirror):
//   RIGHT_* zones sit on the VIEWER'S LEFT  (screen x < 120)
//   LEFT_*  zones sit on the VIEWER'S RIGHT (screen x > 120)
// A 2–4 px gap between adjacent zones shows the dark app background, which
// acts as a natural zone border without needing an explicit stroke per zone.
private fun buildZonePath(zone: BodyZone): Path = Path().apply {
    when (zone) {
        BodyZone.HEAD_NECK -> {
            addOval(Rect(86f, 5f, 154f, 84f))          // head
            addRect(Rect(108f, 78f, 132f, 110f))        // neck (overlaps head bottom for continuity)
        }
        BodyZone.RIGHT_SHOULDER  -> addRoundRect(RoundRect(18f,  106f, 86f,  258f, 13f, 13f))
        BodyZone.LEFT_SHOULDER   -> addRoundRect(RoundRect(154f, 106f, 222f, 258f, 13f, 13f))
        // Front torso
        BodyZone.UPPER_CHEST     -> addRoundRect(RoundRect(88f,  106f, 152f, 214f,  9f,  9f))
        BodyZone.BELLY           -> addRoundRect(RoundRect(88f,  216f, 152f, 324f,  9f,  9f))
        // Back torso (same screen bounds as front, swapped in the back zone list)
        BodyZone.UPPER_BACK      -> addRoundRect(RoundRect(88f,  106f, 152f, 214f,  9f,  9f))
        BodyZone.LOWER_BACK      -> addRoundRect(RoundRect(88f,  216f, 152f, 324f,  9f,  9f))
        // Arms — forearms hang outward of shoulders
        BodyZone.RIGHT_FOREARM   -> addRoundRect(RoundRect(6f,   260f, 80f,  392f, 13f, 13f))
        BodyZone.LEFT_FOREARM    -> addRoundRect(RoundRect(160f, 260f, 234f, 392f, 13f, 13f))
        BodyZone.RIGHT_HAND      -> addOval(Rect(2f,   390f, 82f,  444f))
        BodyZone.LEFT_HAND       -> addOval(Rect(158f, 390f, 238f, 444f))
        // Legs — 6 px gap between thighs at centre
        BodyZone.RIGHT_THIGH     -> addRoundRect(RoundRect(88f,  327f, 117f, 460f,  9f,  9f))
        BodyZone.LEFT_THIGH      -> addRoundRect(RoundRect(123f, 327f, 152f, 460f,  9f,  9f))
        BodyZone.RIGHT_CALF      -> addRoundRect(RoundRect(90f,  463f, 116f, 556f,  9f,  9f))
        BodyZone.LEFT_CALF       -> addRoundRect(RoundRect(124f, 463f, 150f, 556f,  9f,  9f))
        // Feet flare outward from calves
        BodyZone.RIGHT_FOOT      -> addOval(Rect(76f,  554f, 118f, 572f))
        BodyZone.LEFT_FOOT       -> addOval(Rect(122f, 554f, 164f, 572f))
    }
}

private val ZONE_PATHS: Map<BodyZone, Path> by lazy {
    BodyZone.entries.associateWith { buildZonePath(it) }
}

// Pre-computed body outline (union of all zones in each view) for the border stroke.
private fun buildSilhouette(zones: List<BodyZone>): Path {
    var current = Path()
    zones.forEach { zone ->
        val p = ZONE_PATHS[zone] ?: return@forEach
        val merged = Path()
        merged.op(current, p, PathOperation.Union)
        current = merged
    }
    return current
}
private val FRONT_SILHOUETTE: Path by lazy { buildSilhouette(BodyZone.frontZones) }
private val BACK_SILHOUETTE:  Path by lazy { buildSilhouette(BodyZone.backZones) }

// ── Hit testing ───────────────────────────────────────────────────────────────
// Converts a tap in virtual coordinates to the first matching zone.
// Zones earlier in the list take priority where paths overlap (e.g. neck > chest).
private val HIT_CLIP = AndroidRegion(0, 0, VW.toInt() + 1, VH.toInt() + 1)

private fun hitTestZone(vx: Float, vy: Float, zones: List<BodyZone>): BodyZone? {
    val ix = vx.toInt()
    val iy = vy.toInt()
    for (zone in zones) {
        val ap = ZONE_PATHS[zone]?.asAndroidPath() ?: continue
        val r = AndroidRegion()
        r.setPath(ap, HIT_CLIP)
        if (r.contains(ix, iy)) return zone
    }
    return null
}

// ── Public composable ─────────────────────────────────────────────────────────

/**
 * Interactive front/back body diagram. Tapping a zone cycles its swelling
 * severity null → 1 (mild) → 2 (moderate) → 3 (severe) → null (clear).
 * Severity values are stored in [swelling] as zoneId → 1..3.
 */
@Composable
fun BodyMapSelector(
    swelling: Map<String, Int>,
    onSwellingChange: (zone: String, severity: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showBack by remember { mutableStateOf(false) }
    val c = KetoTheme.colors
    val zones = if (showBack) BodyZone.backZones else BodyZone.frontZones

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Front / Back toggle pill
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(c.surf2)
                .border(1.dp, c.bd, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ViewToggleTab("Front", !showBack, Modifier.weight(1f)) { showBack = false }
            ViewToggleTab("Back",   showBack, Modifier.weight(1f)) { showBack = true  }
        }

        // Body canvas — centred, fixed max width; height follows aspect ratio
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BodyCanvas(
                swelling  = swelling,
                zones     = zones,
                showBack  = showBack,
                onZoneTap = { zone ->
                    val next = swelling[zone.id].nextSeverity()
                    onSwellingChange(zone.id, next ?: 0)
                },
                modifier = Modifier.fillMaxWidth(0.52f),
            )
        }

        // Severity legend
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        ) {
            listOf(1 to "Mild", 2 to "Moderate", 3 to "Severe").forEach { (sev, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        Modifier
                            .size(11.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(sev.toFillColor()),
                    )
                    KText(label, size = 11, color = c.txtM)
                }
            }
        }

        // Active zone list
        val active = swelling.entries
            .filter { it.value in 1..3 }
            .sortedBy { it.key }
        if (active.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                KText(
                    "Marked areas".uppercase(),
                    size = 11, color = c.txtM, weight = FontWeight.SemiBold,
                    letterSpacing = 0.7f,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                active.forEach { (id, sev) ->
                    val label = BodyZone.fromId(id)?.label ?: id
                    val sevLabel = when (sev) { 1 -> "Mild"; 2 -> "Moderate"; else -> "Severe" }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.inp)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KText(label, size = 14, color = c.txt)
                        KText(sevLabel, size = 13, color = sev.toTextColor())
                    }
                }
            }
        }
    }
}

// ── Canvas ────────────────────────────────────────────────────────────────────

@Composable
private fun BodyCanvas(
    swelling: Map<String, Int>,
    zones: List<BodyZone>,
    showBack: Boolean,
    onZoneTap: (BodyZone) -> Unit,
    modifier: Modifier = Modifier,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    // pointerInput is keyed only on `zones` (a stable reference), so the gesture
    // block is never restarted and would otherwise capture a frozen snapshot of
    // `onZoneTap` — which closes over the current `swelling` map. Routing taps
    // through rememberUpdatedState keeps each tap reading the latest callback
    // (and therefore the latest severity) without restarting the detector.
    val currentOnZoneTap by rememberUpdatedState(onZoneTap)

    Canvas(
        modifier = modifier
            .aspectRatio(VW / VH)
            .onSizeChanged { canvasSize = it }
            .pointerInput(zones) {
                detectTapGestures { offset ->
                    val w = canvasSize.width.toFloat()
                    val h = canvasSize.height.toFloat()
                    if (w == 0f) return@detectTapGestures
                    val vx = offset.x * VW / w
                    val vy = offset.y * VH / h
                    hitTestZone(vx, vy, zones)?.let(currentOnZoneTap)
                }
            },
    ) {
        val sx = size.width / VW
        val sy = size.height / VH

        withTransform(transformBlock = {
            scale(scaleX = sx, scaleY = sy, pivot = Offset.Zero)
        }) {
            // Fill each zone — dark app background shows through 2–4 px gaps,
            // naturally delineating zone boundaries.
            zones.forEach { zone ->
                val path = ZONE_PATHS[zone] ?: return@forEach
                drawPath(path, swelling[zone.id].toFillColor())
            }
            // Single outer border drawn over all zones for a clean body outline.
            val silhouette = if (showBack) BACK_SILHOUETTE else FRONT_SILHOUETTE
            drawPath(silhouette, SKIN_BORDER, style = Stroke(width = 1.4f))
        }
    }
}

@Composable
private fun ViewToggleTab(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = KetoTheme.colors
    Box(
        modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) c.accent.copy(alpha = 0.16f) else Color.Transparent)
            .then(
                if (selected) Modifier.border(1.5.dp, c.accent, RoundedCornerShape(7.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        KText(
            label,
            size = 13,
            color = if (selected) c.accent else c.txtM,
            weight = FontWeight.SemiBold,
        )
    }
}
