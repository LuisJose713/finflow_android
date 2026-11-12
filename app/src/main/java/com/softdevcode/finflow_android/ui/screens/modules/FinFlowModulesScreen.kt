package com.softdevcode.finflow_android.ui.screens.modules

import com.softdevcode.finflow_android.ui.components.CopAmountField
import com.softdevcode.finflow_android.ui.updates.AppUpdateViewModel
import com.softdevcode.finflow_android.ui.updates.AppUpdatesSection
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softdevcode.finflow_android.domain.model.*
import com.softdevcode.finflow_android.ui.theme.FinFlowGold
import com.softdevcode.finflow_android.ui.theme.FinFlowNavy
import com.softdevcode.finflow_android.ui.theme.FinFlowV4Hero
import com.softdevcode.finflow_android.ui.theme.FinFlowV4Card
import com.softdevcode.finflow_android.ui.theme.FinFlowV4Overline
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun FinFlowModulesScreen(
    userId: Int?,
    onReauthenticate: () -> Unit,
    updateViewModel: AppUpdateViewModel,
    isAdmin: Boolean = false,
    initialSection: String = "accounts",
    vm: FinFlowModulesViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cal = remember { Calendar.getInstance(TimeZone.getTimeZone("America/Bogota")) }
    val selected = initialSection
    var year by rememberSaveable { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var month by rememberSaveable { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) }

    LaunchedEffect(selected) {
        if (selected in listOf("accounts", "budgets", "recurring", "categories", "privacy"))
            vm.refresh(selected, year, month)
    }

    // The assistant is a conversation, not a long settings page. Give it its
    // own viewport, with independent message scrolling and a fixed composer.
    if (selected == "advisor") {
        AdvisorConversationScreen(state, vm)
        return
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState())) {
        val intro = when (selected) {
            "accounts" -> Triple("CUENTAS Y BILLETERAS", "Cada peso en su lugar",
                "Controla tus cuentas, saldos y transferencias.")
            "budgets" -> Triple("PRESUPUESTOS MENSUALES", "Tu dinero tiene un plan",
                "Organiza límites para cada categoría de gasto.")
            "recurring" -> Triple("MOVIMIENTOS RECURRENTES", "Lo importante, bajo control",
                "Programa los pagos y cobros de cada mes.")
            "categories" -> Triple("TUS CATEGORÍAS", "Un orden que se nota",
                "Personaliza la forma de clasificar tus movimientos.")
            "advisor" -> Triple("FINFLOW IA", "Tu asesor financiero",
                "Ideas y análisis para entender mejor tus decisiones.")
            "reports" -> Triple("REPORTES FINANCIEROS", "Tus números, con claridad",
                "Exporta el PDF solo lectura o Excel editable.")
            "privacy" -> Triple("PRIVACIDAD Y DATOS", "Tu información, tus reglas",
                "Consulta y gestiona tus derechos en FinFlow.")
            "security" -> Triple("PROTECCIÓN DE CUENTA", "Tu seguridad primero",
                "Protege tus sesiones y credenciales.")
            "billing" -> Triple("PLANES FINFLOW", "Elige tu experiencia",
                "Consulta precios y promociones disponibles.")
            "updates" -> Triple("FINFLOW AL DÍA", "Siempre avanzando",
                "Busca y descarga las nuevas versiones de la app." )
            "admin" -> Triple("ADMINISTRACIÓN", "Tu plataforma, al detalle",
                "Monitorea FinFlow y administra sus planes.")
            else -> Triple("FINFLOW", "Tu espacio financiero",
                "Organiza cada movimiento, proyecto y meta.")
        }
        FinFlowV4Hero(intro.first, intro.second, intro.third, cardId = selected + ".intro",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
            state.notice?.let {
                Text(it, color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall)
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            when (selected) {
                "accounts" -> AccountsSection(state, vm)
                "budgets" -> BudgetsSection(state, vm, year, month,
                    previous = {
                        if (month == 1) { month = 12; year -= 1 } else month -= 1
                        vm.refresh("budgets", year, month)
                    },
                    next = {
                        if (month == 12) { month = 1; year += 1 } else month += 1
                        vm.refresh("budgets", year, month)
                    })
                "recurring" -> RecurringSection(state, vm)
                "categories" -> CategoriesSection(state, vm, userId)
                "advisor" -> AdvisorConversationScreen(state, vm)
                "reports" -> FinFlowReportsSection(state, vm)
                "privacy" -> FinFlowPrivacySection(state, vm)
                "security" -> FinFlowSecuritySection(state, vm, onReauthenticate)
                "billing" -> FinFlowPlansSection()
                "updates" -> AppUpdatesSection(updateViewModel)
                "admin" -> if (isAdmin) FinFlowAdminSection()
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String, refresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = refresh) { Icon(Icons.Outlined.Refresh, "Actualizar " + title) }
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    FinFlowV4Card(content = content)
}

@Composable
private fun Choices(options: List<Pair<String, String>>, choice: String, set: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = choice == value, onClick = { set(value) },
                label = { Text(label) })
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, detail: String, dismiss: () -> Unit, action: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(title) },
        text = { Text(detail) },
        confirmButton = { TextButton(onClick = action) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } },
    )
}

