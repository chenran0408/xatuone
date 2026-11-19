package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun UiDatePickerDialog(
    initialDateMillis: Long?,
    onDismissRequest: () -> Unit,
    onConfirm: (Long) -> Unit,
    saving: Boolean,
) {
    var epochDay by rememberSaveable {
        mutableLongStateOf(initialDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
            ?: LocalDate.now().toEpochDay())
    }
    val date = LocalDate.ofEpochDay(epochDay)
    val confirm = { onConfirm(date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()) }
    val dismiss = { if (!saving) onDismissRequest() }
    val confirmLabel = if (saving) "正在保存…" else "确定"
    if (LocalUiStyle.current == UiStyle.MATERIAL) {
        val picker = rememberDatePickerState(date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        LaunchedEffect(picker.selectedDateMillis) { picker.selectedDateMillis?.let { epochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() } }
        DatePickerDialog(onDismissRequest = dismiss,
            confirmButton = {
                TextButton(
                    onClick = { picker.selectedDateMillis?.let(onConfirm) },
                    enabled = !saving && picker.selectedDateMillis != null,
                ) { Text(confirmLabel) }
            },
            dismissButton = { TextButton(onClick = dismiss, enabled = !saving) { Text("取消") } },
        ) { DatePicker(picker, showModeToggle = true) }
    } else UiDialog(onDismissRequest = dismiss, title = "选择日期",
        confirmButton = UiDialogAction(confirmLabel, confirm, enabled = !saving),
        dismissButton = UiDialogAction("取消", dismiss, enabled = !saving), text = {
            if (LocalUiStyle.current == UiStyle.COUI) com.itos.coui.picker.CalendarPicker(
                value = date, onValueChange = { epochDay = it.toEpochDay() })
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                fun change(year: Int = date.year, month: Int = date.monthValue, day: Int = date.dayOfMonth) {
                    val first = LocalDate.of(year, month, 1)
                    epochDay = first.withDayOfMonth(day.coerceAtMost(first.lengthOfMonth())).toEpochDay()
                }
                top.yukonga.miuix.kmp.basic.NumberPicker(value = date.year, onValueChange = { change(year = it) },
                    modifier = Modifier.weight(1.4f).semantics { contentDescription = "年" }, range = 1900..2100,
                    visibleItemCount = 3, label = { "${it}年" }, textStyle = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body1)
                top.yukonga.miuix.kmp.basic.NumberPicker(value = date.monthValue, onValueChange = { change(month = it) },
                    modifier = Modifier.weight(1f).semantics { contentDescription = "月" }, range = 1..12,
                    visibleItemCount = 3, label = { "${it}月" }, textStyle = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body1)
                top.yukonga.miuix.kmp.basic.NumberPicker(value = date.dayOfMonth, onValueChange = { change(day = it) },
                    modifier = Modifier.weight(1f).semantics { contentDescription = "日" }, range = 1..date.lengthOfMonth(),
                    visibleItemCount = 3, label = { "${it}日" }, textStyle = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body1)
            }
        })
}
