package com.skintracker.data

enum class BodyZone(val id: String, val label: String) {
    // ── Front ─────────────────────────────────────────────────────────────────
    HEAD_FACE("head_face", "Head & Face"),
    NECK("neck", "Neck"),
    R_SHOULDER("r_shoulder", "R Shoulder"),
    L_SHOULDER("l_shoulder", "L Shoulder"),
    R_UPPER_ARM("r_upper_arm", "R Upper Arm"),
    L_UPPER_ARM("l_upper_arm", "L Upper Arm"),
    R_FOREARM("r_forearm", "R Forearm"),
    L_FOREARM("l_forearm", "L Forearm"),
    R_HAND("r_hand", "R Hand"),
    L_HAND("l_hand", "L Hand"),
    CHEST("chest", "Chest"),
    BELLY("belly", "Belly"),
    R_THIGH("r_thigh", "R Thigh"),
    L_THIGH("l_thigh", "L Thigh"),
    R_CALF("r_calf", "R Calf"),
    L_CALF("l_calf", "L Calf"),
    R_FOOT("r_foot", "R Foot"),
    L_FOOT("l_foot", "L Foot"),
    // ── Back ──────────────────────────────────────────────────────────────────
    BACK_HEAD("back_head", "Head (Back)"),
    BACK_NECK("back_neck", "Neck (Back)"),
    UPPER_BACK("upper_back", "Upper Back"),
    LOWER_BACK("lower_back", "Lower Back"),
    GLUTES("glutes", "Glutes"),
    R_UPPER_ARM_BACK("r_upper_arm_back", "R Upper Arm (Back)"),
    L_UPPER_ARM_BACK("l_upper_arm_back", "L Upper Arm (Back)"),
    R_FOREARM_BACK("r_forearm_back", "R Forearm (Back)"),
    L_FOREARM_BACK("l_forearm_back", "L Forearm (Back)"),
    R_THIGH_BACK("r_thigh_back", "R Thigh (Back)"),
    L_THIGH_BACK("l_thigh_back", "L Thigh (Back)"),
    R_CALF_BACK("r_calf_back", "R Calf (Back)"),
    L_CALF_BACK("l_calf_back", "L Calf (Back)"),
    R_FOOT_SOLE("r_foot_sole", "R Foot (Sole)"),
    L_FOOT_SOLE("l_foot_sole", "L Foot (Sole)");

    companion object {
        fun fromId(id: String): BodyZone? = entries.firstOrNull { it.id == id }
    }
}

/** Cycles null → 1 (mild) → 2 (moderate) → 3 (severe) → null (clear). */
fun Int?.nextSeverity(): Int? = when (this) { null -> 1; 1 -> 2; 2 -> 3; else -> null }