private val accountTypes = listOf(
    "cash" to "Efectivo", "bank" to "Banco", "savings" to "Ahorros", "other" to "Otro",
)
private fun money(v: Double) = com.softdevcode.finflow_android.domain.model.formatCopMoney(v)
private fun amount(s: String): Double? = s.trim().replace(",", ".").toDoubleOrNull()
    ?.takeIf { it.isFinite() && it >= 0 && it <= 1_000_000_000_000.0 }
private fun dateNow(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("America/Bogota")
}.format(java.util.Date())

@Composable
private fun AccountsSection(state: ModulesState, vm: FinFlowModulesViewModel) {
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FinancialAccount?>(null) }
    var transfer by remember { mutableStateOf(false) }
    var archiving by remember { mutableStateOf<FinancialAccount?>(null) }
    Header("Cuentas y billeteras", "Saldos, cuentas y transferencias.",
        refresh = { vm.refresh("accounts") })

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { editing = null; showForm = true }) {
            Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(5.dp))
            Text("Nueva cuenta")
        }
        if (state.accounts.count { !it.archived } >= 2) {
            OutlinedButton(onClick = { transfer = true }) { Text("Transferir") }
        }
    }

    if (!state.busy && state.accounts.isEmpty()) {
        Panel { Text("Crea tu primera billetera para organizar tu dinero.") }
    }
    state.accounts.forEach { acc ->
        Panel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(acc.name, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Text((accountTypes.find { it.first == acc.type }?.second ?: "Otra")
                        + (if (acc.archived) " · Archivada" else " · " + acc.currency),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(money(acc.balance), fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
            Row {
                TextButton(onClick = { editing = acc; showForm = true }) { Text("Editar") }
                TextButton(onClick = { archiving = acc }) {
                    Text(if (acc.archived) "Activar" else "Archivar")
                }
            }
        }
    }
    if (state.transfers.isNotEmpty()) {
        Text("Transferencias recientes", fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium)
        state.transfers.take(8).forEach { tr ->
            Panel {
                Text((state.accounts.find { it.id == tr.fromAccountId }?.name ?: "Origen")
                    + " → " +
                    (state.accounts.find { it.id == tr.toAccountId }?.name ?: "Destino"),
                    fontWeight = FontWeight.SemiBold)
                Text(money(tr.amount) + " · " + tr.date)
                tr.note?.takeIf { it.isNotBlank() }?.let { Text(it) }
            }
        }
    }

    if (showForm) {
        AccountForm(editing, dismiss = { showForm = false }) { name, type, opening ->
            val acc = editing
            if (acc == null) vm.createAccount(name, type, opening)
            else vm.updateAccount(acc.id, name, type)
            showForm = false
        }
    }
    if (transfer) {
        TransferForm(state.accounts.filter { !it.archived }, dismiss = { transfer = false }) {
                from, to, value, note ->
            vm.transfer(from, to, value, dateNow(), note)
            transfer = false
        }
    }
    archiving?.let { acc ->
        ConfirmDialog(
            if (acc.archived) "Reactivar cuenta" else "Archivar cuenta",
            "Se conservarán los movimientos e historial financiero de " + acc.name + ".",
            dismiss = { archiving = null },
            action = { vm.archiveAccount(acc.id, !acc.archived); archiving = null },
        )
    }
}

@Composable
private fun AccountForm(
    current: FinancialAccount?, dismiss: () -> Unit, save: (String, String, Double) -> Unit,
) {
    var name by remember(current?.id) { mutableStateOf(current?.name.orEmpty()) }
    var type by remember(current?.id) { mutableStateOf(current?.type ?: "cash") }
    var balance by remember(current?.id) { mutableStateOf("0") }
    val initial = amount(balance)
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(if (current == null) "Nueva cuenta" else "Editar cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Choices(accountTypes, type) { type = it }
                if (current == null) {
                    CopAmountField(balance, { balance = it },
                        label = { Text("Saldo inicial COP") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { save(name.trim(), type, initial ?: 0.0) },
                enabled = name.trim().length in 2..80 && (current != null || initial != null)) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun TransferForm(
    accounts: List<FinancialAccount>,
    dismiss: () -> Unit, save: (Int, Int, Double, String) -> Unit,
) {
    var from by remember { mutableStateOf(accounts.first().id.toString()) }
    var to by remember { mutableStateOf(accounts.last().id.toString()) }
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val valid = amount(value)
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Transferencia interna") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Desde", fontWeight = FontWeight.Bold)
                Choices(accounts.map { it.id.toString() to it.name }, from) { from = it }
                Text("Hacia", fontWeight = FontWeight.Bold)
                Choices(accounts.map { it.id.toString() to it.name }, to) { to = it }
                CopAmountField(value, { value = it }, label = { Text("Importe COP") },
                    singleLine = true)
                OutlinedTextField(note, { note = it.take(255) }, label = { Text("Nota") })
                Text("No crea ingresos ni gastos adicionales.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                save(from.toInt(), to.toInt(), valid ?: 0.0, note)
            }, enabled = from != to && valid != null && valid > 0) {
                Text("Transferir")
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } },
    )
}

private val monthNames = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")

@Composable
private fun BudgetsSection(
    state: ModulesState, vm: FinFlowModulesViewModel, year: Int, month: Int,
    previous: () -> Unit, next: () -> Unit,
) {
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MonthlyBudget?>(null) }
    var deleting by remember { mutableStateOf<MonthlyBudget?>(null) }
    Header("Presupuestos", "Límites mensuales por categoría.",
        refresh = { vm.refresh("budgets", year, month) })
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = previous) { Icon(Icons.Outlined.ChevronLeft, "Mes anterior") }
        Text(monthNames[month - 1] + " " + year, fontWeight = FontWeight.Bold)
        IconButton(onClick = next) { Icon(Icons.Outlined.ChevronRight, "Mes siguiente") }
    }
    Button(onClick = { editing = null; showForm = true }) {
        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text("Nuevo presupuesto")
    }
    if (!state.busy && state.budgets.isEmpty()) {
        Panel { Text("Todavía no has definido límites para este mes.") }
    }
    state.budgets.forEach { budget ->
        Panel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(budget.categoryName, fontWeight = FontWeight.Bold)
                Text(budget.percentUsed.toString() + "%", fontWeight = FontWeight.Bold,
                    color = if (budget.status == "over")
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(
                progress = { (budget.percentUsed.toFloat() / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Gastado: " + money(budget.spent) + " / " + money(budget.limitAmount))
            Text("Disponible: " + money(budget.remaining),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row {
                TextButton(onClick = { editing = budget; showForm = true }) {
                    Text("Cambiar límite")
                }
                TextButton(onClick = { deleting = budget }) { Text("Eliminar") }
            }
        }
    }
    if (showForm) {
        BudgetForm(editing,
            state.categories.filter { it.type.equals("expense", ignoreCase = true) },
            dismiss = { showForm = false }) { categoryId, value ->
            val budget = editing
            if (budget == null) vm.createBudget(categoryId, year, month, value)
            else vm.changeBudget(budget.id, value)
            showForm = false
        }
    }
    deleting?.let { budget ->
        ConfirmDialog("Eliminar presupuesto", "¿Eliminar el límite de " + budget.categoryName + "?",
            dismiss = { deleting = null },
            action = { vm.deleteBudget(budget.id); deleting = null })
    }
}

@Composable
private fun BudgetForm(
    existing: MonthlyBudget?, categories: List<Category>,
    dismiss: () -> Unit, save: (Int, Double) -> Unit,
) {
    var categoryId by remember(existing?.id) {
        mutableStateOf((existing?.categoryId ?: categories.firstOrNull()?.id ?: 0).toString())
    }
    var value by remember(existing?.id) {
        mutableStateOf(existing?.limitAmount?.let { com.softdevcode.finflow_android.domain.model.copEditableAmount(it) }.orEmpty())
    }
    val amountValue = amount(value)
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(if (existing == null) "Nuevo presupuesto" else "Editar presupuesto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Categoría", fontWeight = FontWeight.Bold)
                if (existing == null) {
                    Choices(categories.map { it.id.toString() to it.name }, categoryId) {
                        categoryId = it
                    }
                } else Text(existing.categoryName)
                CopAmountField(value, { value = it },
                    label = { Text("Límite mensual COP") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { save(categoryId.toInt(), amountValue ?: 0.0) },
                enabled = categoryId.toIntOrNull()?.let { it > 0 } == true &&
                    amountValue != null && amountValue > 0) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun RecurringSection(state: ModulesState, vm: FinFlowModulesViewModel) {
    var showForm by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<RecurringRule?>(null) }
    var processDue by remember { mutableStateOf(false) }
    Header("Movimientos recurrentes", "Ingresos y gastos que se repiten.",
        refresh = { vm.refresh("recurring") })
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { showForm = true }) {
            Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(5.dp))
            Text("Nueva regla")
        }
        OutlinedButton(onClick = { processDue = true }) { Text("Procesar") }
    }
    Text("Procesar vencidos puede crear transacciones financieras reales.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (!state.busy && state.rules.isEmpty()) {
        Panel { Text("No hay reglas recurrentes. Crea la primera para automatizar movimientos.") }
    }
    state.rules.forEach { rule ->
        Panel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(rule.title, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text(money(rule.amount), fontWeight = FontWeight.SemiBold)
            }
            Text((if (rule.type == "income") "Ingreso" else "Gasto") +
                " · Día " + rule.dayOfMonth + " · Próximo: " +
                (rule.nextDueDate ?: "Sin fecha"),
                style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (rule.active) "Activa" else "Pausada", Modifier.weight(1f))
                Switch(checked = rule.active, onCheckedChange = {
                    vm.setRecurringActive(rule.id, it)
                })
                TextButton(onClick = { deleting = rule }) { Text("Eliminar") }
            }
        }
    }
    if (showForm) RecurringForm(state.categories, state.accounts, dismiss = {
        showForm = false
    }) { title, value, type, category, account, day ->
        vm.createRecurringRule(title, value, type, category, account, day)
        showForm = false
    }
    deleting?.let { rule ->
        ConfirmDialog("Eliminar recurrencia",
            "El historial no se borrará. ¿Eliminar " + rule.title + "?",
            dismiss = { deleting = null },
            action = { vm.deleteRecurringRule(rule.id); deleting = null })
    }
    if (processDue) ConfirmDialog("Procesar vencidos",
        "Se crearán los ingresos y gastos pendientes de las reglas activas.",
        dismiss = { processDue = false },
        action = { vm.processDue(); processDue = false })
}

