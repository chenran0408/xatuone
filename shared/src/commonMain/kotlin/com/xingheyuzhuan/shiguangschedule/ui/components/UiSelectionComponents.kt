package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun UiSelectionCard(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    highlighted: Boolean = false, compact: Boolean = false, content: @Composable ColumnScope.() -> Unit)

@Composable
expect fun UiSheet(onDismissRequest: () -> Unit, skipPartiallyExpanded: Boolean = false,
    sheetGesturesEnabled: Boolean = true, content: @Composable ColumnScope.() -> Unit)

data class UiDialogAction(val text: String, val onClick: () -> Unit, val enabled: Boolean = true)

@Composable
expect fun UiDialog(onDismissRequest: () -> Unit, title: String,
    text: (@Composable () -> Unit)? = null, confirmButton: UiDialogAction? = null,
    dismissButton: UiDialogAction? = null, neutralButton: UiDialogAction? = null)
