package com.clairjour.app.ui.screen.shield

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clairjour.app.data.prefs.SettingsRepository
import com.clairjour.app.shield.ShieldRule
import com.clairjour.app.shield.ShieldServiceState
import com.clairjour.app.shield.ShieldSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShieldUiState(
    val settings: ShieldSettings = ShieldSettings(),
    val serviceState: ShieldServiceState = ShieldServiceState.OFF,
    val batteryUnrestricted: Boolean = false
) {
    val serviceRunning: Boolean get() = serviceState == ShieldServiceState.RUNNING
}

class ShieldViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val serviceState = MutableStateFlow(ShieldServiceState.OFF)
    private val batteryUnrestricted = MutableStateFlow(false)

    val state: StateFlow<ShieldUiState> = combine(
        settingsRepository.shieldSettingsFlow,
        serviceState,
        batteryUnrestricted
    ) { settings, service, battery ->
        ShieldUiState(settings, service, battery)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShieldUiState())

    fun refreshServiceStatus(state: ShieldServiceState, batteryUnrestricted: Boolean) {
        serviceState.value = state
        this.batteryUnrestricted.value = batteryUnrestricted
    }

    fun onMasterChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShieldEnabled(enabled) }
    }

    fun onRuleChange(rule: ShieldRule, enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShieldRule(rule, enabled) }
    }

    fun acceptDisclosure() {
        viewModelScope.launch { settingsRepository.setShieldDisclosureAccepted() }
    }

    fun setDiagnostic(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShieldDiagnostic(enabled) }
    }
}
