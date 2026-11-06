package com.clairjour.app.ui.screen.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clairjour.app.ui.components.ClairjourSwitch
import com.clairjour.app.R
import com.clairjour.app.data.AppContainer
import com.clairjour.app.ui.components.viewModelFactoryOf
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditorScreen(
    container: AppContainer,
    date: LocalDate?,
    onDone: () -> Unit
) {
    val vm: JournalEditorViewModel = viewModel(
        key = date?.toString() ?: "today",
        factory = viewModelFactoryOf { JournalEditorViewModel(container.journalRepository, date) }
    )
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.snack_journal_saved)
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.journal_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_close)
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(stringResource(R.string.journal_mood), style = MaterialTheme.typography.titleMedium)
            Slider(
                value = state.mood.toFloat(),
                onValueChange = { vm.setMood(it.toInt()) },
                valueRange = 1f..5f,
                steps = 3
            )
            Text(
                "${state.mood}/5",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.End)
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.notes,
                onValueChange = vm::setNotes,
                label = { Text(stringResource(R.string.journal_notes)) },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.journal_triggers), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            // Keys are the canonical English tokens stored in DB; labels are localized for display.
            val triggerKeys = stringArrayResource(R.array.journal_trigger_keys)
            val triggerLabels = stringArrayResource(R.array.journal_trigger_labels)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 0 until minOf(4, triggerKeys.size)) {
                    val key = triggerKeys[i]
                    val label = triggerLabels.getOrElse(i) { key }
                    FilterChip(
                        selected = state.triggers.contains(key),
                        onClick = { vm.toggleTrigger(key) },
                        label = { Text(label) }
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            ) {
                for (i in 4 until triggerKeys.size) {
                    val key = triggerKeys[i]
                    val label = triggerLabels.getOrElse(i) { key }
                    FilterChip(
                        selected = state.triggers.contains(key),
                        onClick = { vm.toggleTrigger(key) },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.gratitude,
                onValueChange = vm::setGratitude,
                label = { Text(stringResource(R.string.journal_gratitude)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.journal_cravings),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                ClairjourSwitch(
                    checked = state.hadCravings,
                    onCheckedChange = vm::setCravings
                )
            }
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    vm.save {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(savedMessage)
                        }
                        onDone()
                    }
                },
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(stringResource(R.string.action_save)) }

            Spacer(Modifier.height(24.dp))
        }
    }
}
