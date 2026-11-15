package com.softdevcode.finflow_android.ui.screens.navigation

import android.content.Intent

/**
 * Untrusted values supplied by the system assistant. These are only used to
 * prefill the authenticated confirmation screen; NEVER submit on launch.
 */
data class AssistantMovementDraft(
    val type: String,
    val title: String?,
    val amount: String?,
    val category: String?,
) {
    companion object {
        fun fromIntent(intent: Intent?): AssistantMovementDraft? {
            if (intent == null) return null
            val marker = intent.extras?.get("finflow.assistant.create")?.toString()
            if (marker != "true") return null
            fun extra(name: String, max: Int): String? =
                intent.extras?.get("finflow.assistant.$name")?.toString()
                    ?.trim()?.take(max)?.takeIf { it.isNotBlank() }
            val requestedType = extra("type", 10)
            return AssistantMovementDraft(
                type = if (requestedType == "income") "income" else "expense",
                title = extra("title", 255),
                amount = extra("amount", 32),
                category = extra("category", 80),
            )
        }
    }
}
