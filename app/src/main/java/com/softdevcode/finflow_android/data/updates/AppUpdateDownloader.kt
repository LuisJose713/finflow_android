package com.softdevcode.finflow_android.data.updates

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.softdevcode.finflow_android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Uses a public client: financial credentials are never sent to GitHub. */
@Singleton
class AppUpdateDownloader @Inject constructor(@ApplicationContext private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.MINUTES)
        .followSslRedirects(false)
        .build()

    suspend fun download(update: AppUpdate, progress: (Int?) -> Unit): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val temporary = File(directory, "finflow-${update.versionCode}.part")
        val target = File(directory, "finflow-${update.versionCode}.apk")
        try {
            // Remove previous update files, never application data or session storage.
            directory.listFiles()?.filter { it != temporary }?.forEach { it.delete() }
            val request = Request.Builder().url(update.downloadUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("No pudimos descargar la actualización (${response.code}).")
                val finalUrl = response.request.url
                if (finalUrl.scheme != "https" || finalUrl.host !in setOf(
                    "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com",
                )) throw IOException("El servidor de descarga no es válido.")
                val body = response.body ?: throw IOException("La descarga está vacía.")
                val length = body.contentLength()
                if (length > MAX_UPDATE_APK_BYTES || (length >= 0 && update.sizeBytes != null && length != update.sizeBytes))
                    throw IOException("El tamaño de la actualización no coincide.")
                val total = update.sizeBytes ?: length
                val digest = MessageDigest.getInstance("SHA-256")
                var written = 0L
                var previousProgress = -2
                body.byteStream().use { input ->
                    temporary.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            written += count
                            if (written > MAX_UPDATE_APK_BYTES) throw IOException("La descarga supera el tamaño permitido.")
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            val value = if (total > 0) ((written * 100) / total).toInt().coerceIn(0, 100) else -1
                            if (value != previousProgress) {
                                progress(value.takeIf { it >= 0 })
                                previousProgress = value
                            }
                        }
                    }
                }
                verifyDownloadedUpdate(update, written, digest.digest().toHex(), length)
            }
            validatePackage(temporary, update)
            if (!temporary.renameTo(target)) throw IOException("No pudimos guardar la actualización. Reintenta.")
            target
        } finally {
            temporary.delete()
        }
    }

    @Suppress("DEPRECATION")
    private fun validatePackage(file: File, update: AppUpdate) {
        val manager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = manager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw IOException("La actualización no es una APK válida.")
        if (archive.packageName != BuildConfig.APPLICATION_ID ||
            PackageInfoCompat.getLongVersionCode(archive) != update.versionCode.toLong() ||
            PackageInfoCompat.getLongVersionCode(archive) <= BuildConfig.VERSION_CODE) {
            throw IOException("La actualización no corresponde a esta versión de FinFlow.")
        }
        val installed = manager.getPackageInfo(context.packageName, flags)
        val installedSigners = signatures(installed)
        val archiveSigners = signatures(archive)
        if (installedSigners.isEmpty() || installedSigners != archiveSigners) {
            throw IOException("La actualización usa otra firma. Debe publicarse con la misma clave que la app instalada.")
        }
    }

    @Suppress("DEPRECATION")
    private fun signatures(info: PackageInfo): Set<String> {
        val signers = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        return signers?.map { it.toByteArray().toHex() }?.toSet().orEmpty()
    }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

internal fun verifyDownloadedUpdate(update: AppUpdate, bytes: Long, sha256: String, contentLength: Long) {
    if (bytes <= 0 || bytes > MAX_UPDATE_APK_BYTES ||
        (contentLength >= 0 && bytes != contentLength) ||
        (update.sizeBytes != null && bytes != update.sizeBytes)) {
        throw IOException("La descarga quedó incompleta. Vuelve a intentarlo.")
    }
    if (update.sha256 != null && update.sha256 != sha256) {
        throw IOException("No pudimos verificar la actualización. Vuelve a descargarla.")
    }
}
