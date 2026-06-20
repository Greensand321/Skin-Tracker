package com.skintracker.data.repository

import com.skintracker.data.DayEntry
import com.skintracker.data.Flare
import com.skintracker.data.SymptomSnapshot
import com.skintracker.data.db.DayEntryDao
import com.skintracker.data.db.DayEntryEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * In-memory stand-in for the Room-generated DAO so these tests run on the JVM
 * without an Android device/emulator. Exercises exactly the same DayEntryDao
 * contract that DayRepository depends on.
 */
private class InMemoryDao : DayEntryDao {
    val store = mutableMapOf<String, DayEntryEntity>()
    override suspend fun upsert(entity: DayEntryEntity) { store[entity.date] = entity }
    override suspend fun upsertAll(entities: List<DayEntryEntity>) { entities.forEach { store[it.date] = it } }
    override suspend fun get(date: String): DayEntryEntity? = store[date]
    override suspend fun getAll(): List<DayEntryEntity> = store.values.sortedByDescending { it.date }
    override suspend fun deleteByDate(date: String) { store.remove(date) }
    override suspend fun deleteAll() { store.clear() }
}

/**
 * These tests exist to guard the central promise of the JSON-column persistence
 * design (see CLAUDE.md / android/README.md): adding or removing fields from
 * DayEntry must never corrupt or lose previously-stored records. A regression
 * here would mean real user data silently breaking on an app update.
 */
class DayRepositoryTest {

    @Test
    fun `save then load round-trips every field exactly`() = runTest {
        val repo = DayRepository(InMemoryDao())
        val original = DayEntry(
            date = "2026-06-01",
            breakfast = "Eggs and bacon",
            lunch = "Salad", dinner = "Steak",
            breakfastTime = "08:05", lunchTime = null, dinnerTime = "19:30",
            breakfastSymptoms = SymptomSnapshot(itch = 2, redness = 1, bumps = 3, touch = "new detergent"),
            lunchSymptoms = SymptomSnapshot(swelling = mapOf("face" to 2, "hands" to 1)),
            dinnerSymptoms = SymptomSnapshot(),
            flares = listOf(
                Flare(time = "14:20", symptoms = SymptomSnapshot(itch = 5, swelling = mapOf("hands" to 3))),
            ),
        )

        repo.save(original)

        assertEquals(original, repo.load("2026-06-01"))
    }

    @Test
    fun `load returns a blank entry for a date that was never saved`() {
        runTest {
            val repo = DayRepository(InMemoryDao())
            assertEquals(DayEntry(date = "2099-01-01"), repo.load("2099-01-01"))
        }
    }

    @Test
    fun `decoding ignores unknown JSON fields - forward compatibility`() = runTest {
        val dao = InMemoryDao()
        // Simulates a record written by a NEWER app version that added a field
        // this version doesn't know about yet.
        dao.store["2026-06-02"] = DayEntryEntity(
            date = "2026-06-02",
            data = """{"date":"2026-06-02","breakfast":"Omelette","futureField":{"nested":true}}""",
        )

        val loaded = DayRepository(dao).load("2026-06-02")

        assertEquals("Omelette", loaded.breakfast)
    }

    @Test
    fun `decoding fills in defaults for fields missing from older records - backward compatibility`() = runTest {
        val dao = InMemoryDao()
        // Simulates a record written by an OLDER app version, before the
        // per-meal symptom blocks / flares / *Time fields existed.
        dao.store["2026-06-03"] = DayEntryEntity(
            date = "2026-06-03",
            data = """{"date":"2026-06-03","breakfast":"Yogurt","breakfastSymptoms":{"itch":3}}""",
        )

        val loaded = DayRepository(dao).load("2026-06-03")

        assertEquals("Yogurt", loaded.breakfast)
        assertEquals(3, loaded.breakfastSymptoms.itch)
        assertNull(loaded.breakfastTime)
        assertTrue(loaded.lunchSymptoms.isEmpty)
        assertTrue(loaded.flares.isEmpty())
        assertEquals("", loaded.breakfastSymptoms.touch)
    }

    @Test
    fun `loadAll decodes every valid row, newest first, and silently skips corrupt rows`() = runTest {
        val dao = InMemoryDao()
        dao.store["2026-06-01"] = DayEntryEntity("2026-06-01", """{"date":"2026-06-01","breakfast":"A"}""")
        dao.store["2026-06-02"] = DayEntryEntity("2026-06-02", "{ this is not valid json")
        dao.store["2026-06-03"] = DayEntryEntity("2026-06-03", """{"date":"2026-06-03","breakfast":"C"}""")

        val all = DayRepository(dao).loadAll()

        assertEquals(listOf("2026-06-03", "2026-06-01"), all.map { it.date })
    }

    @Test
    fun `save overwrites the previous value for the same date`() = runTest {
        val repo = DayRepository(InMemoryDao())
        repo.save(DayEntry(date = "2026-06-04", breakfast = "First draft"))
        repo.save(DayEntry(date = "2026-06-04", breakfast = "Edited"))

        val all = repo.loadAll()

        assertEquals(1, all.size)
        assertEquals("Edited", all.single().breakfast)
    }
}
