package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.itos.coui.layout.Item
import com.itos.coui.menu.*
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text

@Composable
internal fun CouiValuePicker(title: String, options: List<String>, selectedIndex: Int,
    enabled: Boolean = true, selectedValue: String? = options.getOrNull(selectedIndex),
    onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val anchor = rememberCouiMenuAnchor()
    Box(Modifier.fillMaxWidth()) {
        Item(modifier = Modifier.couiMenuAnchor(anchor), enabled = enabled && options.isNotEmpty(),
            onClick = { anchor.open { expanded = true } },
            title = { Text(text = title, kind = CouiTextKind.PreferenceTitle) },
            summary = { Text(text = selectedValue ?: "未选择", kind = CouiTextKind.PreferenceSummary) },
            trailing = { Text(text = "⌄", kind = CouiTextKind.PreferenceValue) })
        Menu(expanded = expanded, onDismissRequest = { expanded = false }, anchor = anchor,
            items = options.mapIndexed { index, label -> CouiMenuItem(index, label, selected = index == selectedIndex) },
            onItemClick = { expanded = false; onSelected(it.value) })
    }
}
