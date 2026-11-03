package com.clairjour.app

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.Process
import android.util.Log
import com.clairjour.app.data.AppContainer
import com.clairjour.app.data.DefaultAppContainer
import com.clairjour.app.notifications.NotificationChannels
import com.clairjour.app.shield.ShieldWatchdogWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ClairjourApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // The ":shield" process only hosts the accessibility service: no database, no scheduling.
        if (isShieldProcess()) return
        try {
            container = DefaultAppContainer(this)
            NotificationChannels.register(this)
            ShieldWatchdogWorker.schedule(this)
            // Moves the shield settings to their multi-process store right away for the ":shield" process.
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                container.settingsRepository.shieldSettingsFlow.first()
            }
        } catch (t: Throwable) {
            Log.e("Clairjour", "Fatal init error", t)
            throw t
        }
    }

    private fun isShieldProcess(): Boolean {
        val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getProcessName()
        } else {
            getSystemService(ActivityManager::class.java)?.runningAppProcesses
                ?.firstOrNull { it.pid == Process.myPid() }?.processName
        }
        return processName?.endsWith(SHIELD_PROCESS_SUFFIX) == true
    }

    private companion object {
        const val SHIELD_PROCESS_SUFFIX = ":shield"
    }
}
