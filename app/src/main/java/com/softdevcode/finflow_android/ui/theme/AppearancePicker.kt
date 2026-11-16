package com.softdevcode.finflow_android.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.softdevcode.finflow_android.domain.model.AppTheme

@Composable
internal fun AppearancePicker(theme: AppTheme, onSelect: (AppTheme) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { choosing = true },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        border = BorderStroke(1.dp, MaterialTheme.extras.border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
    ) {
        Icon(Icons.Outlined.Palette, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Tema", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        Text(theme.label, color = FinFlowGold, fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium)
        Icon(Icons.Outlined.KeyboardArrowDown, "Elegir apariencia", modifier = Modifier.size(20.dp))
    }
    if (choosing) {
        AlertDialog(
            onDismissRequest = { choosing = false },
            title = { Text("Apariencia") },
            text = {
                Column {
                    AppTheme.entries.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                .selectable(selected = option == theme, role = Role.RadioButton,
                                    onClick = { onSelect(option); choosing = false }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = option == theme, onClick = null)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(option.label, fontWeight = FontWeight.SemiBold)
                                Text(when (option) {
                                    AppTheme.LIGHT -> "Marfil cálido"
                                    AppTheme.DARK -> "Azul marino"
                                    AppTheme.OLED -> "Negro puro"
                                }, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosing = false }) { Text("Cerrar") } },
        )
    }
}
