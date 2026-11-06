package com.clairjour.app.ui.screen.shield

import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clairjour.app.BuildConfig
import com.clairjour.app.R
import com.clairjour.app.data.AppContainer
import com.clairjour.app.shield.ShieldKeepAlive
import com.clairjour.app.shield.ShieldRule
import com.clairjour.app.shield.ShieldServiceState
import com.clairjour.app.shield.ShieldServiceStatus
import com.clairjour.app.ui.components.ClairjourSwitch
import com.clairjour.app.ui.components.viewModelFactoryOf

private val ShieldRule.titleRes: Int
    get() = when (this) {
        ShieldRule.REELS_TAB -> R.string.shield_rule_reels_tab
        ShieldRule.DIRECT_SINGLE_REEL -> R.string.shield_rule_direct_single_reel
        ShieldRule.OTHER_REELS -> R.string.shield_rule_other_reels
        ShieldRule.PROFILE_REELS -> R.string.shield_rule_profile_reels
        ShieldRule.EXPLORE_GRID -> R.string.shield_rule_explore_grid
        ShieldRule.SEARCH_ACCOUNTS_ONLY -> R.string.shield_rule_search_accounts_only
        ShieldRule.HOME_FEED -> R.string.shield_rule_home_feed
        ShieldRule.STORIES -> R.string.shield_rule_stories
    }

private val ShieldRule.bodyRes: Int
    get() = when (this) {
        ShieldRule.REELS_TAB -> R.string.shield_rule_reels_tab_body
        ShieldRule.DIRECT_SINGLE_REEL -> R.string.shield_rule_direct_single_reel_body
        ShieldRule.OTHER_REELS -> R.string.shield_rule_other_reels_body
        ShieldRule.PROFILE_REELS -> R.string.shield_rule_profile_reels_body
        ShieldRule.EXPLORE_GRID -> R.string.shield_rule_explore_grid_body
        ShieldRule.SEARCH_ACCOUNTS_ONLY -> R.string.shield_rule_search_accounts_only_body
        ShieldRule.HOME_FEED -> R.string.shield_rule_home_feed_body
        ShieldRule.STORIES -> R.string.shield_rule_stories_body
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: ShieldViewModel = viewModel(
        factory = viewModelFactoryOf { ShieldViewModel(container.settingsRepository) }
    )
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var showDisclosure by remember { mutableStateOf(false) }
    val refreshStatus = {
        vm.refreshServiceStatus(
            ShieldServiceStatus.state(context),
            ShieldServiceStatus.isIgnoringBatteryOptimizations(context),
            ShieldServiceStatus.isAutostartAllowed(context)
        )
    }

    // The user enables the service in Android settings: re-check whenever the screen comes back,
    // and live while it is shown (HyperOS can kill the service at any time).
    LifecycleResumeEffect(Unit) {
        refreshStatus()
        val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
        val listener = AccessibilityManager.AccessibilityStateChangeListener { refreshStatus() }
        accessibilityManager?.addAccessibilityStateChangeListener(listener)
        onPauseOrDispose { accessibilityManager?.removeAccessibilityStateChangeListener(listener) }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text(stringResource(R.string.shield_disclosure_title)) },
            text = { Text(stringResource(R.string.shield_disclosure_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDisclosure = false
                    vm.acceptDisclosure()
                    ShieldServiceStatus.openAccessibilitySettings(context)
                }) { Text(stringResource(R.string.shield_disclosure_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) {
                    Text(stringResource(R.string.shield_disclosure_decline))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.shield_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            StatusCard(
                serviceState = state.serviceState,
                onActivate = {
                    // The Play Store disclosure is shown once; afterwards the button goes straight to settings.
                    if (state.settings.disclosureAccepted) ShieldServiceStatus.openAccessibilitySettings(context)
                    else showDisclosure = true
                }
            )
            Spacer(Modifier.height(16.dp))

            ShieldToggleRow(
                title = stringResource(R.string.shield_master),
                body = stringResource(R.string.shield_master_body),
                checked = state.settings.enabled,
                onCheckedChange = vm::onMasterChange
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.shield_rules_section).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(6.dp))
            ShieldRule.entries.forEach { rule ->
                ShieldToggleRow(
                    title = stringResource(rule.titleRes),
                    body = stringResource(rule.bodyRes),
                    checked = rule in state.settings.rules,
                    enabled = state.settings.enabled,
                    onCheckedChange = { vm.onRuleChange(rule, it) }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.shield_reliability_section).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(8.dp))
            KeepAliveCard(
                isXiaomi = ShieldServiceStatus.isXiaomi,
                batteryUnrestricted = state.batteryUnrestricted,
                autostartAllowed = state.autostartAllowed,
                done = ShieldKeepAlive.isDone(
                    batteryUnrestricted = state.batteryUnrestricted,
                    autostartAllowed = state.autostartAllowed,
                    hasAutostartSettings = ShieldServiceStatus.hasAutostartSettings
                ),
                onAutostart = if (ShieldServiceStatus.hasAutostartSettings) {
                    { ShieldServiceStatus.openAutostartSettings(context) }
                } else null,
                onBattery = { ShieldServiceStatus.openBatterySettings(context) }
            )

            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ShieldToggleRow(
                    title = stringResource(R.string.shield_diagnostic),
                    body = stringResource(R.string.shield_diagnostic_body),
                    checked = state.settings.diagnostic,
                    onCheckedChange = vm::setDiagnostic
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun StatusCard(serviceState: ShieldServiceState, onActivate: () -> Unit) {
    val running = serviceState == ShieldServiceState.RUNNING
    val (titleRes, bodyRes) = when (serviceState) {
        ShieldServiceState.RUNNING -> R.string.shield_status_on to R.string.shield_status_on_body
        ShieldServiceState.STOPPED_BY_SYSTEM -> R.string.shield_status_stopped to R.string.shield_status_stopped_body
        ShieldServiceState.OFF -> R.string.shield_status_off to R.string.shield_status_off_body
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (running) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(bodyRes),
                style = MaterialTheme.typography.bodySmall,
                color = if (running) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!running) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onActivate, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(
                            if (serviceState == ShieldServiceState.STOPPED_BY_SYSTEM) R.string.shield_restart
                            else R.string.shield_activate
                        )
                    )
                }
            }
        }
    }
}

