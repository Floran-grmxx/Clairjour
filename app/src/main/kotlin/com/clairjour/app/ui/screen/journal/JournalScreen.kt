package com.clairjour.app.ui.screen.journal

import androidx.compose.foundation.background
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clairjour.app.R
import com.clairjour.app.data.AppContainer
import com.clairjour.app.data.db.JournalEntryEntity
import com.clairjour.app.domain.Formatters
import com.clairjour.app.ui.components.viewModelFactoryOf
import kotlinx.datetime.LocalDate
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import com.clairjour.app.ui.theme.ClairjourSage

@Composable
fun JournalScreen(
    container: AppContainer,
    contentPadding: PaddingValues,
    onOpenEditor: (LocalDate?) -> Unit
) {
    val vm: JournalViewModel = viewModel(
        factory = viewModelFactoryOf { JournalViewModel(container.journalRepository) }
    )
    val state by vm.uiState.collectAsState()
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    // The app's bottom bar padding goes on the Scaffold so the add button sits above it.
    Scaffold(
        modifier = Modifier.padding(contentPadding),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenEditor(null) },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_add_journal_entry)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(stringResource(R.string.journal_title), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::setQuery,
                    placeholder = { Text(stringResource(R.string.journal_search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { vm.setQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_search))
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (mood in 1..5) {
                        FilterChip(
                            selected = state.moodFilter == mood,
                            onClick = {
                                vm.setMoodFilter(if (state.moodFilter == mood) null else mood)
                            },
                            label = { Text(stringResource(R.string.journal_filter_mood, mood)) }
                        )
                    }
                    FilterChip(
                        selected = state.cravingsOnly,
                        onClick = { vm.setCravingsOnly(!state.cravingsOnly) },
                        label = { Text(stringResource(R.string.journal_filter_cravings)) }
                    )
                    if (state.moodFilter != null || state.cravingsOnly) {
                        TextButton(onClick = { vm.clearFilters() }) {
                            Text(stringResource(R.string.journal_filter_clear))
                        }
                    }
                }
            }
            if (state.weeks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.journal_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    // Bottom padding keeps the last entry clear of the add button.
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.weeks.flatMap { it.entries }, key = { it.id }) { entry ->
                        SwipeableEntry(
                            entry = entry,
                            onClick = { onOpenEditor(entry.date) },
                            onSwipedToDelete = {
                                // Guard against re-triggers when the state briefly
                                // transitions during LaunchedEffect reset.
                                if (pendingDeleteId == null) pendingDeleteId = entry.id
                            }
                        )
                    }
                }
            }
        }
    }

    val idToDelete = pendingDeleteId
    if (idToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.journal_delete_title)) },
            text = { Text(stringResource(R.string.journal_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(idToDelete)
                    pendingDeleteId = null
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SwipeableEntry(
    entry: JournalEntryEntity,
    onClick: () -> Unit,
    onSwipedToDelete: () -> Unit
) {
    // Non-committing swipe: we intercept the dismiss and open a confirmation dialog.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwipedToDelete()
            }
            false
        }
    )

    // Snap back if state somehow settled on dismiss.
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            // The card is translucent: only draw the delete background while swiping.
            if (dismissState.dismissDirection != SwipeToDismissBoxValue.EndToStart) return@SwipeToDismissBox
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.cd_delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        EntryRow(entry = entry, onClick = onClick)
    }
}

/** Strip color by mood: low in red, neutral in gold, good in green. */
@Composable
private fun moodColor(mood: Int): Color = when {
    mood <= 2 -> MaterialTheme.colorScheme.error
    mood == 3 -> MaterialTheme.colorScheme.secondary
    else -> ClairjourSage
}

@Composable
private fun EntryRow(entry: JournalEntryEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    val triggerKeys = stringArrayResource(R.array.journal_trigger_keys)
    val triggerLabels = stringArrayResource(R.array.journal_trigger_labels)
    val triggersText = entry.triggers
        .map { key -> triggerLabels.getOrNull(triggerKeys.indexOf(key)) ?: key }
        .joinToString(" · ")
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(moodColor(entry.mood))
            )
            Column(Modifier.padding(16.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Formatters.date(entry.date, context),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${entry.mood}/5",
                        style = MaterialTheme.typography.labelLarge,
                        color = moodColor(entry.mood)
                    )
                }
                if (entry.notes.isNotBlank()) {
                    Text(
                        entry.notes.take(120),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                if (triggersText.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(triggersText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
