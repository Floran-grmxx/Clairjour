package com.clairjour.app.ui.screen.journal

import com.clairjour.app.data.db.JournalEntryEntity
import com.clairjour.app.data.repository.JournalRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun entry(idSuffix: String, mood: Int, cravings: Boolean): JournalEntryEntity =
        JournalEntryEntity(
            id = "e-$idSuffix",
            date = LocalDate(2026, 7, 20),
            mood = mood,
            notes = "",
            triggers = emptyList(),
            gratitude = null,
            hadCravings = cravings,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

    @Test
    fun `mood plus cravings filters combine as AND`() = runTest(dispatcher) {
        val repo: JournalRepository = mockk()
        val all = MutableStateFlow(
            listOf(entry("a", 3, false), entry("b", 3, true), entry("c", 5, true))
        )
        every { repo.observeAll() } returns all

        val vm = JournalViewModel(repo)
        vm.setMoodFilter(3); vm.setCravingsOnly(true)

        val state = vm.uiState.first { it.moodFilter == 3 && it.cravingsOnly }
        val entries = state.weeks.flatMap { it.entries }
        assertEquals(1, entries.size)
        assertEquals(3, entries[0].mood)
        assertTrue(entries[0].hadCravings)
    }

    @Test
    fun `search ignores accents and keeps filters`() = runTest(dispatcher) {
        val repo: JournalRepository = mockk()
        every { repo.observeAll() } returns MutableStateFlow(
            listOf(
                entry("a", 3, false).copy(notes = "Soirée calme"),
                entry("b", 3, false).copy(notes = "Stress au travail")
            )
        )

        val vm = JournalViewModel(repo)
        vm.setQuery("soiree")

        val state = vm.uiState.first { it.query == "soiree" }
        assertEquals(listOf("e-a"), state.weeks.flatMap { it.entries }.map { it.id })
    }

    @Test
    fun `clearFilters resets mood and cravings`() = runTest(dispatcher) {
        val repo: JournalRepository = mockk()
        every { repo.observeAll() } returns MutableStateFlow(emptyList())

        val vm = JournalViewModel(repo)
        vm.setMoodFilter(2); vm.setCravingsOnly(true); vm.clearFilters()

        val state = vm.uiState.first { !it.cravingsOnly && it.moodFilter == null }
        assertNull(state.moodFilter)
        assertFalse(state.cravingsOnly)
    }
}
