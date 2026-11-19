package com.xingheyuzhuan.shiguangschedule.ui.settings.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.ui.components.NativeNumberPicker
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialogAction
import com.xingheyuzhuan.shiguangschedule.ui.components.UiMetrics
import com.xingheyuzhuan.shiguangschedule.ui.components.UiScheduleMarker
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.*

/**
 * 节次时间选择底部弹窗 (节次模式)
 */
@Composable
fun CourseTimePickerBottomSheet(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    startSection: Int,
    onStartSectionChange: (Int) -> Unit,
    endSection: Int,
    onEndSectionChange: (Int) -> Unit,
    timeSlots: List<TimeSlot>,
    onDismissRequest: () -> Unit
) {
    var tempSelectedDay by remember { mutableIntStateOf(selectedDay) }
    var tempStartSection by remember { mutableIntStateOf(startSection) }
    var tempEndSection by remember { mutableIntStateOf(endSection) }

    val timeInvalidText = stringResource(Res.string.toast_time_invalid)

    UiDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.title_select_time),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    UiText(
                        text = stringResource(Res.string.label_start_section),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    UiText(
                        text = stringResource(Res.string.label_end_section),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SectionPicker(
                        selectedSection = tempStartSection,
                        onSectionSelected = {
                            tempStartSection = it
                            if (it > tempEndSection) tempEndSection = it
                        },
                        timeSlots = timeSlots,
                        modifier = Modifier.weight(1f),
                    )
                    SectionPicker(
                        selectedSection = tempEndSection,
                        onSectionSelected = { tempEndSection = it },
                        timeSlots = timeSlots,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            UiText(stringResource(Res.string.label_day_of_week), style = MaterialTheme.typography.titleSmall)
            DayPicker(selectedDay = tempSelectedDay, onDaySelected = { tempSelectedDay = it })
        },
        dismissButton = UiDialogAction(stringResource(Res.string.action_cancel), onDismissRequest),
        confirmButton = UiDialogAction(stringResource(Res.string.action_confirm), {
            if (tempStartSection > tempEndSection) {
                ToastManager.show(timeInvalidText)
            } else {
                onDaySelected(tempSelectedDay)
                onStartSectionChange(tempStartSection)
                onEndSectionChange(tempEndSection)
                onDismissRequest()
            }
        }),
    )
}

/**
 * 自定义时间模式：4滚轮时间范围选择底部弹窗
 */
@Composable
fun CustomTimeRangePickerBottomSheet(
    initialStartTime: String,
    initialEndTime: String,
    onDismissRequest: () -> Unit,
    onTimeRangeSelected: (startTime: String, endTime: String) -> Unit
) {
    fun parse(time: String) = time.split(":").let {
        (it.getOrNull(0)?.toIntOrNull() ?: 8) to (it.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    val (startHour, startMinute) = parse(initialStartTime)
    val (endHour, endMinute) = parse(initialEndTime)
    var selectedStartHour by remember { mutableIntStateOf(startHour) }
    var selectedStartMinute by remember { mutableIntStateOf(startMinute) }
    var selectedEndHour by remember { mutableIntStateOf(endHour) }
    var selectedEndMinute by remember { mutableIntStateOf(endMinute) }
    val hours = remember { (0..23).map { it.toString().padStart(2, '0') } }
    val minutes = remember { (0..59).map { it.toString().padStart(2, '0') } }
    val endTimeInvalidText = stringResource(Res.string.toast_end_time_must_be_later)

    UiDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.label_custom_time),
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    UiText(stringResource(Res.string.label_start_time), textAlign = TextAlign.Center)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NativeNumberPicker(hours, selectedStartHour.toString().padStart(2, '0'), { selectedStartHour = it.toInt() }, Modifier.weight(1f))
                        UiText(":")
                        NativeNumberPicker(minutes, selectedStartMinute.toString().padStart(2, '0'), { selectedStartMinute = it.toInt() }, Modifier.weight(1f))
                    }
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    UiText(stringResource(Res.string.label_end_time), textAlign = TextAlign.Center)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NativeNumberPicker(hours, selectedEndHour.toString().padStart(2, '0'), { selectedEndHour = it.toInt() }, Modifier.weight(1f))
                        UiText(":")
                        NativeNumberPicker(minutes, selectedEndMinute.toString().padStart(2, '0'), { selectedEndMinute = it.toInt() }, Modifier.weight(1f))
                    }
                }
            }
        },
        dismissButton = UiDialogAction(stringResource(Res.string.action_cancel), onDismissRequest),
        confirmButton = UiDialogAction(stringResource(Res.string.action_confirm), {
            val startTotal = selectedStartHour * 60 + selectedStartMinute
            val endTotal = selectedEndHour * 60 + selectedEndMinute
            if (startTotal >= endTotal) {
                ToastManager.show(endTimeInvalidText)
            } else {
                onTimeRangeSelected(
                    "${selectedStartHour.toString().padStart(2, '0')}:${selectedStartMinute.toString().padStart(2, '0')}",
                    "${selectedEndHour.toString().padStart(2, '0')}:${selectedEndMinute.toString().padStart(2, '0')}",
                )
                onDismissRequest()
            }
        }),
    )
}

/**
 * 星期选择对话框
 */
@Composable
fun DayPickerDialog(
    selectedDay: Int,
    onDismissRequest: () -> Unit,
    onDaySelected: (Int) -> Unit
) {
    var tempSelectedDay by remember { mutableIntStateOf(selectedDay) }

    UiDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.label_day_of_week),
        text = { DayPicker(selectedDay = tempSelectedDay, onDaySelected = { tempSelectedDay = it }) },
        dismissButton = UiDialogAction(stringResource(Res.string.action_cancel), onDismissRequest),
        confirmButton = UiDialogAction(stringResource(Res.string.action_confirm), {
            onDaySelected(tempSelectedDay)
            onDismissRequest()
        }),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayPicker(selectedDay: Int, onDaySelected: (Int) -> Unit) {
    val days = stringArrayResource(Res.array.week_days_full_names)
    FlowRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        days.forEachIndexed { index, day ->
            val selected = selectedDay == index + 1
            UiScheduleMarker(
                modifier = Modifier.heightIn(min = UiMetrics.TouchTarget)
                    .selectable(selected, role = Role.RadioButton, onClick = { onDaySelected(index + 1) }),
                selected = selected,
                cornerRadius = 12.dp,
            ) {
                UiText(
                    text = day,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 12.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun SectionPicker(
    selectedSection: Int,
    onSectionSelected: (Int) -> Unit,
    timeSlots: List<TimeSlot>,
    modifier: Modifier = Modifier
) {
    val sortedSlots = remember(timeSlots) {
        timeSlots.sortedBy { it.number }
    }
    val displayValues = sortedSlots.map { it.alias ?: it.number.toString() }
    val currentSlot = sortedSlots.find { it.number == selectedSection } ?: sortedSlots.firstOrNull()
    val selectedValueText = currentSlot?.let { it.alias ?: it.number.toString() } ?: ""

    NativeNumberPicker(
        values = displayValues,
        selectedValue = selectedValueText,
        onValueChange = { newValueText ->
            val pickedSlot = sortedSlots.find { (it.alias ?: it.number.toString()) == newValueText }
            pickedSlot?.let { onSectionSelected(it.number) }
        },
        modifier = modifier
    )
}
