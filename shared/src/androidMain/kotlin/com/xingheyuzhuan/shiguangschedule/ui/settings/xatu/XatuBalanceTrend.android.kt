package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import com.xingheyuzhuan.shiguangschedule.ui.components.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuBalanceRecord
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors
import com.xingheyuzhuan.shiguangschedule.xatu.campus.xatuElectricityWindow
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal fun homeQueryTime(at: Long): String = Instant.ofEpochMilli(at).atOffset(ZoneOffset.ofHours(8))
    .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))

@Composable
internal fun BalanceTrend(history: List<XatuBalanceRecord>, compact: Boolean = false, days: Int = 7) {
    val points = remember(history, days) { xatuElectricityWindow(history, days) }
    val style = MaterialTheme.typography.bodySmall
    UiText("过去 $days 天 · 度", style = style, color = uiStyleColors().secondaryText)
    if (points.size < 2) {
        UiFeedback(if (points.isEmpty()) "暂无电量记录" else "暂无趋势", UiFeedbackKind.EMPTY)
        return
    }
    val values = remember(points) { points.map { it.balance.toFloat() } }
    val min = values.min()
    val max = values.max()
    val minPoint = remember(points) { points.minBy { it.balance.toBigDecimal() } }
    val maxPoint = remember(points) { points.maxBy { it.balance.toBigDecimal() } }
    val range = (max - min).coerceAtLeast(1f)
    val duration = (points.last().at - points.first().at).coerceAtLeast(1L)
    val isCoui = LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI
    val lineColor = if (isCoui) com.itos.coui.theme.CouiTheme.colorScheme.accent else uiStyleColors().accent
    val gridColor = if (isCoui) {
        com.itos.coui.theme.CouiTheme.colorScheme.outlineVariant.copy(alpha = .55f)
    } else uiStyleColors().secondaryText.copy(alpha = .15f)
    val description = "过去 $days 天剩余电量观测，${points.joinToString("；") { "${homeQueryTime(it.at)} ${it.balance} 度" }}"
    Canvas(Modifier.fillMaxWidth().height(if (compact) 72.dp else 160.dp).padding(8.dp).semantics {
        contentDescription = description
    }) {
        drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height))
        if (!compact) {
            drawLine(gridColor, Offset(0f, size.height / 2), Offset(size.width, size.height / 2))
            drawLine(gridColor, Offset(0f, 0f), Offset(size.width, 0f))
        }
        val offsets = points.mapIndexed { index, point ->
            val y = if (max == min) size.height / 2 else size.height * (1 - (values[index] - min) / range)
            Offset((point.at - points.first().at).toFloat() / duration * size.width, y)
        }
        offsets.zipWithNext().forEach { (start, end) -> drawLine(lineColor, start, end, strokeWidth = 2.dp.toPx()) }
        offsets.forEach { drawCircle(lineColor, radius = 3.dp.toPx(), center = it) }
    }
    if (!compact) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            UiText("最低 ${minPoint.balance} 度", Modifier.weight(1f), style = style)
            UiText("最高 ${maxPoint.balance} 度", Modifier.weight(1f), style = style, textAlign = TextAlign.End)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            UiText(homeQueryTime(points.first().at), style = style, color = uiStyleColors().secondaryText)
            UiText("${points.first().balance} 度", style = style)
        }
        Column(Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.End) {
            UiText(homeQueryTime(points.last().at), style = style, color = uiStyleColors().secondaryText, textAlign = TextAlign.End)
            UiText("${points.last().balance} 度", style = style, textAlign = TextAlign.End)
        }
    }
}
