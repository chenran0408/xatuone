package com.xingheyuzhuan.shiguangschedule.ui.settings.holiday

import com.xingheyuzhuan.shiguangschedule.ui.components.UiScaffold
import com.xingheyuzhuan.shiguangschedule.ui.components.UiTopAppBar
import com.xingheyuzhuan.shiguangschedule.ui.components.UiButton
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialogAction
import com.xingheyuzhuan.shiguangschedule.ui.components.UiInputField
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCourseIconButton
import com.xingheyuzhuan.shiguangschedule.ui.components.UiLoadingIndicator
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.itos.coui.effect.couiButtonFeedback
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.model.Holiday
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.a11y_delete
import shiguangschedule.shared.generated.resources.a11y_enter_selection_mode
import shiguangschedule.shared.generated.resources.a11y_exit_selection_mode
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_clear_selected
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.action_select_all
import shiguangschedule.shared.generated.resources.action_select_date_range
import shiguangschedule.shared.generated.resources.add_24px
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.close_24px
import shiguangschedule.shared.generated.resources.delete_24px
import shiguangschedule.shared.generated.resources.done_all_24px
import shiguangschedule.shared.generated.resources.holiday_dialog_title_add
import shiguangschedule.shared.generated.resources.holiday_dialog_title_edit
import shiguangschedule.shared.generated.resources.holiday_empty_hint
import shiguangschedule.shared.generated.resources.holiday_field_name
import shiguangschedule.shared.generated.resources.holiday_included_dates_count_format
import shiguangschedule.shared.generated.resources.holiday_management_title
import shiguangschedule.shared.generated.resources.holiday_no_selected_dates
import shiguangschedule.shared.generated.resources.holiday_picker_hint_range_selected
import shiguangschedule.shared.generated.resources.holiday_picker_hint_select_end
import shiguangschedule.shared.generated.resources.holiday_picker_hint_select_start
import shiguangschedule.shared.generated.resources.holiday_picker_title
import shiguangschedule.shared.generated.resources.holiday_sync_online
import shiguangschedule.shared.generated.resources.holiday_type_off
import shiguangschedule.shared.generated.resources.holiday_type_work
import shiguangschedule.shared.generated.resources.menu_open_24px
import shiguangschedule.shared.generated.resources.refresh_24px
import shiguangschedule.shared.generated.resources.title_selected_items_count
import kotlin.time.Instant
import kotlin.time.Clock
import kotlinx.datetime.todayIn

