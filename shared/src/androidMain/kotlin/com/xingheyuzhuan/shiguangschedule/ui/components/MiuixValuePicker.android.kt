package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.DropdownArrowEndAction
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

/** Two callers: semester selection and the existing dorm hierarchy. */
@Composable
internal fun MiuixValuePicker(title: String, values: List<String>, selectedIndex: Int, enabled: Boolean = true,
    selectedValue: String? = values.getOrNull(selectedIndex), onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        BasicComponent(title = title, summary = selectedValue, enabled = enabled && values.isNotEmpty(),
            endActions = { DropdownArrowEndAction(MiuixTheme.colorScheme.onSurfaceVariantActions) },
            role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "选择$title",
            onClick = { open = true })
        WindowListPopup(show = open, onDismissRequest = { open = false }, maxHeight = 320.dp) {
            LazyColumn(Modifier.width(288.dp).heightIn(max = 320.dp),
                state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0))) {
                itemsIndexed(values) { index, value ->
                    DropdownImpl(item = DropdownItem(text = value), optionSize = values.size,
                        isSelected = index == selectedIndex, index = index,
                        onSelectedIndexChange = { open = false; onSelect(it) })
                }
            }
        }
    }
}
