package com.aldrenstudios.selfreign.util

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aldrenstudios.selfreign.R
import java.util.concurrent.TimeUnit

/**
 * Posts a daily encouragement notification containing the day's motivational quote.
 * Scheduling is handled by [schedule]/[cancel] and toggled from Settings.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // On Android 13+ we must hold POST_NOTIFICATIONS to show anything.
        val granted = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        // Pre-13 the permission constant is auto-granted, so treat that as allowed.
        if (!granted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return Result.success()
        }

        val quote = Quotes.forDay()
        // Make sure the channel exists (cheap + idempotent) before posting.
        Notifications.createChannel(applicationContext)
        val notification = NotificationCompat.Builder(applicationContext, Notifications.CHANNEL_ID)
            // Use a dedicated, simple monochrome icon. A large adaptive-icon
            // foreground here can make the system reject the notification and
            // crash the app with "Bad notification posted".
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(quote)
            .setStyle(NotificationCompat.BigTextStyle().bigText(quote))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // ActivityCompat check keeps lint happy; result already validated above.
        if (ActivityCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED ||
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            // Posting can still fail on some OEMs; never let it bubble up and crash.
            runCatching {
                val manager = applicationContext.getSystemService(NotificationManager::class.java)
                manager?.notify(NOTIFICATION_ID, notification)
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_encouragement"
        private const val NOTIFICATION_ID = 1001

        /** Schedules a roughly-daily reminder. Idempotent: keeps the existing schedule. */
        fun schedule(context: Context) {
            // Guard against any WorkManager/initialization failure so toggling the
            // setting can never crash the app.
            runCatching {
                val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                    // Don't fire the instant the user enables it; first nudge comes later.
                    .setInitialDelay(3, TimeUnit.HOURS)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }

        fun cancel(context: Context) {
            runCatching { WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME) }
        }
    }
}
