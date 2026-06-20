package com.skintracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `offKey shifts by the given number of days, including across month and year boundaries`() {
        assertEquals("2026-06-09", DateUtils.offKey("2026-06-08", 1))
        assertEquals("2026-06-07", DateUtils.offKey("2026-06-08", -1))
        assertEquals("2026-07-01", DateUtils.offKey("2026-06-30", 1))
        assertEquals("2027-01-01", DateUtils.offKey("2026-12-31", 1))
    }

    @Test
    fun `fmtDate renders weekday, short month, and day`() {
        // 2024-01-01 was a Monday — a fixed, known reference date.
        assertEquals("Mon, Jan 1", DateUtils.fmtDate("2024-01-01"))
        assertEquals("Wed, Dec 25", DateUtils.fmtDate("2024-12-25"))
    }

    @Test
    fun `isToday is true only for the current date key`() {
        val today = DateUtils.todayKey()
        assertTrue(DateUtils.isToday(today))
        assertFalse(DateUtils.isToday(DateUtils.offKey(today, -1)))
        assertFalse(DateUtils.isToday(DateUtils.offKey(today, 1)))
    }

    @Test
    fun `isFuture is true only for dates after today`() {
        val today = DateUtils.todayKey()
        assertFalse(DateUtils.isFuture(today))
        assertFalse(DateUtils.isFuture(DateUtils.offKey(today, -1)))
        assertTrue(DateUtils.isFuture(DateUtils.offKey(today, 1)))
    }
}
