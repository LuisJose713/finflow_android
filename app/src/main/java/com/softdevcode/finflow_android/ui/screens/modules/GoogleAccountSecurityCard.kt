package com.softdevcode.finflow_android.ui.screens.modules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.data.remote.api.GoogleLinkStatus
import com.softdevcode.finflow_android.ui.screens.auth.GoogleAuthButton
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class GoogleLinkViewModel @Inject constructor(private val api: FinFlowApi) : ViewModel() {
    var linked by mutableStateOf(false)
        private set
    var email by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf("")

    fun refresh() {
        viewModelScope.launch {
            try {
                val result = api.googleStatus()
                linked = result.linked
                email = result.email
            } catch (_: Exception) { error = "No fue posible consultar Google." }
        }
    }
    fun link(idToken: String) {
        if (busy || idToken.length > 8192) return
        busy = true; error = ""
        viewModelScope.launch {
            try {
                val result = api.googleLink(mapOf("idToken" to idToken))
                linked = result.linked
                email = result.email
            } catch (_: Exception) {
                error = "No se vinculó Google. Debes usar el mismo correo de FinFlow."
            } finally { busy = false }
        }
    }
    fun unlink() {
        if (busy) return
        busy = true; error = ""
        viewModelScope.launch {
            try {
                val result = api.googleUnlink()
                if (result.isSuccessful) { linked = false; email = "" }
                else error = "No pudimos desvincular Google."
            } catch (_: Exception) { error = "No pudimos desvincular Google." }
            finally { busy = false }
        }
    }
}

@Composable
fun GoogleAccountSecurityCard(vm: GoogleLinkViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.refresh() }
    var confirmingUnlink by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("CONTINUAR CON GOOGLE", style = MaterialTheme.typography.labelSmall)
            Text("Usa el mismo correo de tu cuenta FinFlow. No se alteran tus movimientos ni credenciales.",
                style = MaterialTheme.typography.bodySmall)
            if (vm.linked) {
                Text("Google vinculado · " + vm.email, color = MaterialTheme.colorScheme.primary)
                OutlinedButton(onClick = { confirmingUnlink = true }, enabled = !vm.busy) {
                    Text("Desvincular Google")
                }
            } else {
                GoogleAuthButton(
                    enabled = !vm.busy,
                    onCredential = vm::link,
                    onError = { vm.error = it },
                )
            }
            if (vm.error.isNotEmpty()) Text(vm.error, color = MaterialTheme.colorScheme.error)
        }
    }
    if (confirmingUnlink) {
        AlertDialog(
            onDismissRequest = { confirmingUnlink = false },
            title = { Text("Desvincular Google") },
            text = { Text("Seguirás pudiendo entrar con tu contraseña de FinFlow.") },
            confirmButton = {
                TextButton(onClick = { confirmingUnlink = false; vm.unlink() }) {
                    Text("Desvincular")
                }
            },
            dismissButton = { TextButton(onClick = { confirmingUnlink = false }) { Text("Cancelar") } },
        )
    }
}
