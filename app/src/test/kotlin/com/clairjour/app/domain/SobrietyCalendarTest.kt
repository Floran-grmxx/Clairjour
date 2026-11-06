package com.clairjour.app.domain

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SobrietyCalendarTest {

    private val today = LocalDate(2026, 11, 6)

    @Test
    fun `tracking start goes back to the streak before a relapse`() {
        val start = SobrietyCalendar.trackingStart(
            currentStart = LocalDate(2026, 10, 20),
            relapses = listOf(LocalDate(2026, 10, 20) to 10)
        )
        assertEquals(LocalDate(2026, 10, 10), start)
    }

    @Test
    fun `tracking start without relapse is the current start`() {
        assertEquals(
            LocalDate(2026, 9, 1),
            SobrietyCalendar.trackingStart(LocalDate(2026, 9, 1), emptyList())
        )
    }

    @Test
    fun `day states follow priority future then relapse then untracked then journal`() {
        val start = LocalDate(2026, 11, 2)
        val relapses = setOf(LocalDate(2026, 11, 4))
        val journal = setOf(LocalDate(2026, 11, 3), LocalDate(2026, 11, 4), LocalDate(2026, 11, 1))

        fun state(day: Int) = SobrietyCalendar.dayState(LocalDate(2026, 11, day), today, start, relapses, journal)

        assertEquals(CalendarDayState.UNTRACKED, state(1))
        assertEquals(CalendarDayState.SOBER, state(2))
        assertEquals(CalendarDayState.SOBER_WITH_JOURNAL, state(3))
        assertEquals(CalendarDayState.RELAPSE, state(4))
        assertEquals(CalendarDayState.SOBER, state(6))
        assertEquals(CalendarDayState.FUTURE, state(7))
    }

    @Test
    fun `month grid starts on monday with blank padding`() {
        // 1 November 2026 is a Sunday: six blanks before it.
        val grid = SobrietyCalendar.monthGrid(today, today, LocalDate(2026, 10, 1), emptySet(), emptySet())
        assertEquals(6 + 30, grid.size)
        (0 until 6).forEach { assertNull(grid[it]) }
        assertEquals(LocalDate(2026, 11, 1), grid[6]?.date)
        assertEquals(LocalDate(2026, 11, 30), grid.last()?.date)
    }

    @Test
    fun `month grid without padding when the month starts on monday`() {
        // 1 June 2026 is a Monday.
        val grid = SobrietyCalendar.monthGrid(LocalDate(2026, 6, 15), today, today, emptySet(), emptySet())
        assertEquals(LocalDate(2026, 6, 1), grid.first()?.date)
        assertEquals(30, grid.size)
    }

    @Test
    fun `shifting months handles year boundaries`() {
        assertEquals(LocalDate(2026, 12, 1), SobrietyCalendar.shiftMonth(today, 1))
        assertEquals(LocalDate(2027, 1, 1), SobrietyCalendar.shiftMonth(today, 2))
        assertEquals(LocalDate(2025, 12, 1), SobrietyCalendar.shiftMonth(LocalDate(2026, 1, 31), -1))
    }
}
