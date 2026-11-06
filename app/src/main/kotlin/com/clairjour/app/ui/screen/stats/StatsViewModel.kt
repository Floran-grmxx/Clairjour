package com.clairjour.app.ui.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clairjour.app.data.db.AddictionEntity
import com.clairjour.app.data.db.MilestoneDao
import com.clairjour.app.data.db.RelapseDao
import com.clairjour.app.data.repository.AddictionRepository
import com.clairjour.app.data.repository.JournalRepository
import com.clairjour.app.domain.CalendarDay
import com.clairjour.app.domain.SobrietyCalendar
import com.clairjour.app.domain.Streak
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class StatsUiState(
    val totalSaved: Double = 0.0,
    val savedPerDay: Double = 0.0,
    val reachedMilestoneDays: Set<Int> = emptySet(),
    val recentMoodPoints: List<Int> = emptyList(),
    val addictions: List<AddictionEntity> = emptyList(),
    val currentStreakDays: Int = 0,
    val recordStreakDays: Int = 0,
    val moodAverage7d: Double? = null,
    val moodAverage30d: Double? = null,
    val topTriggers: List<TriggerCount> = emptyList(),
    val calendarMonth: LocalDate? = null,
    val calendarDays: List<CalendarDay?> = emptyList(),
    val canShowPreviousMonth: Boolean = false,
    val canShowNextMonth: Boolean = false
)

data class TriggerCount(val key: String, val count: Int)

class StatsViewModel(
    private val addictions: AddictionRepository,
    private val journal: JournalRepository,
    private val milestoneDao: MilestoneDao,
    private val relapseDao: RelapseDao
) : ViewModel() {

    /** First day of the month shown in the calendar; null means the current month. */
    private val shownMonth = MutableStateFlow<LocalDate?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<StatsUiState> = addictions.observeActive().flatMapLatest { list ->
        val primary = list.firstOrNull { it.isPrimary } ?: list.firstOrNull()
        // Relapses only make sense for a chosen primary; when there's none, feed an empty list.
        val relapseFlow = primary?.let { relapseDao.observeFor(it.id) } ?: flowOf(emptyList())
        combine(
            journal.observeAll(),
            milestoneDao.observeAll(),
            relapseFlow,
            shownMonth
        ) { allEntries, milestones, relapses, requestedMonth ->
            val entries = allEntries.take(RECENT_ENTRY_COUNT)
            val currentStreak = primary?.let { Streak.daysSince(it.startDate) } ?: 0
            val historicalMax = relapses.maxOfOrNull { it.previousStreakDays } ?: 0
            val recordStreak = maxOf(currentStreak, historicalMax)

            val totalSaved = list.sumOf { (it.costPerDay ?: 0.0) * Streak.daysSince(it.startDate) }
            val moodPoints = entries.map { it.mood }.reversed()

            val avg7 = entries.take(7).map { it.mood }.average().takeIf { !it.isNaN() }
            val avg30 = entries.map { it.mood }.average().takeIf { !it.isNaN() }
            val triggers = entries
                .flatMap { it.triggers }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
                .take(3)
                .map { TriggerCount(it.first, it.second) }

            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val currentMonth = SobrietyCalendar.firstDayOfMonth(today)
            val trackingStart = primary?.let { addiction ->
                SobrietyCalendar.trackingStart(
                    currentStart = addiction.startDate.toLocalDateTime(TimeZone.currentSystemDefault()).date,
                    relapses = relapses.map { it.date to it.previousStreakDays }
                )
            }
            val firstMonth = trackingStart?.let(SobrietyCalendar::firstDayOfMonth) ?: currentMonth
            val month = (requestedMonth ?: currentMonth).coerceIn(firstMonth, currentMonth)

            StatsUiState(
                totalSaved = totalSaved,
                savedPerDay = list.sumOf { it.costPerDay ?: 0.0 },
                reachedMilestoneDays = milestones.map { it.milestoneDays }.toSet(),
                recentMoodPoints = moodPoints,
                addictions = list,
                currentStreakDays = currentStreak,
                recordStreakDays = recordStreak,
                moodAverage7d = avg7,
                moodAverage30d = avg30,
                topTriggers = triggers,
                calendarMonth = month,
                calendarDays = trackingStart?.let { start ->
                    SobrietyCalendar.monthGrid(
                        month = month,
                        today = today,
                        trackingStart = start,
                        relapseDates = relapses.map { it.date }.toSet(),
                        journalDates = allEntries.map { it.date }.toSet()
                    )
                } ?: emptyList(),
                canShowPreviousMonth = month > firstMonth,
                canShowNextMonth = month < currentMonth
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun showPreviousMonth() = shiftShownMonth(-1)

    fun showNextMonth() = shiftShownMonth(1)

    private fun shiftShownMonth(months: Int) {
        // Bounds are applied when the state is built, so the request only needs a base month.
        val base = state.value.calendarMonth ?: return
        shownMonth.value = SobrietyCalendar.shiftMonth(base, months)
    }

    private companion object {
        /** Mood averages, chart and triggers use the most recent entries only. */
        const val RECENT_ENTRY_COUNT = 30
    }
}
