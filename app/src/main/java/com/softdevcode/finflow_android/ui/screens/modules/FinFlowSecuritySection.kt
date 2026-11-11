package com.softdevcode.finflow_android.ui.screens.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.softdevcode.finflow_android.ui.theme.FinFlowGold

/** Security parity with FinFlow Web /security.
 * Re-authentication is mandatory after token-version rotation. */
@Composable
fun FinFlowSecuritySection(
    state: ModulesState,
    vm: FinFlowModulesViewModel,
    onReauthenticate: () -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var revoke by remember { mutableStateOf("") }
    var showRevokeConfirmation by remember { mutableStateOf(false) }

    Text("Protección de tu cuenta",
        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("Actualiza tus credenciales o cierra todas las sesiones activas.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)

    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("CAMBIAR CONTRASEÑA", color = FinFlowGold,
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                current, { current = it },
                label = { Text("Contraseña actual") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                next, { next = it },
                label = { Text("Nueva contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                confirm, { confirm = it },
                label = { Text("Confirmar contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Text("Mínimo 12 caracteres y máximo 72 bytes UTF-8. " +
                    "Al cambiarla, tendrás que iniciar sesión de nuevo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val valid = current.isNotEmpty() &&
                current.toByteArray(Charsets.UTF_8).size <= 72 &&
                next.length >= 12 && next.length <= 128 &&
                next.toByteArray(Charsets.UTF_8).size <= 72 &&
                next == confirm && current != next
            Button(onClick = {
                vm.changePassword(current, next, confirm, onReauthenticate)
                current = ""; next = ""; confirm = ""
            }, enabled = valid && !state.busy,
                modifier = Modifier.fillMaxWidth()) {
                Text("Cambiar contraseña")
            }
        }
    }

    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("CERRAR TODAS LAS SESIONES", color = FinFlowGold,
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text("Revoca las sesiones de FinFlow de todos tus dispositivos, incluido este.",
                style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                revoke, { revoke = it },
                label = { Text("Confirma tu contraseña actual") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = { showRevokeConfirmation = true },
                enabled = revoke.isNotEmpty() &&
                    revoke.toByteArray(Charsets.UTF_8).size <= 72 && !state.busy,
                modifier = Modifier.fillMaxWidth()) {
                Text("Revocar sesiones")
            }
        }
    }

    if (showRevokeConfirmation) {
        AlertDialog(
            onDismissRequest = { showRevokeConfirmation = false },
            title = { Text("Cerrar todas las sesiones") },
            text = { Text("Tendrás que iniciar sesión otra vez en cada dispositivo. ¿Continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    showRevokeConfirmation = false
                    vm.revokeSessions(revoke, onReauthenticate)
                    revoke = ""
                }) { Text("Sí, cerrar sesiones") }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeConfirmation = false }) { Text("Cancelar") }
            },
        )
    }
}
