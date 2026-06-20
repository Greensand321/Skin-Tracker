package com.skintracker.data

import kotlinx.serialization.Serializable

/**
 * Serialization surrogates for [DayEntry] and its nested [SymptomSnapshot] /
 * [Flare] — identical field layout, but annotated with @Serializable so
 * kotlinx.serialization can encode/decode for Room storage and JSON export.
 *
 * Kept separate from the domain classes so they carry no kotlinx.serialization
 * dependency in their JVM static initializer (<clinit>). That dependency causes
 * a NoClassDefFoundError in Android Studio's layoutlib-based Compose Preview
 * renderer, which stubs the serialization runtime with non-functional mocks.
 *
 * Only [com.skintracker.data.repository.DayRepository] and
 * [com.skintracker.data.io.DataPortability] should ever reference these.
 */
@Serializable
internal data class SymptomSnapshotSurrogate(
    val itch: Int? = null,
    val redness: Int? = null,
    val bumps: Int? = null,
    val swelling: Map<String, Int> = emptyMap(),
    val touch: String = "",
) {
    fun toDomain(): SymptomSnapshot = SymptomSnapshot(itch, redness, bumps, swelling, touch)
}

internal fun SymptomSnapshot.toSurrogate(): SymptomSnapshotSurrogate =
    SymptomSnapshotSurrogate(itch, redness, bumps, swelling, touch)

@Serializable
internal data class FlareSurrogate(
    val time: String = "",
    val symptoms: SymptomSnapshotSurrogate = SymptomSnapshotSurrogate(),
) {
    fun toDomain(): Flare = Flare(time, symptoms.toDomain())
}

internal fun Flare.toSurrogate(): FlareSurrogate = FlareSurrogate(time, symptoms.toSurrogate())

@Serializable
internal data class DayEntrySurrogate(
    val date: String,
    val breakfast: String = "",
    val lunch: String = "",
    val dinner: String = "",
    val breakfastTime: String? = null,
    val lunchTime: String? = null,
    val dinnerTime: String? = null,
    val breakfastSymptoms: SymptomSnapshotSurrogate = SymptomSnapshotSurrogate(),
    val lunchSymptoms: SymptomSnapshotSurrogate = SymptomSnapshotSurrogate(),
    val dinnerSymptoms: SymptomSnapshotSurrogate = SymptomSnapshotSurrogate(),
    val flares: List<FlareSurrogate> = emptyList(),
) {
    fun toDomain(): DayEntry = DayEntry(
        date = date,
        breakfast = breakfast,
        lunch = lunch,
        dinner = dinner,
        breakfastTime = breakfastTime,
        lunchTime = lunchTime,
        dinnerTime = dinnerTime,
        breakfastSymptoms = breakfastSymptoms.toDomain(),
        lunchSymptoms = lunchSymptoms.toDomain(),
        dinnerSymptoms = dinnerSymptoms.toDomain(),
        flares = flares.map { it.toDomain() },
    )
}

internal fun DayEntry.toSurrogate(): DayEntrySurrogate = DayEntrySurrogate(
    date = date,
    breakfast = breakfast,
    lunch = lunch,
    dinner = dinner,
    breakfastTime = breakfastTime,
    lunchTime = lunchTime,
    dinnerTime = dinnerTime,
    breakfastSymptoms = breakfastSymptoms.toSurrogate(),
    lunchSymptoms = lunchSymptoms.toSurrogate(),
    dinnerSymptoms = dinnerSymptoms.toSurrogate(),
    flares = flares.map { it.toSurrogate() },
)
