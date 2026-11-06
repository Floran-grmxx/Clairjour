package com.clairjour.app.domain

import com.clairjour.app.data.db.JournalEntryEntity
import java.text.Normalizer
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** Entries of one week, the week being identified by its Monday. */
data class JournalWeek(val monday: LocalDate, val entries: List<JournalEntryEntity>)

/** Search and grouping for the journal list. */
object JournalBrowsing {

    private val diacritics = Regex("\\p{Mn}+")

    /** Lowercase without accents, so "Été" matches "ete". */
    fun normalize(text: String): String =
        diacritics.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase()

    fun matches(entry: JournalEntryEntity, query: String): Boolean {
        val needle = normalize(query.trim())
        if (needle.isEmpty()) return true
        return normalize(entry.notes).contains(needle) ||
            normalize(entry.gratitude.orEmpty()).contains(needle)
    }

    fun mondayOf(date: LocalDate): LocalDate = date.minus(DatePeriod(days = date.dayOfWeek.ordinal))

    /** Groups entries by week, newest week first, keeping each week's entries newest first. */
    fun groupByWeek(entries: List<JournalEntryEntity>): List<JournalWeek> =
        entries
            .sortedByDescending { it.date }
            .groupBy { mondayOf(it.date) }
            .map { (monday, weekEntries) -> JournalWeek(monday, weekEntries) }
}
