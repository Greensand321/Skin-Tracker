package com.skintracker.data.repository

import com.skintracker.data.DateUtils
import com.skintracker.data.DayEntry
import com.skintracker.data.Flare
import com.skintracker.data.SymptomSnapshot

/**
 * In-memory repository for Compose Previews and unit tests. Seeded with two
 * past days so the calendar, overview, and summary screens have something to
 * show on first launch.
 */
class FakeDayRepository : IDayRepository {

    private val days = mutableMapOf<String, DayEntry>()

    init {
        val today = DateUtils.todayKey()
        listOf(
            DayEntry(
                date = DateUtils.offKey(today, -1),
                breakfast = "3 eggs, bacon, half an avocado",
                lunch = "Chicken Caesar (no croutons)",
                dinner = "Ribeye + buttered asparagus",
                breakfastTime = "08:12", lunchTime = "12:40", dinnerTime = "19:05",
                breakfastSymptoms = SymptomSnapshot(itch = 1, redness = 1, bumps = 1),
                lunchSymptoms = SymptomSnapshot(itch = 2, redness = 2, touch = "New hand soap at work"),
                dinnerSymptoms = SymptomSnapshot(itch = 1, redness = 1, bumps = 2),
            ),
            DayEntry(
                date = DateUtils.offKey(today, -2),
                breakfast = "Skipped (fasting)",
                lunch = "Tuna salad",
                dinner = "Pizza night 🍕",
                lunchTime = "12:30", dinnerTime = "19:30",
                lunchSymptoms = SymptomSnapshot(itch = 3, redness = 2),
                dinnerSymptoms = SymptomSnapshot(
                    itch = 4, redness = 4, bumps = 3,
                    swelling = mapOf("face" to 2, "hands" to 1),
                    touch = "Handled warehouse boxes",
                ),
                flares = listOf(
                    Flare(
                        time = "14:20",
                        symptoms = SymptomSnapshot(
                            itch = 5, redness = 4,
                            swelling = mapOf("hands" to 2),
                            touch = "Sudden flare after touching cardboard",
                        ),
                    ),
                ),
            ),
        ).forEach { days[it.date] = it }
    }

    override suspend fun load(date: String): DayEntry = days[date] ?: DayEntry(date = date)

    override suspend fun loadAll(): List<DayEntry> = days.values.sortedByDescending { it.date }

    override suspend fun save(entry: DayEntry) { days[entry.date] = entry }

    override suspend fun saveAll(entries: List<DayEntry>) { entries.forEach { days[it.date] = it } }

    override suspend fun delete(key: String) { days.remove(key) }
    override suspend fun deleteAll() { days.clear() }

    fun loadSync(date: String): DayEntry = days[date] ?: DayEntry(date = date)
    fun allSync(): Map<String, DayEntry> = days.toMap()
}
