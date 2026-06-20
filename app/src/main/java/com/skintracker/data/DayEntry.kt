package com.skintracker.data

/**
 * A point-in-time skin reading. Attached to each meal (the "lower half" of the
 * meshed meal page) and to every standalone [Flare]. All fields are optional so
 * a partially-filled reading is still a valid one.
 *
 * Serialization lives in [SymptomSnapshotSurrogate] — see [DayEntrySurrogate]
 * for why the domain classes carry no kotlinx.serialization dependency.
 */
data class SymptomSnapshot(
    val itch: Int? = null,                  // 1–5 or null
    val redness: Int? = null,               // 1–5 or null
    val bumps: Int? = null,                 // 1–5 or null
    val swelling: Map<String, Int> = emptyMap(),  // body-map zoneId → severity 1–3
    val touch: String = "",                 // anything unusual that could be a culprit
) {
    /** True when nothing at all has been recorded in this reading. */
    val isEmpty: Boolean
        get() = itch == null && redness == null && bumps == null &&
            swelling.isEmpty() && touch.isBlank()

    /** Highest 1–5 symptom intensity recorded here (0 when none). */
    val worstSymptom: Int get() = maxOf(itch ?: 0, redness ?: 0, bumps ?: 0)

    /** Highest 1–3 swelling severity recorded here (0 when none). */
    val worstSwelling: Int get() = swelling.values.maxOrNull() ?: 0
}

/**
 * A standalone flare-up logged between meals (Workflow B). Carries its own
 * timestamp and symptom reading, independent of the three meal slots.
 */
data class Flare(
    val time: String,                       // "HH:mm", recorded when the flare is logged
    val symptoms: SymptomSnapshot = SymptomSnapshot(),
)

/**
 * One day's log. The day is a container: three meal slots (food + a per-meal
 * [SymptomSnapshot]) plus any number of standalone [flares]. Adding a field
 * only requires a default value here and in [DayEntrySurrogate] — the JSON
 * column means no Room migration (see CLAUDE.md "Persistence").
 */
data class DayEntry(
    val date: String,                       // "YYYY-MM-DD" (primary key)
    val breakfast: String = "",
    val lunch: String = "",
    val dinner: String = "",
    val breakfastTime: String? = null,      // "HH:mm", stamped when the meal is first logged today
    val lunchTime: String? = null,
    val dinnerTime: String? = null,
    val breakfastSymptoms: SymptomSnapshot = SymptomSnapshot(),
    val lunchSymptoms: SymptomSnapshot = SymptomSnapshot(),
    val dinnerSymptoms: SymptomSnapshot = SymptomSnapshot(),
    val flares: List<Flare> = emptyList(),
) {
    /** Generic getters used by the wizard so step code stays declarative. */
    fun mealText(meal: Meal): String = when (meal) {
        Meal.BREAKFAST -> breakfast
        Meal.LUNCH -> lunch
        Meal.DINNER -> dinner
    }

    fun mealTime(meal: Meal): String? = when (meal) {
        Meal.BREAKFAST -> breakfastTime
        Meal.LUNCH -> lunchTime
        Meal.DINNER -> dinnerTime
    }

    fun mealSymptoms(meal: Meal): SymptomSnapshot = when (meal) {
        Meal.BREAKFAST -> breakfastSymptoms
        Meal.LUNCH -> lunchSymptoms
        Meal.DINNER -> dinnerSymptoms
    }

    fun withMealText(meal: Meal, value: String): DayEntry = when (meal) {
        Meal.BREAKFAST -> copy(breakfast = value)
        Meal.LUNCH -> copy(lunch = value)
        Meal.DINNER -> copy(dinner = value)
    }

    fun withMealTime(meal: Meal, value: String?): DayEntry = when (meal) {
        Meal.BREAKFAST -> copy(breakfastTime = value)
        Meal.LUNCH -> copy(lunchTime = value)
        Meal.DINNER -> copy(dinnerTime = value)
    }

    fun withMealSymptoms(meal: Meal, value: SymptomSnapshot): DayEntry = when (meal) {
        Meal.BREAKFAST -> copy(breakfastSymptoms = value)
        Meal.LUNCH -> copy(lunchSymptoms = value)
        Meal.DINNER -> copy(dinnerSymptoms = value)
    }

    /** True when nothing — food or symptoms — has been recorded for [meal]. */
    fun mealEmpty(meal: Meal): Boolean =
        mealText(meal).isEmpty() && mealSymptoms(meal).isEmpty

    /** Every symptom reading on the day: the three meals plus each flare. */
    fun allSymptoms(): List<SymptomSnapshot> =
        listOf(breakfastSymptoms, lunchSymptoms, dinnerSymptoms) + flares.map { it.symptoms }

    /** True when the day has any logged content at all. */
    val hasAnyData: Boolean
        get() = Meal.entries.any { !mealEmpty(it) } || flares.isNotEmpty()
}

/** Worst-moment-of-day rollup used for the calendar / overview colour coding. */
enum class DaySeverity { NONE, CLEAR, MILD, SEVERE }

/**
 * Reduces a whole day to a single severity bucket using a "worst moment" rule
 * across every meal and flare reading:
 *  - SEVERE — any symptom ≥ 4, or any swelling severity ≥ 2
 *  - MILD   — any symptom 2–3, or any swelling severity 1
 *  - CLEAR  — something logged, but no symptoms
 *  - NONE   — nothing logged
 */
fun DayEntry.severity(): DaySeverity {
    if (!hasAnyData) return DaySeverity.NONE
    val snaps = allSymptoms()
    val worstSym = snaps.maxOf { it.worstSymptom }
    val worstSwell = snaps.maxOf { it.worstSwelling }
    return when {
        worstSym >= 4 || worstSwell >= 2 -> DaySeverity.SEVERE
        worstSym >= 2 || worstSwell >= 1 -> DaySeverity.MILD
        else -> DaySeverity.CLEAR
    }
}
