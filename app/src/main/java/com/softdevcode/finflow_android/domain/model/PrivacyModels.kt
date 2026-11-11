package com.softdevcode.finflow_android.domain.model

import com.google.gson.annotations.SerializedName

data class FinFlowPrivacyPolicy(
    val version: String = "",
    val effectiveDate: String = "",
    val ready: Boolean = false,
    val controllerName: String = "",
    val controllerNit: String = "",
    val contactEmail: String = "",
    val contactAddress: String = "",
    val contactPhone: String = "",
    val rights: List<String> = emptyList(),
    val marketingOptional: Boolean = true,
)

data class FinFlowPrivacyStatus(
    val accepted: Boolean = false,
    val version: String = "",
    val policyReady: Boolean = false,
    val marketingConsent: Boolean = false,
    val acceptedAt: String? = null,
)

data class FinFlowMarketingChoice(val marketingConsent: Boolean)
data class FinFlowRightsRequest(val type: String, val description: String)
data class FinFlowRightsEntry(
    val id: Int,
    @SerializedName("request_type") val requestType: String,
    val description: String? = null,
    val status: String,
    @SerializedName("created_at") val createdAt: String? = null,
)
