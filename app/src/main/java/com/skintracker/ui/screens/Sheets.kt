@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.skintracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skintracker.data.DateUtils
import com.skintracker.data.DaySeverity
import com.skintracker.data.Meal
import com.skintracker.data.SymptomSnapshot
import com.skintracker.data.severity
import com.skintracker.model.AppViewModel
import com.skintracker.ui.components.KText
import com.skintracker.ui.components.KetoTextArea
import com.skintracker.ui.components.PrimaryButton
import com.skintracker.ui.components.SymptomRow
import com.skintracker.ui.theme.KetoTheme

/** Full-screen modal scaffold matching the web `.fs-modal`. */
@Composable
private fun FullScreenSheet(title: String, onClose: () -> Unit, body: @Composable () -> Unit) {
    val c = KetoTheme.colors
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, c.bd, RoundedCornerShape(0.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                KText(title, size = 16, color = c.gold, weight = FontWeight.Bold)
                Box(Modifier.clickable { onClose() }.padding(4.dp)) {
                    KText("✕", size = 18, color = c.txtM)
                }
            }
            Box(Modifier.fillMaxSize()) { body() }
        }
    }
}

// ── Overview: list of all logged days ───────────────────────────────────────
@Composable
fun OverviewSheet(vm: AppViewModel, onJump: (String) -> Unit, onClose: () -> Unit) {
    val c = KetoTheme.colors
    FullScreenSheet("📋 All Days", onClose) {
        val keys = vm.loggedKeys()
        if (keys.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                KText("No days logged yet", size = 15, color = c.txtM)
            }
            return@FullScreenSheet
        }

        // Stagger each card in from below on open. visibleKeys is additive —
        // keys are never removed so already-visible items stay stable after a delete.
        val visibleKeys = remember { mutableStateListOf<String>() }
        LaunchedEffect(Unit) {
            keys.forEachIndexed { i, key ->
                delay(i * 32L)
                if (key !in visibleKeys) visibleKeys.add(key)
            }
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(keys, key = { it }) { key ->
                val e = vm.entryFor(key)

                AnimatedVisibility(
                    visible = key in visibleKeys,
                    enter = slideInVertically(tween(220)) { it / 2 } + fadeIn(tween(220)),
                ) {
                    // Swipe left to delete. The background turns red as the swipe progresses.
                    val swipeState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { v ->
                            if (v == SwipeToDismissBoxValue.EndToStart) { vm.deleteDay(key); true }
                            else false
                        },
                    )
                    SwipeToDismissBox(
                        state = swipeState,
                        enableDismissFromEndToStart = true,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val swipeBg by animateColorAsState(
                                targetValue = if (swipeState.targetValue == SwipeToDismissBoxValue.EndToStart)
                                    c.red else c.surf2,
                                animationSpec = tween(180),
                                label = "swipe_bg",
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(swipeBg)
                                    .padding(end = 20.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                KText("🗑️", size = 22)
                            }
                        },
                    ) {
                        val sev = e.severity()
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (sev == DaySeverity.SEVERE) c.red.copy(alpha = 0.08f) else c.surf)
                                .border(1.dp, c.bd, RoundedCornerShape(16.dp))
                                .clickable { onJump(key) }
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            KText(DateUtils.fmtDate(key), size = 15, color = c.gold, weight = FontWeight.Bold)
                            listOf("🍳" to e.breakfast, "🥗" to e.lunch, "🍽️" to e.dinner).forEach { (ic, txt) ->
                                if (txt.isNotEmpty()) KText("$ic $txt", size = 13, color = c.txtM, maxLines = 1)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                when (sev) {
                                    DaySeverity.SEVERE -> Stat("🔴", "Severe")
                                    DaySeverity.MILD -> Stat("🟡", "Mild")
                                    DaySeverity.CLEAR -> Stat("🟢", "Clear")
                                    DaySeverity.NONE -> {}
                                }
                                if (e.flares.isNotEmpty()) Stat("⚡", "${e.flares.size} flare${if (e.flares.size != 1) "s" else ""}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(icon: String, value: String) {
    val c = KetoTheme.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(c.inp)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        KText(icon, size = 12)
        KText(value, size = 12, color = c.txt, weight = FontWeight.Bold)
    }
}

// ── Body map: swelling severity per body zone ───────────────────────────────
//
// PLACEHOLDER (filler). The real interaction — a front/back body silhouette
// where each tap on a zone cycles swelling severity 0→1→2→3→0 (redder each
// tap) — is intentionally deferred. The underlying data already exists
// (SymptomSnapshot.swelling: Map<zoneId, severity>) and AppViewModel exposes
// `setMealSwelling(meal, zone, severity)`, so this sheet can be fleshed out
// without touching the model. See android-skin/CLAUDE.md "body map".
@Composable
fun BodyMapSheet(meal: Meal, onClose: () -> Unit) {
    val c = KetoTheme.colors
    FullScreenSheet("🧍 Body Map — Swelling", onClose) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KText("🚧", size = 44)
            KText("Body map coming soon", size = 18, color = c.txt, weight = FontWeight.Bold)
            KText(
                "A front/back body outline where you tap each area to mark swelling " +
                    "severity will live here. For now, use the meal's notes box to jot " +
                    "down where you're swollen.",
                size = 14,
                color = c.txtM,
            )
        }
    }
}

// ── Flare-up: log a standalone skin event, independent of any meal (Workflow B) ──
//
// The in-app entry point for sudden flare-ups. Symptoms are held locally and
// committed once via `onLog` (→ AppViewModel.addFlare), which timestamps the
// flare to now and appends it to today's entry. The home-screen widget
// (deferred) will eventually call into the same addFlare path.
@Composable
fun FlareSheet(onLog: (SymptomSnapshot) -> Unit, onClose: () -> Unit) {
    val c = KetoTheme.colors
    var itch by remember { mutableStateOf<Int?>(null) }
    var redness by remember { mutableStateOf<Int?>(null) }
    var bumps by remember { mutableStateOf<Int?>(null) }
    var touch by remember { mutableStateOf("") }

    FullScreenSheet("⚡ Sudden Flare-Up", onClose) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            KText(
                "Log how your skin is right now — independent of any meal. It's timestamped to this moment.",
                size = 13, color = c.txtM,
            )
            SymptomRow("🌡 Itchiness", itch) { itch = it }
            SymptomRow("🔴 Redness", redness) { redness = it }
            SymptomRow("🟤 Bumps", bumps) { bumps = it }
            KText("🧍 Body map (swelling) coming soon — note where below for now.", size = 12, color = c.txtD)
            KetoTextArea(
                value = touch,
                placeholder = "Anything you just touched or used…",
                minLines = 2,
            ) { touch = it }

            val empty = itch == null && redness == null && bumps == null && touch.isBlank()
            PrimaryButton(text = "Log Flare-Up ✓", modifier = Modifier.fillMaxWidth()) {
                if (empty) {
                    onClose() // nothing entered — dismiss without adding an empty flare
                } else {
                    onLog(SymptomSnapshot(itch = itch, redness = redness, bumps = bumps, touch = touch.trim()))
                    onClose()
                }
            }
        }
    }
}

