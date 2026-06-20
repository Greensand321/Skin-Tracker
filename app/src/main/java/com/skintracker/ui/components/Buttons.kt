package com.skintracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skintracker.ui.theme.KetoTheme

/** Solid green primary action ("Next →" / "Finish ✓"). */
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = KetoTheme.colors
    PillButton(
        text = text,
        modifier = modifier,
        bg = c.accent,
        textColor = Color.White,
        weight = FontWeight.Bold,
        onClick = onClick,
    )
}

/** Subtle back ("‹") button. */
@Composable
fun BackButton(onClick: () -> Unit) {
    val c = KetoTheme.colors
    PillButton(
        text = "‹",
        bg = c.surf2,
        textColor = c.txtM,
        border = c.bd,
        weight = FontWeight.Bold,
        padding = PaddingValues(horizontal = 18.dp, vertical = 17.dp),
        onClick = onClick,
    )
}

/** Transparent "Skip" button. */
@Composable
fun SkipButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = KetoTheme.colors
    PillButton(
        text = "Skip",
        modifier = modifier,
        bg = Color.Transparent,
        textColor = c.txtM,
        border = c.bd,
        weight = FontWeight.Normal,
        onClick = onClick,
    )
}

/** Full-width call-to-action for logging a standalone flare-up (Workflow B). */
@Composable
fun FlareButton(onClick: () -> Unit) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.red.copy(alpha = 0.10f))
            .border(1.5.dp, c.red.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 11.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        KText("⚡ Log a sudden flare-up", size = 14, color = c.red, weight = FontWeight.SemiBold)
    }
}

@Composable
private fun PillButton(
    text: String,
    bg: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    border: Color? = null,
    weight: FontWeight = FontWeight.Bold,
    padding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 17.dp),
    onClick: () -> Unit,
) {
    var m = modifier
        .clip(RoundedCornerShape(13.dp))
        .background(bg)
    if (border != null) m = m.border(1.dp, border, RoundedCornerShape(13.dp))
    Box(
        modifier = m.clickable { onClick() }.padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        // Single line, never wraps — keeps every action-row button the same
        // height regardless of label length (e.g. "Next →" vs "🥑 Keto"),
        // letting it overflow the padding slightly rather than wrapping to a
        // taller two-line layout.
        KText(
            text,
            size = 17,
            color = textColor,
            weight = weight,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }
}
