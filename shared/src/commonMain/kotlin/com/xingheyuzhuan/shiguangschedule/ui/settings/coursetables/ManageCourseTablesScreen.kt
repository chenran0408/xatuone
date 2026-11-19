package com.xingheyuzhuan.shiguangschedule.ui.settings.coursetables

import com.xingheyuzhuan.shiguangschedule.ui.components.UiScaffold
import com.xingheyuzhuan.shiguangschedule.ui.components.UiTopAppBar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCourseIconButton
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialogAction
import com.xingheyuzhuan.shiguangschedule.ui.components.UiInputField
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_add_new_table
import shiguangschedule.shared.generated.resources.a11y_current_table
import shiguangschedule.shared.generated.resources.a11y_delete
import shiguangschedule.shared.generated.resources.a11y_edit
import shiguangschedule.shared.generated.resources.a11y_save
import shiguangschedule.shared.generated.resources.action_add
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.add_24px
import shiguangschedule.shared.generated.resources.check_circle_24px
import shiguangschedule.shared.generated.resources.confirm_delete
import shiguangschedule.shared.generated.resources.course_table_created_at_prefix
import shiguangschedule.shared.generated.resources.delete_24px
import shiguangschedule.shared.generated.resources.dialog_text_confirm_delete
import shiguangschedule.shared.generated.resources.dialog_title_add_table
import shiguangschedule.shared.generated.resources.dialog_title_edit_table
import shiguangschedule.shared.generated.resources.edit_24px
import shiguangschedule.shared.generated.resources.label_table_name
import shiguangschedule.shared.generated.resources.text_no_tables_hint
import shiguangschedule.shared.generated.resources.title_manage_course_tables
import shiguangschedule.shared.generated.resources.toast_add_table_success
import shiguangschedule.shared.generated.resources.toast_delete_last_table_failed
import shiguangschedule.shared.generated.resources.toast_delete_table_success
import shiguangschedule.shared.generated.resources.toast_edit_table_success
import shiguangschedule.shared.generated.resources.toast_name_empty
import shiguangschedule.shared.generated.resources.toast_switch_table_success
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCourseTablesScreen(
    onBack: () -> Unit,
    viewModel: ManageCourseTablesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // --- 对话框状态管理 ---
    var showAddTableDialog by remember { mutableStateOf(false) }
    var newTableName by remember { mutableStateOf("") }

    var showEditTableDialog by remember { mutableStateOf(false) }
    var editingTableInfo by remember { mutableStateOf<CourseTable?>(null) }
    var editedTableName by remember { mutableStateOf("") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var tableToDelete by remember { mutableStateOf<CourseTable?>(null) }

    // --- 资源字符串 ---
    val titleManageTables = stringResource(Res.string.title_manage_course_tables)
    val a11yAddNewTable = stringResource(Res.string.a11y_add_new_table)
    val textNoTablesHint = stringResource(Res.string.text_no_tables_hint)
    val dialogTitleAddTable = stringResource(Res.string.dialog_title_add_table)
    val labelTableName = stringResource(Res.string.label_table_name)
    val actionAdd = stringResource(Res.string.action_add)
    val actionCancel = stringResource(Res.string.action_cancel)
    val toastNameEmpty = stringResource(Res.string.toast_name_empty)
    val toastEditSuccess = stringResource(Res.string.toast_edit_table_success)
    val dialogTitleEditTable = stringResource(Res.string.dialog_title_edit_table)
    val a11ySave = stringResource(Res.string.a11y_save)
    val dialogTitleConfirmDelete = stringResource(Res.string.confirm_delete)
    val actionDelete = stringResource(Res.string.a11y_delete)
    val toastDeleteLastFailed = stringResource(Res.string.toast_delete_last_table_failed)

    UiScaffold(
        topBar = {
            UiTopAppBar(title = titleManageTables, onBack = onBack, actions = {
                if (LocalUiStyle.current == UiStyle.COUI) {
                    UiCourseIconButton(
                        icon = vectorResource(Res.drawable.add_24px),
                        description = a11yAddNewTable,
                        inToolbar = true,
                        onClick = { showAddTableDialog = true },
                    )
                }
            })
        },
        floatingActionButton = {
            if (LocalUiStyle.current == UiStyle.MIUIX) {
                top.yukonga.miuix.kmp.basic.FloatingActionButton(onClick = { showAddTableDialog = true }) {
                    top.yukonga.miuix.kmp.basic.Icon(vectorResource(Res.drawable.add_24px), contentDescription = a11yAddNewTable)
                }
            } else if (LocalUiStyle.current == UiStyle.MATERIAL) {
                FloatingActionButton(onClick = { showAddTableDialog = true }) {
                    Icon(vectorResource(Res.drawable.add_24px), contentDescription = a11yAddNewTable)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = if (LocalUiStyle.current == UiStyle.COUI) 0.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.courseTables.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    UiText(text = textNoTablesHint, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(
                        if (LocalUiStyle.current == UiStyle.COUI) 0.dp else 8.dp
                    )
                ) {
                    items(uiState.courseTables, key = { it.id }) { tableInfo ->
                        val isSelected = tableInfo.id == uiState.currentActiveTableId
                        val switchSuccessMsg = stringResource(Res.string.toast_switch_table_success, tableInfo.name)

                        CourseTableCard(
                            tableInfo = tableInfo,
                            isSelected = isSelected,
                            onDeleteClick = {
                                tableToDelete = it
                                showDeleteConfirmDialog = true
                            },
                            onEditClick = {
                                editingTableInfo = it
                                editedTableName = it.name
                                showEditTableDialog = true
                            },
                            onCardClick = {
                                viewModel.switchCourseTable(it.id)
                                ToastManager.show(switchSuccessMsg)
                            }
                        )
                    }
                }
            }
        }

        // --- Add Dialog ---
        if (showAddTableDialog) {
            val addSuccessMsg = stringResource(Res.string.toast_add_table_success, newTableName)
            UiDialog(
                onDismissRequest = {
                    showAddTableDialog = false
                    newTableName = ""
                },
                title = dialogTitleAddTable,
                text = {
                    UiInputField(
                        value = newTableName,
                        onValueChange = { newTableName = it },
                        label = labelTableName,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = UiDialogAction(actionAdd, {
                        if (newTableName.isNotBlank()) {
                            viewModel.createNewCourseTable(newTableName)
                            ToastManager.show(addSuccessMsg)
                            showAddTableDialog = false
                            newTableName = ""
                        } else {
                            ToastManager.show(toastNameEmpty)
                        }
                    }),
                dismissButton = UiDialogAction(actionCancel, {
                        showAddTableDialog = false
                        newTableName = ""
                    })
            )
        }

        // --- Edit Dialog ---
        if (showEditTableDialog && editingTableInfo != null) {
            UiDialog(
                onDismissRequest = {
                    showEditTableDialog = false
                    editingTableInfo = null
                    editedTableName = ""
                },
                title = dialogTitleEditTable,
                text = {
                    UiInputField(
                        value = editedTableName,
                        onValueChange = { editedTableName = it },
                        label = labelTableName,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = UiDialogAction(a11ySave, {
                        if (editedTableName.isNotBlank()) {
                            editingTableInfo?.let { tableToEdit ->
                                viewModel.updateCourseTable(tableToEdit.copy(name = editedTableName))
                                ToastManager.show(toastEditSuccess)
                                showEditTableDialog = false
                                editingTableInfo = null
                                editedTableName = ""
                            }
                        } else {
                            ToastManager.show(toastNameEmpty)
                        }
                    }),
                dismissButton = UiDialogAction(actionCancel, {
                        showEditTableDialog = false
                        editingTableInfo = null
                        editedTableName = ""
                    })
            )
        }

        // --- Delete Dialog ---
        if (showDeleteConfirmDialog && tableToDelete != null) {
            val confirmDeleteText = stringResource(Res.string.dialog_text_confirm_delete, tableToDelete?.name ?: "")
            val deleteSuccessMsg = stringResource(Res.string.toast_delete_table_success, tableToDelete?.name ?: "")

            UiDialog(
                onDismissRequest = {
                    showDeleteConfirmDialog = false
                    tableToDelete = null
                },
                title = dialogTitleConfirmDelete,
                text = { UiText(confirmDeleteText) },
                confirmButton = UiDialogAction(actionDelete, {
                        if (uiState.courseTables.size > 1) {
                            tableToDelete?.let {
                                viewModel.deleteCourseTable(it)
                                ToastManager.show(deleteSuccessMsg)
                            }
                            showDeleteConfirmDialog = false
                            tableToDelete = null
                        } else {
                            ToastManager.show(toastDeleteLastFailed)
                            showDeleteConfirmDialog = false
                            tableToDelete = null
                        }
                    }),
                dismissButton = UiDialogAction(actionCancel, {
                        showDeleteConfirmDialog = false
                        tableToDelete = null
                    })
            )
        }
    }
}

@Composable
fun CourseTableCard(
    tableInfo: CourseTable,
    isSelected: Boolean,
    onDeleteClick: (CourseTable) -> Unit,
    onEditClick: (CourseTable) -> Unit,
    onCardClick: (CourseTable) -> Unit
) {
    val a11yCurrentTable = stringResource(Res.string.a11y_current_table)
    val a11yEdit = stringResource(Res.string.a11y_edit)
    val a11yDelete = stringResource(Res.string.a11y_delete)

    val formattedDate = formatTimestamp(tableInfo.createdAt)

    val createdAtText = stringResource(Res.string.course_table_created_at_prefix, formattedDate)

    if (LocalUiStyle.current == UiStyle.MIUIX) {
        top.yukonga.miuix.kmp.basic.BasicComponent(
            modifier = Modifier.fillMaxWidth(),
            title = tableInfo.name,
            summary = createdAtText,
            startAction = if (isSelected) ({
                top.yukonga.miuix.kmp.basic.Icon(
                    vectorResource(Res.drawable.check_circle_24px),
                    contentDescription = a11yCurrentTable,
                    tint = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary,
                )
            }) else null,
            endActions = {
                top.yukonga.miuix.kmp.basic.IconButton(onClick = { onEditClick(tableInfo) }) {
                    top.yukonga.miuix.kmp.basic.Icon(vectorResource(Res.drawable.edit_24px), a11yEdit)
                }
                top.yukonga.miuix.kmp.basic.IconButton(onClick = { onDeleteClick(tableInfo) }) {
                    top.yukonga.miuix.kmp.basic.Icon(vectorResource(Res.drawable.delete_24px), a11yDelete)
                }
            },
            onClick = { onCardClick(tableInfo) },
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        )
    } else if (LocalUiStyle.current == UiStyle.COUI) {
        val colors = com.itos.coui.layout.CouiPageDefaults.themeColors()
        com.itos.coui.layout.Group(
            modifier = Modifier.fillMaxWidth(),
            colors = if (isSelected) colors.copy(card = colors.accent.copy(alpha = .12f)) else colors,
        ) {
            com.itos.coui.layout.Item(
                onClick = { onCardClick(tableInfo) },
                icon = if (isSelected) ({
                    Icon(
                        vectorResource(Res.drawable.check_circle_24px),
                        contentDescription = a11yCurrentTable,
                        tint = colors.accent,
                    )
                }) else null,
                summary = {
                    com.itos.coui.text.Text(
                        text = createdAtText,
                        kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                    )
                },
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.edit_24px),
                            description = a11yEdit,
                            tint = colors.accent,
                            onClick = { onEditClick(tableInfo) },
                        )
                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.delete_24px),
                            description = a11yDelete,
                            tint = colors.summary,
                            onClick = { onDeleteClick(tableInfo) },
                        )
                    }
                },
                title = {
                    com.itos.coui.text.Text(
                        text = tableInfo.name,
                        kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                    )
                },
            )
        }
    } else {
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth().clickable { onCardClick(tableInfo) },
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(tableInfo.name, style = MaterialTheme.typography.titleMedium)
                    Text(createdAtText, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isSelected) Icon(vectorResource(Res.drawable.check_circle_24px), a11yCurrentTable,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp))
                    UiCourseIconButton(vectorResource(Res.drawable.edit_24px), a11yEdit, MaterialTheme.colorScheme.onSurface, onClick = { onEditClick(tableInfo) })
                    UiCourseIconButton(vectorResource(Res.drawable.delete_24px), a11yDelete, MaterialTheme.colorScheme.error, onClick = { onDeleteClick(tableInfo) })
                }
            }
        }
    }
}

/**
 * 跨平台时间戳格式化 (yyyy-MM-dd HH:mm)
 */
private fun formatTimestamp(timestamp: Long): String {
    val localDateTime = Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())

    val year = localDateTime.year
    val month = localDateTime.month.number.toString().padStart(2, '0')
    val day = localDateTime.day.toString().padStart(2, '0')
    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')

    return "$year-$month-$day $hour:$minute"
}
