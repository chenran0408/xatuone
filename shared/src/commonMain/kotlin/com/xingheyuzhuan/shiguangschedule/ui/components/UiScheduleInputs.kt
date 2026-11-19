package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun UiValuePicker(
    title: String,
    values: List<String>,
    selectedIndex: Int,
    selectedValue: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
)

@Composable
expect fun CouiDateRangePickerDialog(
    startDate: String,
    endDate: String,
    title: String,
    startLabel: String,
    endLabel: String,
    confirmLabel: String,
    cancelLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
)

@Composable
expect fun UiTimeWheelPicker(
    value: Int,
    range: IntRange,
    label: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
)
