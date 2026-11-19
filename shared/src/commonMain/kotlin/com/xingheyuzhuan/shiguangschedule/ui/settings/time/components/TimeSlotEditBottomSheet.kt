package com.xingheyuzhuan.shiguangschedule.ui.settings.time.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.ui.components.UiButton
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCard
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialogAction
import com.xingheyuzhuan.shiguangschedule.ui.components.UiInputField
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.UiTimeWheelPicker
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_add
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_save_changes
import shiguangschedule.shared.generated.resources.dialog_title_add_time_slot
import shiguangschedule.shared.generated.resources.dialog_title_edit_time_slot
import shiguangschedule.shared.generated.resources.label_time_picker_end
import shiguangschedule.shared.generated.resources.label_time_picker_hour
import shiguangschedule.shared.generated.resources.label_time_picker_minute
import shiguangschedule.shared.generated.resources.label_time_picker_start
import shiguangschedule.shared.generated.resources.label_time_slot_alias
import shiguangschedule.shared.generated.resources.toast_end_time_must_be_later
import shiguangschedule.shared.generated.resources.toast_time_conflict

@Composable
fun TimeSlotEditContent(
    existingTimeSlots: List<TimeSlot>,
    initialNumber: Int,
    initialStartTime: String,
    initialEndTime: String,
    initialAlias: String?,
    isEditing: Boolean,
    dialogActions: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (number: Int, startTime: String, endTime: String, alias: String?) -> Unit
) {
    val (initialStartHour, initialStartMinute) = parseTimeString(initialStartTime)
    val (initialEndHour, initialEndMinute) = parseTimeString(initialEndTime)

    var startHourState by remember { mutableIntStateOf(initialStartHour) }
    var startMinuteState by remember { mutableIntStateOf(initialStartMinute) }
    var endHourState by remember { mutableIntStateOf(initialEndHour) }
    var endMinuteState by remember { mutableIntStateOf(initialEndMinute) }
    var aliasState by remember { mutableStateOf(initialAlias ?: "") }

    val dialogTitleEdit = stringResource(Res.string.dialog_title_edit_time_slot)
    val dialogTitleAdd = stringResource(Res.string.dialog_title_add_time_slot)
    val labelStart = stringResource(Res.string.label_time_picker_start)
    val labelEnd = stringResource(Res.string.label_time_picker_end)
    val labelHour = stringResource(Res.string.label_time_picker_hour)
    val labelMinute = stringResource(Res.string.label_time_picker_minute)
    val actionCancel = stringResource(Res.string.action_cancel)
    val actionSaveChanges = stringResource(Res.string.action_save_changes)
    val actionAdd = stringResource(Res.string.action_add)
    val toastEndTimeMustBeLater = stringResource(Res.string.toast_end_time_must_be_later)
    val toastTimeConflict = stringResource(Res.string.toast_time_conflict)

    val currentTimeRange by remember(startHourState, startMinuteState, endHourState, endMinuteState) {
        derivedStateOf {
            val start = LocalTime(startHourState, startMinuteState)
            val end = LocalTime(endHourState, endMinuteState)
            "${formatTime(start)} - ${formatTime(end)}"
        }
    }

    val confirmAction = {
        val startTimeObj = LocalTime(startHourState, startMinuteState)
        val endTimeObj = LocalTime(endHourState, endMinuteState)

        if (endTimeObj <= startTimeObj) {
            ToastManager.show(toastEndTimeMustBeLater)
        } else {
            val hasConflict = existingTimeSlots.any { slot ->
                if (isEditing && slot.number == initialNumber) return@any false
                val slotStart = parseLocalTimeSafely(slot.startTime)
                val slotEnd = parseLocalTimeSafely(slot.endTime)
                maxOf(startTimeObj, slotStart) < minOf(endTimeObj, slotEnd)
            }

            if (hasConflict) {
                ToastManager.show(toastTimeConflict)
            } else {
                onConfirm(
                    initialNumber,
                    formatTime(startTimeObj),
                    formatTime(endTimeObj),
                    aliasState.ifBlank { null }
                )
            }
        }
    }

    val formFields: @Composable ColumnScope.() -> Unit = {
        UiInputField(
            value = aliasState,
            onValueChange = { if (it.length <= 5) aliasState = it },
            label = stringResource(Res.string.label_time_slot_alias),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        )
        UiText(
            text = "${aliasState.length}/5",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            color = uiStyleColors().secondaryText,
            textAlign = TextAlign.End,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (dialogActions) {
                    UiText(
                        labelStart,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        UiText(labelStart, style = MaterialTheme.typography.bodySmall)
                        UiText(labelHour, style = MaterialTheme.typography.labelSmall, color = uiStyleColors().secondaryText)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        UiText(":", style = MaterialTheme.typography.bodySmall)
                        UiText(labelMinute, style = MaterialTheme.typography.labelSmall, color = uiStyleColors().secondaryText)
                    }
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (dialogActions) {
                    UiText(
                        labelEnd,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        UiText(labelEnd, style = MaterialTheme.typography.bodySmall)
                        UiText(labelHour, style = MaterialTheme.typography.labelSmall, color = uiStyleColors().secondaryText)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        UiText(":", style = MaterialTheme.typography.bodySmall)
                        UiText(labelMinute, style = MaterialTheme.typography.labelSmall, color = uiStyleColors().secondaryText)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UiTimeWheelPicker(
                    value = startHourState,
                    range = 0..23,
                    label = "$labelStart $labelHour",
                    onValueChange = { startHourState = it },
                    modifier = Modifier
                        .height(150.dp)
                        .weight(1f)
                )
                UiText(
                    ":",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    textAlign = TextAlign.Center
                )
                UiTimeWheelPicker(
                    value = startMinuteState,
                    range = 0..59,
                    label = "$labelStart $labelMinute",
                    onValueChange = { startMinuteState = it },
                    modifier = Modifier
                        .height(150.dp)
                        .weight(1f)
                )
            }

            UiText(
                "-",
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UiTimeWheelPicker(
                    value = endHourState,
                    range = 0..23,
                    label = "$labelEnd $labelHour",
                    onValueChange = { endHourState = it },
                    modifier = Modifier
                        .height(150.dp)
                        .weight(1f)
                )
                UiText(
                    ":",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    textAlign = TextAlign.Center
                )
                UiTimeWheelPicker(
                    value = endMinuteState,
                    range = 0..59,
                    label = "$labelEnd $labelMinute",
                    onValueChange = { endMinuteState = it },
                    modifier = Modifier
                        .height(150.dp)
                        .weight(1f)
                )
            }
        }

        if (dialogActions) {
            UiText(
                text = currentTimeRange,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            UiCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    UiText(
                        text = currentTimeRange,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        UiButton(text = actionCancel, onClick = onDismiss)
                        Spacer(modifier = Modifier.width(8.dp))
                        UiButton(
                            text = if (isEditing) actionSaveChanges else actionAdd,
                            selected = true,
                            onClick = confirmAction,
                        )
                    }
                }
            }
        }
    }

    if (dialogActions) {
        UiDialog(
            onDismissRequest = onDismiss,
            title = if (isEditing) dialogTitleEdit else dialogTitleAdd,
            text = { Column(modifier = Modifier.fillMaxWidth(), content = formFields) },
            confirmButton = UiDialogAction(
                text = if (isEditing) actionSaveChanges else actionAdd,
                onClick = confirmAction,
            ),
            dismissButton = UiDialogAction(text = actionCancel, onClick = onDismiss),
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UiText(
                    text = if (isEditing) dialogTitleEdit else dialogTitleAdd,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            formFields()
        }
    }
}

// 供 BottomSheet 和外部使用的辅助计算工具函数

fun calculateInitialTimes(
    isEditing: Boolean,
    editingTimeSlot: TimeSlot?,
    localTimeSlots: List<TimeSlot>,
    breakDur: Int,
    classDur: Int
): Pair<String, String> {
    if (isEditing && editingTimeSlot != null) return Pair(editingTimeSlot.startTime, editingTimeSlot.endTime)

    return if (localTimeSlots.isNotEmpty()) {
        val lastEndTimeStr = localTimeSlots.maxOf { it.endTime }
        val lastEndTime = parseLocalTimeSafely(lastEndTimeStr, fallback = LocalTime(8, 0))
        val start = lastEndTime.addMinutes(breakDur)
        val end = start.addMinutes(classDur)
        Pair(formatTime(start), formatTime(end))
    } else {
        val start = LocalTime(8, 0)
        Pair(formatTime(start), formatTime(start.addMinutes(classDur)))
    }
}

fun formatTwoDigits(value: Int): String = value.toString().padStart(2, '0')

fun formatTime(time: LocalTime): String = "${formatTwoDigits(time.hour)}:${formatTwoDigits(time.minute)}"

fun parseTimeString(timeString: String): Pair<Int, Int> {
    return try {
        val time = LocalTime.parse(timeString)
        Pair(time.hour, time.minute)
    } catch (_: Exception) {
        Pair(0, 0)
    }
}

fun parseLocalTimeSafely(timeStr: String, fallback: LocalTime = LocalTime(23, 59)): LocalTime {
    return try {
        LocalTime.parse(timeStr)
    } catch (_: Exception) {
        fallback
    }
}

fun LocalTime.addMinutes(minutes: Int): LocalTime {
    val totalMinutes = this.hour * 60 + this.minute + minutes
    val newTotalMinutes = ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    return LocalTime(newTotalMinutes / 60, newTotalMinutes % 60)
}
