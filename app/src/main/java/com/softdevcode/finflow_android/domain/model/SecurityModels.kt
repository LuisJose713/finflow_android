package com.softdevcode.finflow_android.domain.model

data class ChangeFinFlowPasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String,
)
data class ConfirmFinFlowPasswordRequest(val password: String)
