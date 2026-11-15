package com.softdevcode.finflow_android.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.softdevcode.finflow_android.BuildConfig
import kotlinx.coroutines.launch

/** Android Credential Manager; Google verifies identity, FinFlow owns sessions. */
@Composable
fun GoogleAuthButton(
    enabled: Boolean = true,
    onCredential: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
    if (!webClientId.matches(Regex("^[A-Za-z0-9._-]+\\.apps\\.googleusercontent\\.com$"))) return

    val context = LocalContext.current
    val manager = remember(context) { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf(false) }

    OutlinedButton(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = enabled && !pending,
        onClick = {
            if (pending) return@OutlinedButton
            pending = true
            scope.launch {
                try {
                    val option = GetSignInWithGoogleOption.Builder(webClientId).build()
                    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                    // Use an Activity-owned context so Credential Manager can show its UI.
                    val result = manager.getCredential(context, request)
                    val credential = result.credential
                    if (credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                        if (token.isNotBlank()) onCredential(token)
                        else onError("Google no proporcionó una credencial válida.")
                    } else {
                        onError("Google no devolvió una credencial compatible.")
                    }
                } catch (_: GetCredentialCancellationException) {
                    // Cancellation is a user choice, not a login failure.
                } catch (_: GoogleIdTokenParsingException) {
                    onError("No se pudo interpretar la respuesta de Google.")
                } catch (_: GetCredentialException) {
                    onError("Google no está disponible. Revisa tu cuenta y vuelve a intentarlo.")
                } catch (_: Exception) {
                    onError("No pudimos comenzar el acceso con Google.")
                } finally {
                    pending = false
                }
            }
        },
    ) {
        if (pending) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text("Continuar con Google", style = MaterialTheme.typography.labelLarge)
    }
}
