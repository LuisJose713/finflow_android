package com.softdevcode.finflow_android.ui.screens.modules

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softdevcode.finflow_android.data.local.TokenManager
import com.softdevcode.finflow_android.data.notifications.DailyReminders
import com.softdevcode.finflow_android.data.notifications.NativeMessages
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.domain.model.*
import com.softdevcode.finflow_android.ui.theme.FinFlowGold
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class NotificationsState(
    val enabled: Boolean = false, val busy: Boolean = false, val error: String? = null,
    val notice: String? = null, val campaigns: NotificationCampaignList? = null,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val api: FinFlowApi, private val tokens: TokenManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val mutable = MutableStateFlow(NotificationsState())
    val state = mutable.asStateFlow()
    init {
        viewModelScope.launch {
            tokens.userId.collect { owner -> mutable.update { it.copy(enabled = owner != null &&
                NativeMessages.enabled(context, owner) && NativeMessages.canNotify(context)) } }
        }
    }
    fun permissionDenied() { mutable.update { it.copy(enabled = false, error = "Permite las notificaciones desde los ajustes del teléfono.") } }
    fun setEnabled(enabled: Boolean) {
        if (state.value.busy) return
        if (enabled && !NativeMessages.canNotify(context)) { permissionDenied(); return }
        viewModelScope.launch {
            val owner = tokens.userId.first() ?: return@launch
            mutable.update { it.copy(busy = true, error = null, notice = null) }
            if (!enabled) { NativeMessages.setEnabled(context, owner, false); mutable.update { it.copy(enabled = false) } }
            try {
                api.setNotificationDevice(NotificationDevice(NativeMessages.deviceId(context), enabled = enabled))
                if (tokens.userId.first() != owner) return@launch
                NativeMessages.setEnabled(context, owner, enabled)
                mutable.update { it.copy(enabled = enabled, notice = if (enabled) "Notificaciones activadas." else "Notificaciones desactivadas.") }
            } catch (ex: CancellationException) { throw ex }
            catch (_: Exception) { mutable.update { it.copy(error = if (enabled) "No pudimos activar las notificaciones. Revisa la conexión." else "Desactivadas en este teléfono; no pudimos confirmar el cambio con el servidor.") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun testLocalNotification() {
        if (!state.value.enabled) return
        val displayed = NativeMessages.showLocalTest(context)
        mutable.update {
            it.copy(
                notice = if (displayed) "Prueba local publicada. Comprueba las notificaciones del teléfono." else null,
                error = if (displayed) null else "El canal de mensajes está bloqueado. Revisa Ajustes > Aplicaciones > FinFlow > Notificaciones."
            )
        }
    }
    fun loadCampaigns() {
        if (state.value.busy) return
        viewModelScope.launch {
            mutable.update { it.copy(busy = true, error = null) }
            try {
                val campaigns = api.getNotificationCampaigns()
                mutable.update { it.copy(campaigns = campaigns) }
            }
            catch (ex: CancellationException) { throw ex }
            catch (_: Exception) { mutable.update { it.copy(error = "No pudimos consultar los mensajes programados.") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun schedule(input: NotificationSchedule, completed: () -> Unit) {
        if (state.value.busy) return
        viewModelScope.launch {
            mutable.update { it.copy(busy = true, error = null, notice = null) }
            try {
                api.scheduleNotification(input)
                mutable.update { it.copy(notice = "Mensaje programado.") }
                completed()
                val campaigns = api.getNotificationCampaigns()
                mutable.update { it.copy(campaigns = campaigns, notice = "Mensaje programado.") }
            } catch (ex: CancellationException) { throw ex }
            catch (_: Exception) { mutable.update { it.copy(error = if (it.notice != null) "Mensaje guardado; no pudimos actualizar la lista." else "No pudimos confirmar la programación. Actualiza la lista antes de repetir el envío.") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun cancel(id: String) {
        if (state.value.busy) return
        viewModelScope.launch {
            mutable.update { it.copy(busy = true, error = null, notice = null) }
            try {
                val result = api.cancelNotification(id)
                if (!result.isSuccessful) error("cancel rejected")
                val campaigns = api.getNotificationCampaigns()
                mutable.update { it.copy(campaigns = campaigns, notice = "Mensaje cancelado.") }
            } catch (ex: CancellationException) { throw ex }
            catch (_: Exception) { mutable.update { it.copy(error = "El mensaje ya se procesó o no pudo cancelarse.") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}

@Composable
fun NativeNotificationSettings(vm: NotificationsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setEnabled(true) else vm.permissionDenied()
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("NOTIFICACIONES", color = FinFlowGold, fontWeight = FontWeight.Bold)
            Text("Recordatorios y mensajes de FinFlow", style = MaterialTheme.typography.titleMedium)
            Text("Recibe los recordatorios de las 10:00 y las 16:00, y los mensajes que programe el administrador. Android puede retrasarlos en segundo plano por ahorro de batería.", style = MaterialTheme.typography.bodySmall)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            if (!state.enabled) Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33 && !DailyReminders.notificationPermission(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else vm.setEnabled(true)
            }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.busy) "Guardando…" else "Activar notificaciones")
            }
            if (state.enabled) Text("Notificaciones activas. Para modificar los permisos, utiliza los ajustes de Android.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = vm::testLocalNotification, enabled = state.enabled && !state.busy,
                modifier = Modifier.fillMaxWidth()) {
                Text("Probar aviso en este teléfono")
            }
            Text("La prueba confirma que Android puede mostrar avisos; los mensajes programados requieren conexión y sincronización con el servidor.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AdminNotificationScheduler(vm: NotificationsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colombia = remember { ZoneId.of("America/Bogota") }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(colombia) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf("SELF") }
    var scheduled by remember { mutableStateOf<Instant?>(null) }
    LaunchedEffect(Unit) { vm.loadCampaigns() }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("NOTIFICACIONES PROGRAMADAS", color = FinFlowGold, fontWeight = FontWeight.Bold)
            OutlinedTextField(title, { title = it.take(80) }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(body, { body = it.take(320) }, label = { Text("Mensaje") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Text("Destinatarios", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = audience == "SELF", onClick = { audience = "SELF" }, label = { Text("Solo yo") })
                FilterChip(selected = audience == "ALL", onClick = { audience = "ALL" }, label = { Text("Suscritos") })
            }
            OutlinedButton(onClick = {
                val now = ZonedDateTime.now(colombia).plusMinutes(5)
                DatePickerDialog(context, { _, year, month, day ->
                    TimePickerDialog(context, { _, hour, minute ->
                        scheduled = LocalDate.of(year, month + 1, day).atTime(hour, minute).atZone(colombia).toInstant()
                    }, now.hour, now.minute, true).show()
                }, now.year, now.monthValue - 1, now.dayOfMonth).show()
            }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(scheduled?.let { formatter.format(it) + " · Colombia" } ?: "Elegir fecha y hora · Colombia")
            }
            Text("La hora indica el inicio del envío. La recepción depende del permiso, la conexión y la batería del teléfono.", style = MaterialTheme.typography.bodySmall)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Button(onClick = {
                scheduled?.let { instant -> vm.schedule(NotificationSchedule(title.trim(), body.trim(), audience, instant.toString())) {
                    title = ""; body = ""; scheduled = null
                } }
            }, enabled = !state.busy && title.isNotBlank() && body.isNotBlank() && scheduled?.isAfter(Instant.now().plusSeconds(30)) == true,
                modifier = Modifier.fillMaxWidth()) { Text(if (state.busy) "Guardando…" else "Programar mensaje") }
            TextButton(onClick = vm::loadCampaigns, enabled = !state.busy) { Text("Actualizar programación") }
            state.campaigns?.campaigns?.forEach { message ->
                HorizontalDivider()
                Text(message.title, fontWeight = FontWeight.SemiBold)
                Text(message.body, style = MaterialTheme.typography.bodySmall)
                Text(formatter.format(Instant.parse(message.scheduledAt)) + " · " + notificationStatus(message.status), style = MaterialTheme.typography.labelSmall)
                if (message.status == "SCHEDULED") OutlinedButton(onClick = { vm.cancel(message.id) }, enabled = !state.busy) { Text("Cancelar envío") }
            }
        }
    }
}

internal fun notificationStatus(status: String): String = when (status) {
    "SCHEDULED" -> "Programado"; "SENDING" -> "Procesando"; "SENT" -> "Procesado"
    "CANCELLED" -> "Cancelado"; "FAILED" -> "Con error"; else -> status
}
