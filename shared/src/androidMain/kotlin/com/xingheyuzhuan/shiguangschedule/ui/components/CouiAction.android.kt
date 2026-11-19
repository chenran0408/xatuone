package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme

// alpha7 exposes dialog actions and toolbar buttons, but no public general-purpose Button.
@Composable
internal fun CouiAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, filled: Boolean = false, minHeight: Dp = 48.dp, contentColor: Color = Color.Unspecified) {
    val source = remember { MutableInteractionSource() }
    val colors = CouiTheme.colorScheme
    // Keep the action roles aligned with alpha7's Dialog actions: filled actions
    // use the accent/onAccent pair, while quiet actions remain accent text.
    val foreground = when {
        !enabled -> colors.onSurfaceVariant.copy(alpha = .38f)
        contentColor != Color.Unspecified -> contentColor
        filled -> colors.onAccent
        else -> colors.accentOnSurface
    }
    val container = if (filled) {
        colors.accent.copy(alpha = if (enabled) 1f else .12f)
    } else Color.Transparent
    Box(modifier.defaultMinSize(minHeight = minHeight).clip(CouiPageDefaults.shape)
        .background(container)
        .couiButtonFeedback(interactionSource = source, enabled = enabled)
        .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(text = text, kind = CouiTextKind.PreferenceValue, color = foreground)
    }
}
