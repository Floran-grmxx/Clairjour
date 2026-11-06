package com.clairjour.app.ui.screen.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clairjour.app.data.repository.JournalRepository
import com.clairjour.app.domain.JournalBrowsing
import com.clairjour.app.domain.JournalWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class JournalListUiState(
    val weeks: List<JournalWeek> = emptyList(),
    val query: String = "",
    val moodFilter: Int? = null,
    val cravingsOnly: Boolean = false
)

class JournalViewModel(
    private val repository: JournalRepository
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    private val moodFilterFlow = MutableStateFlow<Int?>(null)
    private val cravingsFlow = MutableStateFlow(false)

    // Search runs in memory: SQLite LIKE does not ignore accents.
    val uiState: StateFlow<JournalListUiState> = combine(
        repository.observeAll(),
        queryFlow,
        moodFilterFlow,
        cravingsFlow
    ) { entries, query, mood, cravings ->
        val filtered = entries
            .filter { JournalBrowsing.matches(it, query) }
            .let { list -> if (mood != null) list.filter { it.mood == mood } else list }
            .let { list -> if (cravings) list.filter { it.hadCravings } else list }
        JournalListUiState(
            weeks = JournalBrowsing.groupByWeek(filtered),
            query = query,
            moodFilter = mood,
            cravingsOnly = cravings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalListUiState())

    fun setQuery(q: String) { queryFlow.value = q }
    fun setMoodFilter(mood: Int?) { moodFilterFlow.value = mood }
    fun setCravingsOnly(v: Boolean) { cravingsFlow.value = v }
    fun clearFilters() {
        moodFilterFlow.value = null
        cravingsFlow.value = false
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}

data class EditorUiState(
    val date: LocalDate,
    val mood: Int = 3,
    val notes: String = "",
    val triggers: List<String> = emptyList(),
    val gratitude: String = "",
    val hadCravings: Boolean = false,
    val loaded: Boolean = false
)

class JournalEditorViewModel(
    private val repository: JournalRepository,
    targetDate: LocalDate? = null
) : ViewModel() {

    private val editDate = targetDate ?: repository.today()
    private val _state = MutableStateFlow(EditorUiState(date = editDate))
    val state: StateFlow<EditorUiState> = _state

    init {
        viewModelScope.launch {
            val entity = repository.observeByDate(editDate).first()
            _state.value = if (entity != null) {
                EditorUiState(
                    date = entity.date,
                    mood = entity.mood,
                    notes = entity.notes,
                    triggers = entity.triggers,
                    gratitude = entity.gratitude.orEmpty(),
                    hadCravings = entity.hadCravings,
                    loaded = true
                )
            } else {
                _state.value.copy(loaded = true)
            }
        }
    }

    fun setMood(v: Int) { _state.value = _state.value.copy(mood = v.coerceIn(1, 5)) }
    fun setNotes(v: String) { _state.value = _state.value.copy(notes = v) }
    fun setGratitude(v: String) { _state.value = _state.value.copy(gratitude = v) }
    fun setCravings(v: Boolean) { _state.value = _state.value.copy(hadCravings = v) }
    fun toggleTrigger(t: String) {
        val current = _state.value.triggers
        _state.value = _state.value.copy(
            triggers = if (current.contains(t)) current - t else current + t
        )
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            try {
                repository.upsert(
                    date = s.date,
                    mood = s.mood,
                    notes = s.notes,
                    triggers = s.triggers,
                    gratitude = s.gratitude.ifBlank { null },
                    hadCravings = s.hadCravings
                )
                onDone()
            } catch (_: Exception) {
                // save failure is silent; entry remains editable
            }
        }
    }
}
