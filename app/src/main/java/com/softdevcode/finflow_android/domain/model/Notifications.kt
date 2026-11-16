package com.softdevcode.finflow_android.domain.model

data class NotificationDevice(val deviceId: String, val platform: String = "ANDROID", val enabled: Boolean)
data class ScheduledMessage(val id: String, val title: String, val body: String, val scheduledAt: String)
data class NotificationSchedule(val title: String, val body: String, val audience: String, val scheduledAt: String)
data class NotificationCampaign(
    val id: String, val title: String, val body: String, val audience: String, val scheduledAt: String,
    val status: String, val acceptedWeb: Int, val failedWeb: Int, val nativeDevices: Int,
)
data class NotificationCapabilities(val webReady: Boolean, val timezone: String, val webSubscriptions: Int, val nativeDevices: Int)
data class NotificationCampaignList(val capabilities: NotificationCapabilities, val campaigns: List<NotificationCampaign>)
