package com.softdevcode.finflow_android.ui.screens.auth

import android.content.Intent
import android.net.Uri

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import com.softdevcode.finflow_android.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var privacyAccepted by remember { mutableStateOf(false) }
    var marketingConsent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.loadRegistrationPolicy() }

    var name        by remember { mutableStateOf("") }
    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var showPass    by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val passwordsMatch = password == confirmPass || confirmPass.isEmpty()

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onRegisterSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(com.softdevcode.finflow_android.ui.theme.Primary.copy(alpha = 0.07f), MaterialTheme.colorScheme.background)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(60.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(54.dp)
                        .background(
                            com.softdevcode.finflow_android.ui.theme.FinFlowNavy,
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.finflow_brand_symbol),
                        contentDescription = "Marca de FinFlow",
                        modifier = Modifier.size(46.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text("FinFlow", style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold))
            }

            Spacer(Modifier.height(26.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateToLogin) {
                    Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.6f), modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("Crear cuenta", style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp))
            }

            Spacer(Modifier.height(6.dp))
            Text("Completa tu información para empezar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(0.45f),
                modifier = Modifier.fillMaxWidth().padding(start = 48.dp))

            Spacer(Modifier.height(36.dp))

            // Name
            OutlinedTextField(
                value = name, onValueChange = { name = it; viewModel.clearError() },
                label = { Text("Nombre completo") },
                leadingIcon = { Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next, capitalization = KeyboardCapitalization.Words),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp), colors = finFlowTextFieldColors())

            Spacer(Modifier.height(14.dp))

            // Email
            OutlinedTextField(
                value = email, onValueChange = { email = it; viewModel.clearError() },
                label = { Text("Correo electrónico") },
                leadingIcon = { Icon(Icons.Outlined.Email, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp), colors = finFlowTextFieldColors())

            Spacer(Modifier.height(14.dp))

            // Password
            OutlinedTextField(
                value = password, onValueChange = { password = it; viewModel.clearError() },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.5f)) },
                trailingIcon = { IconButton(onClick = { showPass = !showPass }) {
                    Icon(if (showPass) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.4f)) } },
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp), colors = finFlowTextFieldColors())

            if (password.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                PasswordStrengthBar(password)
            }

            Spacer(Modifier.height(14.dp))

            // Confirm password
            OutlinedTextField(
                value = confirmPass, onValueChange = { confirmPass = it },
                label = { Text("Confirmar contraseña") },
                leadingIcon = { Icon(Icons.Outlined.LockOpen, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.5f)) },
                trailingIcon = {
                    if (confirmPass.isNotEmpty()) Icon(
                        if (passwordsMatch) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel, null,
                        tint = if (passwordsMatch) com.softdevcode.finflow_android.ui.theme.Success else com.softdevcode.finflow_android.ui.theme.Danger
                    )
                    else IconButton(onClick = { showConfirm = !showConfirm }) {
                        Icon(if (showConfirm) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.4f)) }
                },
                visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                isError = !passwordsMatch,
                supportingText = if (!passwordsMatch) { { Text("Las contraseñas no coinciden", color = MaterialTheme.colorScheme.error) } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); if (passwordsMatch && name.isNotBlank() && email.isNotBlank() && password.isNotBlank() &&
                        uiState.registrationPolicy != null &&
                        (uiState.registrationPolicy?.ready != true || privacyAccepted))
                        viewModel.register(email, password, name, confirmPass, privacyAccepted, marketingConsent) }),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp), colors = finFlowTextFieldColors())

            Spacer(Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("PRIVACIDAD · TU DECISIÓN",
                        color = com.softdevcode.finflow_android.ui.theme.FinFlowGold,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold)
                    Text("Tratamiento de datos personales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    val policy = uiState.registrationPolicy
                    if (uiState.isLoadingRegistrationPolicy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Consultando la política vigente…")
                    } else if (policy == null) {
                        Text(uiState.registrationPolicyError ?: "No se pudo cargar la política.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { viewModel.loadRegistrationPolicy() }) {
                            Text("Reintentar")
                        }
                    } else {
                        Text((policy.controllerName.ifBlank { "FinFlow" }) +
                            " · NIT " + policy.controllerNit,
                            style = MaterialTheme.typography.bodySmall)
                        Text("Política versión " + policy.version +
                            if (policy.ready) " · Publicada" else " · En revisión",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://finflow-opal-five.vercel.app/privacy")))
                        }) {
                            Text("Leer política completa")
                        }
                        if (policy.ready) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = privacyAccepted,
                                    onCheckedChange = { privacyAccepted = it })
                                Text(
                                    "He leído y autorizo expresamente el tratamiento " +
                                    "necesario para utilizar FinFlow.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = marketingConsent,
                                    onCheckedChange = { marketingConsent = it })
                                Text("Opcional: acepto comunicaciones comerciales.",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Text(
                                "La política formal aún está en revisión. No se registrará " +
                                "una autorización legal anticipada.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = uiState.error != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Spacer(Modifier.height(10.dp))
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            val canRegister = !uiState.isLoading && !uiState.isLoadingRegistrationPolicy &&
                uiState.registrationPolicy != null && name.isNotBlank() &&
                email.isNotBlank() && password.isNotBlank() && passwordsMatch &&
                confirmPass.isNotBlank() &&
                (uiState.registrationPolicy?.ready != true || privacyAccepted)

            Button(
                onClick = { viewModel.register(email, password, name, confirmPass, privacyAccepted, marketingConsent) },
                enabled = canRegister,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = com.softdevcode.finflow_android.ui.theme.Primary),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Crear cuenta", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp), color = androidx.compose.ui.graphics.Color.White)
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿Ya tienes cuenta? ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                TextButton(onClick = onNavigateToLogin, contentPadding = PaddingValues(0.dp)) {
                    Text("Inicia sesión", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = com.softdevcode.finflow_android.ui.theme.Primary)
                }
            }

            Spacer(Modifier.height(40.dp))
            Text("SOFTDEVCODE SAS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.2f), letterSpacing = 2.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PasswordStrengthBar(password: String) {
    val strength = when {
        password.length >= 8 && password.any { it.isUpperCase() } && password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() } -> 3
        password.length >= 6 && (password.any { it.isUpperCase() } || password.any { it.isDigit() }) -> 2
        else -> 1
    }
    val (color, label) = when (strength) {
        3    -> com.softdevcode.finflow_android.ui.theme.Success to "Contraseña fuerte"
        2    -> com.softdevcode.finflow_android.ui.theme.Warning to "Contraseña media"
        else -> com.softdevcode.finflow_android.ui.theme.Danger to "Contraseña débil"
    }
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { i ->
                LinearProgressIndicator(
                    progress = { if (i < strength) 1f else 0f },
                    modifier = Modifier.weight(1f).height(3.dp),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.outline.copy(0.3f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }

}
