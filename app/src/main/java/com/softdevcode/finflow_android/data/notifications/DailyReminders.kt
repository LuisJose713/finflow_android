package com.softdevcode.finflow_android.data.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.softdevcode.finflow_android.MainActivity
import com.softdevcode.finflow_android.R
import java.util.Calendar

/**
 * Two private, local-time reminders: 10:00 and 16:00.
 * setAndAllowWhileIdle avoids Android's exact-alarm special permission;
 * delivery can be delayed by the OS's battery restrictions.
 */
object DailyReminders {
    private const val PREFS = "finflow_daily_notifications"
    private const val CHANNEL = "finflow_daily_reminders"
    private const val SLOT_10 = 1010
    private const val SLOT_16 = 1016
    const val ACTION_REMINDER = "com.softdevcode.finflow_android.DAILY_REMINDER"
    const val EXTRA_SLOT = "slot"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun wasAsked(context: Context): Boolean = prefs(context).getBoolean("asked", false)
    fun markAsked(context: Context) { prefs(context).edit().putBoolean("asked", true).apply() }

    fun notificationPermission(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun setSessionActive(context: Context, active: Boolean) {
        prefs(context).edit().putBoolean("session_active", active).apply()
        if (!active) cancel(context) else reschedule(context)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("enabled", enabled).apply()
        reschedule(context)
    }

    fun reschedule(context: Context) {
        if (!prefs(context).getBoolean("session_active", false) ||
            !prefs(context).getBoolean("enabled", false) ||
            !notificationPermission(context)) {
            cancel(context)
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Recordatorios financieros", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        scheduleSlot(context, SLOT_10, 10)
        scheduleSlot(context, SLOT_16, 16)
    }

    private fun pendingAlarm(context: Context, slot: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, slot,
            Intent(context, DailyReminderReceiver::class.java)
                .setAction(ACTION_REMINDER).putExtra(EXTRA_SLOT, slot),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun scheduleSlot(context: Context, slot: Int, hour: Int) {
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DATE, 1)
        }
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pendingAlarm(context, slot))
    }

    private fun cancel(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (slot in listOf(SLOT_10, SLOT_16)) {
            alarms.cancel(pendingAlarm(context, slot))
        }
    }

    internal fun deliver(context: Context, slot: Int) {
        if (!prefs(context).getBoolean("session_active", false) ||
            !prefs(context).getBoolean("enabled", false) ||
            !notificationPermission(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val hour = if (slot == SLOT_10) 10 else 16
        scheduleSlot(context, slot, hour)
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("finflow_reminder_destination", if (slot == SLOT_10) "transactions" else "dashboard")
        val tap = PendingIntent.getActivity(
            context, slot, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (slot == SLOT_10) "Es hora de registrar tus movimientos" else "¿Cuánto has gastado hoy?"
        val description = if (slot == SLOT_10)
            "Mantén tus finanzas al día. Registra tus ingresos y gastos."
        else "Consulta el resumen de tus gastos de hoy en FinFlow."
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.finflow_brand_symbol)
            .setContentTitle(title)
            .setContentText(description)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        manager.notify(slot, notification)
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DailyReminders.ACTION_REMINDER) {
            val slot = intent.getIntExtra(DailyReminders.EXTRA_SLOT, -1)
            if (slot == 1010 || slot == 1016) DailyReminders.deliver(context, slot)
        } else if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            DailyReminders.reschedule(context)
        }
    }
}
