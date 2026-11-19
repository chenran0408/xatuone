package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_add_new_table
import shiguangschedule.shared.generated.resources.action_add
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.add_24px
import shiguangschedule.shared.generated.resources.course_table_created_at_prefix
import shiguangschedule.shared.generated.resources.course_table_id_prefix
import shiguangschedule.shared.generated.resources.dialog_title_add_table
import shiguangschedule.shared.generated.resources.label_current
import shiguangschedule.shared.generated.resources.label_table_name
import shiguangschedule.shared.generated.resources.text_no_course_tables
import shiguangschedule.shared.generated.resources.toast_add_table_success
import shiguangschedule.shared.generated.resources.toast_name_empty
import kotlin.time.Instant

/**
 * 适配 kotlinx-datetime 0.8.0 的格式化器定义
 */
private val defaultDateTimeFormat = LocalDateTime.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    day()
    char(' ')
    hour()
    char(':')
    minute()
}

/**
 * 格式化毫秒时间戳为 yyyy-MM-dd HH:mm 格式
 */
private fun formatEpochMillis(epochMillis: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return defaultDateTimeFormat.format(localDateTime)
}

/**
 * 专门为弹窗提供 Koin 注入的仓库 ViewModel
 */
@KoinViewModel
class CourseTablePickerDeps(
    val courseTableRepository: CourseTableRepository,
    val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    fun createNewCourseTable(name: String) {
        viewModelScope.launch {
            courseTableRepository.createNewCourseTable(name)
        }
    }
}

@Composable
fun CourseTablePickerDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onTableSelected: (CourseTable) -> Unit,
    deps: CourseTablePickerDeps = koinViewModel()
) {
    val courseTables by deps.courseTableRepository.getAllCourseTables().collectAsState(initial = emptyList())
    val appSettings by deps.appSettingsRepository.getAppSettings().collectAsState(initial = null)

    var selectedTable by remember { mutableStateOf<CourseTable?>(null) }

    var showAddTableDialog by remember { mutableStateOf(false) }
    var newTableName by remember { mutableStateOf("") }

    LaunchedEffect(courseTables, appSettings) {
        if (selectedTable == null && appSettings?.currentCourseTableId != null) {
            selectedTable = courseTables.find { it.id == appSettings?.currentCourseTableId }
        }
    }

    UiDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = {
            Column {
                if (courseTables.isEmpty()) {
                    UiText(
                        text = stringResource(Res.string.text_no_course_tables),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(courseTables) { courseTable ->
                            val isCurrentActive = courseTable.id == appSettings?.currentCourseTableId
                            val isSelectedForDialog = courseTable.id == selectedTable?.id

                            CourseTablePickerCard(
                                courseTable = courseTable,
                                isSelected = isSelectedForDialog,
                                isCurrentActive = isCurrentActive,
                                onCardClick = {
                                    selectedTable = it
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = UiDialogAction(text = stringResource(Res.string.action_confirm), onClick = {
            selectedTable?.let { onTableSelected(it) }
            onDismissRequest()
        }, enabled = selectedTable != null),
        neutralButton = UiDialogAction(text = stringResource(Res.string.action_add), onClick = { showAddTableDialog = true }),
        dismissButton = UiDialogAction(text = stringResource(Res.string.action_cancel), onClick = onDismissRequest)

    )

    if (showAddTableDialog) {
        val toastAddSuccess = stringResource(Res.string.toast_add_table_success, newTableName)
        val toastNameEmpty = stringResource(Res.string.toast_name_empty)

        UiDialog(
            onDismissRequest = {
                showAddTableDialog = false
                newTableName = ""
            },
            title = stringResource(Res.string.dialog_title_add_table),
            text = {
                UiInputField(
                    value = newTableName,
                    onValueChange = { newTableName = it },
                    label = stringResource(Res.string.label_table_name),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = UiDialogAction(text = stringResource(Res.string.action_add), onClick = {
                    if (newTableName.isNotBlank()) {
                        deps.createNewCourseTable(newTableName)

                        // 跨平台 ToastManager 提示
                        ToastManager.show(toastAddSuccess)

                        showAddTableDialog = false
                        newTableName = ""
                    } else {
                        ToastManager.show(toastNameEmpty)
                    }
                }),
            dismissButton = UiDialogAction(text = stringResource(Res.string.action_cancel), onClick = {
                    showAddTableDialog = false
                    newTableName = ""
                })
        )
    }
}

@Composable
fun CourseTablePickerCard(
    courseTable: CourseTable,
    isSelected: Boolean,
    isCurrentActive: Boolean,
    onCardClick: (CourseTable) -> Unit
) {
    UiSelectionCard(
        selected = isSelected,
        highlighted = isCurrentActive,
        onClick = { onCardClick(courseTable) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                UiText(text = courseTable.name, style = MaterialTheme.typography.titleMedium)
                UiText(
                    text = stringResource(
                        Res.string.course_table_id_prefix,
                        courseTable.id.take(8) + "..."
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                UiText(
                    text = stringResource(
                        Res.string.course_table_created_at_prefix,
                        formatEpochMillis(courseTable.createdAt)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = uiStyleColors().secondaryText.copy(alpha = 0.7f)
                )
            }
            if (isCurrentActive) {
                UiText(
                    stringResource(Res.string.label_current),
                    style = MaterialTheme.typography.labelSmall,
                    color = uiStyleColors().accent
                )
            }
        }
    }
}