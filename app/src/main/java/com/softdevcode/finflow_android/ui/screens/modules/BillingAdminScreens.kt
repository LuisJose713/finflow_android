package com.softdevcode.finflow_android.ui.screens.modules

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softdevcode.finflow_android.domain.model.*
import com.softdevcode.finflow_android.ui.theme.FinFlowGold
import java.text.NumberFormat
import java.util.Locale

private fun billingCop(value: Long): String =
    NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(value)

@Composable
private fun BillingPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            content = content)
    }
}

@Composable
fun FinFlowPlansSection(vm: BillingAdminViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var chosen by rememberSaveable { mutableStateOf("") }
    var coupon by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.refreshBilling() }

    Text("Planes FinFlow", style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold)
    Text("Elige qué funcionalidades necesitas. Los precios se consultan en vivo.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

    state.entitlement?.let { entitlement ->
        BillingPanel {
            Text("TU PLAN ACTUAL", color = FinFlowGold,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold)
            Text(entitlement.planCode, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Text(if (entitlement.pro) "Beneficios Pro activos" else "Plan gratuito")
            entitlement.validUntil?.let { Text("Vigente hasta: " + it.substringBefore("T")) }
        }
    }

    state.catalog?.offers?.forEach { offer ->
        BillingPanel {
            Text(offer.title, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Text(offer.description, style = MaterialTheme.typography.bodyMedium)
            Text(billingCop(offer.priceCop),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold)
            if (offer.savingsPercent > 0) {
                Text("Descuento aplicado: " + offer.savingsPercent + "%",
                    color = FinFlowGold)
            }
            if (offer.promoCode != null) {
                Text("Promoción: " + offer.promoCode)
            }
            OutlinedButton(onClick = { chosen = offer.code; coupon = "" }) {
                Text("Consultar este plan")
            }
        }
    }

    if (chosen.isNotEmpty()) {
        BillingPanel {
            Text("Consultar precio", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = coupon, onValueChange = { coupon = it.trim().take(32) },
                label = { Text("Código promocional (opcional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { vm.quote(chosen, coupon) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Consultar oferta") }
            state.quote?.takeIf { it.code == chosen }?.let { quote ->
                Text("Precio cotizado: " + billingCop(quote.priceCop),
                    fontWeight = FontWeight.Bold)
                if (quote.promoTitle != null) Text(quote.promoTitle)
            }
        }
    }
    if (state.catalog?.checkoutEnabled != true) {
        Text("Las ofertas son informativas: FinFlow aún no permite pagar ni " +
            "activar una suscripción desde esta pantalla.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FinFlowAdminSection(vm: BillingAdminViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var editPlan by remember { mutableStateOf<BillingPlan?>(null) }
    var editPromotion by remember { mutableStateOf<BillingPromotion?>(null) }
    var newPromotion by remember { mutableStateOf(false) }
    var showGrant by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refreshAdmin() }

    Text("Panel de administración",
        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("Indicadores agregados y herramientas reservadas a administradores.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

    val overview = state.overview
    if (overview != null) {
        BillingPanel {
            Text("RESUMEN DE LA PLATAFORMA", color = FinFlowGold,
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            listOf(
                "Usuarios totales" to overview.users,
                "Usuarios nuevos (30 días)" to overview.newUsers30Days,
                "Usuarios activos (30 días)" to overview.activeUsers30Days,
                "Transacciones (30 días)" to overview.transactions30Days,
                "Metas registradas" to overview.goals,
                "Gastos planificados" to overview.plannedExpenses,
            ).forEach { (label, count) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(count.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    state.health?.let { health ->
        BillingPanel {
            Text("SERVICIOS · " + health.overall,
                color = FinFlowGold, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold)
            Text("Última revisión: " + health.checkedAt.substringBefore("T"),
                style = MaterialTheme.typography.bodySmall)
            health.services.forEach { service ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(service.name, Modifier.weight(1f))
                    Text(service.status, fontWeight = FontWeight.Bold,
                        color = if (service.status == "UP")
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (state.plans.isNotEmpty()) {
        Text("Planes", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        state.plans.forEach { plan ->
            BillingPanel {
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(plan.title + " · " + plan.code,
                        modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text(billingCop(plan.priceCop))
                }
                Text(if (plan.active) "Publicado" else "Inactivo",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { editPlan = plan }) { Text("Editar plan") }
            }
        }
    }
    if (state.promotions.isNotEmpty()) {
        Text("Promociones", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        state.promotions.forEach { promotion ->
            BillingPanel {
                Text(promotion.code + " · " + promotion.title,
                    fontWeight = FontWeight.Bold)
                Text(promotion.planCode + " · " +
                    promotion.discountValue + " " + promotion.discountType)
                Text((if (promotion.active) "Activa" else "Inactiva") +
                    (if (promotion.published) " · Pública" else " · Privada"))
                Text("Canjes: " + promotion.redemptionCount,
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { editPromotion = promotion; newPromotion = false }) {
                    Text("Editar promoción")
                }
            }
        }
    }

    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { vm.refreshAdmin() }) { Text("Actualizar") }
        OutlinedButton(onClick = { newPromotion = true; editPromotion = null }) {
            Text("Nueva promoción")
        }
        OutlinedButton(onClick = { showGrant = true }) { Text("Conceder acceso") }
    }

    editPlan?.let { plan ->
        EditAdminPlanDialog(plan, close = { editPlan = null }) { changed ->
            vm.savePlan(changed)
            editPlan = null
        }
    }
    if (newPromotion || editPromotion != null) {
        EditAdminPromotionDialog(
            current = editPromotion,
            close = { editPromotion = null; newPromotion = false },
        ) { changed ->
            vm.savePromotion(changed, newPromotion)
            newPromotion = false
            editPromotion = null
        }
    }
    if (showGrant) {
        AdminGrantDialog(close = { showGrant = false }) { request ->
            vm.grantPlan(request)
            showGrant = false
        }
    }
}

@Composable
private fun EditAdminPlanDialog(
    current: BillingPlan,
    close: () -> Unit,
    submit: (BillingPlan) -> Unit,
) {
    var title by remember(current.code) { mutableStateOf(current.title) }
    var description by remember(current.code) { mutableStateOf(current.description) }
    var price by remember(current.code) { mutableStateOf(current.priceCop.toString()) }
    var active by remember(current.code) { mutableStateOf(current.active) }
    val priceLong = price.toLongOrNull()
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Editar " + current.code) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it.take(90) },
                    label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(description, { description = it.take(240) },
                    label = { Text("Descripción") }, maxLines = 3)
                OutlinedTextField(price, { price = it.filter(Char::isDigit).take(9) },
                    label = { Text("Precio COP") }, singleLine = true)
                Row {
                    Text("Plan activo", Modifier.weight(1f))
                    Switch(active, onCheckedChange = { active = it }, enabled = current.code != "FREE")
                }
                Text("Esta modificación afecta a los precios públicos de FinFlow.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                submit(current.copy(title = title.trim(), description = description.trim(),
                    priceCop = priceLong ?: current.priceCop,
                    active = if (current.code == "FREE") true else active))
            }, enabled = title.trim().length in 2..90 && description.length <= 240 &&
                priceLong != null && priceLong in
                    (if (current.code == "FREE") 0L else 1000L)..10_000_000L) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = close) { Text("Cancelar") } },
    )
}

@Composable
private fun EditAdminPromotionDialog(
    current: BillingPromotion?, close: () -> Unit,
    submit: (BillingPromotion) -> Unit,
) {
    var code by remember(current?.code) { mutableStateOf(current?.code.orEmpty()) }
    var title by remember(current?.code) { mutableStateOf(current?.title.orEmpty()) }
    var plan by remember(current?.code) { mutableStateOf(current?.planCode ?: "MONTHLY") }
    var discountType by remember(current?.code) { mutableStateOf(current?.discountType ?: "PERCENT") }
    var discount by remember(current?.code) { mutableStateOf(current?.discountValue?.toString() ?: "10") }
    var starts by remember(current?.code) { mutableStateOf(current?.startsAt ?: "") }
    var ends by remember(current?.code) { mutableStateOf(current?.endsAt ?: "") }
    var limit by remember(current?.code) { mutableStateOf(current?.maxRedemptions?.toString() ?: "") }
    var active by remember(current?.code) { mutableStateOf(current?.active ?: true) }
    var published by remember(current?.code) { mutableStateOf(current?.published ?: false) }

    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (current == null) "Nueva promoción" else "Editar promoción") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(code, { if (current == null)
                    code = it.uppercase(Locale.ROOT).take(32) },
                    label = { Text("Código") }, readOnly = current != null,
                    singleLine = true)
                OutlinedTextField(title, { title = it.take(90) },
                    label = { Text("Título") }, singleLine = true)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("MONTHLY", "YEARLY", "LIFETIME").forEach { option ->
                        FilterChip(selected = plan == option,
                            onClick = { plan = option }, label = { Text(option) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PERCENT", "FIXED").forEach { option ->
                        FilterChip(selected = discountType == option,
                            onClick = { discountType = option }, label = { Text(option) })
                    }
                }
                OutlinedTextField(discount, { discount = it.filter(Char::isDigit).take(9) },
                    label = { Text("Descuento") }, singleLine = true)
                OutlinedTextField(starts, { starts = it.take(30) },
                    label = { Text("Inicio ISO UTC (p. ej. 2026-12-01T00:00:00Z)") },
                    singleLine = true)
                OutlinedTextField(ends, { ends = it.take(30) },
                    label = { Text("Fin ISO UTC") }, singleLine = true)
                OutlinedTextField(limit, { limit = it.filter(Char::isDigit).take(6) },
                    label = { Text("Límite de canjes (opcional)") }, singleLine = true)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Activa", Modifier.weight(1f))
                    Switch(active, onCheckedChange = { active = it })
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Visible al público", Modifier.weight(1f))
                    Switch(published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            val value = discount.toLongOrNull()
            // No java.time API 26 dependency: preserve Android 7+ support.
            // The server validates actual Instant ranges before saving.
            val isoPattern = Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")
            val isoValid = isoPattern.matches(starts) && isoPattern.matches(ends) && starts < ends
            TextButton(
                enabled = code.matches(Regex("[A-Z0-9_-]{4,32}")) &&
                    title.trim().length in 2..90 && value != null &&
                    value > 0 && isoValid && (limit.isBlank() || (limit.toIntOrNull()?.let { it in 1..100000 } == true)),
                onClick = {
                    submit(BillingPromotion(code.trim(), title.trim(), plan, discountType,
                        value ?: 0L, starts, ends, limit.toIntOrNull(),
                        current?.redemptionCount ?: 0, active, published))
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = close) { Text("Cancelar") } },
    )
}

@Composable
private fun AdminGrantDialog(
    close: () -> Unit, submit: (BillingGrantRequest) -> Unit,
) {
    var userId by rememberSaveable { mutableStateOf("") }
    var planCode by rememberSaveable { mutableStateOf("MONTHLY") }
    var reason by rememberSaveable { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }
    val id = userId.toLongOrNull()
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Conceder acceso manual") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Solo administradores. Esto concede beneficios Pro sin pago " +
                    "y queda registrado en auditoría.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(userId, { userId = it.filter(Char::isDigit) },
                    label = { Text("ID interno del usuario") }, singleLine = true)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("MONTHLY", "YEARLY", "LIFETIME").forEach { code ->
                        FilterChip(selected = planCode == code,
                            onClick = { planCode = code }, label = { Text(code) })
                    }
                }
                OutlinedTextField(reason, { reason = it.take(240) },
                    label = { Text("Motivo obligatorio de auditoría") }, maxLines = 3)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(confirmed, onCheckedChange = { confirmed = it })
                    Text("Confirmo esta asignación manual.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = confirmed && id != null && id > 0 && reason.trim().length in 8..240,
                onClick = { submit(BillingGrantRequest(id ?: return@TextButton, planCode,
                    reason.trim())) },
            ) { Text("Conceder acceso") }
        },
        dismissButton = { TextButton(onClick = close) { Text("Cancelar") } },
    )
}
