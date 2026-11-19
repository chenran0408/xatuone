package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

@Composable
expect fun uiScheduleCourseShape(cornerRadius: Dp): Shape

@Composable
expect fun UiScheduleCourseCard(
    modifier: Modifier,
    shape: Shape,
    containerColor: Color,
    raised: Boolean,
    pressed: Boolean,
    content: @Composable ColumnScope.() -> Unit,
)

@Composable
expect fun UiScheduleMarker(
    modifier: Modifier,
    selected: Boolean,
    cornerRadius: Dp,
    content: @Composable BoxScope.() -> Unit,
)

@Composable
expect fun UiFloatingScheduleActions(
    modifier: Modifier,
    onCancel: () -> Unit,
    content: @Composable RowScope.(() -> Unit) -> Unit,
)
