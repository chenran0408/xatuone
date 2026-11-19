package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.key
import com.itos.coui.backdrop.rememberCouiBackdrop
import com.itos.coui.chip.ChipTabRow
import com.itos.coui.chip.CouiChipTabItem
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog
import com.itos.coui.picker.CalendarPicker
import com.itos.coui.picker.WheelPicker
import com.itos.coui.theme.CouiTheme
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun UiValuePicker(
    title: String,
    values: List<String>,
    selectedIndex: Int,
    selectedValue: String,
    enabled: Boolean,
    modifier: Modifier,
    onSelect: (Int) -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (enabled) expanded = !expanded },
                modifier = modifier,
            ) {
                OutlinedTextField(
                    value = selectedValue,
                    onValueChange = {},
                    readOnly = true,
                    enabled = enabled,
                    label = { Text(title) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = enabled,
                    ).fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    values.forEachIndexed { index, value ->
                        DropdownMenuItem(
                            text = { Text(value) },
                            onClick = {
                                onSelect(index)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
        UiStyle.MIUIX -> MiuixValuePicker(
            title = title,
            values = values,
            selectedIndex = selectedIndex,
            selectedValue = selectedValue,
            enabled = enabled,
            onSelect = onSelect,
        )
        UiStyle.COUI -> CouiValuePicker(
            title = title,
            options = values,
            selectedIndex = selectedIndex,
            selectedValue = selectedValue,
            enabled = enabled,
            onSelected = onSelect,
        )
    }
}

@Composable
actual fun CouiDateRangePickerDialog(
    startDate: String,
    endDate: String,
    title: String,
    startLabel: String,
    endLabel: String,
    confirmLabel: String,
    cancelLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var startText by rememberSaveable(startDate) { mutableStateOf(startDate) }
    var endText by rememberSaveable(endDate) { mutableStateOf(endDate) }
    var selectedEndpoint by rememberSaveable { mutableIntStateOf(0) }
    val start = remember(startText) { java.time.LocalDate.parse(startText) }
    val end = remember(endText) { java.time.LocalDate.parse(endText) }
    val backdrop = rememberCouiBackdrop()

    Dialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmAction = CouiDialogAction(
            text = confirmLabel,
            onClick = { onConfirm(startText, endText) },
            style = CouiDialogActionStyle.Primary,
            colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
        ),
        dismissAction = CouiDialogAction(cancelLabel, onDismiss),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChipTabRow(
                items = listOf(
                    CouiChipTabItem("start", "$startLabel  $startText"),
                    CouiChipTabItem("end", "$endLabel  $endText"),
                ),
                selectedKey = if (selectedEndpoint == 0) "start" else "end",
                onSelectedKeyChange = { selectedEndpoint = if (it == "start") 0 else 1 },
                backdrop = backdrop,
                animationLevel = 3,
            )
            key(selectedEndpoint, if (selectedEndpoint == 0) startText else endText) {
                CalendarPicker(
                    value = if (selectedEndpoint == 0) start else end,
                    onValueChange = { date ->
                        if (selectedEndpoint == 0) {
                            startText = date.toString()
                            if (end < date) endText = date.toString()
                        } else {
                            endText = date.toString()
                        }
                    },
                    minDate = if (selectedEndpoint == 0) {
                        java.time.LocalDate.of(1900, 1, 1)
                    } else {
                        start
                    },
                    maxDate = java.time.LocalDate.of(2100, 12, 31),
                )
            }
        }
    }
}

@Composable
actual fun UiTimeWheelPicker(
    value: Int,
    range: IntRange,
    label: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier,
) {
    when (LocalUiStyle.current) {
        UiStyle.COUI -> WheelPicker(
            modifier = modifier,
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            label = label,
            textSize = 18.dp,
            focusTextSize = 22.dp,
            formatter = { it.toString().padStart(2, '0') },
        )
        UiStyle.MATERIAL, UiStyle.MIUIX -> {
            val values = remember(range) { range.map { it.toString().padStart(2, '0') } }
            NativeNumberPicker(
                values = values,
                selectedValue = value.toString().padStart(2, '0'),
                onValueChange = { onValueChange(it.toInt()) },
                modifier = modifier,
                dividerColor = uiStyleColors().accent,
            )
        }
    }
}
