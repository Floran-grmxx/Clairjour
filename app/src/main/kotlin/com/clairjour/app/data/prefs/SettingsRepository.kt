package com.clairjour.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.clairjour.app.shield.ShieldRule
import com.clairjour.app.shield.ShieldSettings
import com.clairjour.app.shield.ShieldSettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppLanguage(val tag: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    FRENCH("fr");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clairjour_prefs")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val themeMode = stringPreferencesKey("theme_mode")
        val language = stringPreferencesKey("language")
        val notifPledgeEnabled = booleanPreferencesKey("notif_pledge_enabled")
        val notifJournalEnabled = booleanPreferencesKey("notif_journal_enabled")
        val notifPledgeHour = intPreferencesKey("notif_pledge_hour")
        val notifPledgeMinute = intPreferencesKey("notif_pledge_minute")
        val notifJournalHour = intPreferencesKey("notif_journal_hour")
        val notifJournalMinute = intPreferencesKey("notif_journal_minute")
        val shieldEnabled = booleanPreferencesKey("shield_enabled")
        val shieldDiagnostic = booleanPreferencesKey("shield_diagnostic")
        val shieldDisclosureAccepted = booleanPreferencesKey("shield_disclosure_accepted")
        fun shieldRule(rule: ShieldRule) = booleanPreferencesKey("shield_rule_${rule.key}")
    }

    val onboardingDoneFlow: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.onboardingDone] ?: false
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[Keys.themeMode] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map {
        AppLanguage.fromTag(it[Keys.language])
    }

    val notifPledgeEnabledFlow: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.notifPledgeEnabled] ?: true
    }

    val notifJournalEnabledFlow: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.notifJournalEnabled] ?: true
    }

    val notifPledgeTimeFlow: Flow<Pair<Int, Int>> = context.dataStore.data.map {
        (it[Keys.notifPledgeHour] ?: 8) to (it[Keys.notifPledgeMinute] ?: 0)
    }

    val notifJournalTimeFlow: Flow<Pair<Int, Int>> = context.dataStore.data.map {
        (it[Keys.notifJournalHour] ?: 21) to (it[Keys.notifJournalMinute] ?: 0)
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.onboardingDone] = done }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.language] = language.tag }
    }

    suspend fun setNotifPledgeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.notifPledgeEnabled] = enabled }
    }

    suspend fun setNotifJournalEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.notifJournalEnabled] = enabled }
    }

    suspend fun setNotifPledgeTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.notifPledgeHour] = hour
            it[Keys.notifPledgeMinute] = minute
        }
    }

    suspend fun setNotifJournalTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.notifJournalHour] = hour
            it[Keys.notifJournalMinute] = minute
        }
    }

    private val shieldStore = ShieldSettingsStore.get(context)

    /**
     * Shield settings live in their own multi-process store, read by the ":shield" process.
     * The first read copies the values formerly kept in these app preferences.
     */
    val shieldSettingsFlow: Flow<ShieldSettings> = flow {
        shieldStore.migrateOnce(::legacyShieldSettings)
        emitAll(shieldStore.settings)
    }

    private suspend fun legacyShieldSettings(): ShieldSettings? {
        val preferences = context.dataStore.data.first()
        val hasLegacyValues = preferences.asMap().keys.any { it.name.startsWith(LEGACY_SHIELD_KEY_PREFIX) }
        if (!hasLegacyValues) return null
        return ShieldSettings(
            enabled = preferences[Keys.shieldEnabled] ?: true,
            rules = ShieldRule.entries.filter {
                preferences[Keys.shieldRule(it)] ?: it.enabledByDefault
            }.toSet(),
            diagnostic = preferences[Keys.shieldDiagnostic] ?: false,
            disclosureAccepted = preferences[Keys.shieldDisclosureAccepted] ?: false
        )
    }

    suspend fun setShieldEnabled(enabled: Boolean) = shieldStore.setEnabled(enabled)

    suspend fun setShieldRule(rule: ShieldRule, enabled: Boolean) = shieldStore.setRule(rule, enabled)

    suspend fun setShieldDiagnostic(enabled: Boolean) = shieldStore.setDiagnostic(enabled)

    suspend fun setShieldDisclosureAccepted() = shieldStore.setDisclosureAccepted()

    private companion object {
        const val LEGACY_SHIELD_KEY_PREFIX = "shield_"
    }
}
