package com.softdevcode.finflow_android.data.updates

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.softdevcode.finflow_android.MainActivity
import com.softdevcode.finflow_android.R
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

private const val UPDATE_CHANNEL = "finflow_app_updates"

internal fun scheduleUpdateChecks(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(UPDATE_CHANNEL, "Actualizaciones de FinFlow", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }
    val work = PeriodicWorkRequestBuilder<AppUpdateWorker>(6, TimeUnit.HOURS)
        .setInitialDelay(15, TimeUnit.MINUTES)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork("finflow-app-updates", ExistingPeriodicWorkPolicy.KEEP, work)
}

class AppUpdateWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return Result.success()
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return Result.success()
        return try {
            val update = AppUpdateRepository().check() ?: return Result.success()
            val preferences = context.getSharedPreferences("app_update_notices", Context.MODE_PRIVATE)
            if (preferences.getInt("notified_version", 0) >= update.versionCode) return Result.success()
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pending = PendingIntent.getActivity(context, 2104, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_update_notification)
                .setContentTitle("FinFlow tiene una nueva versión")
                .setContentText("Versión " + update.versionName + ". Toca para descargarla y actualizar.")
                .setContentIntent(pending).setAutoCancel(true).build()
            manager.notify(2104, notification)
            preferences.edit().putInt("notified_version", update.versionCode).apply()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
