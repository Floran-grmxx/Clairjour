package com.clairjour.app.domain

import com.clairjour.app.data.db.JournalEntryEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalBrowsingTest {

    private fun entry(date: LocalDate, notes: String = "", gratitude: String? = null) = JournalEntryEntity(
        id = date.toString(),
        date = date,
        mood = 3,
        notes = notes,
        triggers = emptyList(),
        gratitude = gratitude,
        hadCravings = false,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0)
    )

    @Test
    fun `search ignores case and accents`() {
        val day = entry(LocalDate(2026, 11, 3), notes = "Réunion TENDUE, envie forte")
        assertTrue(JournalBrowsing.matches(day, "reunion"))
        assertTrue(JournalBrowsing.matches(day, "tendue"))
        assertTrue(JournalBrowsing.matches(day, "  Envie "))
        assertFalse(JournalBrowsing.matches(day, "soirée"))
    }

    @Test
    fun `search also looks at gratitude`() {
        val day = entry(LocalDate(2026, 11, 3), gratitude = "Le café avec Léa")
        assertTrue(JournalBrowsing.matches(day, "lea"))
    }

    @Test
    fun `blank search matches everything`() {
        assertTrue(JournalBrowsing.matches(entry(LocalDate(2026, 11, 3)), "   "))
    }

    @Test
    fun `monday of a sunday is six days before`() {
        assertEquals(LocalDate(2026, 11, 2), JournalBrowsing.mondayOf(LocalDate(2026, 11, 8)))
        assertEquals(LocalDate(2026, 11, 2), JournalBrowsing.mondayOf(LocalDate(2026, 11, 2)))
    }

    @Test
    fun `entries are grouped by week newest first`() {
        val weeks = JournalBrowsing.groupByWeek(
            listOf(
                entry(LocalDate(2026, 10, 30)),
                entry(LocalDate(2026, 11, 2)),
                entry(LocalDate(2026, 11, 5)),
                entry(LocalDate(2026, 10, 26))
            )
        )
        assertEquals(listOf(LocalDate(2026, 11, 2), LocalDate(2026, 10, 26)), weeks.map { it.monday })
        assertEquals(listOf(LocalDate(2026, 11, 5), LocalDate(2026, 11, 2)), weeks[0].entries.map { it.date })
        assertEquals(listOf(LocalDate(2026, 10, 30), LocalDate(2026, 10, 26)), weeks[1].entries.map { it.date })
    }
}
