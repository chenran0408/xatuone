package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

// alpha7 has no public indeterminate progress indicator.
@Composable
internal fun CouiLoadingIndicator(modifier: Modifier) {
    val transition = rememberInfiniteTransition()
    val angle by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)))
    val color = com.itos.coui.theme.CouiTheme.colorScheme.accent
    Canvas(modifier.size(24.dp).semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }) {
        val stroke = 2.dp.toPx()
        drawArc(color, angle, 260f, false, topLeft = Offset(stroke, stroke),
            size = Size(size.width - stroke * 2, size.height - stroke * 2), style = Stroke(stroke, cap = StrokeCap.Round))
    }
}
