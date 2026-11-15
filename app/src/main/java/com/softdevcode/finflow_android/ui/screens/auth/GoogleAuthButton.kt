package com.softdevcode.finflow_android.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.softdevcode.finflow_android.BuildConfig

/**
 * Optional UI: never show a dead Google button before authorized OAuth IDs
 * are provisioned in Google Cloud and CI/Gradle.
 */
@Composable
fun GoogleAuthButton(
    enabled: Boolean = true,
    onCredential: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val id = BuildConfig.GOOGLE_WEB_CLIENT_ID
    if (!id.matches(Regex("^[A-Za-z0-9._-]+\\.apps\\.googleusercontent\\.com$"))) return
    val context = LocalContext.current
    val options = remember(id) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail().requestIdToken(id).build()
    }
    val client = remember(context, options) { GoogleSignIn.getClient(context, options) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val token = account.idToken
            if (token.isNullOrBlank()) {
                onError("Google no pudo proporcionar una credencial válida.")
            } else {
                onCredential(token)
            }
        } catch (_: ApiException) {
            onError("Acceso de Google cancelado o no disponible.")
        } catch (_: Exception) {
            onError("No pudimos validar Google en este dispositivo.")
        }
    }
    OutlinedButton(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = enabled,
        onClick = { launcher.launch(client.signInIntent) },
    ) {
        Text("Continuar con Google", style = MaterialTheme.typography.labelLarge)
    }
}
