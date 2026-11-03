package com.clairjour.app.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.clairjour.app.MainActivity
import com.clairjour.app.R

private const val PLEDGE_NOTIF_ID = 1001
private const val JOURNAL_NOTIF_ID = 1002

class PledgeReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        showReminder(
            applicationContext,
            NotificationChannels.PLEDGE,
            applicationContext.getString(R.string.notif_pledge_title),
            applicationContext.getString(R.string.notif_pledge_body),
            PLEDGE_NOTIF_ID
        )
        return Result.success()
    }
}

class JournalReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        showReminder(
            applicationContext,
            NotificationChannels.JOURNAL,
            applicationContext.getString(R.string.notif_journal_title),
            applicationContext.getString(R.string.notif_journal_body),
            JOURNAL_NOTIF_ID
        )
        return Result.success()
    }
}

internal fun showReminder(context: Context, channelId: String, title: String, body: String, notifId: Int) {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pending = PendingIntent.getActivity(
        context, notifId, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Public version shown on the lockscreen: no personal content, just app identity.
    val publicVersion = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(context.getString(R.string.app_name))
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .build()

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(body)
        .setContentIntent(pending)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        .setPublicVersion(publicVersion)
        .build()

    NotificationManagerCompat.from(context).notify(notifId, notification)
}
