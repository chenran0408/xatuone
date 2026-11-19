package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.switch.Switch as CouiSwitch
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch

@Composable
actual fun UiActionButton(onClick: () -> Unit, modifier: Modifier, enabled: Boolean, text: String) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> Button(onClick, modifier.heightIn(min = UiMetrics.TouchTarget), enabled = enabled) { UiText(text) }
        UiStyle.MIUIX -> MiuixButton(
            onClick, modifier.heightIn(min = UiMetrics.TouchTarget), enabled = enabled,
            colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.buttonColorsPrimary(),
        ) { UiText(text) }
        UiStyle.COUI -> CouiAction(text, onClick, modifier, enabled, filled = true)
    }
}

@Composable
actual fun UiInputField(value: String, onValueChange: (String) -> Unit, modifier: Modifier,
    label: String, enabled: Boolean, singleLine: Boolean, keyboardOptions: KeyboardOptions) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> OutlinedTextField(value, onValueChange, modifier, enabled = enabled,
            label = { UiText(label) }, singleLine = singleLine, keyboardOptions = keyboardOptions)
        UiStyle.MIUIX -> MiuixTextField(value, onValueChange, modifier, label = label, enabled = enabled,
            singleLine = singleLine, keyboardOptions = keyboardOptions)
        // COUI has no general inline TextField. Compose editing preserves keyboard/selection
        // behavior inside its native preference row, rather than substituting a Material field.
        UiStyle.COUI -> androidx.compose.foundation.text.BasicTextField(
            value, onValueChange, modifier,
            enabled = enabled, singleLine = singleLine, keyboardOptions = keyboardOptions,
            textStyle = com.itos.coui.text.rememberCouiTextStyle(com.itos.coui.text.CouiTextKind.Body)
                .copy(color = com.itos.coui.theme.CouiTheme.colorScheme.onSurface),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(com.itos.coui.theme.CouiTheme.colorScheme.accent),
            decorationBox = { input ->
                com.itos.coui.layout.Item(
                    enabled = enabled,
                    title = { com.itos.coui.text.Text(text = label, kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                    summary = { Box(Modifier.fillMaxWidth()) { input() } },
                )
            },
        )
    }
}

@Composable
actual fun UiSettingRow(title: String, summary: String, modifier: Modifier, onClick: (() -> Unit)?,
    leadingContent: (@Composable () -> Unit)?, trailingContent: (@Composable () -> Unit)?) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> androidx.compose.material3.ListItem(
            modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            headlineContent = { UiText(title) },
            supportingContent = if (summary.isEmpty()) null else ({ UiText(summary) }),
            leadingContent = leadingContent, trailingContent = trailingContent,
        )
        UiStyle.MIUIX -> top.yukonga.miuix.kmp.basic.BasicComponent(
            title = title, summary = summary.takeIf { it.isNotEmpty() }, modifier = modifier,
            onClick = onClick, startAction = leadingContent,
            endActions = trailingContent?.let { action -> { action() } },
        )
        UiStyle.COUI -> com.itos.coui.layout.Item(
            modifier = modifier, onClick = onClick, icon = leadingContent,
            title = { com.itos.coui.text.Text(text = title, kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start) },
            summary = if (summary.isEmpty()) null else ({ com.itos.coui.text.Text(text = summary, kind = com.itos.coui.text.CouiTextKind.PreferenceSummary) }),
            trailing = { trailingContent?.invoke() },
        )
    }
}

@Composable
actual fun UiSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier, enabled: Boolean) {
    val touchModifier = modifier.sizeIn(minWidth = UiMetrics.TouchTarget, minHeight = UiMetrics.TouchTarget)
    when (LocalUiStyle.current) {
        UiStyle.MIUIX -> MiuixSwitch(checked, onCheckedChange, touchModifier, enabled = enabled)
        UiStyle.COUI -> CouiSwitch(
            modifier = touchModifier, checked = checked, onCheckedChange = onCheckedChange, enabled = enabled,
        )
        UiStyle.MATERIAL -> {
            val colors = uiStyleColors()
            Switch(
                checked, onCheckedChange, touchModifier, enabled = enabled,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.accent),
            )
        }
    }
}

@Composable
actual fun UiListDivider() {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> androidx.compose.material3.HorizontalDivider()
        UiStyle.MIUIX -> top.yukonga.miuix.kmp.basic.HorizontalDivider()
        UiStyle.COUI -> com.itos.coui.layout.Divider()
    }
}

@Composable
actual fun UiLoadingIndicator(modifier: Modifier) {
    when (LocalUiStyle.current) {
        UiStyle.MIUIX -> top.yukonga.miuix.kmp.basic.CircularProgressIndicator(modifier)
        UiStyle.MATERIAL -> androidx.compose.material3.CircularProgressIndicator(modifier, color = uiStyleColors().accent, strokeWidth = 2.dp)
        UiStyle.COUI -> CouiLoadingIndicator(modifier)
    }
}
