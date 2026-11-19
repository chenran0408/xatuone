package com.xingheyuzhuan.shiguangschedule.ui.settings.time

import com.xingheyuzhuan.shiguangschedule.ui.components.UiScaffold
import com.xingheyuzhuan.shiguangschedule.ui.components.UiTopAppBar
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCard
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCourseIconButton
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialogAction
import com.xingheyuzhuan.shiguangschedule.ui.components.UiLoadingIndicator
import com.xingheyuzhuan.shiguangschedule.ui.components.UiSettingRow
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_add_schedule_scheme
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.a11y_cancel_selection
import shiguangschedule.shared.generated.resources.a11y_delete
import shiguangschedule.shared.generated.resources.a11y_delete_selected
import shiguangschedule.shared.generated.resources.a11y_edit_schedule
import shiguangschedule.shared.generated.resources.action_add_combo_schedule
import shiguangschedule.shared.generated.resources.action_add_public_schedule
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_copy
import shiguangschedule.shared.generated.resources.add_24px
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.confirm_delete
import shiguangschedule.shared.generated.resources.content_copy_24px
import shiguangschedule.shared.generated.resources.delete_24px
import shiguangschedule.shared.generated.resources.dialog_msg_confirm_delete_schedules
import shiguangschedule.shared.generated.resources.edit_24px
import shiguangschedule.shared.generated.resources.follow_course_table
import shiguangschedule.shared.generated.resources.schedule_type_combo
import shiguangschedule.shared.generated.resources.schedule_type_exclusive
import shiguangschedule.shared.generated.resources.schedule_type_public
import shiguangschedule.shared.generated.resources.title_exclusive_schedule
import shiguangschedule.shared.generated.resources.title_schedule_management
import shiguangschedule.shared.generated.resources.title_selected_items_count

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeScheduleManagementScreen(
    onBack: () -> Unit,
    onEditSingleSchedule: (tableId: String?, isPublic: Boolean, copyFromId: String?) -> Unit,
    onEditComboSchedule: (comboId: String?, copyFromId: String?) -> Unit,
    viewModel: TimeScheduleManagementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 多选模式状态
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedKeys = remember { mutableStateListOf<String>() }

    var showAddMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // 当退出多选模式时清空已选
    fun exitSelectionMode() {
        isSelectionMode = false
        selectedKeys.clear()
    }

    UiScaffold(
        topBar = {
            if (isSelectionMode) {
                // 多选模式下的 TopAppBar
                UiTopAppBar(title = stringResource(
                                Res.string.title_selected_items_count,
                                selectedKeys.size
                            ),
                navigationIcon = {
                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.arrow_back_24px),
                            description = stringResource(Res.string.a11y_cancel_selection),
                            onClick = { exitSelectionMode() },
                            inToolbar = true,
                        )
                    },
                actions = {
                        // 右上角批量删除入口
                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.delete_24px),
                            description = stringResource(Res.string.a11y_delete_selected),
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteConfirmDialog = true },
                            enabled = selectedKeys.isNotEmpty(),
                            inToolbar = true,
                        )
                    })
            } else {
                // 常规模式下的 TopAppBar
                UiTopAppBar(title = stringResource(Res.string.title_schedule_management),
                navigationIcon = {
                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.arrow_back_24px),
                            description = stringResource(Res.string.a11y_back),
                            onClick = onBack,
                            inToolbar = true,
                        )
                    },
                actions = {
                        Box {
                            UiCourseIconButton(
                                icon = vectorResource(Res.drawable.add_24px),
                                description = stringResource(Res.string.a11y_add_schedule_scheme),
                                onClick = { showAddMenu = true },
                                inToolbar = true,
                            )
                        }
                    })
            }
        }
    ) { paddingValues ->
        if (!uiState.isDataLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                UiLoadingIndicator()
            }
            return@UiScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = uiState.items,
                key = { "${it.type.name}_${it.id}" }
            ) { item ->
                val itemKey = "${item.type.name}_${item.id}"
                val isItemMultiSelected = selectedKeys.contains(itemKey)

                ScheduleCard(
                    item = item,
                    isSelectionMode = isSelectionMode,
                    isMultiSelected = isItemMultiSelected,
                    onSelect = {
                        if (isSelectionMode) {
                            // 课表专属作息不允许删除/多选
                            if (item.type == ScheduleType.EXCLUSIVE) return@ScheduleCard

                            if (isItemMultiSelected) {
                                selectedKeys.remove(itemKey)
                                if (selectedKeys.isEmpty()) isSelectionMode = false
                            } else {
                                selectedKeys.add(itemKey)
                            }
                        } else {
                            viewModel.bindTimeSchedule(item)
                        }
                    },
                    onLongClick = {
                        // 专属作息不开启多选模式
                        if (item.type != ScheduleType.EXCLUSIVE) {
                            isSelectionMode = true
                            if (!selectedKeys.contains(itemKey)) {
                                selectedKeys.add(itemKey)
                            }
                        }
                    },
                    onEdit = {
                        // 普通编辑模式：传入原本的 id，copyFromId 为 null
                        when (item.type) {
                            ScheduleType.EXCLUSIVE -> onEditSingleSchedule(item.id, false, null)
                            ScheduleType.PUBLIC -> onEditSingleSchedule(item.id, true, null)
                            ScheduleType.COMBO -> onEditComboSchedule(item.id, null)
                        }
                    },
                    onCopy = {
                        // 复制模式（走新增逻辑）：tableId/comboId 传 null，把当前 item.id 作为 copyFromId 传入
                        when (item.type) {
                            ScheduleType.PUBLIC -> onEditSingleSchedule(null, true, item.id)
                            ScheduleType.COMBO -> onEditComboSchedule(null, item.id)
                            ScheduleType.EXCLUSIVE -> {} // 专属作息无复制按钮
                        }
                    }
                )
            }
        }

        // 删除确认弹窗
        if (showDeleteConfirmDialog) {
            UiDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = stringResource(Res.string.confirm_delete),
                text = {
                    UiText(
                        stringResource(
                            Res.string.dialog_msg_confirm_delete_schedules,
                            selectedKeys.size
                        )
                    )
                },
                confirmButton = UiDialogAction(stringResource(Res.string.a11y_delete), {
                    showDeleteConfirmDialog = false
                    viewModel.deleteSchedules(selectedKeys.toSet())
                    exitSelectionMode()
                }),
                dismissButton = UiDialogAction(stringResource(Res.string.action_cancel), {
                    showDeleteConfirmDialog = false
                }),
            )
        }
        if (showAddMenu) {
            UiDialog(
                onDismissRequest = { showAddMenu = false },
                title = stringResource(Res.string.a11y_add_schedule_scheme),
                text = {
                    Column {
                        UiSettingRow(
                            title = stringResource(Res.string.action_add_public_schedule),
                            onClick = {
                                showAddMenu = false
                                onEditSingleSchedule(null, true, null)
                            },
                        )
                        UiSettingRow(
                            title = stringResource(Res.string.action_add_combo_schedule),
                            onClick = {
                                showAddMenu = false
                                onEditComboSchedule(null, null)
                            },
                        )
                    }
                },
                dismissButton = UiDialogAction(stringResource(Res.string.action_cancel), { showAddMenu = false }),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleCard(
    item: TimeScheduleItemUiModel,
    isSelectionMode: Boolean,
    isMultiSelected: Boolean,
    onSelect: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit
) {
    val isExclusive = item.type == ScheduleType.EXCLUSIVE

    val cardColor = when {
        LocalUiStyle.current == UiStyle.MIUIX && item.isSelected ->
            top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary.copy(alpha = .14f)
        LocalUiStyle.current == UiStyle.MIUIX ->
            top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainer
        item.isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    UiCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isSelectionMode && isExclusive) 0.5f else 1.0f)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode && isExclusive) return@combinedClickable
                    onSelect()
                },
                onLongClick = {
                    if (isExclusive) return@combinedClickable
                    onLongClick()
                }
            ),
        containerColor = if (isSelectionMode && isMultiSelected) uiStyleColors().accent.copy(alpha = .14f)
        else cardColor,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelectionMode) {
                    val toggleableState = when {
                        isExclusive -> ToggleableState.Indeterminate
                        isMultiSelected -> ToggleableState.On
                        else -> ToggleableState.Off
                    }

                    if (LocalUiStyle.current == UiStyle.MIUIX) {
                        top.yukonga.miuix.kmp.basic.Checkbox(
                            state = toggleableState,
                            onClick = if (isExclusive) null else onSelect,
                            enabled = !isExclusive,
                        )
                    } else {
                        TriStateCheckbox(
                            state = toggleableState,
                            onClick = if (isExclusive) null else { { onSelect() } },
                            enabled = !isExclusive
                        )
                    }
                } else {
                    if (LocalUiStyle.current == UiStyle.MIUIX) {
                        top.yukonga.miuix.kmp.basic.RadioButton(selected = item.isSelected, onClick = onSelect)
                    } else RadioButton(selected = item.isSelected, onClick = onSelect)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                UiText(
                    text = item.name ?: stringResource(Res.string.title_exclusive_schedule),
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScheduleTypeTag(type = item.type)

                    UiText(
                        text = item.createdAtFormatted ?: stringResource(Res.string.follow_course_table),
                        style = MaterialTheme.typography.bodySmall,
                        color = uiStyleColors().secondaryText
                    )
                }
            }

            AnimatedVisibility(
                visible = !isSelectionMode && item.isEditable,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 非专属作息（公共作息、组合作息）在编辑按钮左侧展示复制按钮
                    if (!isExclusive) {
                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.content_copy_24px),
                            description = stringResource(Res.string.action_copy),
                            tint = uiStyleColors().accent,
                            onClick = onCopy,
                        )
                    }

                    UiCourseIconButton(
                        icon = vectorResource(Res.drawable.edit_24px),
                        description = stringResource(Res.string.a11y_edit_schedule),
                        tint = uiStyleColors().accent,
                        onClick = onEdit,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleTypeTag(type: ScheduleType) {
    val (textRes, containerColor, contentColor) = when (type) {
        ScheduleType.EXCLUSIVE -> Triple(
            Res.string.schedule_type_exclusive,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        ScheduleType.PUBLIC -> Triple(
            Res.string.schedule_type_public,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        ScheduleType.COMBO -> Triple(
            Res.string.schedule_type_combo,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    val resolvedContainer = if (LocalUiStyle.current == UiStyle.MIUIX) {
        if (type == ScheduleType.EXCLUSIVE) top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceVariant
        else top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary.copy(alpha = .12f)
    } else containerColor
    Box(
        modifier = Modifier
            .background(resolvedContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        UiText(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelSmall,
            color = if (LocalUiStyle.current == UiStyle.MIUIX) uiStyleColors().accent else contentColor,
        )
    }
}
