package com.softdevcode.finflow_android

import com.google.gson.Gson
import com.softdevcode.finflow_android.data.updates.fetchUpdate
import com.softdevcode.finflow_android.data.updates.parseAppUpdate
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class AppUpdateTest {
    private fun manifest(code: Int = 7, url: String =
        "https://github.com/LuisJose713/finflow_android/releases/download/v1.2.4/FinFlow-1.2.4.apk",
        applicationId: String = BuildConfig.APPLICATION_ID): String = Gson().toJson(mapOf(
        "applicationId" to applicationId, "versionCode" to code,
        "versionName" to "1.2.4", "downloadUrl" to url,
        "releaseNotes" to "Inicio más rápido y nuevas mejoras.",
    ))

    @Test fun onlyNewerVersionCodesTriggerAnUpdate() {
        assertNotNull(parseAppUpdate(manifest(7), 6))
        assertNull(parseAppUpdate(manifest(6), 6))
        assertNull(parseAppUpdate(manifest(5), 6))
        // Display names never cause a downgrade or suppress a real update.
        assertEquals(7, parseAppUpdate(manifest(7), 6)?.versionCode)
    }

    @Test fun rejectsWrongAppsAndUntrustedDownloads() {
        val urls = listOf(
            "http://github.com/LuisJose713/finflow_android/releases/download/v7/app.apk",
            "https://github.com.attacker.com/LuisJose713/finflow_android/releases/download/v7/app.apk",
            "https://github.com/other/repo/releases/download/v7/app.apk",
            "https://github.com/LuisJose713/finflow_android/releases/download/v7/app.zip",
            "https://github.com/LuisJose713/finflow_android/releases/download/v7/app.apk?redirect=evil",
        )
        urls.forEach { url -> assertThrows(IllegalArgumentException::class.java) { parseAppUpdate(manifest(url = url), 6) } }
        assertThrows(IllegalArgumentException::class.java) {
            parseAppUpdate(manifest(applicationId = "another.app"), 6)
        }
    }

    @Test fun publishedManifestIsFetchedWithoutAuthentication() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(manifest()))
            val update = fetchUpdate(OkHttpClient(), server.url("/finflow-update.json").toString(), 6)
            assertEquals("1.2.4", update?.versionName)
            val request = server.takeRequest()
            assertNull(request.getHeader("Authorization"))
            assertNull(request.getHeader("Cookie"))
            assertEquals("no-cache", request.getHeader("Cache-Control"))
        }
    }

    @Test fun noPublishedApkIsNotAnErrorButServerFailureIs() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(404))
            assertNull(fetchUpdate(OkHttpClient(), server.url("/").toString(), 6))
            server.enqueue(MockResponse().setResponseCode(503))
            assertThrows(IOException::class.java) { fetchUpdate(OkHttpClient(), server.url("/").toString(), 6) }
        }
    }

    @Test fun oversizedOrMalformedManifestsAreRejected() {
        assertThrows(Exception::class.java) { parseAppUpdate("not json", 6) }
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("x".repeat(70_000)))
            assertThrows(IOException::class.java) { fetchUpdate(OkHttpClient(), server.url("/").toString(), 6) }
        }
    }
}
