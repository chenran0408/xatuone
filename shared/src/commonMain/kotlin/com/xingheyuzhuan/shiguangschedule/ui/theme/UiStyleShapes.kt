package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle

internal fun shapesForStyle(style: UiStyle): Shapes = when (style) {
    UiStyle.MATERIAL -> Shapes()
    UiStyle.MIUIX -> Shapes(
        extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )
    UiStyle.COUI -> Shapes(
        extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )
}
