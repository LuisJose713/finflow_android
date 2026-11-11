package com.softdevcode.finflow_android.ui.updates

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softdevcode.finflow_android.BuildConfig
import com.softdevcode.finflow_android.ui.theme.FinFlowV4Card
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** One activity-scoped host, so checks and prompts survive switching tabs. */
@Composable
fun AppUpdateHost(viewModel: AppUpdateViewModel) {
    val owner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(owner, viewModel) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                viewModel.check()
                delay(15 * 60_000L)
            }
        }
    }

    val update = state.available
    if (state.showDialog && update != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Hay una nueva versión de FinFlow") },
            text = {
                Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Versión ${update.versionName}", fontWeight = FontWeight.Bold)
                    Text(update.releaseNotes.ifBlank { "Mejoras y correcciones para tu experiencia en FinFlow." })
                    Text("Descarga la APK y ábrela para actualizar. Android te pedirá confirmar la instalación.",
                        style = MaterialTheme.typography.bodySmall)
                    state.downloadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl)))
                        viewModel.dismiss()
                    } catch (e: ActivityNotFoundException) {
                        viewModel.downloadFailed()
                    } catch (e: SecurityException) {
                        viewModel.downloadFailed()
                    }
                }) { Text("Descargar actualización") }
            },
            dismissButton = { TextButton(onClick = viewModel::dismiss) { Text("Ahora no") } },
        )
    }
}

@Composable
fun AppUpdatesSection(viewModel: AppUpdateViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FinFlowV4Card {
        Text("FinFlow para Android", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Versión instalada: ${BuildConfig.VERSION_NAME}")
        Text("Te avisaremos al abrir la app cuando haya una nueva APK publicada.")
        state.available?.let {
            Text("Disponible: ${it.versionName}", fontWeight = FontWeight.Bold)
            Button(onClick = viewModel::showUpdate) { Text("Ver actualización") }
        }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        if (state.checking) LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedButton(onClick = { viewModel.check(manual = true) }, enabled = !state.checking) {
            Text(if (state.checking) "Buscando…" else "Buscar actualizaciones")
        }
    }
}
