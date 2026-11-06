package com.clairjour.app.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

enum class CalendarDayState { UNTRACKED, SOBER, SOBER_WITH_JOURNAL, RELAPSE, FUTURE }

data class CalendarDay(val date: LocalDate, val state: CalendarDayState)

/** Builds the month grid shown in Stats: which days were sober, journaled or relapses. */
object SobrietyCalendar {

    /**
     * First tracked day: the current start date, or earlier when past relapses
     * show a streak that began before it.
     */
    fun trackingStart(currentStart: LocalDate, relapses: List<Pair<LocalDate, Int>>): LocalDate =
        relapses
            .map { (relapseDate, previousStreakDays) -> relapseDate.minus(DatePeriod(days = previousStreakDays)) }
            .plus(currentStart)
            .min()

    fun dayState(
        date: LocalDate,
        today: LocalDate,
        trackingStart: LocalDate,
        relapseDates: Set<LocalDate>,
        journalDates: Set<LocalDate>
    ): CalendarDayState = when {
        date > today -> CalendarDayState.FUTURE
        date in relapseDates -> CalendarDayState.RELAPSE
        date < trackingStart -> CalendarDayState.UNTRACKED
        date in journalDates -> CalendarDayState.SOBER_WITH_JOURNAL
        else -> CalendarDayState.SOBER
    }

    /**
     * Cells of [month] (any date inside it) laid out Monday to Sunday.
     * Leading nulls pad the first week so day 1 falls under its weekday.
     */
    fun monthGrid(
        month: LocalDate,
        today: LocalDate,
        trackingStart: LocalDate,
        relapseDates: Set<LocalDate>,
        journalDates: Set<LocalDate>
    ): List<CalendarDay?> {
        val firstDay = firstDayOfMonth(month)
        val leadingBlanks = firstDay.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal
        val days = generateSequence(firstDay) { it.plus(DatePeriod(days = 1)) }
            .takeWhile { it.month == firstDay.month }
            .map { CalendarDay(it, dayState(it, today, trackingStart, relapseDates, journalDates)) }
            .toList()
        return List(leadingBlanks) { null } + days
    }

    fun firstDayOfMonth(date: LocalDate): LocalDate = LocalDate(date.year, date.month, 1)

    fun shiftMonth(month: LocalDate, months: Int): LocalDate =
        firstDayOfMonth(month).plus(DatePeriod(months = months))
}
