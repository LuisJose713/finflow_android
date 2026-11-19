package com.softdevcode.finflow_android.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.softdevcode.finflow_android.MainActivity
import com.softdevcode.finflow_android.R
import com.softdevcode.finflow_android.data.local.TokenManager
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.domain.model.NotificationDevice
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Authenticated inbox synchronization. Android may defer background work for battery saving. */
object NativeMessages {
    private val syncLock = Mutex()
    private const val WORK = "finflow-admin-messages"
    private const val CHANNEL = "finflow_admin_messages"
    private fun prefs(context: Context) = context.getSharedPreferences("finflow_message_preferences", Context.MODE_PRIVATE)
    @Synchronized fun deviceId(context: Context): String {
        val preferences = prefs(context)
        return preferences.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            preferences.edit().putString("device_id", it).commit()
        }
    }
    fun enabled(context: Context, owner: Int): Boolean = prefs(context).getBoolean("enabled_$owner", false)

    /** Check the system permission AND this app's messages channel. */
    fun canNotify(context: Context): Boolean {
        if (!DailyReminders.notificationPermission(context)) return false
        if (Build.VERSION.SDK_INT < 26) return true
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "Mensajes de FinFlow", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    /** Verifies Android display permissions locally, without contacting users or scheduling campaigns. */
    fun showLocalTest(context: Context): Boolean {
        createChannel(context)
        if (!canNotify(context)) return false
        val tap = PendingIntent.getActivity(context, 3030,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.finflow_brand_symbol)
            .setContentTitle("FinFlow · Prueba de notificación")
            .setContentText("Tu teléfono puede mostrar los avisos de FinFlow.")
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        context.getSystemService(NotificationManager::class.java).notify("finflow-local-test", 3030, notification)
        return true
    }

    fun bindSession(context: Context, owner: Int?) {
        val previous = prefs(context).getInt("active_owner", -1)
        if (previous != owner) {
            // Remove this app's account-scoped messages when switching owners.
            val manager = context.getSystemService(NotificationManager::class.java)
            prefs(context).all.keys.filter { it.startsWith("posted_${previous}_") }.forEach {
                manager.cancel("finflow-message-$previous", it.removePrefix("posted_${previous}_").hashCode())
            }
        }
        prefs(context).edit().putInt("active_owner", owner ?: -1).apply()
        if (owner == null || !enabled(context, owner) || !canNotify(context)) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK)
            WorkManager.getInstance(context).cancelUniqueWork("$WORK-now")
            return
        }
        val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<NativeMessageWorker>(15, TimeUnit.MINUTES).setConstraints(network).build())
        WorkManager.getInstance(context).enqueueUniqueWork("$WORK-now", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<NativeMessageWorker>().setConstraints(network).build())
    }
    fun setEnabled(context: Context, owner: Int, enabled: Boolean) {
        prefs(context).edit().putBoolean("enabled_$owner", enabled).apply()
        DailyReminders.setEnabled(context, enabled)
        bindSession(context, owner)
    }
    suspend fun sync(context: Context, api: FinFlowApi, tokens: TokenManager) {
        if (!syncLock.tryLock()) return
        try {
        val owner = tokens.userId.first() ?: return
        if (prefs(context).getInt("active_owner", -1) != owner || !enabled(context, owner)
            || !canNotify(context) || tokens.getTokenOnce() == null) return
        val device = deviceId(context)
        api.setNotificationDevice(NotificationDevice(device, enabled = true))
        val messages = api.getNotificationMessages(device)
        createChannel(context)
        if (!canNotify(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        for (message in messages) {
            if (tokens.userId.first() != owner || prefs(context).getInt("active_owner", -1) != owner
                || !enabled(context, owner) || !canNotify(context)) return
            // Store IDs only, never message text or credentials in preferences.
            val key = "posted_${owner}_${message.id}"
            if (!prefs(context).contains(key)) {
                val tap = PendingIntent.getActivity(context, message.id.hashCode(),
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                manager.notify("finflow-message-$owner", message.id.hashCode(),
                    NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.finflow_brand_symbol)
                        .setContentTitle(message.title).setContentText(message.body)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
                        .setContentIntent(tap).setAutoCancel(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build())
                prefs(context).edit().putLong(key, System.currentTimeMillis()).commit()
            }
            api.acknowledgeNotification(message.id, device)
        }
        } finally { syncLock.unlock() }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NativeMessageDependencies {
    fun api(): FinFlowApi
    fun tokenManager(): TokenManager
}

class NativeMessageWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        val dependencies = EntryPointAccessors.fromApplication(applicationContext, NativeMessageDependencies::class.java)
        NativeMessages.sync(applicationContext, dependencies.api(), dependencies.tokenManager())
        Result.success()
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: Exception) {
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
