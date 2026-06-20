package com.skintracker.data

/**
 * The daily logging wizard. Each meal page is "meshed": the meal (food + photos)
 * on top and the skin reading (symptoms + swelling + touch) on the lower half,
 * so food and skin state are captured together under one timestamp.
 *
 *   breakfast, lunch, dinner, summary
 *
 * Standalone flare-ups (Workflow B) are logged outside the wizard, not as a step.
 */
enum class Step(
    val id: String,
    val icon: String,
    val label: String,
    val title: String,
    val sub: String,
) {
    BREAKFAST("breakfast", "🍳", "Meal 1 of 3", "Breakfast", "What did you eat — and how's your skin?"),
    LUNCH("lunch", "🥗", "Meal 2 of 3", "Lunch", "What did you have — and how's your skin?"),
    DINNER("dinner", "🍽️", "Meal 3 of 3", "Dinner", "What did you eat — and how's your skin?"),
    SUMMARY("summary", "✅", "Done!", "Day Summary", ""),
    ;

    val isMeal: Boolean get() = this == BREAKFAST || this == LUNCH || this == DINNER

    val meal: Meal?
        get() = when (this) {
            BREAKFAST -> Meal.BREAKFAST
            LUNCH -> Meal.LUNCH
            DINNER -> Meal.DINNER
            else -> null
        }

    companion object {
        /** Steps that show a progress dot (everything except the summary). */
        val dotted: List<Step> = entries.filter { it != SUMMARY }
    }
}

// 1–5 intensity descriptors shared by the Itchiness / Redness / Bumps rows.
val SYMPTOM_LABELS = mapOf(1 to "Faint", 2 to "Mild", 3 to "Moderate", 4 to "Strong", 5 to "Severe")

// Placeholder examples for the free-text inputs.
val PLACEHOLDERS = mapOf(
    "breakfast" to "2 eggs, bacon, avocado",
    "lunch" to "Grilled chicken salad",
    "dinner" to "Steak with broccoli",
    "touch" to "Anything unusual you touched or used…",
)
