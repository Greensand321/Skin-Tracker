package com.skintracker.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skintracker.data.CalendarDay
import com.skintracker.data.DateUtils
import com.skintracker.data.DayEntry
import com.skintracker.data.DaySeverity
import com.skintracker.data.severity
import com.skintracker.ui.theme.KetoTheme
import java.time.LocalDate

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

private enum class CalTier { SEVERE, MILD, CLEAR, NONE }

/**
 * Bottom-anchored month-grid calendar. Tapping a day in the displayed month
 * jumps straight to it; tapping an adjacent-month day navigates the grid there
 * instead of selecting it. Future months/days are dimmed and inert.
 *
 * Day colour reflects that day's symptom severity (worst moment of the day):
 * red = severe flare-up, gold = mild symptoms, green = logged but clear, no
 * colour = nothing logged. See [DayEntry.severity].
 */
@Composable
fun CalendarPanel(
    viewedKey: String,
    entries: Map<String, DayEntry>,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    val c = KetoTheme.colors
    val today = remember { DateUtils.todayKey() }
    val todayDate = remember { LocalDate.parse(today) }
    val viewedDate = remember(viewedKey) { LocalDate.parse(viewedKey) }

    var year by remember { mutableStateOf(viewedDate.year) }
    var month by remember { mutableStateOf(viewedDate.monthValue) }
    var showPicker by remember { mutableStateOf(false) }

    fun go(y: Int, m: Int) {
        when {
            m < 1 -> { year = y - 1; month = 12 }
            m > 12 -> { year = y + 1; month = 1 }
            else -> { year = y; month = m }
        }
    }

    val canGoNext = year < todayDate.year || (year == todayDate.year && month < todayDate.monthValue)

    Box(Modifier.fillMaxSize()) {

        // ── Scrim ─────────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable { onClose() }
        )

        // ── Panel (bottom-aligned, eats all its own touch events) ────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(c.bg)
                .border(1.dp, c.bdI, RoundedCornerShape(20.dp))
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showPicker) {
                val yearsWithData = remember(entries) {
                    entries.keys.mapNotNull { it.take(4).toIntOrNull() }.toSet()
                }
                CalMonthYearPicker(
                    initialMonth = month,
                    initialYear = year,
                    yearsWithData = yearsWithData,
                    onApply = { y, m -> year = y; month = m; showPicker = false },
                    onClose = { showPicker = false },
                )
            } else {
                // Header: ‹ Month Year ▾ ›
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CalNavButton("‹") { go(year, month - 1) }
                    Row(
                        modifier = Modifier.clickable { showPicker = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KText("${MONTH_NAMES[month - 1]} $year", size = 16, color = c.gold, weight = FontWeight.Bold)
                        KText(" ▾", size = 11, color = c.gold.copy(alpha = 0.55f))
                    }
                    CalNavButton("›", enabled = canGoNext) { go(year, month + 1) }
                }

                // Day-of-week row (Sunday-first, matches the web grid)
                Row(Modifier.fillMaxWidth()) {
                    listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { dow ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            KText(dow, size = 10, color = c.txtD, weight = FontWeight.Bold)
                        }
                    }
                }

                // Month grid — 42 cells (6 weeks × 7 days), never scrolls on its own
                val days = remember(year, month) { DateUtils.monthGrid(year, month) }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(258.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    userScrollEnabled = false,
                ) {
                    items(days) { day ->
                        CalendarCell(
                            day = day,
                            isToday = day.key == today,
                            isViewing = day.key == viewedKey,
                            isFuture = day.key > today,
                            entry = entries[day.key],
                            onClick = {
                                if (day.inCurrentMonth) {
                                    onSelect(day.key)
                                } else {
                                    val d = LocalDate.parse(day.key)
                                    go(d.year, d.monthValue)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalNavButton(symbol: String, enabled: Boolean = true, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .size(32.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(RoundedCornerShape(9.dp))
            .background(c.surf2)
            .border(1.dp, c.bd, RoundedCornerShape(9.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        KText(symbol, size = 16, color = c.txt)
    }
}

@Composable
private fun CalendarCell(
    day: CalendarDay,
    isToday: Boolean,
    isViewing: Boolean,
    isFuture: Boolean,
    entry: DayEntry?,
    onClick: () -> Unit,
) {
    val c = KetoTheme.colors

    val tier = when (entry?.severity()) {
        DaySeverity.SEVERE -> CalTier.SEVERE
        DaySeverity.MILD -> CalTier.MILD
        DaySeverity.CLEAR -> CalTier.CLEAR
        else -> CalTier.NONE        // null entry or DaySeverity.NONE
    }

    val (bg, fg) = when (tier) {
        CalTier.SEVERE -> c.red to Color.White
        CalTier.MILD -> c.gold to Color.Black
        CalTier.CLEAR -> c.accent to Color.White
        CalTier.NONE -> Color.Transparent to if (isToday) c.gold else c.txtM
    }

    // CSS cascade in the web app applies `.is-viewing`'s white ring over
    // `.is-today`'s gold ring when a cell is both — replicated by priority here.
    val ring = when {
        isViewing -> Color.White
        isToday -> c.gold
        else -> null
    }

    val faded = isFuture || !day.inCurrentMonth

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val cellScale by animateFloatAsState(
        targetValue = if (pressed) 0.78f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "cell_scale",
    )

    Box(
        Modifier
            .aspectRatio(1f)
            .scale(cellScale)
            .alpha(if (!faded) 1f else if (isFuture) 0.2f else 0.28f)
            .clip(CircleShape)
            .background(bg)
            .let { m -> if (ring != null) m.border(2.dp, ring, CircleShape) else m }
            .clickable(interactionSource = interactionSource, indication = null, enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        KText(
            "${day.dayOfMonth}",
            size = 13,
            color = fg,
            weight = if (tier != CalTier.NONE || isToday) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ── MONTH/YEAR PICKER ───────────────────────────────────────────────────────
private val CPKR_ITEM_HEIGHT = 40.dp
private const val CPKR_VISIBLE_ITEMS = 5

/**
 * Native counterpart of the web app's `#calPicker` (CLAUDE.md "Calendar Panel").
 * Replaces the header/grid in-place when the month/year label is tapped. Two
 * scroll-snapped wheels pick the month and year; "Go →" jumps the calendar
 * grid there and closes, "Go to Today" jumps straight to the current
 * month/year and closes immediately (mirrors web's `pickerGoToday()`).
 */
@Composable
private fun CalMonthYearPicker(
    initialMonth: Int, // 1-12
    initialYear: Int,
    yearsWithData: Set<Int>,
    onApply: (year: Int, month: Int) -> Unit,
    onClose: () -> Unit,
) {
    val c = KetoTheme.colors
    val now = remember { LocalDate.now() }

    // Mirrors openCalPicker(): minY = earliest year with data (else now-5), maxY = now+5.
    val years = remember(yearsWithData) {
        val minY = yearsWithData.minOrNull() ?: (now.year - 5)
        val maxY = now.year + 5
        (minY..maxY).toList()
    }

    val monthListState = rememberLazyListState()
    val yearListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        monthListState.scrollToItem(initialMonth - 1)
        yearListState.scrollToItem(years.indexOf(initialYear).coerceAtLeast(0))
    }

    val centerMonth by rememberCenteredIndex(monthListState)
    val centerYearIdx by rememberCenteredIndex(yearListState)
    val centerYear = years.getOrElse(centerYearIdx) { initialYear }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header: ✕  Month Year  Go →
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onClose() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                KText("✕", size = 17, color = c.txtM)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                KText("${MONTH_NAMES[centerMonth]} $centerYear", size = 16, color = c.gold, weight = FontWeight.Bold)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.accent)
                    .clickable { onApply(centerYear, centerMonth + 1) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                KText("Go →", size = 13, color = Color.White, weight = FontWeight.Bold)
            }
        }

        // Wheels: Month | Year
        Row(Modifier.fillMaxWidth()) {
            WheelColumn(
                label = "MONTH",
                items = MONTH_NAMES,
                listState = monthListState,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .align(Alignment.Bottom)
                    .padding(horizontal = 4.dp)
                    .width(1.dp)
                    .height(CPKR_ITEM_HEIGHT * CPKR_VISIBLE_ITEMS)
                    .background(c.bd),
            )
            WheelColumn(
                label = "YEAR",
                items = years.map { it.toString() },
                listState = yearListState,
                dimmed = { idx -> years[idx] !in yearsWithData },
                modifier = Modifier.weight(1f),
            )
        }

        // Footer: Go to Today
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, c.bd, RoundedCornerShape(8.dp))
                    .clickable { onApply(now.year, now.monthValue) }
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                KText("Go to Today", size = 12, color = c.txtM)
            }
        }
    }
}

/**
 * One scroll-snapped wheel of [items], centred via [SnapPosition.Center] with
 * [CPKR_ITEM_HEIGHT]-tall rows and top/bottom content padding equal to two
 * rows — so [CPKR_VISIBLE_ITEMS] (5) rows are visible and the middle one sits
 * under the highlight band, mirroring the web picker's CPKR_IH/CPKR_PAD.
 */
@Composable
private fun WheelColumn(
    label: String,
    items: List<String>,
    listState: LazyListState,
    dimmed: (Int) -> Boolean = { false },
    modifier: Modifier = Modifier,
) {
    val c = KetoTheme.colors
    val flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Center)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        KText(label, size = 10, color = c.txtD, weight = FontWeight.Bold, letterSpacing = 1f)
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(CPKR_ITEM_HEIGHT * CPKR_VISIBLE_ITEMS),
        ) {
            // Center highlight band
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(CPKR_ITEM_HEIGHT)
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.surf2),
            )
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = CPKR_ITEM_HEIGHT * (CPKR_VISIBLE_ITEMS / 2)),
            ) {
                itemsIndexed(items) { idx, text ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(CPKR_ITEM_HEIGHT),
                        contentAlignment = Alignment.Center,
                    ) {
                        KText(
                            text,
                            size = 16,
                            color = if (dimmed(idx)) c.txtD else c.txt,
                            modifier = if (dimmed(idx)) Modifier.alpha(0.38f) else Modifier,
                        )
                    }
                }
            }
            // Top/bottom fade — purely decorative, doesn't intercept touches
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(c.bg, Color.Transparent, Color.Transparent, c.bg))),
            )
        }
    }
}

/**
 * Index of the wheel item nearest the viewport's vertical center — the
 * "selected" value, mirroring the web picker's onCpkrScroll() snap-detection.
 */
@Composable
private fun rememberCenteredIndex(listState: LazyListState): State<Int> = remember {
    derivedStateOf {
        val info = listState.layoutInfo
        val visible = info.visibleItemsInfo
        if (visible.isEmpty()) return@derivedStateOf 0
        val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
        visible.minBy { kotlin.math.abs((it.offset + it.size / 2) - center) }.index
    }
}
