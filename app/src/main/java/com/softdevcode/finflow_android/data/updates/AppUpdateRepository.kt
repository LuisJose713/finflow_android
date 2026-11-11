package com.softdevcode.finflow_android.data.updates

import com.google.gson.JsonParser
import com.softdevcode.finflow_android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class AppUpdate(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
)

internal const val UPDATE_MANIFEST_URL =
    "https://github.com/LuisJose713/finflow_android/releases/latest/download/finflow-update.json"

/** Only published APK assets in our own public repository can be offered. */
internal fun parseAppUpdate(json: String, installedVersionCode: Int): AppUpdate? {
    require(json.length <= 65_536) { "Update manifest too large" }
    val root = JsonParser.parseString(json).asJsonObject
    require(root["applicationId"].asString == BuildConfig.APPLICATION_ID)
    val code = root["versionCode"].asString.toInt()
    require(code > 0)
    val name = root["versionName"].asString.trim()
    require(name.isNotEmpty() && name.length <= 80)
    val url = root["downloadUrl"].asString
    val uri = URI(url)
    require(uri.scheme == "https" && uri.host == "github.com" && uri.port == -1)
    require(uri.userInfo == null && uri.query == null && uri.fragment == null)
    val path = uri.rawPath
    require(path.startsWith("/LuisJose713/finflow_android/releases/download/"))
    require(path.removePrefix("/LuisJose713/finflow_android/releases/download/")
        .matches(Regex("[A-Za-z0-9._-]+/[A-Za-z0-9._-]+\\.apk")))
    val notes = root["releaseNotes"]?.takeUnless { it.isJsonNull }?.asString.orEmpty().take(6_000)
    return if (code > installedVersionCode) AppUpdate(code, name, url, notes) else null
}

@Singleton
class AppUpdateRepository @Inject constructor() {
    // A separate public client deliberately carries no FinFlow token or financial data.
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun check(): AppUpdate? = withContext(Dispatchers.IO) {
        fetchUpdate(client, UPDATE_MANIFEST_URL, BuildConfig.VERSION_CODE)
    }
}

internal fun fetchUpdate(client: OkHttpClient, url: String, installedVersionCode: Int): AppUpdate? {
    val request = Request.Builder().url(url).header("Accept", "application/json")
        .header("Cache-Control", "no-cache").build()
    client.newCall(request).execute().use { response ->
        // No published APK yet is an expected state, not an app failure.
        if (response.code == 404) return null
        if (!response.isSuccessful) throw IOException("Update check failed (${response.code})")
        val body = response.body ?: throw IOException("Missing update manifest")
        // Bound memory even if a release asset is accidentally replaced with a large file.
        val source = body.source()
        source.request(65_537L)
        if (source.buffer.size > 65_536L) throw IOException("Update manifest too large")
        return parseAppUpdate(source.readUtf8(), installedVersionCode)
    }
}
