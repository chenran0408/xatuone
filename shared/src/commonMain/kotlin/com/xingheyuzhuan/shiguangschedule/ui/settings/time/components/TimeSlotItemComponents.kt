package com.xingheyuzhuan.shiguangschedule.ui.settings.time.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCard
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCourseIconButton
import com.xingheyuzhuan.shiguangschedule.ui.components.UiInputField
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_delete_time_slot
import shiguangschedule.shared.generated.resources.delete_24px
import shiguangschedule.shared.generated.resources.label_break_duration_minutes
import shiguangschedule.shared.generated.resources.label_class_duration_minutes
import shiguangschedule.shared.generated.resources.time_slot_section_number
import shiguangschedule.shared.generated.resources.title_default_duration_settings
import shiguangschedule.shared.generated.resources.toast_break_duration_non_negative
import shiguangschedule.shared.generated.resources.toast_class_duration_positive

@Composable
fun DefaultDurationSettings(
    defaultClassDuration: Int,
    onClassDurationChange: (Int) -> Unit,
    defaultBreakDuration: Int,
    onBreakDurationChange: (Int) -> Unit
) {
    val titleDefaultDurationSettings = stringResource(Res.string.title_default_duration_settings)
    val labelClassDuration = stringResource(Res.string.label_class_duration_minutes)
    val toastClassDurationPositive = stringResource(Res.string.toast_class_duration_positive)
    val labelBreakDuration = stringResource(Res.string.label_break_duration_minutes)
    val toastBreakDurationNonNegative = stringResource(Res.string.toast_break_duration_non_negative)

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        UiText(titleDefaultDurationSettings, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            UiInputField(
                value = if (defaultClassDuration == 0) "" else defaultClassDuration.toString(),
                onValueChange = { newValueStr ->
                    val newIntValue = newValueStr.toIntOrNull()
                    if (newValueStr.isEmpty()) {
                        onClassDurationChange(0)
                    } else if (newIntValue != null && newIntValue > 0) {
                        onClassDurationChange(newIntValue)
                    } else if (newIntValue != null) {
                        ToastManager.show(toastClassDurationPositive)
                    }
                },
                label = labelClassDuration,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            UiInputField(
                value = if (defaultBreakDuration == -1) "" else defaultBreakDuration.toString(),
                onValueChange = { newValueStr ->
                    val newIntValue = newValueStr.toIntOrNull()
                    if (newValueStr.isEmpty()) {
                        onBreakDurationChange(-1)
                    } else if (newIntValue != null && newIntValue >= 0) {
                        onBreakDurationChange(newIntValue)
                    } else if (newIntValue != null) {
                        ToastManager.show(toastBreakDurationNonNegative)
                    }
                },
                label = labelBreakDuration,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TimeSlotItem(
    timeSlot: TimeSlot,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val a11yDeleteTimeSlot = stringResource(Res.string.a11y_delete_time_slot)

    UiCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEditClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UiText(
                text = stringResource(Res.string.time_slot_section_number, timeSlot.number.toString()),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(72.dp),
                maxLines = 1
            )
            UiText(
                text = timeSlot.alias ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = uiStyleColors().accent,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            UiText(
                text = "${timeSlot.startTime} - ${timeSlot.endTime}",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                maxLines = 1
            )
            UiCourseIconButton(
                icon = vectorResource(Res.drawable.delete_24px),
                description = a11yDeleteTimeSlot,
                tint = MaterialTheme.colorScheme.error,
                onClick = onDeleteClick,
            )
        }
    }
}
