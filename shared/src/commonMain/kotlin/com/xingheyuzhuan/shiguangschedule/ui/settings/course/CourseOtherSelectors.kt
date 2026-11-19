package com.xingheyuzhuan.shiguangschedule.ui.settings.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.model.DualColor
import com.xingheyuzhuan.shiguangschedule.ui.components.UiButton
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialogAction
import com.xingheyuzhuan.shiguangschedule.ui.components.UiMetrics
import com.xingheyuzhuan.shiguangschedule.ui.components.UiScheduleMarker
import com.xingheyuzhuan.shiguangschedule.ui.components.UiSettingRow
import com.xingheyuzhuan.shiguangschedule.ui.components.UiSwitch
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_apply_to_all_schemes
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.action_double_week
import shiguangschedule.shared.generated.resources.action_select_all
import shiguangschedule.shared.generated.resources.action_single_week
import shiguangschedule.shared.generated.resources.check_24px
import shiguangschedule.shared.generated.resources.title_select_color
import shiguangschedule.shared.generated.resources.title_select_weeks


@Composable
fun ColorIndicatorSection(
    colorIndex: Int,
    colorMaps: List<DualColor>,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val displayColor = colorMaps.getOrNull(colorIndex)?.let {
        if (isDark) it.dark else it.light
    } ?: MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(16.dp)
            .background(displayColor)
            .clickable(
                onClickLabel = stringResource(Res.string.title_select_color),
                role = Role.Button,
                onClick = onClick,
            )
    )
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekSelectorBottomSheet(
    totalWeeks: Int,
    selectedWeeks: Set<Int>,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<Int>) -> Unit
) {
    var tempSelectedWeeks by remember { mutableStateOf(selectedWeeks) }
    val titleSelectWeeks = stringResource(Res.string.title_select_weeks)
    val actionSelectAll = stringResource(Res.string.action_select_all)
    val actionSingleWeek = stringResource(Res.string.action_single_week)
    val actionDoubleWeek = stringResource(Res.string.action_double_week)
    val actionCancel = stringResource(Res.string.action_cancel)
    val actionConfirm = stringResource(Res.string.action_confirm)

    UiDialog(
        onDismissRequest = onDismissRequest,
        title = titleSelectWeeks,
        confirmButton = UiDialogAction(actionConfirm, { onConfirm(tempSelectedWeeks) }),
        dismissButton = UiDialogAction(actionCancel, onDismissRequest),
        text = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                UiButton(
                    text = actionSelectAll,
                    selected = tempSelectedWeeks.size == totalWeeks && totalWeeks > 0,
                    onClick = {
                        tempSelectedWeeks = if (tempSelectedWeeks.size == totalWeeks) emptySet()
                        else (1..totalWeeks).toSet()
                    },
                )
                UiButton(
                    text = actionSingleWeek,
                    selected = tempSelectedWeeks.isNotEmpty() && tempSelectedWeeks.all { it % 2 != 0 },
                    onClick = {
                        tempSelectedWeeks = (1..totalWeeks).filter { it % 2 != 0 }.toSet()
                    },
                )
                UiButton(
                    text = actionDoubleWeek,
                    selected = tempSelectedWeeks.isNotEmpty() && tempSelectedWeeks.all { it % 2 == 0 },
                    onClick = {
                        tempSelectedWeeks = (1..totalWeeks).filter { it % 2 == 0 }.toSet()
                    },
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (week in 1..totalWeeks) {
                    val selected = week in tempSelectedWeeks
                    UiScheduleMarker(
                        modifier = Modifier.size(48.dp).toggleable(
                            value = selected,
                            role = Role.Checkbox,
                            onValueChange = {
                                tempSelectedWeeks = if (it) tempSelectedWeeks + week else tempSelectedWeeks - week
                            },
                        ),
                        selected = selected,
                        cornerRadius = 8.dp,
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            UiText(week.toString(), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerBottomSheet(
    colorMaps: List<DualColor>,
    selectedIndex: Int,
    showApplyToAllOption: Boolean = false,
    onDismissRequest: () -> Unit,
    onConfirm: (colorIndex: Int, applyToAll: Boolean) -> Unit
) {
    var tempSelectedIndex by remember { mutableIntStateOf(selectedIndex) }
    var applyToAll by remember { mutableStateOf(false) }
    val isDark = LocalIsDarkTheme.current

    UiDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.title_select_color),
        text = {
            FlowRow(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                colorMaps.forEachIndexed { index, dualColor ->
                    val selected = tempSelectedIndex == index
                    UiScheduleMarker(
                        modifier = Modifier.size(UiMetrics.TouchTarget)
                            .selectable(selected, role = Role.RadioButton, onClick = { tempSelectedIndex = index })
                            .semantics { contentDescription = (index + 1).toString() },
                        selected = selected,
                        cornerRadius = 12.dp,
                    ) {
                        Box(
                            modifier = Modifier.align(Alignment.Center)
                                .size(32.dp).clip(CircleShape)
                                .background(if (isDark) dualColor.dark else dualColor.light),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.check_24px),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
            if (showApplyToAllOption) {
                UiSettingRow(
                    title = stringResource(Res.string.action_apply_to_all_schemes),
                    onClick = { applyToAll = !applyToAll },
                    trailingContent = { UiSwitch(applyToAll, onCheckedChange = null) },
                )
            }
        },
        dismissButton = UiDialogAction(stringResource(Res.string.action_cancel), onDismissRequest),
        confirmButton = UiDialogAction(stringResource(Res.string.action_confirm), {
            onConfirm(tempSelectedIndex, applyToAll)
            onDismissRequest()
        }),
    )
}
