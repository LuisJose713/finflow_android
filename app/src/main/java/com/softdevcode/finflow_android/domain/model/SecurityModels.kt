package com.softdevcode.finflow_android.domain.model

data class ChangeFinFlowPasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String,
)
data class ChangeFinFlowEmailRequest(
    val currentPassword: String,
    val newEmail: String,
    val confirmEmail: String,
)
data class ConfirmFinFlowPasswordRequest(val password: String)
