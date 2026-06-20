package com.skintracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skintracker.ui.theme.KetoTheme

/**
 * The bottom header bar. In the web app `.hdr` has `order:3`, so it renders
 * below the main content — replicated here by placing it last in the Column.
 */
@Composable
fun HeaderBar(
    dateText: String,
    nextEnabled: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDateClick: () -> Unit,
    onOverview: () -> Unit,
    onTheme: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = KetoTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.bg)
            .border(width = 1.dp, color = c.bd, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        KText("Skin Tracker", size = 15, color = c.gold, weight = FontWeight.Bold)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton("‹", onClick = onPrev)
            // Date text slides out upward / in from below when the day changes,
            // giving a clear sense of calendar direction.
            AnimatedContent(
                targetState = dateText,
                transitionSpec = {
                    (fadeIn(tween(130)) + slideInVertically(tween(130)) { it / 3 }) togetherWith
                    (fadeOut(tween(100)) + slideOutVertically(tween(100)) { -it / 3 })
                },
                label = "date_chip",
            ) { text ->
                DateChip(text, onDateClick)
            }
            IconButton("›", enabled = nextEnabled, onClick = onNext)
            IconButton("📋", onClick = onOverview)
            IconButton("🎨", onClick = onTheme)
            IconButton("⚙️", onClick = onSettings)
        }
    }
}

@Composable
private fun DateChip(text: String, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .widthIn(min = 68.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(c.surf2)
            .border(1.dp, c.bd, RoundedCornerShape(9.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        KText(text, size = 13, color = c.txt)
    }
}

@Composable
private fun IconButton(symbol: String, enabled: Boolean = true, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .size(34.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(RoundedCornerShape(9.dp))
            .background(c.surf2)
            .border(1.dp, c.bd, RoundedCornerShape(9.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        KText(symbol, size = 16, color = c.txt)
    }
}
