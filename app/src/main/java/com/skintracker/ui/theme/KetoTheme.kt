package com.skintracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colour palette mirroring the CSS custom properties used by the web
 * app (--bg, --surf, --txt, --accent, etc.). Every screen reads colours from
 * here via [LocalKetoColors] rather than from MaterialTheme, so the 14 themes
 * port over 1:1 from the original `index.html`.
 *
 * Translucent surface colours are intentionally kept as white/black with alpha
 * (exactly like the web `rgba(...)` values) and composited over [bg]; Compose
 * blends them the same way the browser does.
 */
data class KetoColors(
    val bg: Color,
    val surf: Color,
    val surf2: Color,
    val inp: Color,
    val bd: Color,
    val bdI: Color,
    val txt: Color,
    val txtM: Color,
    val txtD: Color,
    val accent: Color,
    val gold: Color,
    val red: Color,
    val blue: Color,
    val isDark: Boolean,
)

// Shared brand colours — identical across every theme in the web app.
private val ACCENT = Color(0xFF5CB85C)
private val GOLD = Color(0xFFF5C842)
private val RED = Color(0xFFE05252)
private val BLUE = Color(0xFF5B9CF6)

// ── Dark base ("Midnight"). Other dark themes only override bg/surf/surf2. ──
private fun darkBase(
    bg: Color,
    surf: Color = Color(0x0EFFFFFF),   // rgba(255,255,255,.055)
    surf2: Color = Color(0x14FFFFFF),  // rgba(255,255,255,.08)
) = KetoColors(
    bg = bg,
    surf = surf,
    surf2 = surf2,
    inp = Color(0x0DFFFFFF),           // rgba(255,255,255,.05)
    bd = Color(0x12FFFFFF),            // rgba(255,255,255,.07)
    bdI = Color(0x1AFFFFFF),           // rgba(255,255,255,.1)
    txt = Color(0xFFE2E8F0),
    txtM = Color(0xFF94A3B8),
    txtD = Color(0xFF475569),
    accent = ACCENT, gold = GOLD, red = RED, blue = BLUE,
    isDark = true,
)

// ── Light base ("Pearl"). Light themes override bg only. ──
private fun lightBase(bg: Color) = KetoColors(
    bg = bg,
    surf = Color(0xD1FFFFFF),          // rgba(255,255,255,.82)
    surf2 = Color(0x0A000000),         // rgba(0,0,0,.04)
    inp = Color(0x0D000000),           // rgba(0,0,0,.05)
    bd = Color(0x17000000),            // rgba(0,0,0,.09)
    bdI = Color(0x26000000),           // rgba(0,0,0,.15)
    txt = Color(0xFF1D1D1F),
    txtM = Color(0xFF6E6E73),
    txtD = Color(0xFF86868B),
    accent = ACCENT, gold = GOLD, red = RED, blue = BLUE,
    isDark = false,
)

/** All 14 themes, keyed by the same ids used in the web app. */
val KETO_THEMES: Map<String, KetoColors> = mapOf(
    // Dark
    "midnight" to darkBase(Color(0xFF060D18)),
    "obsidian" to darkBase(Color(0xFF000000), surf = Color(0x0AFFFFFF), surf2 = Color(0x12FFFFFF)),
    "graphite" to darkBase(Color(0xFF18191A), surf = Color(0x0FFFFFFF), surf2 = Color(0x17FFFFFF)),
    "navy" to darkBase(Color(0xFF020817), surf = Color(0x0DFFFFFF), surf2 = Color(0x14FFFFFF)),
    "twilight" to darkBase(Color(0xFF0D0618), surf = Color(0x0DFFFFFF), surf2 = Color(0x14FFFFFF)),
    "aurora" to darkBase(Color(0xFF011A1A), surf = Color(0x0DFFFFFF), surf2 = Color(0x14FFFFFF)),
    "forest" to darkBase(Color(0xFF011A08), surf = Color(0x0DFFFFFF), surf2 = Color(0x14FFFFFF)),
    "ember" to darkBase(Color(0xFF150803), surf = Color(0x0DFFFFFF), surf2 = Color(0x14FFFFFF)),
    // Light
    "pearl" to lightBase(Color(0xFFF5F5F7)),
    "azure" to lightBase(Color(0xFFEFF6FF)),
    "blossom" to lightBase(Color(0xFFFFF0F5)),
    "meadow" to lightBase(Color(0xFFF0FDF4)),
    "lavender" to lightBase(Color(0xFFF5F3FF)),
    "sunset" to lightBase(Color(0xFFFFF7ED)),
)

/** Display metadata for the theme picker. */
data class ThemeInfo(val id: String, val emoji: String, val label: String, val dark: Boolean)

val THEME_LIST: List<ThemeInfo> = listOf(
    ThemeInfo("midnight", "🌙", "Midnight", true),
    ThemeInfo("obsidian", "⬛", "Obsidian", true),
    ThemeInfo("graphite", "🪨", "Graphite", true),
    ThemeInfo("navy", "🌊", "Navy", true),
    ThemeInfo("twilight", "🌆", "Twilight", true),
    ThemeInfo("aurora", "🌌", "Aurora", true),
    ThemeInfo("forest", "🌲", "Forest", true),
    ThemeInfo("ember", "🔥", "Ember", true),
    ThemeInfo("pearl", "🤍", "Pearl", false),
    ThemeInfo("azure", "💧", "Azure", false),
    ThemeInfo("blossom", "🌸", "Blossom", false),
    ThemeInfo("meadow", "🌿", "Meadow", false),
    ThemeInfo("lavender", "💜", "Lavender", false),
    ThemeInfo("sunset", "🌅", "Sunset", false),
)

val LocalKetoColors: ProvidableCompositionLocal<KetoColors> =
    compositionLocalOf { KETO_THEMES.getValue("midnight") }

/** Convenience accessor: `KetoTheme.colors.accent`. */
object KetoTheme {
    val colors: KetoColors
        @Composable get() = LocalKetoColors.current
}

@Composable
fun KetoTracker(
    themeId: String,
    content: @Composable () -> Unit,
) {
    val colors = KETO_THEMES[themeId] ?: KETO_THEMES.getValue("midnight")

    // MaterialTheme is still provided so ripples, text-selection handles and
    // any stray Material components inherit sensible colours.
    val material = if (colors.isDark) {
        darkColorScheme(
            primary = colors.accent,
            background = colors.bg,
            surface = colors.bg,
            onBackground = colors.txt,
            onSurface = colors.txt,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            background = colors.bg,
            surface = colors.bg,
            onBackground = colors.txt,
            onSurface = colors.txt,
        )
    }

    CompositionLocalProvider(LocalKetoColors provides colors) {
        MaterialTheme(
            colorScheme = material,
            typography = KetoTypography,
            content = content,
        )
    }
}

/** Resolves the auto-theme choice given the system dark-mode setting. */
@Composable
fun resolveAutoTheme(darkId: String, lightId: String): String =
    if (isSystemInDarkTheme()) darkId else lightId
