package com.clairjour.app.ui.screen.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clairjour.app.R
import com.clairjour.app.data.AppContainer
import com.clairjour.app.domain.CalendarDay
import com.clairjour.app.domain.CalendarDayState
import com.clairjour.app.domain.Formatters
import com.clairjour.app.domain.Milestone
import com.clairjour.app.domain.Milestones
import com.clairjour.app.ui.components.ClairjourCard
import com.clairjour.app.ui.components.viewModelFactoryOf
import java.time.format.TextStyle
import kotlinx.datetime.LocalDate

@Composable
fun StatsScreen(
    container: AppContainer,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val vm: StatsViewModel = viewModel(
        factory = viewModelFactoryOf {
            StatsViewModel(
                container.addictionRepository,
                container.journalRepository,
                container.milestoneDao,
                container.relapseDao
            )
        }
    )
    val state by vm.state.collectAsState()

    // Map trigger DB keys → localized labels for display.
    val triggerKeys = stringArrayResource(R.array.journal_trigger_keys)
    val triggerLabels = stringArrayResource(R.array.journal_trigger_labels)
    val labelForKey: (String) -> String = { key ->
        val idx = triggerKeys.indexOf(key)
        if (idx in triggerLabels.indices) triggerLabels[idx] else key
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                title = stringResource(R.string.stats_current_streak),
                value = pluralStringResource(R.plurals.stats_days_value, state.currentStreakDays, state.currentStreakDays),
                detail = pluralStringResource(R.plurals.stats_record_days, state.recordStreakDays, state.recordStreakDays),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = stringResource(R.string.stats_total_saved),
                value = Formatters.currency(state.totalSaved, context),
                detail = stringResource(R.string.stats_saved_per_day, Formatters.currency(state.savedPerDay, context)),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))

        MilestoneTimeline(
            reachedDays = state.reachedMilestoneDays,
            currentStreakDays = state.currentStreakDays
        )
        Spacer(Modifier.height(12.dp))

        val month = state.calendarMonth
        if (month != null && state.calendarDays.isNotEmpty()) {
            SobrietyCalendarCard(
                month = month,
                days = state.calendarDays,
                canShowPrevious = state.canShowPreviousMonth,
                canShowNext = state.canShowNextMonth,
                onPrevious = vm::showPreviousMonth,
                onNext = vm::showNextMonth
            )
            Spacer(Modifier.height(12.dp))
        }

        MoodCard(
            points = state.recentMoodPoints,
            average7d = state.moodAverage7d,
            average30d = state.moodAverage30d
        )
        Spacer(Modifier.height(12.dp))

        if (state.topTriggers.isNotEmpty()) {
            ClairjourCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SectionLabel(stringResource(R.string.stats_top_triggers))
                    Spacer(Modifier.height(8.dp))
                    state.topTriggers.forEach { triggerCount ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(labelForKey(triggerCount.key), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "×${triggerCount.count}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StatCard(title: String, value: String, detail: String, modifier: Modifier = Modifier) {
    ClairjourCard(modifier = modifier) {
        Column {
            SectionLabel(title)
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Horizontal strip of milestone badges: reached, next (with days left) and locked. */
@Composable
private fun MilestoneTimeline(reachedDays: Set<Int>, currentStreakDays: Int) {
    val next = Milestones.next(currentStreakDays)
    ClairjourCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 16.dp)) {
        Column {
            Box(Modifier.padding(horizontal = 20.dp)) {
                SectionLabel(stringResource(R.string.detail_milestones))
            }
            Spacer(Modifier.height(12.dp))
            // Open on the last reached badges so the next milestone is in view.
            val nextIndex = next?.let(Milestones.all::indexOf) ?: Milestones.all.lastIndex
            val listState = rememberLazyListState()
            // Data arrives after the first frame: scroll once the real streak is known.
            LaunchedEffect(nextIndex) { listState.scrollToItem((nextIndex - 3).coerceAtLeast(0)) }
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                items(Milestones.all, key = { it.days }) { milestone ->
                    MilestoneBadge(
                        milestone = milestone,
                        reached = milestone.days in reachedDays,
                        isNext = milestone == next,
                        daysLeft = milestone.days - currentStreakDays
                    )
                }
            }
        }
    }
}

@Composable
private fun MilestoneBadge(milestone: Milestone, reached: Boolean, isNext: Boolean, daysLeft: Int) {
    val gold = MaterialTheme.colorScheme.secondary
    val label = stringResource(milestone.labelRes)
    val detail = when {
        reached -> stringResource(R.string.cd_milestone_reached)
        isNext -> pluralStringResource(R.plurals.stats_milestone_in_days, daysLeft, daysLeft)
        else -> ""
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label $detail" }
    ) {
        val circle = Modifier.size(48.dp)
        when {
            reached -> Box(circle.background(gold, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
            }
            isNext -> Box(circle.border(2.dp, gold, CircleShape), contentAlignment = Alignment.Center) {
                Text(daysLeft.toString(), style = MaterialTheme.typography.titleMedium, color = gold)
            }
            else -> Box(circle.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (reached || isNext) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isNext) {
            Text(detail, style = MaterialTheme.typography.labelSmall, color = gold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SobrietyCalendarCard(
    month: LocalDate,
    days: List<CalendarDay?>,
    canShowPrevious: Boolean,
    canShowNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val monthTitle = remember(month, locale) {
        val monthName = java.time.Month.of(month.monthNumber).getDisplayName(TextStyle.FULL_STANDALONE, locale)
        "${monthName.replaceFirstChar { it.titlecase(locale) }} ${month.year}"
    }
    val weekdayInitials = remember(locale) {
        java.time.DayOfWeek.entries.map { it.getDisplayName(TextStyle.NARROW_STANDALONE, locale) }
    }
    ClairjourCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(monthTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onPrevious, enabled = canShowPrevious) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.cd_previous_month)
                    )
                }
                IconButton(onClick = onNext, enabled = canShowNext) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.cd_next_month)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                weekdayInitials.forEach { initial ->
                    Text(
                        initial,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            days.chunked(7).forEach { week ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    (0 until 7).forEach { index ->
                        CalendarCell(day = week.getOrNull(index), modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem(calendarColor(CalendarDayState.SOBER), stringResource(R.string.stats_calendar_sober))
                LegendItem(
                    calendarColor(CalendarDayState.SOBER_WITH_JOURNAL),
                    stringResource(R.string.stats_calendar_journal)
                )
                LegendItem(calendarColor(CalendarDayState.RELAPSE), stringResource(R.string.stats_calendar_relapse))
            }
        }
    }
}

@Composable
private fun calendarColor(state: CalendarDayState): Color = when (state) {
    CalendarDayState.SOBER -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
    CalendarDayState.SOBER_WITH_JOURNAL -> MaterialTheme.colorScheme.secondary
    CalendarDayState.RELAPSE -> MaterialTheme.colorScheme.error
    CalendarDayState.UNTRACKED -> MaterialTheme.colorScheme.surfaceVariant
    CalendarDayState.FUTURE -> Color.Transparent
}

@Composable
private fun CalendarCell(day: CalendarDay?, modifier: Modifier = Modifier) {
    Box(modifier.height(32.dp), contentAlignment = Alignment.Center) {
        if (day != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(calendarColor(day.state), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (day.state) {
                        CalendarDayState.SOBER_WITH_JOURNAL -> MaterialTheme.colorScheme.onSecondary
                        CalendarDayState.RELAPSE -> MaterialTheme.colorScheme.onError
                        CalendarDayState.FUTURE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MoodCard(points: List<Int>, average7d: Double?, average30d: Double?) {
    ClairjourCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            SectionLabel(stringResource(R.string.stats_mood_over_time))
            Spacer(Modifier.height(8.dp))
            if (points.isEmpty()) {
                Text(
                    stringResource(R.string.stats_empty_mood),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
            } else {
                MoodChart(points)
                if (average7d != null && average30d != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.stats_mood_averages, "%.1f".format(average7d), "%.1f".format(average30d)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodChart(points: List<Int>) {
    val stroke = MaterialTheme.colorScheme.secondary
    val axis = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val maxY = 5f
        val minY = 1f
        val pad = 8f
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        val step = if (points.size > 1) w / (points.size - 1) else w
        var prev: Offset? = null
        points.forEachIndexed { i, mood ->
            val x = pad + step * i
            val ratio = (mood - minY) / (maxY - minY)
            val y = pad + h - h * ratio
            val p = Offset(x, y)
            if (prev != null) {
                drawLine(color = stroke, start = prev!!, end = p, strokeWidth = 4f)
            }
            drawCircle(color = stroke, radius = 6f, center = p)
            prev = p
        }
        drawLine(
            color = axis,
            start = Offset(pad, size.height - pad),
            end = Offset(size.width - pad, size.height - pad),
            strokeWidth = 1f
        )
    }
}
