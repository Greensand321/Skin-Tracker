package com.skintracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skintracker.data.Step
import com.skintracker.ui.theme.KetoTheme

/**
 * The `.card` surface: rounded, bordered, translucent.
 *
 * [compact] mirrors the web app's `.card-meal` variant:
 *   - padding: 14px vertical / 18px horizontal  (vs 24/20 for normal)
 *   - gap:     9dp between children              (vs 18dp for normal)
 */
@Composable
fun KetoCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    content: @Composable () -> Unit,
) {
    val c = KetoTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surf)
            .border(1.dp, c.bd, RoundedCornerShape(20.dp))
            .padding(
                horizontal = if (compact) 18.dp else 20.dp,
                vertical   = if (compact) 14.dp else 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 18.dp),
    ) { content() }
}

/** Step label / title / subtitle header used at the top of most step cards. */
@Composable
fun StepHeading(step: Step, showLabelAndSub: Boolean = true) {
    val c = KetoTheme.colors
    Column {
        if (showLabelAndSub) {
            KText(
                step.label.uppercase(),
                size = 11,
                weight = FontWeight.Normal,
                color = c.txtM,
                letterSpacing = 2f,
            )
        }
        KText(
            "${step.icon} ${step.title}",
            size = 30,
            weight = FontWeight.ExtraBold,
            color = c.gold,
        )
        if (showLabelAndSub && step.sub.isNotEmpty()) {
            KText(step.sub, size = 14, color = c.txtM, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Progress dots — filled (done), elongated (active), faint (future). */
@Composable
fun Dots(currentIndex: Int) {
    val c = KetoTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
    ) {
        Step.dotted.forEach { s ->
            val idx = s.ordinal
            val (width, color) = when {
                idx < currentIndex -> 7.dp to c.gold
                idx == currentIndex -> 22.dp to c.accent
                else -> 7.dp to c.bdI
            }
            Dot(width = width, color = color)
        }
    }
}

/**
 * A single progress dot — eases its width and color rather than snapping, so
 * stepping through the wizard reads as the active dot growing/sliding into
 * place instead of dots silently swapping states. A constant 4dp corner
 * radius covers both shapes: on the 7×7dp resting size it's clamped to a
 * circle, and on the 22×7dp active pill it reads as a rounded bar — so no
 * shape animation is needed alongside the size/color ones.
 */
@Composable
private fun Dot(width: Dp, color: Color) {
    val animatedWidth by animateDpAsState(width, animationSpec = tween(220), label = "dotWidth")
    val animatedColor by animateColorAsState(color, animationSpec = tween(220), label = "dotColor")
    Box(
        Modifier
            .size(width = animatedWidth, height = 7.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(animatedColor)
    )
}

/** Thin wrapper around Text with the sizing conventions the app uses. */
@Composable
fun KText(
    text: String,
    size: Int,
    color: Color = KetoTheme.colors.txt,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Float = 0f,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        letterSpacing = letterSpacing.sp,
        lineHeight = (size * 1.25).sp,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = overflow,
    )
}

/** Outlined chip-style mini button (e.g. "Quick Select", "Supplements"). */
@Composable
fun fullWidthOutlineShape() = RoundedCornerShape(10.dp)

/** Reusable bordered container with the standard interactive border. */
fun Modifier.ketoBorder(color: Color, width: Float = 1f, radius: Int = 13) =
    this.border(BorderStroke(width.dp, color), RoundedCornerShape(radius.dp))
