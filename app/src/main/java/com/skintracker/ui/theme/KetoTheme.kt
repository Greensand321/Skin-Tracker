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

// Shared brand colours.
private val ACCENT = Color(0xFFC0714A)  // terracotta — primary interactive
private val GOLD   = Color(0xFFD4943A)  // warm amber — highlights / mild severity
private val RED    = Color(0xFFD94F4F)  // alert — severe severity / errors
private val BLUE   = Color(0xFF5B9CF6)  // clinical info

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

/** All 14 themes — skin-tone and earthy palettes. */
val KETO_THEMES: Map<String, KetoColors> = mapOf(
    // ── Dark ──────────────────────────────────────────────────────────────────
    // Dusk: very dark warm brown — like dusk light on skin
    "dusk"      to darkBase(Color(0xFF1A1008), surf = Color(0x0FFFFFFF), surf2 = Color(0x17FFFFFF)),
    // Charcoal: warm grey-black — clinical dark
    "charcoal"  to darkBase(Color(0xFF141412), surf = Color(0x0AFFFFFF), surf2 = Color(0x12FFFFFF)),
    // Mahogany: deep reddish-brown — rich dark skin tone
    "mahogany"  to darkBase(Color(0xFF1A0A06), surf = Color(0x0EFFFFFF), surf2 = Color(0x15FFFFFF)),
    // Midnight: warm near-black anchor
    "midnight"  to darkBase(Color(0xFF0D0B09)),
    // Cocoa: dark chocolate — deep brown warmth
    "cocoa"     to darkBase(Color(0xFF120C08), surf = Color(0x0EFFFFFF), surf2 = Color(0x16FFFFFF)),
    // Smoke: cool grey-brown — stone / ash
    "smoke"     to darkBase(Color(0xFF121315), surf = Color(0x0CFFFFFF), surf2 = Color(0x14FFFFFF)),
    // Walnut: deep warm umber — aged wood tone
    "walnut"    to darkBase(Color(0xFF140E09), surf = Color(0x0FFFFFFF), surf2 = Color(0x17FFFFFF)),
    // ── Light ─────────────────────────────────────────────────────────────────
    // Ivory: warm off-white — pale skin, clinical neutral
    "ivory"     to lightBase(Color(0xFFFBF8F4)),
    // Sand: peachy warm sand — sun-kissed
    "sand"      to lightBase(Color(0xFFFFF4E6)),
    // Clay: light terracotta blush — earthy warm red undertone
    "clay"      to lightBase(Color(0xFFFFF0E8)),
    // Linen: soft warm taupe — natural, calm
    "linen"     to lightBase(Color(0xFFF5EFE6)),
    // Peach: soft peach blush — gentle warmth
    "peach"     to lightBase(Color(0xFFFEF0EA)),
    // Blush: very light rose — rosacea / pink undertone
    "blush"     to lightBase(Color(0xFFFDF0F0)),
    // Cream: rich warm cream — moisturiser / skincare
    "cream"     to lightBase(Color(0xFFFDFAF5)),
)

/** Display metadata for the theme picker. */
data class ThemeInfo(val id: String, val emoji: String, val label: String, val dark: Boolean)

val THEME_LIST: List<ThemeInfo> = listOf(
    // Dark
    ThemeInfo("dusk",      "🌄", "Dusk",      true),
    ThemeInfo("charcoal",  "🩶", "Charcoal",  true),
    ThemeInfo("mahogany",  "🟤", "Mahogany",  true),
    ThemeInfo("midnight",  "🌑", "Midnight",  true),
    ThemeInfo("cocoa",     "☕", "Cocoa",     true),
    ThemeInfo("smoke",     "💨", "Smoke",     true),
    ThemeInfo("walnut",    "🪵", "Walnut",    true),
    // Light
    ThemeInfo("ivory",     "🤍", "Ivory",     false),
    ThemeInfo("sand",      "🏖️", "Sand",      false),
    ThemeInfo("clay",      "🏺", "Clay",      false),
    ThemeInfo("linen",     "🧵", "Linen",     false),
    ThemeInfo("peach",     "🍑", "Peach",     false),
    ThemeInfo("blush",     "🌸", "Blush",     false),
    ThemeInfo("cream",     "🧴", "Cream",     false),
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