@Composable
private fun RecurringForm(
    categories: List<Category>, accounts: List<FinancialAccount>,
    dismiss: () -> Unit,
    save: (String, Double, String, Int, Int?, Int) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("expense") }
    var categoryId by remember { mutableStateOf("0") }
    var accountId by remember { mutableStateOf("0") }
    var day by remember { mutableStateOf("1") }
    val valueParsed = amount(value)
    val dayParsed = day.toIntOrNull()
    val availableCategories = categories.filter { it.type.equals(type, ignoreCase = true) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Nueva recurrencia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Nombre") },
                    singleLine = true)
                CopAmountField(value, { value = it }, label = { Text("Monto COP") },
                    singleLine = true)
                Choices(listOf("expense" to "Gasto", "income" to "Ingreso"), type) {
                    type = it; categoryId = "0"
                }
                Text("Categoría", fontWeight = FontWeight.Bold)
                Choices(availableCategories.map { it.id.toString() to it.name }, categoryId) {
                    categoryId = it
                }
                Text("Cuenta opcional", fontWeight = FontWeight.Bold)
                Choices(listOf("0" to "Ninguna") + accounts.filter { !it.archived }
                    .map { it.id.toString() to it.name }, accountId) { accountId = it }
                OutlinedTextField(day, { day = it }, label = { Text("Día del mes (1 a 28)") },
                    singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                save(title, valueParsed ?: 0.0, type, categoryId.toInt(),
                    accountId.toIntOrNull()?.takeIf { it > 0 }, dayParsed ?: 1)
            }, enabled = title.trim().length in 2..120 && valueParsed != null &&
                valueParsed > 0 && categoryId.toIntOrNull()?.let { it > 0 } == true &&
                dayParsed != null && dayParsed in 1..28) {
                Text("Crear")
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun CategoriesSection(state: ModulesState, vm: FinFlowModulesViewModel, userId: Int?) {
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var deleting by remember { mutableStateOf<Category?>(null) }
    Header("Categorías", "Personaliza tus grupos de ingresos y gastos.",
        refresh = { vm.refresh("categories") })
    Button(onClick = { editing = null; showForm = true }) {
        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text("Nueva categoría")
    }
    listOf("Expense" to "Gastos", "Income" to "Ingresos").forEach { (type, label) ->
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        state.categories.filter { it.type.equals(type, ignoreCase = true) }.forEach { cat ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(cat.name, fontWeight = FontWeight.SemiBold)
                        Text(if (cat.userId == null) "Del sistema" else "Personal",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (userId != null && cat.userId == userId) {
                        TextButton(onClick = { editing = cat; showForm = true }) { Text("Editar") }
                        TextButton(onClick = { deleting = cat }) { Text("Eliminar") }
                    }
                }
            }
        }
    }
    if (showForm) CategoryForm(editing, dismiss = { showForm = false }) { name, type ->
        val category = editing
        if (category == null) vm.createCategory(name, type)
        else vm.updateCategory(category.id, name, type)
        showForm = false
    }
    deleting?.let { category ->
        ConfirmDialog("Eliminar categoría", "¿Eliminar " + category.name + "?",
            dismiss = { deleting = null },
            action = { vm.deleteCategory(category.id); deleting = null })
    }
}

