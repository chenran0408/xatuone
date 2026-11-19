package com.xingheyuzhuan.shiguangschedule.ui.settings.course

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.DualColor
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCard
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCourseIconButton
import com.xingheyuzhuan.shiguangschedule.ui.components.UiInputField
import com.xingheyuzhuan.shiguangschedule.ui.components.UiSettingRow
import com.xingheyuzhuan.shiguangschedule.ui.components.UiSwitch
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_delete_time_slot
import shiguangschedule.shared.generated.resources.award_star_24px
import shiguangschedule.shared.generated.resources.chevron_right_24px
import shiguangschedule.shared.generated.resources.credit_error_invalid
import shiguangschedule.shared.generated.resources.delete_24px
import shiguangschedule.shared.generated.resources.label_credit
import shiguangschedule.shared.generated.resources.label_course_weeks
import shiguangschedule.shared.generated.resources.label_custom_time
import shiguangschedule.shared.generated.resources.label_day_of_week
import shiguangschedule.shared.generated.resources.label_none
import shiguangschedule.shared.generated.resources.label_position
import shiguangschedule.shared.generated.resources.label_remark
import shiguangschedule.shared.generated.resources.label_section_range_suffix
import shiguangschedule.shared.generated.resources.label_teacher
import shiguangschedule.shared.generated.resources.sticky_note_2_24px
import shiguangschedule.shared.generated.resources.text_weeks_selected
import shiguangschedule.shared.generated.resources.week_days_full_names

/**
 * 课程方案卡片
 * 负责展示单个课程的时间、地点、老师等具体安排
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseSchemeCard(
    scheme: CourseScheme,
    courseColorMaps: List<DualColor>,
    timeSlots: List<TimeSlot>,
    onTeacherChange: (String) -> Unit,
    onPositionChange: (String) -> Unit,
    onCreditChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onColorClick: () -> Unit,
    onTimeClick: () -> Unit,
    onWeeksClick: () -> Unit,
    onDayClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onToggleCustomTime: (Boolean) -> Unit,
    showRemoveButton: Boolean
) {
    UiCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Box {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp).padding(16.dp)) {
                UiInputField(
                    value = scheme.teacher,
                    onValueChange = onTeacherChange,
                    label = stringResource(Res.string.label_teacher),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                UiInputField(
                    value = scheme.position,
                    onValueChange = onPositionChange,
                    label = stringResource(Res.string.label_position),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(8.dp))

                val nativeFields = LocalUiStyle.current != UiStyle.MATERIAL
                val creditChanged: (String) -> Unit = { value ->
                    if (value.isEmpty() || (value.all { it.isDigit() || it == '.' } && value.count { it == '.' } <= 1)) {
                        onCreditChange(value)
                    }
                }
                if (nativeFields) {
                    UiInputField(
                        value = scheme.credit,
                        onValueChange = creditChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(Res.string.label_credit),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    if (scheme.credit.endsWith(".")) {
                        UiText(stringResource(Res.string.credit_error_invalid), color = MaterialTheme.colorScheme.error)
                    }
                } else OutlinedTextField(
                    value = scheme.credit,
                    onValueChange = creditChanged,
                    placeholder = { Text(stringResource(Res.string.label_credit)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(vectorResource(Res.drawable.award_star_24px), null, Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = scheme.credit.endsWith("."),
                    supportingText = if (scheme.credit.endsWith(".")) {
                        { Text(stringResource(Res.string.credit_error_invalid)) }
                    } else null
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (nativeFields) {
                    UiInputField(
                        value = scheme.remark,
                        onValueChange = onRemarkChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (LocalUiStyle.current == UiStyle.COUI) Modifier.heightIn(min = 56.dp, max = 140.dp) else Modifier),
                        label = stringResource(Res.string.label_remark),
                    )
                    UiText(
                        text = "${scheme.remark.length} / 300",
                        modifier = Modifier.align(Alignment.End),
                        style = MaterialTheme.typography.labelSmall,
                    )
                } else OutlinedTextField(
                    value = scheme.remark,
                    onValueChange = onRemarkChange,
                    placeholder = { Text(stringResource(Res.string.label_remark)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp, max = 140.dp),
                    leadingIcon = { Icon(imageVector = vectorResource(Res.drawable.sticky_note_2_24px), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    minLines = 1,
                    maxLines = 5,
                    supportingText = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${scheme.remark.length} / 300",
                                modifier = Modifier.align(Alignment.CenterEnd),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                UiSettingRow(
                    title = stringResource(Res.string.label_custom_time),
                    trailingContent = {
                        UiSwitch(checked = scheme.isCustomTime, onCheckedChange = onToggleCustomTime)
                    },
                )

                val dayNames = stringArrayResource(Res.array.week_days_full_names)
                val weekday = dayNames.getOrNull(scheme.day - 1).orEmpty()
                val chevron: @Composable () -> Unit = {
                    Icon(vectorResource(Res.drawable.chevron_right_24px), contentDescription = null)
                }
                if (scheme.isCustomTime) {
                    UiSettingRow(
                        title = stringResource(Res.string.label_day_of_week),
                        summary = weekday,
                        onClick = onDayClick,
                        trailingContent = chevron,
                    )
                }
                val timeDescription = if (scheme.isCustomTime) {
                    "${scheme.customStartTime.ifBlank { "00:00" }}–${scheme.customEndTime.ifBlank { "00:00" }}"
                } else {
                    val start = timeSlots.find { it.number == scheme.startSection }?.alias
                        ?: scheme.startSection.toString()
                    val end = timeSlots.find { it.number == scheme.endSection }?.alias
                        ?: scheme.endSection.toString()
                    val sections = if (start == end) start else "$start–$end"
                    "$sections ${stringResource(Res.string.label_section_range_suffix)}"
                }
                UiSettingRow(
                    title = if (scheme.isCustomTime) stringResource(Res.string.label_custom_time) else weekday,
                    summary = timeDescription,
                    onClick = onTimeClick,
                    trailingContent = chevron,
                )
                UiSettingRow(
                    title = stringResource(Res.string.label_course_weeks),
                    summary = if (scheme.weeks.isEmpty()) stringResource(Res.string.label_none) else
                        stringResource(Res.string.text_weeks_selected, scheme.weeks.sorted().joinToString(", ")),
                    onClick = onWeeksClick,
                    trailingContent = chevron,
                )
                if (showRemoveButton) {
                    Box(modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) {
                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.delete_24px),
                            description = stringResource(Res.string.a11y_delete_time_slot),
                            tint = MaterialTheme.colorScheme.error,
                            onClick = onRemoveClick,
                        )
                    }
                }
            }
            Box(Modifier.matchParentSize()) {
                ColorIndicatorSection(
                    colorIndex = scheme.colorIndex,
                    colorMaps = courseColorMaps,
                    onClick = onColorClick,
                )
            }
        }
    }
}
