package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.itos.coui.dialog.CouiDialogAction

@Composable
actual fun UiSelectionCard(selected: Boolean, onClick: () -> Unit, modifier: Modifier,
    highlighted: Boolean, compact: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val current by rememberUpdatedState(content)
    val body = remember { movableContentOf { Column(content = current) } }
    val style = LocalUiStyle.current
    val selectedContainerColor by animateColorAsState(
        targetValue = when (style) {
            UiStyle.MATERIAL -> if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
            UiStyle.MIUIX -> if (selected) top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary.copy(alpha = .14f) else top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainer
            UiStyle.COUI -> if (selected) com.itos.coui.theme.CouiTheme.colorScheme.accent.copy(alpha = .14f) else com.itos.coui.theme.CouiTheme.colorScheme.card
        },
        animationSpec = when (style) {
            UiStyle.MATERIAL -> tween(180)
            UiStyle.MIUIX -> spring(dampingRatio = .9f, stiffness = 520f)
            UiStyle.COUI -> tween(220)
        },
        label = "selectionContainer"
    )
    val selectedScale by animateFloatAsState(
        targetValue = if (selected) 1f else .99f,
        animationSpec = when (style) {
            UiStyle.MATERIAL -> tween(140)
            UiStyle.MIUIX -> spring(dampingRatio = .86f, stiffness = 480f)
            UiStyle.COUI -> tween(180)
        },
        label = "selectionScale"
    )
    val selection = Modifier.heightIn(min = UiMetrics.TouchTarget)
        .graphicsLayer {
            scaleX = selectedScale
            scaleY = selectedScale
        }
        .selectable(selected, role = Role.RadioButton, onClick = onClick).semantics(mergeDescendants = true) {}
    Box(modifier = modifier) {
        if (compact) {
            val currentWeekOutline = if (highlighted && !selected) {
                Modifier.border(1.dp, uiStyleColors().accent, RoundedCornerShape(12.dp))
            } else Modifier
            UiScheduleMarker(selection.fillMaxWidth().then(currentWeekOutline), selected, cornerRadius = 12.dp) {
                body()
            }
        } else {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> Card(modifier = selection, colors = CardDefaults.cardColors(
            containerColor = if (highlighted && !selected) MaterialTheme.colorScheme.surfaceContainerHigh else selectedContainerColor)) {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { body() }
                RadioButton(selected = selected, onClick = null)
            }
        }
        UiStyle.MIUIX -> top.yukonga.miuix.kmp.basic.BasicComponent(modifier = selection,
            endActions = { top.yukonga.miuix.kmp.basic.RadioButton(selected, onClick = null) }) { body() }
        UiStyle.COUI -> com.itos.coui.layout.Item(modifier = selection,
            title = { body() }, trailing = { com.itos.coui.radiobutton.RadioButton(selected = selected) })
    }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun UiSheet(onDismissRequest: () -> Unit, skipPartiallyExpanded: Boolean,
    sheetGesturesEnabled: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val current by rememberUpdatedState(content)
    val body = remember { movableContentOf { Column(content = current) } }
    val height = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * .78f }
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> ModalBottomSheet(onDismissRequest = onDismissRequest,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded),
            sheetGesturesEnabled = sheetGesturesEnabled) { body() }
        UiStyle.MIUIX -> top.yukonga.miuix.kmp.window.WindowBottomSheet(show = true,
            onDismissRequest = onDismissRequest, enableNestedScroll = sheetGesturesEnabled,
            insideMargin = androidx.compose.ui.unit.DpSize(0.dp, 0.dp)) {
            Box(Modifier.heightIn(max = height)) { body() }
        }
        UiStyle.COUI -> com.itos.coui.dialog.Dialog(onDismissRequest = onDismissRequest,
            confirmAction = CouiDialogAction("关闭", onDismissRequest)) {
            Box(Modifier.heightIn(max = height)) { body() }
        }
    }
}

@Composable
actual fun UiDialog(onDismissRequest: () -> Unit, title: String,
    text: (@Composable () -> Unit)?, confirmButton: UiDialogAction?, dismissButton: UiDialogAction?, neutralButton: UiDialogAction?) {
    val height by rememberUpdatedState(with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * .55f })
    val currentText by rememberUpdatedState(text)
    val body = remember { movableContentOf {
        Column(Modifier.fillMaxWidth().heightIn(max = height).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            currentText?.let { ProvideTextStyle(MaterialTheme.typography.bodyMedium) { it() } }
        }
    } }
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> AlertDialog(onDismissRequest = onDismissRequest, title = { Text(title) }, text = { body() },
            confirmButton = { confirmButton?.let { TextButton(onClick = it.onClick, enabled = it.enabled) { Text(it.text) } } },
            dismissButton = {
                neutralButton?.let { TextButton(onClick = it.onClick, enabled = it.enabled) { Text(it.text) } }
                dismissButton?.let { TextButton(onClick = it.onClick, enabled = it.enabled) { Text(it.text) } }
            })
        UiStyle.MIUIX -> top.yukonga.miuix.kmp.window.WindowDialog(show = true, title = title, onDismissRequest = onDismissRequest) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f, fill = false)) { body() }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOfNotNull(neutralButton, dismissButton).forEach { action ->
                        top.yukonga.miuix.kmp.basic.TextButton(
                            text = action.text, onClick = action.onClick, enabled = action.enabled,
                        )
                    }
                    confirmButton?.let { action ->
                        top.yukonga.miuix.kmp.basic.TextButton(
                            text = action.text, onClick = action.onClick, enabled = action.enabled,
                            colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                }
            }
        }
        UiStyle.COUI -> {
            // COUI's public Dialog owns action arrangement, focus, sizing and window motion.
            val confirm = requireNotNull(confirmButton ?: dismissButton)
            com.itos.coui.dialog.Dialog(onDismissRequest = onDismissRequest, title = title,
                confirmAction = CouiDialogAction(confirm.text, confirm.onClick, confirm.enabled),
                dismissAction = dismissButton?.takeIf { confirmButton != null }?.let { CouiDialogAction(it.text, it.onClick, it.enabled) },
                neutralAction = neutralButton?.let { CouiDialogAction(it.text, it.onClick, it.enabled) }) {
                Box(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) { body() }
            }
        }
    }
}