// ── Quick-select: tap food chips to append to the meal text ──────────────────
//
// Selection state is tracked in its own session-scoped Set — *not* derived by
// re-parsing the meal text on every recomposition (mirrors the web app's
// openQuickSel/_qsSelected, index.html ~L1485). Deriving it from the text via
// string-splitting is fragile: free-typed text containing commas parses into
// bogus "items", a typed "2 eggs" never matches the "Eggs" chip (so tapping it
// appends a redundant duplicate), and editing the text outside this sheet
// silently desyncs the chips' highlighted state from what's actually there.
@Composable
fun QuickSelectSheet(vm: AppViewModel, meal: Meal, onClose: () -> Unit) {
    val c = KetoTheme.colors
    val title = "⚡ " + meal.field.replaceFirstChar { it.uppercase() }
    val baseText = remember { vm.entry.mealText(meal) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun toggle(food: String) {
        selected = if (food in selected) selected - food else selected + food
        val additions = selected.joinToString(", ")
        vm.setMealText(
            meal,
            when {
                additions.isEmpty() -> baseText
                baseText.isEmpty() -> additions
                else -> "$baseText, $additions"
            },
        )
    }

    FullScreenSheet(title, onClose) {
        Column(
            Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KText("Tap items to add them to this meal.", size = 13, color = c.txtM)
            FlowChips {
                vm.quickSelectItems.forEach { food ->
                    FoodChip(food, selected = food in selected) { toggle(food) }
                }
            }
        }
    }
}

@Composable
private fun FoodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) c.accent.copy(alpha = 0.15f) else c.surf)
            .border(1.5.dp, if (selected) c.accent else c.bdI, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        KText(label, size = 15, color = if (selected) c.accent else c.txt, weight = FontWeight.SemiBold)
    }
}

/** Simple wrapping row of chips using FlowRow. */
@Composable
private fun FlowChips(content: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}
