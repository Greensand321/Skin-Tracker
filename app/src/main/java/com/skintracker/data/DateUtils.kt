package com.skintracker.data

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Date helpers mirroring todayKey()/fmtDate()/offKey() from index.html, using
 * java.time. Day keys are ISO `YYYY-MM-DD` strings, same as localStorage keys.
 */
object DateUtils {
    fun todayKey(): String = LocalDate.now().toString()

    fun offKey(key: String, days: Long): String =
        LocalDate.parse(key).plusDays(days).toString()

    /** "Mon, Jan 15" — same format as the web fmtDate(). */
    fun fmtDate(key: String): String {
        val d = LocalDate.parse(key)
        val dow = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        val mon = d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        return "$dow, $mon ${d.dayOfMonth}"
    }

    fun isToday(key: String): Boolean = key == todayKey()
    fun isFuture(key: String): Boolean = key > todayKey()

    /**
     * 42-cell (6×7), Sunday-first month grid for [year]/[month] (1-12) —
     * mirrors the web app's `buildCal()` leading/current/trailing day layout
     * (CLAUDE.md "Calendar Panel") so every month always renders a full
     * 6-row grid, with adjacent-month days included for navigation.
     */
    fun monthGrid(year: Int, month: Int): List<CalendarDay> {
        val first = LocalDate.of(year, month, 1)
        val daysInMonth = first.lengthOfMonth()
        val startDow = first.dayOfWeek.value % 7 // DayOfWeek Mon=1..Sun=7 -> Sun=0..Sat=6

        val cells = mutableListOf<CalendarDay>()

        val prev = first.minusMonths(1)
        val prevLen = prev.lengthOfMonth()
        for (i in startDow - 1 downTo 0) {
            val dom = prevLen - i
            cells += CalendarDay(LocalDate.of(prev.year, prev.monthValue, dom).toString(), dom, inCurrentMonth = false)
        }

        for (dom in 1..daysInMonth) {
            cells += CalendarDay(LocalDate.of(year, month, dom).toString(), dom, inCurrentMonth = true)
        }

        val next = first.plusMonths(1)
        var dom = 1
        while (cells.size < 42) {
            cells += CalendarDay(LocalDate.of(next.year, next.monthValue, dom).toString(), dom, inCurrentMonth = false)
            dom++
        }
        return cells
    }
}

/** One cell in a month grid: its date key, day-of-month number, and whether it belongs to the displayed month. */
data class CalendarDay(val key: String, val dayOfMonth: Int, val inCurrentMonth: Boolean)
