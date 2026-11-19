package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.shape.CouiShape
import com.itos.coui.theme.CouiTheme
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.getValue

@Composable
actual fun uiScheduleCourseShape(cornerRadius: Dp): Shape =
    if (LocalUiStyle.current == UiStyle.COUI) CouiShape(cornerRadius)
    else RoundedCornerShape(cornerRadius)

@Composable
actual fun UiScheduleCourseCard(
    modifier: Modifier,
    shape: Shape,
    containerColor: Color,
    raised: Boolean,
    pressed: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> Card(
            modifier = modifier.graphicsLayer { scaleX = if (pressed) .98f else 1f; scaleY = if (pressed) .98f else 1f },
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (raised || pressed) 4.dp else 1.dp),
            content = content,
        )
        UiStyle.MIUIX -> {
            val scale = animateFloatAsState(
                targetValue = if (pressed) .97f else 1f,
                animationSpec = spring(dampingRatio = .72f, stiffness = 520f),
                label = "miuixCoursePress",
            )
            val color by animateColorAsState(
                targetValue = if (raised) MiuixTheme.colorScheme.primary.copy(alpha = .22f) else containerColor,
                label = "miuixCourseSurface",
            )
            MiuixCard(
                modifier = modifier.clip(shape).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
                colors = MiuixCardDefaults.defaultColors(color = color),
                content = content,
            )
        }
        UiStyle.COUI -> {
            val scale = animateFloatAsState(
                targetValue = if (pressed) .98f else 1f,
                animationSpec = spring(dampingRatio = .86f, stiffness = 360f),
                label = "couiCoursePress",
            )
            val color by animateColorAsState(
                targetValue = if (raised) lerp(containerColor, CouiTheme.colorScheme.accent, .18f) else containerColor,
                label = "couiCourseSurface",
            )
            Box(
                modifier = modifier
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                    .clip(shape)
                    .background(color)
                    .then(if (raised) Modifier.border(2.dp, CouiTheme.colorScheme.accent, shape) else Modifier),
            ) {
                Column(content = content)
            }
        }
    }
}

@Composable
actual fun UiScheduleMarker(
    modifier: Modifier,
    selected: Boolean,
    cornerRadius: Dp,
    content: @Composable BoxScope.() -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> Surface(
            modifier = modifier,
            shape = RoundedCornerShape(cornerRadius),
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            tonalElevation = if (selected) 1.dp else 0.dp,
        ) { Box(contentAlignment = Alignment.Center, content = content) }
        UiStyle.MIUIX -> {
            val color by animateColorAsState(
                targetValue = if (selected) MiuixTheme.colorScheme.primaryContainer else Color.Transparent,
                label = "miuixScheduleMarker",
            )
            MiuixCard(
                modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
                colors = MiuixCardDefaults.defaultColors(color = color),
            ) { Box(contentAlignment = Alignment.Center, content = content) }
        }
        UiStyle.COUI -> {
            val color by animateColorAsState(
                targetValue = if (selected) CouiTheme.colorScheme.accent.copy(alpha = .14f) else Color.Transparent,
                label = "couiScheduleMarker",
            )
            val shape = CouiPageDefaults.shape
            Box(modifier.clip(shape).background(color).then(
                if (selected) Modifier.border(1.dp, CouiTheme.colorScheme.accent.copy(alpha = .65f), shape) else Modifier
            ), content = content)
        }
    }
}

@Composable
actual fun UiFloatingScheduleActions(
    modifier: Modifier,
    onCancel: () -> Unit,
    content: @Composable RowScope.(() -> Unit) -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> Surface(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
        ) { Row(content = { content(onCancel) }) }
        UiStyle.MIUIX -> MiuixCard(
            modifier = modifier,
            colors = MiuixCardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
        ) { Row(content = { content(onCancel) }) }
        UiStyle.COUI -> {
            val shape = CouiPageDefaults.shape
            Row(modifier.clip(shape).background(CouiTheme.colorScheme.card)
                .border(1.dp, CouiTheme.colorScheme.accent.copy(alpha = .45f), shape),
                content = { content(onCancel) })
        }
    }
}
