package com.clairjour.app.shield

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.core.Serializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/** On-disk form of the shield settings. Rules are stored by [ShieldRule.key], never by enum name. */
@Serializable
private data class StoredShieldSettings(
    val enabled: Boolean = true,
    val ruleKeys: Set<String> = ShieldRule.defaults.map { it.key }.toSet(),
    val diagnostic: Boolean = false,
    val disclosureAccepted: Boolean = false,
    val seenRunning: Boolean = false,
    val migratedFromAppPreferences: Boolean = false
) {
    fun toSettings() = ShieldSettings(
        enabled = enabled,
        rules = ShieldRule.entries.filter { it.key in ruleKeys }.toSet(),
        diagnostic = diagnostic,
        disclosureAccepted = disclosureAccepted
    )
}

private object StoredShieldSettingsSerializer : Serializer<StoredShieldSettings> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue = StoredShieldSettings()

    override suspend fun readFrom(input: InputStream): StoredShieldSettings = try {
        json.decodeFromString(StoredShieldSettings.serializer(), input.readBytes().decodeToString())
    } catch (error: SerializationException) {
        throw CorruptionException("Unreadable shield settings", error)
    }

    override suspend fun writeTo(t: StoredShieldSettings, output: OutputStream) {
        output.write(json.encodeToString(StoredShieldSettings.serializer(), t).encodeToByteArray())
    }
}

/**
 * Shield settings shared between the app process and the separate ":shield" process that runs
 * the accessibility service (so that HyperOS "clear recent apps" cannot kill it with the app).
 * A multi-process DataStore keeps both processes in sync.
 */
class ShieldSettingsStore private constructor(context: Context) {

    private val dataStore: DataStore<StoredShieldSettings> = MultiProcessDataStoreFactory.create(
        serializer = StoredShieldSettingsSerializer,
        produceFile = { File(context.filesDir, "datastore/shield_settings.json") }
    )

    val settings: Flow<ShieldSettings> = dataStore.data.map { it.toSettings() }

    val seenRunning: Flow<Boolean> = dataStore.data.map { it.seenRunning }

    suspend fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }

    suspend fun setRule(rule: ShieldRule, enabled: Boolean) = update {
        it.copy(ruleKeys = if (enabled) it.ruleKeys + rule.key else it.ruleKeys - rule.key)
    }

    suspend fun setDiagnostic(enabled: Boolean) = update { it.copy(diagnostic = enabled) }

    suspend fun setDisclosureAccepted() = update { it.copy(disclosureAccepted = true) }

    suspend fun setSeenRunning(running: Boolean) = update { it.copy(seenRunning = running) }

    /** Copies the settings once from the former single-process app preferences. */
    suspend fun migrateOnce(legacy: suspend () -> ShieldSettings?) {
        dataStore.updateData { current ->
            if (current.migratedFromAppPreferences) return@updateData current
            val old = legacy() ?: return@updateData current.copy(migratedFromAppPreferences = true)
            current.copy(
                enabled = old.enabled,
                ruleKeys = old.rules.map { it.key }.toSet(),
                diagnostic = old.diagnostic,
                disclosureAccepted = old.disclosureAccepted,
                migratedFromAppPreferences = true
            )
        }
    }

    private suspend fun update(transform: (StoredShieldSettings) -> StoredShieldSettings) {
        dataStore.updateData(transform)
    }

    companion object {
        @Volatile private var instance: ShieldSettingsStore? = null

        /** One instance per process: DataStore forbids two instances on the same file. */
        fun get(context: Context): ShieldSettingsStore =
            instance ?: synchronized(this) {
                instance ?: ShieldSettingsStore(context.applicationContext).also { instance = it }
            }
    }
}