/**
 * 节假日管理主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayManagementScreen(
    onBack: () -> Unit,
    viewModel: HolidayViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 弹窗与选择模式状态
    var showEditDialog by remember { mutableStateOf(false) }
    var editingHoliday by remember { mutableStateOf<Holiday?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedHolidays = remember { mutableStateListOf<Holiday>() }

    // 退出选择模式并清空已选项
    val exitSelection = { isSelectionMode = false; selectedHolidays.clear() }

    // 监听 ViewModel 消息并展示 Snackbar
    LaunchedEffect(uiState.message) {
        uiState.message?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessage() }
    }

    UiScaffold(
        topBar = {
            UiTopAppBar(title = if (isSelectionMode) {
                            stringResource(Res.string.title_selected_items_count, selectedHolidays.size)
                        } else {
                            stringResource(Res.string.holiday_management_title)
                        },
                navigationIcon = {
                    UiCourseIconButton(
                        icon = vectorResource(if (isSelectionMode) Res.drawable.close_24px else Res.drawable.arrow_back_24px),
                        description = stringResource(if (isSelectionMode) Res.string.action_cancel else Res.string.a11y_back),
                        inToolbar = true,
                        onClick = { if (isSelectionMode) exitSelection() else onBack() },
                    )
                },
                actions = {
                    if (isSelectionMode) {
                        // 全选 / 取消全选
                        val isAllSelected = uiState.holidays.isNotEmpty() && selectedHolidays.size == uiState.holidays.size
                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.done_all_24px),
                            description = stringResource(Res.string.action_select_all),
                            inToolbar = true,
                            onClick = {
                                selectedHolidays.clear()
                                if (!isAllSelected) selectedHolidays.addAll(uiState.holidays)
                            },
                        )
                        // 批量删除
                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.delete_24px),
                            description = stringResource(Res.string.a11y_delete),
                            inToolbar = true,
                            enabled = selectedHolidays.isNotEmpty(),
                            onClick = { viewModel.deleteHolidays(selectedHolidays.toList()); exitSelection() },
                        )
                    } else {
                        // 在线同步
                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.refresh_24px),
                            description = stringResource(Res.string.holiday_sync_online),
                            inToolbar = true,
                            enabled = !uiState.isLoading,
                            onClick = { viewModel.syncFromApi() },
                        )
                    }
                    if (!isSelectionMode && LocalUiStyle.current == UiStyle.COUI) {
                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.add_24px),
                            description = stringResource(Res.string.holiday_dialog_title_add),
                            inToolbar = true,
                            onClick = { editingHoliday = null; showEditDialog = true },
                        )
                    }
                    // 切换选择模式
                    UiCourseIconButton(
                        icon = vectorResource(Res.drawable.menu_open_24px),
                        description = stringResource(
                                if (isSelectionMode) Res.string.a11y_exit_selection_mode
                                else Res.string.a11y_enter_selection_mode
                            ),
                        inToolbar = true,
                        enabled = uiState.holidays.isNotEmpty() || isSelectionMode,
                        onClick = { if (isSelectionMode) exitSelection() else isSelectionMode = true },
                    )
                })
        },
        floatingActionButton = {
            // 新增按钮（仅在非选择模式显示）
            if (!isSelectionMode && LocalUiStyle.current != UiStyle.COUI) {
                FloatingActionButton(onClick = { editingHoliday = null; showEditDialog = true }) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.add_24px),
                        contentDescription = stringResource(Res.string.holiday_dialog_title_add)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.holidays.isEmpty() && !uiState.isLoading) {
                // 空数据提示
                UiText(
                    text = stringResource(Res.string.holiday_empty_hint),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (LocalUiStyle.current == UiStyle.COUI) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    lazyItems(uiState.holidays, key = { "${it.name}_${it.isHoliday}_${it.dates.hashCode()}" }) { holiday ->
                        HolidayGridCard(
                            holiday = holiday,
                            isSelected = selectedHolidays.contains(holiday),
                            onClick = {
                                if (isSelectionMode) {
                                    if (!selectedHolidays.remove(holiday)) selectedHolidays.add(holiday)
                                } else {
                                    editingHoliday = holiday
                                    showEditDialog = true
                                }
                            },
                            onLongClick = { if (!isSelectionMode) { isSelectionMode = true; selectedHolidays.add(holiday) } }
                        )
                    }
                }
            } else {
                // 节假日双列网格列表
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.holidays, key = { "${it.name}_${it.isHoliday}_${it.dates.hashCode()}" }) { holiday ->
                        HolidayGridCard(
                            holiday = holiday,
                            isSelected = selectedHolidays.contains(holiday),
                            onClick = {
                                if (isSelectionMode) {
                                    if (!selectedHolidays.remove(holiday)) selectedHolidays.add(holiday)
                                } else {
                                    editingHoliday = holiday
                                    showEditDialog = true
                                }
                            },
                            onLongClick = { if (!isSelectionMode) { isSelectionMode = true; selectedHolidays.add(holiday) } }
                        )
                    }
                }
            }
            // 加载指示器
            if (uiState.isLoading) UiLoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }

    // 编辑/添加对话框
    if (showEditDialog) {
        HolidayEditDialog(
            initialHoliday = editingHoliday,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, isHoliday, dates ->
                viewModel.saveHoliday(name, isHoliday, dates, oldHoliday = editingHoliday)
                showEditDialog = false
            }
        )
    }
}

/**
 * 网格列表中的节假日卡片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HolidayGridCard(
    holiday: Holiday,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    if (LocalUiStyle.current == UiStyle.COUI) {
        val colors = com.itos.coui.layout.CouiPageDefaults.themeColors()
        val interactionSource = remember { MutableInteractionSource() }
        val dateSummary = holiday.dates.sorted().joinToString(", ")
            .ifEmpty { stringResource(Res.string.holiday_no_selected_dates) }
        com.itos.coui.layout.Group(
            modifier = Modifier.fillMaxWidth(),
            colors = colors.copy(
                card = if (isSelected) colors.accent.copy(alpha = .14f) else colors.card,
            ),
        ) {
            com.itos.coui.layout.Item(
                modifier = Modifier
                    .semantics { selected = isSelected }
                    .couiButtonFeedback(interactionSource = interactionSource, list = true)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    ),
                icon = if (isSelected) {
                    {
                        Icon(
                            vectorResource(Res.drawable.done_all_24px),
                            contentDescription = null,
                            tint = colors.accent,
                        )
                    }
                } else null,
                title = {
                    com.itos.coui.text.Text(
                        text = holiday.name,
                        kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                    )
                },
                summary = {
                    Column {
                        com.itos.coui.text.Text(
                            text = stringResource(if (holiday.isHoliday) Res.string.holiday_type_off else Res.string.holiday_type_work),
                            kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                            color = if (holiday.isHoliday) colors.accent else colors.title,
                        )
                        com.itos.coui.text.Text(
                            text = dateSummary,
                            kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                        )
                    }
                },
            )
        }
        return
    }

    val cardShape = MaterialTheme.shapes.medium
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                holiday.isHoliday -> MaterialTheme.colorScheme.surfaceContainerHigh
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp)
            .clip(cardShape)
            .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, cardShape) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 节假日名称及类型标识
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = holiday.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(if (holiday.isHoliday) Res.string.holiday_type_off else Res.string.holiday_type_work),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (holiday.isHoliday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(8.dp))
            // 日期列表预览
            Text(
                text = holiday.dates.sorted().joinToString(", ").ifEmpty { stringResource(Res.string.holiday_no_selected_dates) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 节假日添加/编辑弹窗（含日期选择）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HolidayEditDialog(
    initialHoliday: Holiday?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, isHoliday: Boolean, dates: Set<LocalDate>) -> Unit
) {
    var name by remember { mutableStateOf(initialHoliday?.name ?: "") }
    var isHoliday by remember { mutableStateOf(initialHoliday?.isHoliday ?: true) }
    val selectedDates = remember { mutableStateListOf<LocalDate>().apply { addAll(initialHoliday?.dates ?: emptySet()) } }
    var showDateRangePicker by remember { mutableStateOf(false) }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var rangeStart by remember(showDateRangePicker) { mutableStateOf<LocalDate?>(null) }
    var rangeEnd by remember(showDateRangePicker) { mutableStateOf<LocalDate?>(null) }

    UiDialog(
        onDismissRequest = onDismiss,
        title = stringResource(if (initialHoliday == null) Res.string.holiday_dialog_title_add else Res.string.holiday_dialog_title_edit),
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 名称输入框
                UiInputField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(Res.string.holiday_field_name),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // 类型切换 (休/班)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UiButton(
                        selected = isHoliday,
                        onClick = { isHoliday = true },
                        text = stringResource(Res.string.holiday_type_off),
                    )
                    UiButton(
                        selected = !isHoliday,
                        onClick = { isHoliday = false },
                        text = stringResource(Res.string.holiday_type_work),
                    )
                }
                // 日期管理展示区域
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        UiText(
                            text = stringResource(Res.string.holiday_included_dates_count_format, selectedDates.size),
                        )
                        UiButton(
                            text = stringResource(Res.string.action_select_date_range),
                            selected = false,
                            onClick = { showDateRangePicker = true },
                        )
                    }
                    DateChipFlow(dates = selectedDates, onRemove = { selectedDates.remove(it) })
                }
            }
        },
        confirmButton = UiDialogAction(
            stringResource(Res.string.action_confirm),
            { onConfirm(name.trim(), isHoliday, selectedDates.toSet()) },
            enabled = name.isNotBlank() && selectedDates.isNotEmpty(),
        ),
        dismissButton = UiDialogAction(stringResource(Res.string.action_cancel), onDismiss),
    )

    // 日期范围选择器弹窗
    if (showDateRangePicker) {
        if (LocalUiStyle.current == UiStyle.COUI) {
            val displayDate = rangeEnd ?: rangeStart ?: today
            UiDialog(
                title = stringResource(Res.string.holiday_picker_title),
                onDismissRequest = { showDateRangePicker = false },
                text = {
                    val selectionHint = when {
                        rangeStart != null && rangeEnd != null -> stringResource(Res.string.holiday_picker_hint_range_selected)
                        rangeStart != null -> stringResource(Res.string.holiday_picker_hint_select_end)
                        else -> stringResource(Res.string.holiday_picker_hint_select_start)
                    }
                    com.itos.coui.text.Text(
                        text = selectionHint,
                        kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                    )
                    com.itos.coui.picker.CalendarPicker(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp),
                        value = java.time.LocalDate.parse(displayDate.toString()),
                        onValueChange = { picked ->
                            val date = LocalDate.parse(picked.toString())
                            if (rangeStart == null || rangeEnd != null) {
                                rangeStart = date
                                rangeEnd = null
                            } else {
                                rangeEnd = date
                            }
                        },
                    )
                },
                dismissButton = UiDialogAction(stringResource(Res.string.action_cancel), { showDateRangePicker = false }),
                confirmButton = UiDialogAction(
                    text = stringResource(Res.string.action_confirm),
                    enabled = rangeStart != null,
                    onClick = {
                        val start = rangeStart ?: return@UiDialogAction
                        val end = rangeEnd ?: start
                        var date = minOf(start, end)
                        val last = maxOf(start, end)
                        while (date <= last) {
                            if (!selectedDates.contains(date)) selectedDates.add(date)
                            date = LocalDate.fromEpochDays(date.toEpochDays() + 1)
                        }
                        showDateRangePicker = false
                    },
                ),
            )
        } else {
            val pickerState = rememberDateRangePickerState()
            DatePickerDialog(
                onDismissRequest = { showDateRangePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val startMillis = pickerState.selectedStartDateMillis
                        val endMillis = pickerState.selectedEndDateMillis ?: startMillis
                        if (startMillis != null && endMillis != null) {
                            val startDate = Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(TimeZone.UTC).date
                            val endDate = Instant.fromEpochMilliseconds(endMillis).toLocalDateTime(TimeZone.UTC).date
                            var curr = startDate
                            while (curr <= endDate) {
                                if (!selectedDates.contains(curr)) selectedDates.add(curr)
                                curr = LocalDate.fromEpochDays(curr.toEpochDays() + 1)
                            }
                        }
                        showDateRangePicker = false
                    }) { Text(stringResource(Res.string.action_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDateRangePicker = false }) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(Res.string.holiday_included_dates_count_format, selectedDates.size),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                if (selectedDates.isNotEmpty()) {
                                    TextButton(onClick = { selectedDates.clear() }, modifier = Modifier.height(28.dp), contentPadding = PaddingValues(0.dp)) {
                                        Text(
                                            text = stringResource(Res.string.action_clear_selected),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                            DateChipFlow(dates = selectedDates, onRemove = { selectedDates.remove(it) }, maxHeight = 90.dp)
                        }
                    }
                    DateRangePicker(
                        state = pickerState,
                        title = { Text(stringResource(Res.string.holiday_picker_title), modifier = Modifier.padding(16.dp)) },
                        headline = {
                            val text = when {
                                pickerState.selectedStartDateMillis != null && pickerState.selectedEndDateMillis != null ->
                                    stringResource(Res.string.holiday_picker_hint_range_selected)
                                pickerState.selectedStartDateMillis != null ->
                                    stringResource(Res.string.holiday_picker_hint_select_end)
                                else ->
                                    stringResource(Res.string.holiday_picker_hint_select_start)
                            }
                            Text(text, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp), style = MaterialTheme.typography.labelMedium)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 可复用的流式日期标签展示组件
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DateChipFlow(
    dates: List<LocalDate>,
    onRemove: (LocalDate) -> Unit,
    maxHeight: Dp? = null
) {
    val sortedDates = remember(dates.toList()) { dates.sorted() }
    if (sortedDates.isEmpty()) {
        UiText(text = stringResource(Res.string.holiday_no_selected_dates))
    } else {
        val shouldScroll = maxHeight != null && sortedDates.size > 8
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (shouldScroll) {
                        Modifier
                            .height(maxHeight)
                            .verticalScroll(rememberScrollState())
                    } else Modifier
                )
        ) {
            sortedDates.forEach { date ->
                    if (LocalUiStyle.current == UiStyle.COUI) {
                        UiButton(
                            text = date.toString(),
                            selected = true,
                            onClick = { onRemove(date) },
                        )
                    } else {
                        InputChip(
                            selected = true,
                            onClick = { onRemove(date) },
                            label = { Text(date.toString(), style = MaterialTheme.typography.labelSmall) },
                            trailingIcon = {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.close_24px),
                                    contentDescription = stringResource(Res.string.a11y_delete),
                                    modifier = Modifier.height(12.dp)
                                )
                            }
                        )
                    }
            }
        }
    }
}
