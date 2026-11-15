package com.softdevcode.finflow_android.ui.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.softdevcode.finflow_android.ui.theme.FinFlowGold
import com.softdevcode.finflow_android.ui.theme.FinFlowNavy

/** Recorrido nativo sobre controles reales; nunca guarda datos automáticamente. */
enum class GuidedTourStep(val title: String, val hint: String, val destination: String) {
    MOVEMENT("Crea un movimiento", "Pulsa «Nuevo movimiento» para registrar un ingreso o gasto.", "dashboard"),
    TYPE("Selecciona el tipo", "Elige «Gasto» o «Ingreso».", "add_transaction"),
    DESCRIPTION("Escribe el concepto", "Indica el título del movimiento, por ejemplo «Mercado».", "add_transaction"),
    AMOUNT("Indica el monto", "Escribe cuánto ingresó o salió, en pesos colombianos.", "add_transaction"),
    CATEGORY("Elige una categoría", "Pulsa «Categoría» y escoge la que corresponda.", "add_transaction"),
    SAVE("Guarda cuando quieras", "Confirma aquí solo si deseas registrar el movimiento. La guía nunca lo guarda por ti.", "add_transaction"),
    NEW_GOAL("Crea una meta", "Pulsa «Nueva meta» para definir un objetivo de ahorro.", "goals"),
    GOAL_NAME("Ponle nombre", "Escribe el nombre de tu meta, por ejemplo «Fondo de emergencia».", "create_goal"),
    GOAL_AMOUNT("Define el objetivo", "Indica cuánto deseas ahorrar. No se creará ninguna meta automáticamente.", "create_goal"),
    NEW_ACCOUNT("Crea una billetera", "Pulsa «Nueva cuenta» y elige dónde organizar tu dinero.", "module/accounts"),
    NEW_CATEGORY("Crea una categoría", "Pulsa «Nueva categoría» para clasificar ingresos y gastos.", "module/categories");

    fun next(): GuidedTourStep? = values().getOrNull(ordinal + 1)
    companion object { val total: Int get() = values().size }
}

fun Modifier.guideAnchor(enabled: Boolean, onPosition: (Rect) -> Unit): Modifier =
    if (enabled) onGloballyPositioned { onPosition(it.boundsInRoot()) } else this

/** La máscara es decorativa: los controles nativos resaltados conservan sus gestos. */
@Composable
fun GuidedTourOverlay(step: GuidedTourStep, target: Rect?, next: () -> Unit, exit: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenHeight = with(androidx.compose.ui.platform.LocalDensity.current) { maxHeight.toPx() }
        val top = target != null && target.center.y > screenHeight * .57f
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color(0xB9000716))
            target?.let { r ->
                val p = 7.dp.toPx()
                val x = (r.left - p).coerceAtLeast(0f)
                val y = (r.top - p).coerceAtLeast(0f)
                val w = (r.width + p * 2).coerceAtMost(size.width - x)
                val h = (r.height + p * 2).coerceAtMost(size.height - y)
                if (w > 0 && h > 0) {
                    val pos = Offset(x, y)
                    val area = Size(w, h)
                    val corners = CornerRadius(16.dp.toPx())
                    drawRoundRect(Color.Transparent, pos, area, corners, blendMode = BlendMode.Clear)
                    drawRoundRect(FinFlowGold, pos, area, corners, style = Stroke(2.dp.toPx()))
                }
            }
        }
        Surface(
            modifier = Modifier.align(if (top) Alignment.TopCenter else Alignment.BottomCenter)
                .fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            color = FinFlowNavy, contentColor = Color.White,
            shape = RoundedCornerShape(20.dp), shadowElevation = 12.dp,
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("GUÍA FINFLOW · " + (step.ordinal + 1) + "/" + GuidedTourStep.total,
                    color = FinFlowGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(step.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(step.hint, color = Color(0xFFD5DDEC), style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = exit) { Text("Salir", color = Color(0xFFD5DDEC)) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = next, colors = ButtonDefaults.buttonColors(
                        containerColor = FinFlowGold, contentColor = FinFlowNavy
                    )) { Text(if (step.next() == null) "Finalizar" else "Siguiente") }
                }
            }
        }
    }
}
