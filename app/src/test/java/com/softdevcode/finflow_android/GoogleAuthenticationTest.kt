package com.softdevcode.finflow_android

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.di.publicAuthPaths
import com.softdevcode.finflow_android.domain.model.GoogleRegisterRequest
import com.softdevcode.finflow_android.util.authErrorMessage
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import java.io.IOException
import java.util.concurrent.TimeUnit

class GoogleAuthenticationTest {
    private val fallback = "No se pudo completar el acceso."

    private fun error(status: Int, body: String) = HttpException(
        Response.error<Unit>(status, body.toResponseBody("application/json".toMediaType())),
    )

    @Test fun aCleanBuildIncludesThePublicGoogleClientId() {
        assertTrue(BuildConfig.GOOGLE_WEB_CLIENT_ID.matches(
            Regex("^[A-Za-z0-9._-]+\\.apps\\.googleusercontent\\.com$"),
        ))
    }

    @Test fun passwordRegistrationExplainsEachConflict() {
        for (message in listOf("El email ya está registrado", "El nombre de usuario ya está en uso")) {
            val body = Gson().toJson(mapOf("error" to message))
            assertEquals(message, authErrorMessage(error(409, body), fallback))
        }
    }

    @Test fun googleRegistrationAndAccountLinkingAreDifferentConflicts() {
        val registration = authErrorMessage(error(409,
            """{"code":"GOOGLE_REGISTRATION_REQUIRED","error":"Registration required"}"""), fallback)
        val linking = authErrorMessage(error(409,
            """{"code":"GOOGLE_LINK_REQUIRED","error":"Link required"}"""), fallback)
        assertTrue(registration.contains("Regístrate"))
        assertFalse(registration.contains("vincula"))
        assertTrue(linking.contains("vincula Google desde Seguridad"))
    }

    @Test fun htmlAndInternalServerErrorsAreNeverDisplayed() {
        assertEquals(fallback, authErrorMessage(error(500,
            """{"error":"database password internal details"}"""), fallback))
        assertEquals(fallback, authErrorMessage(error(502, "<html>Bad gateway</html>"), fallback))
        assertFalse(authErrorMessage(error(409, "not json"), fallback).contains("HTTP"))
        assertEquals(fallback, authErrorMessage(error(400, """{"error":{"unexpected":true}}"""), fallback))
    }

    @Test fun networkAndRateLimitFailuresGiveUsefulInstructions() {
        assertTrue(authErrorMessage(IOException("internal host"), fallback).contains("conexión"))
        assertTrue(authErrorMessage(error(429, "{}"), fallback).contains("Demasiados intentos"))
    }

    @Test fun googleRegistrationUsesItsOwnEndpointAndPreservesExplicitConsent() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(
                """{"token":"test-session","refreshToken":"test-refresh",
                    "user":{"id":8,"email":"test@example.com","name":"Ana","role":"USER"}}"""))
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build().create(FinFlowApi::class.java)
            val response = api.googleRegister(GoogleRegisterRequest(
                idToken = "test-google-token", privacyAccepted = true,
                privacyVersion = "published-test-version", marketingConsent = false,
            ))
            assertEquals(8, response.user.id)
            assertEquals("test-refresh", response.refreshToken)
            val request = server.takeRequest(1, TimeUnit.SECONDS)!!
            assertEquals("/auth/google/register", request.path)
            assertEquals("POST", request.method)
            val json = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals("test-google-token", json["idToken"].asString)
            assertTrue(json["privacyAccepted"].asBoolean)
            assertEquals("published-test-version", json["privacyVersion"].asString)
            assertFalse(json["marketingConsent"].asBoolean)
            assertFalse(json.has("password"))
            assertFalse(json.has("email"))
            assertFalse(json.has("name"))
        } finally { server.shutdown() }
    }

    @Test fun unpublishedPolicyDoesNotInventConsent() {
        val json = JsonParser.parseString(Gson().toJson(GoogleRegisterRequest("test-token"))).asJsonObject
        assertFalse(json.has("privacyAccepted"))
        assertFalse(json.has("privacyVersion"))
        assertFalse(json.has("marketingConsent"))
    }

    @Test fun googleSignInAndRegistrationNeverUseAStoredBearerSession() {
        for (method in listOf("googleLogin", "googleRegister")) {
            val path = FinFlowApi::class.java.methods.single { it.name == method }.getAnnotation(POST::class.java).value
            assertTrue("/$path must skip bearer injection and token refresh", "/$path" in publicAuthPaths)
        }
        assertFalse("/auth/google/link" in publicAuthPaths)
    }
}
