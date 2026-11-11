package com.softdevcode.finflow_android.ui.updates

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softdevcode.finflow_android.data.updates.AppUpdate
import com.softdevcode.finflow_android.data.updates.AppUpdateRepository
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
)

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val repository: AppUpdateRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AppUpdateState())
    val state = _state.asStateFlow()
    private var lastCheckAt: Long? = null
    private var dismissedCode: Int? = null

    fun check(manual: Boolean = false) {
        if (_state.value.checking) return
        val now = SystemClock.elapsedRealtime()
        if (!manual && lastCheckAt?.let { now - it < 15 * 60_000L } == true) return
        lastCheckAt = now
        _state.update { it.copy(checking = true, message = null) }
        viewModelScope.launch {
            try {
                val update = repository.check()
                _state.update {
                    it.copy(available = update,
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

    fun downloadFailed() = _state.update {
        it.copy(downloadError = "No pudimos abrir la descarga. Instala o habilita un navegador e inténtalo de nuevo.")
    }
}
