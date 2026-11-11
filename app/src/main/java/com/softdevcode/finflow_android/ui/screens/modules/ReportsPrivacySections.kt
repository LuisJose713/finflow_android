package com.softdevcode.finflow_android.ui.screens.modules

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.softdevcode.finflow_android.ui.theme.FinFlowGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FinFlowReportsSection(state: ModulesState, vm: FinFlowModulesViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var transactionType by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var month by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    // Storage Access Framework: the user chooses exactly where each report is saved.
    // No storage permission, background file creation, or automated APK build needed.
    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) vm.exportReport(
            "pdf", transactionType.ifBlank { null },
            month.toIntOrNull(), year.toIntOrNull(), search.trim().ifBlank { null },
        ) { bytes ->
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                            ?: error("No hay acceso al archivo")
                    }
                    status = "PDF FinFlow guardado correctamente."
                } catch (e: Exception) {
                    status = "No se pudo guardar el archivo seleccionado."
                }
            }
        }
    }
    val excelPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri != null) vm.exportReport(
            "xlsx", transactionType.ifBlank { null },
            month.toIntOrNull(), year.toIntOrNull(), search.trim().ifBlank { null },
        ) { bytes ->
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                            ?: error("No hay acceso al archivo")
                    }
                    status = "Excel FinFlow guardado correctamente."
                } catch (e: Exception) {
                    status = "No se pudo guardar el archivo seleccionado."
                }
            }
        }
    }

    Text("Reportes FinFlow", fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleLarge)
    Text("El mismo PDF financiero protegido y Excel editable de la versión Web.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("FILTROS DEL INFORME", fontWeight = FontWeight.Bold,
                color = FinFlowGold, style = MaterialTheme.typography.labelSmall)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                listOf("" to "Todos", "income" to "Ingresos", "expense" to "Gastos")
                    .forEach { (key, label) ->
                        FilterChip(selected = transactionType == key,
                            onClick = { transactionType = key },
                            label = { Text(label) })
                    }
            }
            OutlinedTextField(
                year, { year = it.filter(Char::isDigit).take(4) },
                label = { Text("Año (opcional)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                month, { month = it.filter(Char::isDigit).take(2) },
                label = { Text("Mes 1–12 (opcional)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                search, { search = it.take(120) },
                label = { Text("Buscar por descripción (opcional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            val validMonth = month.isBlank() || (month.toIntOrNull()?.let { it in 1..12 } == true)
            val validYear = year.isBlank() || (year.toIntOrNull()?.let { it in 2000..2100 } == true)
            val ready = !state.busy && validMonth && validYear
            Button(
                onClick = { status = null; pdfPicker.launch("FinFlow_Movimientos.pdf") },
                enabled = ready, modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar informe PDF · solo lectura") }
            OutlinedButton(
                onClick = { status = null; excelPicker.launch("FinFlow_Movimientos.xlsx") },
                enabled = ready, modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar Excel · editable") }
            if (!validMonth || !validYear) {
                Text("Revisa el mes y año seleccionados.",
                    color = MaterialTheme.colorScheme.error)
            }
            status?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private val rightTypes = listOf(
    "ACCESS" to "Consultar", "RECTIFY" to "Rectificar",
    "DELETE" to "Eliminar", "REVOKE" to "Revocar",
)

@Composable
fun FinFlowPrivacySection(state: ModulesState, vm: FinFlowModulesViewModel) {
    val context = LocalContext.current
    val policy = state.privacyPolicy
    val status = state.privacyStatus
    var requestType by rememberSaveable { mutableStateOf("ACCESS") }
    var description by rememberSaveable { mutableStateOf("") }
    var marketingChange by remember { mutableStateOf<Boolean?>(null) }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Privacidad y tus datos", Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = { vm.refresh("privacy") }) {
            Icon(Icons.Outlined.Refresh, "Actualizar privacidad")
        }
    }
    Text("Controla tus permisos y ejerce tus derechos sobre la información personal.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)

    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TRATAMIENTO DE DATOS", fontWeight = FontWeight.Bold,
                color = FinFlowGold, style = MaterialTheme.typography.labelSmall)
            Text(policy?.controllerName ?: "Consultando responsable…",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (policy != null) {
                Text("NIT: " + policy.controllerNit)
                Text("Política: versión " + policy.version)
                Text(if (policy.ready) "Política publicada" else "Política en revisión")
                Text("Contacto: " + policy.contactEmail)
            }
            if (status != null) {
                Text(if (status.accepted)
                    "Autorización vigente registrada para la versión " + status.version
                    else "No consta autorización para la versión vigente.")
            }
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://finflow-opal-five.vercel.app/privacy")))
                }, modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Leer política completa en FinFlow Web")
            }
            Text("La lectura del documento no equivale a consentimiento. " +
                "Esta pantalla no registra autorizaciones de forma automática.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (status?.accepted == true) {
        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Comunicaciones comerciales", fontWeight = FontWeight.Bold)
                    Text("Opcional. Puedes activarlas o retirarlas.",
                        style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = status.marketingConsent, onCheckedChange = {
                    marketingChange = it
                })
            }
        }
    }
    marketingChange?.let { desired ->
        AlertDialog(
            onDismissRequest = { marketingChange = null },
            title = { Text("Confirmar preferencia") },
            text = { Text(if (desired) "¿Autorizar comunicaciones comerciales?"
                else "¿Retirar tu autorización de comunicaciones comerciales?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.setMarketingChoice(desired)
                    marketingChange = null
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { marketingChange = null }) { Text("Cancelar") }
            },
        )
    }

    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("SOLICITUD DE DERECHOS", color = FinFlowGold,
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            Text("¿Qué necesitas solicitar?", fontWeight = FontWeight.SemiBold)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                rightTypes.forEach { (key, title) ->
                    FilterChip(selected = requestType == key,
                        onClick = { requestType = key },
                        label = { Text(title) })
                }
            }
            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(500) },
                label = { Text("Describe tu solicitud") },
                minLines = 2, maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    vm.requestPrivacyRight(requestType, description.trim())
                    description = ""
                }, enabled = !state.busy && description.length <= 500,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Registrar solicitud") }
            Text("Las solicitudes quedan pendientes de revisión; no se eliminan datos automáticamente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (state.privacyRequests.isNotEmpty()) {
        Text("Mis solicitudes", fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium)
        state.privacyRequests.forEach { request ->
            Card(Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(15.dp)) {
                    Text("Solicitud #" + request.id + " · " + request.requestType,
                        fontWeight = FontWeight.SemiBold)
                    Text(request.status + " · " + (request.createdAt ?: ""),
                        style = MaterialTheme.typography.bodySmall)
                    request.description?.let { Text(it) }
                }
            }
        }
    }
}
