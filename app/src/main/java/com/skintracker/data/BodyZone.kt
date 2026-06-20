package com.skintracker.data

/**
 * Every tappable body zone on the front/back map.
 *
 * Anatomical convention (patient-facing): RIGHT_* zones appear on the
 * viewer's LEFT in the front view. The back view uses the same screen
 * orientation (no mirror) — RIGHT still means the patient's right side.
 *
 * Zone IDs are the keys stored in [SymptomSnapshot.swelling]; changing
 * them is a breaking change to the JSON export format.
 */
enum class BodyZone(val id: String, val label: String) {
    HEAD_NECK("head_neck", "Head & Neck"),
    RIGHT_SHOULDER("right_shoulder", "R Shoulder"),
    LEFT_SHOULDER("left_shoulder", "L Shoulder"),
    UPPER_CHEST("upper_chest", "Chest"),
    BELLY("belly", "Belly"),
    UPPER_BACK("upper_back", "Upper Back"),
    LOWER_BACK("lower_back", "Lower Back"),
    RIGHT_FOREARM("right_forearm", "R Forearm"),
    LEFT_FOREARM("left_forearm", "L Forearm"),
    RIGHT_HAND("right_hand", "R Hand"),
    LEFT_HAND("left_hand", "L Hand"),
    RIGHT_THIGH("right_thigh", "R Thigh"),
    LEFT_THIGH("left_thigh", "L Thigh"),
    RIGHT_CALF("right_calf", "R Calf"),
    LEFT_CALF("left_calf", "L Calf"),
    RIGHT_FOOT("right_foot", "R Foot"),
    LEFT_FOOT("left_foot", "L Foot");

    companion object {
        fun fromId(id: String): BodyZone? = entries.firstOrNull { it.id == id }

        val frontZones: List<BodyZone> = listOf(
            HEAD_NECK,
            RIGHT_SHOULDER, LEFT_SHOULDER,
            UPPER_CHEST, BELLY,
            RIGHT_FOREARM, LEFT_FOREARM,
            RIGHT_HAND, LEFT_HAND,
            RIGHT_THIGH, LEFT_THIGH,
            RIGHT_CALF, LEFT_CALF,
            RIGHT_FOOT, LEFT_FOOT,
        )

        val backZones: List<BodyZone> = listOf(
            HEAD_NECK,
            RIGHT_SHOULDER, LEFT_SHOULDER,
            UPPER_BACK, LOWER_BACK,
            RIGHT_FOREARM, LEFT_FOREARM,
            RIGHT_HAND, LEFT_HAND,
            RIGHT_THIGH, LEFT_THIGH,
            RIGHT_CALF, LEFT_CALF,
            RIGHT_FOOT, LEFT_FOOT,
        )
    }
}

/** Cycles null → 1 (mild) → 2 (moderate) → 3 (severe) → null (clear). */
fun Int?.nextSeverity(): Int? = when (this) {
    null -> 1
    1 -> 2
    2 -> 3
    else -> null
}