/** Many phones (Xiaomi first) stop accessibility services in the background unless allowed to run freely. */
@Composable
private fun KeepAliveCard(
    isXiaomi: Boolean,
    batteryUnrestricted: Boolean,
    autostartAllowed: Boolean?,
    done: Boolean,
    onAutostart: (() -> Unit)?,
    onBattery: () -> Unit
) {
    // Folded once everything is set; a missing setting always unfolds it.
    var expanded by rememberSaveable(done) { mutableStateOf(!done) }
    Surface(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.shield_keep_alive_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (done) {
                    Text(
                        stringResource(R.string.shield_keep_alive_done),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            if (!expanded) return@Column
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(
                    if (isXiaomi) R.string.shield_keep_alive_body_xiaomi else R.string.shield_keep_alive_body
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (onAutostart != null) {
                if (autostartAllowed == false) {
                    // The decisive setting: without it the shield never comes back after "clear all".
                    Button(onClick = onAutostart, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.shield_keep_alive_autostart))
                    }
                    Text(
                        stringResource(R.string.shield_keep_alive_autostart_missing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    OutlinedButton(onClick = onAutostart, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(
                                if (autostartAllowed == true) R.string.shield_keep_alive_autostart_done
                                else R.string.shield_keep_alive_autostart
                            )
                        )
                    }
                }
            }
            OutlinedButton(onClick = onBattery, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        if (batteryUnrestricted) R.string.shield_keep_alive_battery_done
                        else R.string.shield_keep_alive_battery
                    )
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.shield_keep_alive_recents),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShieldToggleRow(
    title: String,
    body: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (body != null) {
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        ClairjourSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
