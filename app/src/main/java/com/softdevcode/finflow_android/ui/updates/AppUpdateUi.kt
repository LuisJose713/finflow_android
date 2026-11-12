package com.softdevcode.finflow_android.ui.updates

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softdevcode.finflow_android.BuildConfig
import com.softdevcode.finflow_android.ui.theme.FinFlowV4Card
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

/** Activity-scoped download state survives navigation and configuration changes. */
@Composable
fun AppUpdateHost(viewModel: AppUpdateViewModel) {
    val owner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(context) {
        if (Build.VERSION.SDK_INT >= 33) {
            val preferences = context.getSharedPreferences("app_update_notices", Context.MODE_PRIVATE)
            if (!preferences.getBoolean("permission_requested", false) &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                preferences.edit().putBoolean("permission_requested", true).apply()
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val apk = viewModel.state.value.downloadedApk
        if (apk != null && canInstallUpdates(context)) installUpdate(context, apk, viewModel)
        else viewModel.installationFailed("Autoriza a FinFlow para instalar esta actualización y vuelve a pulsar Instalar.")
    }

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
                    Text("Versión " + update.versionName, fontWeight = FontWeight.Bold)
                    Text(update.releaseNotes.ifBlank { "Mejoras y correcciones para tu experiencia en FinFlow." })
                    when {
                        state.downloading -> {
                            Text("Descargando actualización" + (state.downloadProgress?.let { " · $it%" } ?: "…"))
                            val progress = state.downloadProgress
                            if (progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                            else LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                            Text("Puedes seguir usando FinFlow mientras termina la descarga.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        state.downloadedApk?.exists() == true ->
                            Text("Descarga lista. Pulsa Instalar y confirma la actualización en Android.")
                        else -> Text("Descárgala aquí. Android te pedirá confirmar la instalación.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    state.downloadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !state.downloading, onClick = {
                    val apk = state.downloadedApk?.takeIf { it.exists() }
                    if (apk == null) viewModel.downloadUpdate()
                    else if (canInstallUpdates(context)) installUpdate(context, apk, viewModel)
                    else try {
                        permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + context.packageName)))
                    } catch (e: Exception) {
                        viewModel.installationFailed("No pudimos abrir el permiso de instalación. Revisa los ajustes de Android.")
                    }
                }) {
                    Text(when {
                        state.downloading -> "Descargando…"
                        state.downloadedApk?.exists() == true -> "Instalar actualización"
                        else -> "Descargar actualización"
                    })
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismiss) {
                    Text(if (state.downloading) "Seguir usando FinFlow" else "Ahora no")
                }
            },
        )
    }
}

private fun canInstallUpdates(context: Context): Boolean =
    Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

private fun installUpdate(context: Context, apk: File, viewModel: AppUpdateViewModel) {
    try {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".updates", apk)
        context.startActivity(Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
        viewModel.dismiss()
    } catch (e: Exception) {
        viewModel.installationFailed("No pudimos abrir el instalador de Android. Vuelve a intentarlo.")
    }
}

@Composable
fun AppUpdatesSection(viewModel: AppUpdateViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FinFlowV4Card {
        Text("FinFlow para Android", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Versión instalada: " + BuildConfig.VERSION_NAME)
        Text("Te avisaremos cuando haya una nueva versión publicada. Puedes descargarla e iniciar su instalación desde aquí.")
        state.available?.let {
            Text("Disponible: " + it.versionName, fontWeight = FontWeight.Bold)
            Button(onClick = viewModel::showUpdate) {
                Text(if (state.downloading) "Ver descarga" else "Ver actualización")
            }
        }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        if (state.checking) LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedButton(onClick = { viewModel.check(manual = true) }, enabled = !state.checking && !state.downloading) {
            Text(if (state.checking) "Buscando…" else "Buscar actualizaciones")
        }
    }
}

