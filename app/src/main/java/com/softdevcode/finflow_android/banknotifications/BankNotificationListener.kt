package com.softdevcode.finflow_android.banknotifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification

/**
 * Privacy-first listener foundation. Disabled by default: no bank packages are
 * trusted until the user explicitly selects them in a future consent screen.
 * Never persist, log or transmit raw notification text.
 */
class BankNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val preferences = getSharedPreferences("bank_notification_consent", MODE_PRIVATE)
        if (!preferences.getBoolean("enabled", false)) return
        val allowedPackages = preferences.getStringSet("allowed_packages", emptySet()) ?: return
        if (notification.packageName !in allowedPackages) return

        // Intentionally do not parse or save financial information yet.
        // Next steps: user-facing consent UI, strict parser tests, local preview,
        // deduplication, and explicit confirmation before backend writes.
        val extras = notification.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && body.isBlank()) return
    }
}
