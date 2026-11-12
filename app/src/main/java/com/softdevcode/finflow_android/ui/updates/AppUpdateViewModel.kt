package com.softdevcode.finflow_android.ui.updates

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softdevcode.finflow_android.data.updates.AppUpdate
import com.softdevcode.finflow_android.data.updates.AppUpdateRepository
import com.softdevcode.finflow_android.data.updates.AppUpdateDownloader
import java.io.File
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUpdateState(
    val checking: Boolean = false,
    val available: AppUpdate? = null,
    val showDialog: Boolean = false,
    val message: String? = null,
    val downloadError: String? = null,
    val downloading: Boolean = false,
    val downloadProgress: Int? = null,
    val downloadedApk: File? = null,
)

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val repository: AppUpdateRepository,
    private val downloader: AppUpdateDownloader,
) : ViewModel() {
    private val _state = MutableStateFlow(AppUpdateState())
    val state = _state.asStateFlow()
    private var lastCheckAt: Long? = null
    private var dismissedCode: Int? = null

    fun check(manual: Boolean = false) {
        if (_state.value.checking || _state.value.downloading) return
        val now = SystemClock.elapsedRealtime()
        if (!manual && lastCheckAt?.let { now - it < 15 * 60_000L } == true) return
        lastCheckAt = now
        _state.update { it.copy(checking = true, message = null) }
        viewModelScope.launch {
            try {
                val update = repository.check()
                _state.update {
                    it.copy(available = update,
                        downloadedApk = if (update?.versionCode == it.available?.versionCode) it.downloadedApk else null,
                        showDialog = update != null && (manual || update.versionCode != dismissedCode),
                        message = if (manual && update == null) "No hay una versión más reciente publicada." else null,
                        downloadError = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Offline and server errors must never interrupt login or navigation.
                if (manual) _state.update {
                    it.copy(message = "No pudimos buscar actualizaciones. Revisa tu conexión y reintenta.")
                }
            } finally {
                _state.update { it.copy(checking = false) }
            }
        }
    }

    fun showUpdate() = _state.update { it.copy(showDialog = it.available != null, downloadError = null) }

    fun dismiss() {
        dismissedCode = _state.value.available?.versionCode
        _state.update { it.copy(showDialog = false, downloadError = null) }
    }

    fun downloadUpdate() {
        if (_state.value.downloading) return
        val update = _state.value.available ?: return
        _state.update { it.copy(downloading = true, downloadProgress = null, downloadError = null, downloadedApk = null) }
        viewModelScope.launch {
            try {
                val apk = downloader.download(update) { progress ->
                    _state.update { it.copy(downloadProgress = progress) }
                }
                _state.update { it.copy(downloadedApk = apk, showDialog = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(downloadError = e.message ?: "No pudimos descargar la actualización. Reintenta.") }
            } finally {
                _state.update { it.copy(downloading = false) }
            }
        }
    }

    fun installationFailed(message: String) = _state.update { it.copy(downloadError = message) }
}

