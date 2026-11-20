package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.components.UiActionButton
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDatePickerDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialogAction
import com.xingheyuzhuan.shiguangschedule.ui.components.UiInputField
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
internal fun ConfirmSemesterDatesDialog(event: WebUiEvent.ConfirmSemesterDates, onResult: (Pair<String, Int>?) -> Unit) {
    var date by rememberSaveable(event.semester) { mutableStateOf(event.startDate) }
    var weeks by rememberSaveable(event.semester) { mutableStateOf(event.totalWeeks?.toString().orEmpty()) }
    var chooseDate by rememberSaveable { mutableStateOf(false) }
    val total = weeks.toIntOrNull()
    if (chooseDate) {
        UiDatePickerDialog(
            initialDateMillis = date?.let { LocalDate.parse(it).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() },
            onDismissRequest = { chooseDate = false },
            onConfirm = {
                date = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString()
                chooseDate = false
            },
        )
    } else {
        UiDialog(
            onDismissRequest = { onResult(null) },
            title = "确认学期日期",
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    UiText(event.semester)
                    UiActionButton({ chooseDate = true }, text = date?.let { "开学日期：$it" } ?: "选择开学日期")
                    UiInputField(weeks, { weeks = it }, Modifier.fillMaxWidth(), label = "学期总周数",
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    UiText("课程排至第 ${event.lastCourseWeek} 周")
                }
            },
            confirmButton = UiDialogAction("保存课表", { date?.let { day -> total?.let { onResult(day to it) } } },
                enabled = date != null && total != null && total in event.lastCourseWeek..53),
            dismissButton = UiDialogAction("取消", { onResult(null) }),
        )
    }
}
