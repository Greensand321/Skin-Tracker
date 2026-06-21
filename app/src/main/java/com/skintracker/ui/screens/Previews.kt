package com.skintracker.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.skintracker.data.Meal
import com.skintracker.data.Step
import com.skintracker.data.SymptomSnapshot
import com.skintracker.model.AppViewModel
import com.skintracker.model.SymptomField
import com.skintracker.ui.theme.KetoTracker

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Build a preview VM pinned to a specific wizard step. */
private fun vmAt(step: Step, themeId: String = "midnight"): AppViewModel =
    AppViewModel.preview().apply {
        setTheme(themeId)
        editAt(step.ordinal)
    }

// ── Wizard steps (Midnight dark theme) ────────────────────────────────────────

@Preview(name = "Step 1 — Breakfast", showBackground = true, heightDp = 900, widthDp = 390)
@Composable
private fun PreviewBreakfast() {
    KetoTracker("midnight") { WizardScreen(vmAt(Step.BREAKFAST)) }
}

@Preview(name = "Step 2 — Lunch", showBackground = true, heightDp = 900, widthDp = 390)
@Composable
private fun PreviewLunch() {
    KetoTracker("midnight") { WizardScreen(vmAt(Step.LUNCH)) }
}

@Preview(name = "Step 3 — Dinner", showBackground = true, heightDp = 900, widthDp = 390)
@Composable
private fun PreviewDinner() {
    KetoTracker("midnight") { WizardScreen(vmAt(Step.DINNER)) }
}

@Preview(name = "Step 4 — Summary (today)", showBackground = true, heightDp = 900, widthDp = 390)
@Composable
private fun PreviewSummary() {
    KetoTracker("midnight") { WizardScreen(vmAt(Step.SUMMARY)) }
}

// ── Summary with data filled in ───────────────────────────────────────────────

@Preview(name = "Summary — with data", showBackground = true, heightDp = 1000, widthDp = 390)
@Composable
private fun PreviewSummaryFilled() {
    val vm = AppViewModel.preview().apply {
        setTheme("midnight")
        setMealText(Meal.BREAKFAST, "3 eggs, bacon, avocado")
        setMealSymptom(Meal.BREAKFAST, SymptomField.ITCH, 1)
        setMealText(Meal.LUNCH, "Chicken Caesar salad")
        setMealSymptom(Meal.LUNCH, SymptomField.REDNESS, 3)
        setMealTouch(Meal.LUNCH, "New hand soap at work")
        setMealText(Meal.DINNER, "Ribeye + buttered asparagus")
        setMealSymptom(Meal.DINNER, SymptomField.BUMPS, 4)
        addFlare(SymptomSnapshot(itch = 5, redness = 4, touch = "Handled cardboard boxes"))
        editAt(Step.SUMMARY.ordinal)
    }
    KetoTracker("midnight") { WizardScreen(vm) }
}

// ── Overlays ──────────────────────────────────────────────────────────────────

@Preview(name = "Overlay — Theme Picker", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewThemePicker() {
    KetoTracker("midnight") {
        com.skintracker.ui.components.ThemePanel(
            currentId = "midnight",
            autoEnabled = false,
            darkAutoId = "midnight",
            lightAutoId = "ivory",
            onPick = {},
            onPickAuto = { _, _ -> },
            onToggleAuto = {},
            onClose = {},
        )
    }
}

@Preview(name = "Overlay — Overview", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewOverview() {
    KetoTracker("midnight") {
        OverviewSheet(vm = AppViewModel.preview(), onJump = {}, onClose = {})
    }
}

@Preview(name = "Overlay — Calendar", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewCalendar() {
    val vm = AppViewModel.preview()
    KetoTracker("midnight") {
        com.skintracker.ui.components.CalendarPanel(
            viewedKey = vm.viewedKey,
            entries = vm.allEntries,
            onSelect = {},
            onClose = {},
        )
    }
}

@Preview(name = "Overlay — Body Map", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewBodyMap() {
    KetoTracker("midnight") {
        BodyMapSheet(meal = Meal.BREAKFAST, swelling = emptyMap(), onSwellingChange = { _, _ -> }, onClose = {})
    }
}

@Preview(name = "Overlay — Quick Select", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewQuickSelect() {
    KetoTracker("midnight") {
        QuickSelectSheet(
            vm = AppViewModel.preview(),
            meal = Meal.BREAKFAST,
            onClose = {},
        )
    }
}

@Preview(name = "Overlay — Settings", showBackground = true, heightDp = 780, widthDp = 390)
@Composable
private fun PreviewSettings() {
    KetoTracker("midnight") {
        SettingsSheet(vm = AppViewModel.preview(), onTheme = {}, onClose = {})
    }
}

// ── Themes ────────────────────────────────────────────────────────────────────

@Preview(name = "Theme — Ivory (light)", showBackground = true, heightDp = 900, widthDp = 390)
@Composable
private fun PreviewIvory() {
    KetoTracker("ivory") { WizardScreen(vmAt(Step.BREAKFAST, "ivory")) }
}

@Preview(name = "Theme — Blush (light)", showBackground = true, heightDp = 900, widthDp = 390)
@Composable
private fun PreviewBlush() {
    KetoTracker("blush") { WizardScreen(vmAt(Step.LUNCH, "blush")) }
}

@Preview(name = "Theme — Walnut (dark)", showBackground = true, heightDp = 900, widthDp = 390)
@Composable
private fun PreviewWalnut() {
    KetoTracker("walnut") { WizardScreen(vmAt(Step.DINNER, "walnut")) }
}

@Preview(name = "Theme — Mahogany (dark)", showBackground = true, heightDp = 900, widthDp = 390)
@Composable
private fun PreviewMahogany() {
    KetoTracker("mahogany") { WizardScreen(vmAt(Step.SUMMARY, "mahogany")) }
}