@Composable
private fun CategoryForm(
    existing: Category?, dismiss: () -> Unit, save: (String, String) -> Unit,
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var type by remember(existing?.id) {
        mutableStateOf(if (existing?.type.equals("income", ignoreCase = true)) "Income" else "Expense")
    }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(if (existing == null) "Nueva categoría" else "Editar categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") },
                    singleLine = true)
                Choices(listOf("Expense" to "Gasto", "Income" to "Ingreso"), type) { type = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { save(name.trim(), type) },
                enabled = name.trim().length in 2..80) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun AdvisorConversationScreen(state: ModulesState, vm: FinFlowModulesViewModel) {
    var prompt by rememberSaveable { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(state.chat.size, state.busy) {
        val count = state.chat.size + if (state.busy) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(42.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text("FinFlow AI", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text("Tu asistente financiero", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = vm::clearMessages,
                enabled = state.chat.isNotEmpty() && !state.busy) {
                Icon(Icons.Outlined.Add, contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nuevo chat")
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        state.error?.let { error ->
            Text(error,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.chat.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 90.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null,
                            tint = FinFlowGold, modifier = Modifier.size(40.dp))
                        Text("¿En qué puedo ayudarte?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold)
                        Text("Pregunta lo que quieras sobre tus finanzas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(state.chat.size) { index ->
                val message = state.chat[index]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        if (message.user) Arrangement.End else Arrangement.Start,
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = 660.dp)
                            .padding(start = if (message.user) 38.dp else 0.dp,
                                end = if (message.user) 0.dp else 38.dp),
                        shape = RoundedCornerShape(
                            topStart = if (message.user) 16.dp else 5.dp,
                            topEnd = if (message.user) 5.dp else 16.dp,
                            bottomStart = 16.dp, bottomEnd = 16.dp),
                        color = if (message.user) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(message.text,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            if (state.busy) {
                item {
                    Text("FinFlow está escribiendo…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it.take(2000) },
                placeholder = { Text("Escribe tu mensaje…") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(15.dp),
                minLines = 1, maxLines = 4,
            )
            FilledIconButton(
                onClick = {
                    val message = prompt.trim()
                    if (message.isNotBlank()) {
                        vm.sendChat(message)
                        prompt = ""
                    }
                },
                enabled = prompt.isNotBlank() && !state.busy,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(Icons.Outlined.Send, contentDescription = "Enviar mensaje")
            }
        }
        Text("La IA puede equivocarse. Verifica la información importante.",
            modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

