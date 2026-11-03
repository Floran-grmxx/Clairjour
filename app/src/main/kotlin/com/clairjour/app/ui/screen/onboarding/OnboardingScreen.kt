package com.clairjour.app.ui.screen.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clairjour.app.R
import com.clairjour.app.data.AppContainer
import com.clairjour.app.data.prefs.AppLanguage
import com.clairjour.app.data.prefs.LocaleManager
import com.clairjour.app.domain.AddictionType
import com.clairjour.app.notifications.ReminderScheduler
import com.clairjour.app.ui.components.viewModelFactoryOf
import kotlinx.datetime.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Guards the "sober since" date picker: users can pick today or earlier, never
 * the future — which would freeze the counter until the chosen day arrives.
 */
@OptIn(ExperimentalMaterial3Api::class)
private object PastOrTodayDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= System.currentTimeMillis()
    override fun isSelectableYear(year: Int): Boolean =
        year <= java.time.Year.now().value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    container: AppContainer,
    onDone: () -> Unit
) {
    val vm: OnboardingViewModel = viewModel(
        factory = viewModelFactoryOf {
            OnboardingViewModel(container.settingsRepository, container.addictionRepository)
        }
    )

    var step by rememberSaveable { mutableIntStateOf(0) }
    var language by rememberSaveable { mutableStateOf(AppLanguage.SYSTEM) }
    var type by rememberSaveable { mutableStateOf(AddictionType.ALCOHOL) }
    var name by rememberSaveable { mutableStateOf("") }
    var costPerDay by rememberSaveable { mutableStateOf("") }
    var unitPerDay by rememberSaveable { mutableStateOf("") }
    var unitLabel by rememberSaveable { mutableStateOf("") }
    var startDateMs by rememberSaveable { mutableLongStateOf(OnboardingViewModel.defaultStart().toEpochMilliseconds()) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Always schedule when user opted in: WorkManager will fire and the OS will
        // drop the notification silently if permission was refused. If the user later
        // grants it, the workers are already running.
        if (granted) {
            ReminderScheduler.schedule(context, 8, 0, 21, 0)
        }
    }

    val submit: () -> Unit = {
        vm.finish(
            language = language,
            addictionType = type,
            addictionName = name.ifBlank { context.getString(type.labelRes) },
            startDate = Instant.fromEpochMilliseconds(startDateMs),
            costPerDay = costPerDay.replace(',', '.').toDoubleOrNull(),
            unitPerDay = unitPerDay.replace(',', '.').toDoubleOrNull(),
            unitLabel = unitLabel.ifBlank { null },
            onDone = onDone
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(220)) togetherWith fadeOut(tween(120)))
            },
            label = "onboarding-steps",
            modifier = Modifier.weight(1f)
        ) { current ->
            when (current) {
                0 -> LanguageStep(
                    selected = language,
                    onSelect = {
                        language = it
                        LocaleManager.apply(it)
                        vm.setLanguage(it)
                    }
                )
                1 -> WelcomeStep()
                2 -> AddictionStep(
                    type = type,
                    onTypeSelected = { type = it },
                    name = name,
                    onNameChange = { name = it },
                    startDateMs = startDateMs,
                    onStartDateChange = { startDateMs = it },
                    costPerDay = costPerDay,
                    onCostChange = { costPerDay = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    unitPerDay = unitPerDay,
                    onUnitChange = { unitPerDay = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    unitLabel = unitLabel,
                    onUnitLabelChange = { unitLabel = it },
                    onSkipOptional = {
                        // Skip optional financial fields: clear them and jump to next step.
                        costPerDay = ""
                        unitPerDay = ""
                        unitLabel = ""
                        step = 3
                    }
                )
                3 -> NotifStep(
                    onEnable = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // Scheduling happens inside the launcher callback (see above).
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            // No runtime permission on API < 33 → schedule immediately.
                            ReminderScheduler.schedule(context, 8, 0, 21, 0)
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { if (step > 0) step-- },
                enabled = step > 0
            ) { Text(stringResource(R.string.action_back)) }

            StepIndicator(step = step, count = 4)

            Button(
                onClick = {
                    if (step < 3) step++ else submit()
                },
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text(
                    if (step < 3) stringResource(R.string.action_next)
                    else stringResource(R.string.onboarding_finish)
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int, count: Int) {
    val description = stringResource(R.string.cd_step_indicator, step + 1, count)
    Row(
        modifier = Modifier.semantics { contentDescription = description }
    ) {
        repeat(count) { i ->
            val active = i <= step
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(if (active) 20.dp else 8.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Clairjour",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Composable
private fun LanguageStep(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Text(
            stringResource(R.string.onboarding_language_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_language_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        AppLanguage.entries.forEach { lang ->
            FilterChip(
                selected = selected == lang,
                onClick = { onSelect(lang) },
                label = {
                    Text(
                        when (lang) {
                            AppLanguage.SYSTEM -> stringResource(R.string.settings_theme_system)
                            AppLanguage.ENGLISH -> "English"
                            AppLanguage.FRENCH -> "Français"
                        }
                    )
                },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddictionStep(
    type: AddictionType,
    onTypeSelected: (AddictionType) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    startDateMs: Long,
    onStartDateChange: (Long) -> Unit,
    costPerDay: String,
    onCostChange: (String) -> Unit,
    unitPerDay: String,
    onUnitChange: (String) -> Unit,
    unitLabel: String,
    onUnitLabelChange: (String) -> Unit,
    onSkipOptional: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val optionalSuffix = " " + stringResource(R.string.field_optional_suffix)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDateMs,
            selectableDates = PastOrTodayDates
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let(onStartDateChange)
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_done)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Text(
            stringResource(R.string.onboarding_addiction_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_addiction_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AddictionType.entries) { t ->
                FilterChip(
                    selected = type == t,
                    onClick = { onTypeSelected(t) },
                    label = { Text(stringResource(t.labelRes)) }
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.addiction_edit_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        // "Sober since" date picker: matters most for people who joined already sober.
        Surface(
            onClick = { showDatePicker = true },
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.onboarding_start_date_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val formatted = remember(startDateMs) {
                        val date = java.time.Instant.ofEpochMilli(startDateMs)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                    }
                    Text(formatted, style = MaterialTheme.typography.bodyLarge)
                }
                TextButton(onClick = { showDatePicker = true }) {
                    Text(stringResource(R.string.action_edit))
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = costPerDay,
            onValueChange = onCostChange,
            label = { Text(stringResource(R.string.addiction_edit_cost) + optionalSuffix) },
            prefix = { Text("€ ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = unitPerDay,
            onValueChange = onUnitChange,
            label = { Text(stringResource(R.string.addiction_edit_units) + optionalSuffix) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = unitLabel,
            onValueChange = onUnitLabelChange,
            label = { Text(stringResource(R.string.addiction_edit_units_label) + optionalSuffix) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = onSkipOptional,
            modifier = Modifier.align(Alignment.End)
        ) { Text(stringResource(R.string.onboarding_configure_later)) }
    }
}

@Composable
private fun NotifStep(onEnable: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.onboarding_notif_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_notif_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onEnable,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.onboarding_notif_enable), fontWeight = FontWeight.SemiBold) }
    }
}
