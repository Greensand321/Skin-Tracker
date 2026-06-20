package com.skintracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skintracker.ui.theme.KETO_THEMES
import com.skintracker.ui.theme.KetoTheme
import com.skintracker.ui.theme.THEME_LIST
import com.skintracker.ui.theme.ThemeInfo

/**
 * Bottom-anchored theme picker overlay.
 *
 * Layered as two independent children of a full-screen Box:
 *   1. Scrim (behind) — tapping it closes the panel
 *   2. Panel (in front) — pointerInput consumes all events so taps inside
 *      never reach the scrim, while individual swatches fire their onClick.
 *
 * Auto-theme mode (CLAUDE.md "Theme System") relabels the two sections
 * "Night"/"Day", highlights the swatches currently assigned to each, and
 * routes taps to [onPickAuto] instead of [onPick] — picking a swatch updates
 * that slot in place rather than applying-and-closing, mirroring the web
 * app's `renderThemeGrid()` auto-mode behaviour.
 */
@Composable
fun ThemePanel(
    currentId: String,
    autoEnabled: Boolean,
    darkAutoId: String,
    lightAutoId: String,
    onPick: (String) -> Unit,
    onPickAuto: (forDark: Boolean, id: String) -> Unit,
    onToggleAuto: () -> Unit,
    onClose: () -> Unit,
) {
    val c = KetoTheme.colors

    Box(Modifier.fillMaxSize()) {

        // ── 1. Scrim ──────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable { onClose() }
        )

        // ── 2. Panel (bottom-aligned, eats all its own touch events) ─────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(c.bg)
                .border(1.dp, c.bdI, RoundedCornerShape(20.dp))
                // Consume ALL pointer events so nothing leaks to the scrim.
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header row
            Box(Modifier.fillMaxWidth()) {
                KText("🎨 Themes", size = 16, color = c.gold, weight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart))
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .clickable { onClose() }
                        .padding(4.dp)
                ) {
                    KText("✕", size = 16, color = c.txtM)
                }
            }

            if (autoEnabled) {
                ThemeSection("🌙 Night Theme", THEME_LIST.filter { it.dark }, darkAutoId) { onPickAuto(true, it) }
                ThemeSection("☀️ Day Theme", THEME_LIST.filter { !it.dark }, lightAutoId) { onPickAuto(false, it) }
            } else {
                ThemeSection("Dark", THEME_LIST.filter { it.dark }, currentId, onPick)
                ThemeSection("Light", THEME_LIST.filter { !it.dark }, currentId, onPick)
            }

            AutoThemeToggle(enabled = autoEnabled, onToggle = onToggleAuto)
        }
    }
}

@Composable
private fun ThemeSection(
    title: String,
    themes: List<ThemeInfo>,
    selectedId: String,
    onPick: (String) -> Unit,
) {
    val c = KetoTheme.colors
    val rows = (themes.size + 3) / 4
    KText(title.uppercase(), size = 11, color = c.txtM, weight = FontWeight.Bold, letterSpacing = 1.8f)
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .height((rows * 76).dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false,
    ) {
        items(themes) { t ->
            ThemeSwatch(t, selected = t.id == selectedId, onPick = onPick)
        }
    }
}

/** Toggle row for auto-theme — mirrors the web app's `.auto-tog` swatch tile. */
@Composable
private fun AutoThemeToggle(enabled: Boolean, onToggle: () -> Unit) {
    val c = KetoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surf2)
            .border(2.dp, if (enabled) c.accent else c.bd, RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KText("🌓", size = 16)
        Column {
            KText("Auto theme", size = 14, color = c.txt, weight = FontWeight.SemiBold)
            KText(
                if (enabled) "On — follows system dark/light mode" else "Off — using a single theme everywhere",
                size = 12,
                color = c.txtM,
            )
        }
    }
}

@Composable
private fun ThemeSwatch(info: ThemeInfo, selected: Boolean, onPick: (String) -> Unit) {
    val c = KetoTheme.colors
    val swatchBg = KETO_THEMES[info.id]?.bg ?: c.surf
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surf2)
            .border(2.dp, if (selected) c.accent else c.bd, RoundedCornerShape(12.dp))
            .clickable { onPick(info.id) }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .background(swatchBg)
                .border(1.dp, c.bdI, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            KText(info.emoji, size = 14)
        }
        KText(
            info.label,
            size = 10,
            color = if (selected) c.accent else c.txtM,
            weight = FontWeight.SemiBold,
        )
    }
}
