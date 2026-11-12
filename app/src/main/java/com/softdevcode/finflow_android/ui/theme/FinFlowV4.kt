package com.softdevcode.finflow_android.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.softdevcode.finflow_android.R

/**
 * Design-contract implementation of FinFlow Web V4 (src/styles.scss):
 * --ff-v4-navy #09162a, raised #172b4a, gold #d7a94d, radius 18px,
 * canvas #f5f7fb, navy-to-slate premium hero and delicate 1px card borders.
 *
 * Only the presentation is shared; Android retains native, accessible controls.
 */
@Composable
fun FinFlowV4Overline(text: String, color: Color = FinFlowGoldMuted) {
    Text(
        text = text.uppercase(),
        color = color,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.65.sp,
        ),
    )
}

@Composable
fun FinFlowV4Hero(
    kicker: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    cardId: String? = null,
) {
    val controls = LocalPresentationControls.current
    val customizable = cardId != null && controls.state.enabled
    if (customizable && cardId in controls.state.hidden) {
        Row(
            modifier = modifier.fillMaxWidth().heightIn(min = 44.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { controls.setHidden(cardId!!, false) },
                enabled = cardId !in controls.state.saving,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(Icons.Outlined.OpenInFull, contentDescription = "Mostrar presentación",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp))
            }
        }
        return
    }
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier.fillMaxWidth()
            .border(1.dp, FinFlowGold.copy(alpha = 0.24f), shape)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(FinFlowNavy, FinFlowNavy2, FinFlowHeroEnd)
                )
            )
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                color = FinFlowGold.copy(alpha = 0.09f),
                radius = size.minDimension * 0.49f,
                center = Offset(size.width * 0.91f, size.height * 0.3f),
            )
            drawCircle(
                color = FinFlowGold.copy(alpha = 0.2f),
                radius = size.minDimension * 0.4f,
                center = Offset(size.width * 0.95f, size.height * 0.12f),
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 22.dp, top = 22.dp, bottom = 22.dp,
                end = if (customizable) 56.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FinFlowV4Overline(kicker, FinFlowGoldPale)
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.1).sp,
                ),
            )
            Text(
                subtitle,
                color = Color(0xFFC3D1E3),
                style = MaterialTheme.typography.bodyMedium,
            )
            cardId?.let { controls.state.errors[it] }?.let { Text(it, color = FinFlowGoldPale, style = MaterialTheme.typography.bodySmall) }
        }
        if (customizable) IconButton(
            onClick = { controls.setHidden(cardId!!, true) },
            enabled = cardId !in controls.state.saving,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        ) { Icon(Icons.Outlined.Close, "Ocultar presentación", tint = Color.White) }
    }
}

@Composable
fun FinFlowV4Card(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            content = content,
        )
    }
}

@Composable
fun FinFlowV4PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 46.dp),
        shape = RoundedCornerShape(11.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = FinFlowNavy,
            contentColor = Color.White,
        ),
    ) {
        icon?.invoke()
        if (icon != null) Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}

/** Brand geometry matches public/finflow-icon-classic.svg in the Web repo. */
@Composable
fun FinFlowV4BrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(FinFlowNavy),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.finflow_brand_symbol),
            contentDescription = "FinFlow",
            modifier = Modifier.fillMaxSize().padding(4.dp),
        )
    }
}

/** Native version of the official Web wordmark: gold dot over the white 'i'. */
@Composable
fun FinFlowV4Wordmark(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    val style = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.8).sp,
    )
    Row(
        modifier = modifier.semantics { contentDescription = "FinFlow" },
        verticalAlignment = Alignment.Bottom,
    ) {
        Text("F", color = color, style = style)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 0.dp, end = 1.dp, bottom = 2.dp),
        ) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(FinFlowGold))
            Spacer(Modifier.height(3.dp))
            Box(Modifier.width(3.dp).height(15.dp).background(color))
        }
        Text("nFlow", color = color, style = style)
    }
}

