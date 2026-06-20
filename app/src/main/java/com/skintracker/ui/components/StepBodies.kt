package com.skintracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skintracker.data.DayEntry
import com.skintracker.data.Meal
import com.skintracker.data.PLACEHOLDERS
import com.skintracker.data.SYMPTOM_LABELS
import com.skintracker.model.SymptomField
import com.skintracker.ui.theme.KetoTheme

/** Styled multi-line input mirroring the CSS `textarea`. */
@Composable
fun KetoTextArea(
    value: String,
    placeholder: String,
    minLines: Int,
    onValueChange: (String) -> Unit,
) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.inp)
            .border(1.5.dp, c.bd, RoundedCornerShape(13.dp))
            .padding(14.dp)
    ) {
        if (value.isEmpty()) {
            KText(placeholder, size = 17, color = c.txtD)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (minLines * 24).dp),
            textStyle = LocalTextStyle.current.copy(
                color = c.txt, fontSize = 17.sp, lineHeight = 25.sp,
            ),
            cursorBrush = SolidColor(c.accent),
        )
    }
}

/** Full-width pill button — "⚡ Quick Select" / "🧍 Body Map" style. */
@Composable
fun QuickButton(text: String, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.surf2)
            .border(1.dp, c.bd, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 9.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        KText(text, size = 14, color = c.txtM, weight = FontWeight.SemiBold)
    }
}

/** Small uppercase section label separating the meal (top) from the skin reading (bottom). */
@Composable
private fun SectionLabel(text: String) {
    val c = KetoTheme.colors
    KText(
        text.uppercase(),
        size = 12,
        color = c.txtM,
        weight = FontWeight.SemiBold,
        letterSpacing = 0.8f,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}

// ── Meshed meal step (breakfast/lunch/dinner) ───────────────────────────────
// Top half: the meal (food text + quick-select + photos, added by the screen).
// Bottom half: the skin reading at this moment (symptoms + swelling + touch).
@Composable
fun MealBody(
    meal: Meal,
    entry: DayEntry,
    onText: (String) -> Unit,
    onQuickSelect: () -> Unit,
    onSymptom: (SymptomField, Int) -> Unit,
    onTouch: (String) -> Unit,
    onOpenBodyMap: () -> Unit,
) {
    // ── Top half — the meal ──
    SectionLabel("🍽 The Meal")
    KetoTextArea(
        value = entry.mealText(meal),
        placeholder = PLACEHOLDERS[meal.field] ?: "",
        minLines = 3,
        onValueChange = onText,
    )
    QuickButton("⚡ Quick Select", onQuickSelect)

    // ── Bottom half — the skin reading ──
    SectionLabel("🩹 Your Skin Right Now")
    val s = entry.mealSymptoms(meal)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SymptomRow("🌡 Itchiness", s.itch) { onSymptom(SymptomField.ITCH, it) }
        SymptomRow("🔴 Redness", s.redness) { onSymptom(SymptomField.REDNESS, it) }
        SymptomRow("🟤 Bumps", s.bumps) { onSymptom(SymptomField.BUMPS, it) }
        val zones = s.swelling.size
        QuickButton("🧍 Body Map — Swelling" + if (zones > 0) " · $zones area${if (zones != 1) "s" else ""}" else "", onOpenBodyMap)
        KetoTextArea(
            value = s.touch,
            placeholder = PLACEHOLDERS["touch"] ?: "",
            minLines = 2,
            onValueChange = onTouch,
        )
    }
}

/** A labelled 1–5 symptom button row, shared by the meal page and flare sheet. */
@Composable
fun SymptomRow(label: String, selected: Int?, onPick: (Int) -> Unit) {
    val labels = SYMPTOM_LABELS
    val c = KetoTheme.colors
    Column {
        KText(label.uppercase(), size = 12, color = c.txtM, weight = FontWeight.SemiBold, letterSpacing = 0.7f, modifier = Modifier.padding(bottom = 5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            (1..5).forEach { n ->
                val sel = selected == n
                // Bounce the button when it becomes selected: pop to 1.14× then settle.
                val bounceScale = remember { Animatable(1f) }
                LaunchedEffect(sel) {
                    if (sel) {
                        bounceScale.animateTo(
                            1.14f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
                        )
                        bounceScale.animateTo(
                            1f,
                            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .scale(bounceScale.value)
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) c.accent.copy(alpha = 0.12f) else c.inp)
                        .border(2.dp, if (sel) c.accent else c.bd, RoundedCornerShape(12.dp))
                        .clickable { onPick(n) }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    KText("$n", size = 17, color = if (sel) c.accent else c.txt, weight = FontWeight.ExtraBold)
                    KText(
                        (labels[n] ?: "").uppercase(),
                        size = 8,
                        color = if (sel) c.accent else c.txtM,
                        letterSpacing = 0.7f,
                    )
                }
            }
        }
    }
}
