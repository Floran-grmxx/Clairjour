package com.clairjour.app.shield

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.clairjour.app.R
import com.clairjour.app.notifications.NotificationChannels
import com.clairjour.app.notifications.showReminder
import kotlinx.coroutines.flow.first
import java.time.Duration

/**
 * MIUI/HyperOS can silently switch the accessibility service off. This periodic check
 * notifies the user once each time the shield, which had been running, is found stopped.
 */
class ShieldWatchdogWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = ShieldSettingsStore.get(applicationContext)
        val running = ShieldServiceStatus.state(applicationContext) == ShieldServiceState.RUNNING
        val wasRunning = store.seenRunning.first()
        val wanted = store.settings.first().enabled
        when {
            running -> if (!wasRunning) store.setSeenRunning(true)
            wasRunning -> {
                store.setSeenRunning(false)
                if (wanted) {
                    showReminder(
                        applicationContext,
                        NotificationChannels.SHIELD,
                        applicationContext.getString(R.string.notif_shield_stopped_title),
                        applicationContext.getString(R.string.notif_shield_stopped_body),
                        NOTIFICATION_ID
                    )
                }
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "clairjour_shield_watchdog"
        private const val NOTIFICATION_ID = 1003

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ShieldWatchdogWorker>(Duration.ofMinutes(15)).build()
            )
        }
    }
}
