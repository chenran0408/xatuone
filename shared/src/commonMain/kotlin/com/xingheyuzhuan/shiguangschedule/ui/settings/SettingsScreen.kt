package com.xingheyuzhuan.shiguangschedule.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTopAppBarState
import com.xingheyuzhuan.shiguangschedule.ui.components.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.koin.compose.koinInject
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccountRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.ui.components.NativeNumberPicker
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.date_format_year_month_day
import shiguangschedule.shared.generated.resources.day_of_week_monday
import shiguangschedule.shared.generated.resources.day_of_week_sunday
import shiguangschedule.shared.generated.resources.dialog_title_manual_set_week
import shiguangschedule.shared.generated.resources.dialog_title_select_total_weeks
import shiguangschedule.shared.generated.resources.dialog_title_set_first_day_of_week
import shiguangschedule.shared.generated.resources.holiday_management_desc
import shiguangschedule.shared.generated.resources.holiday_management_title
import shiguangschedule.shared.generated.resources.item_course_conversion
import shiguangschedule.shared.generated.resources.item_course_management
import shiguangschedule.shared.generated.resources.item_current_week
import shiguangschedule.shared.generated.resources.item_first_day_of_week
import shiguangschedule.shared.generated.resources.item_more_options
import shiguangschedule.shared.generated.resources.item_personalization
import shiguangschedule.shared.generated.resources.item_quick_actions
import shiguangschedule.shared.generated.resources.item_set_start_date
import shiguangschedule.shared.generated.resources.item_show_non_current_week
import shiguangschedule.shared.generated.resources.item_show_weekends
import shiguangschedule.shared.generated.resources.item_total_weeks
import shiguangschedule.shared.generated.resources.more_horiz_24px
import shiguangschedule.shared.generated.resources.section_title_advanced_features
import shiguangschedule.shared.generated.resources.section_title_general_settings
import shiguangschedule.shared.generated.resources.status_current_week_format
import shiguangschedule.shared.generated.resources.status_not_set
import shiguangschedule.shared.generated.resources.status_set_start_date_first
import shiguangschedule.shared.generated.resources.status_total_weeks_format
import shiguangschedule.shared.generated.resources.title_course_notification_settings
import shiguangschedule.shared.generated.resources.title_manage_course_tables
import shiguangschedule.shared.generated.resources.title_schedule_management
import shiguangschedule.shared.generated.resources.title_schedule_settings
import shiguangschedule.shared.generated.resources.title_vacation

private val SETTING_PADDING = 16.dp
private val SECTION_SPACING = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    openStartDatePicker: Boolean = false,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val semesterDateSaving by viewModel.semesterDateSaving.collectAsState()
    val courseTables: CourseTableRepository = koinInject()
    val tables by courseTables.getAllCourseTables().collectAsState(initial = emptyList())


    UiScaffold(
        containerColor = uiStyleColors().background,
        topBar = { UiTopAppBar(title = "日期与周次", onBack = onBack) }
    ) { innerPadding ->
        if (!uiState.isReady) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) UiFeedback("正在读取…", UiFeedbackKind.LOADING)
                else CircularProgressIndicator()
            }
        } else {
            val appSettings = uiState.appSettings
            val courseTableConfig = uiState.courseConfig
            val displayCurrentWeek = uiState.currentWeek

            val showWeekends = courseTableConfig?.showWeekends ?: false
            val semesterStartDateString = courseTableConfig?.semesterStartDate
            val semesterTotalWeeks = courseTableConfig?.semesterTotalWeeks ?: 20
            val firstDayOfWeekInt = courseTableConfig?.firstDayOfWeek ?: DayOfWeek.MONDAY.isoDayNumber

            val semesterStartDate: LocalDate? = remember(semesterStartDateString) {
                semesterStartDateString?.let {
                    try {
                        LocalDate.parse(it)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            var showTotalWeeksDialog by remember { mutableStateOf(false) }
            var showManualWeekDialog by remember { mutableStateOf(false) }
            var showDatePickerModal by rememberSaveable { mutableStateOf(openStartDatePicker) }
            var showFirstDayOfWeekDialog by remember { mutableStateOf(false) }
            var semesterDateError by rememberSaveable { mutableStateOf<String?>(null) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .uiContentWidth()
                    .padding(innerPadding)
                    .padding(horizontal = SETTING_PADDING),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = 16.dp
                )
            ) {
                item {
                    UiText(
                        text = "当前课表：${tables.firstOrNull { it.id == appSettings.currentCourseTableId }?.name ?: "正在读取…"}",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                item {
                    GeneralSettingsSection(
                        showNonCurrentWeek = appSettings.showNonCurrentWeekCourses,
                        onShowNonCurrentWeekChanged = { isChecked -> viewModel.onShowNonCurrentWeekChanged(isChecked) },
                        showWeekends = showWeekends,
                        onShowWeekendsChanged = { isChecked -> viewModel.onShowWeekendsChanged(isChecked) },
                        semesterStartDate = semesterStartDate,
                        semesterTotalWeeks = semesterTotalWeeks,
                        firstDayOfWeekInt = firstDayOfWeekInt,
                        displayCurrentWeek = displayCurrentWeek,
                        onSemesterStartDateClick = { showDatePickerModal = true },
                        onSemesterTotalWeeksClick = { showTotalWeeksDialog = true },
                        onManualWeekClick = { showManualWeekDialog = true },
                        onFirstDayOfWeekClick = { showFirstDayOfWeekDialog = true }
                    )
                }

            }

            if (showDatePickerModal) {
                UiDatePickerDialog(initialDateMillis = semesterStartDate?.let {
                    LocalDate(it.year, it.month.number, it.day).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                }, onDismissRequest = {
                    if (openStartDatePicker) onBack() else showDatePickerModal = false
                }, onConfirm = {
                    viewModel.onSemesterStartDateSelected(it) { error ->
                        if (error == null) {
                            showDatePickerModal = false
                            showPlatformToast("开学日期已保存")
                            if (openStartDatePicker) onBack()
                        } else {
                            semesterDateError = error
                        }
                    }
                }, saving = semesterDateSaving)
            }
            semesterDateError?.let { error ->
                UiDialog(
                    onDismissRequest = { semesterDateError = null },
                    title = "保存失败",
                    text = { UiText(error) },
                    confirmButton = UiDialogAction(
                        onClick = { semesterDateError = null },
                        text = stringResource(Res.string.action_confirm)
                    )
                )
            }

            if (showTotalWeeksDialog) {
                NumberPickerDialog(
                    title = stringResource(Res.string.dialog_title_select_total_weeks),
                    range = 1..30,
                    initialValue = semesterTotalWeeks,
                    onDismiss = { showTotalWeeksDialog = false },
                    onConfirm = { selectedWeeks ->
                        viewModel.onSemesterTotalWeeksSelected(selectedWeeks)
                        showTotalWeeksDialog = false
                    }
                )
            }

            if (showManualWeekDialog) {
                ManualWeekPickerDialog(
                    totalWeeks = semesterTotalWeeks,
                    currentWeek = displayCurrentWeek,
                    onDismiss = { showManualWeekDialog = false },
                    onConfirm = { weekNumber ->
                        if (weekNumber != displayCurrentWeek) {
                            viewModel.onCurrentWeekManuallySet(weekNumber)
                        }
                        showManualWeekDialog = false
                    }
                )
            }

            if (showFirstDayOfWeekDialog) {
                DayOfWeekPickerDialog(
                    initialDayOfWeekInt = firstDayOfWeekInt,
                    onDismiss = { showFirstDayOfWeekDialog = false },
                    onConfirm = { selectedDayInt ->
                        viewModel.onFirstDayOfWeekSelected(selectedDayInt)
                        showFirstDayOfWeekDialog = false
                    }
                )
            }
        }
    }
}

/**
 * 通用设置卡片
 */
@Composable
private fun GeneralSettingsSection(
    showNonCurrentWeek: Boolean,
    onShowNonCurrentWeekChanged: (Boolean) -> Unit,
    showWeekends: Boolean,
    onShowWeekendsChanged: (Boolean) -> Unit,
    semesterStartDate: LocalDate?,
    semesterTotalWeeks: Int,
    firstDayOfWeekInt: Int,
    displayCurrentWeek: Int?,
    onSemesterStartDateClick: () -> Unit,
    onSemesterTotalWeeksClick: () -> Unit,
    onManualWeekClick: () -> Unit,
    onFirstDayOfWeekClick: () -> Unit
) {
    val formattedDate = semesterStartDate?.let {
        stringResource(
            Res.string.date_format_year_month_day,
            it.year.toString(),
            it.month.number.toString(),
            it.day.toString(),
        )
    } ?: stringResource(Res.string.status_not_set)
    val weekStatusText = when {
        semesterStartDate == null -> stringResource(Res.string.status_set_start_date_first)
        displayCurrentWeek == null -> stringResource(Res.string.title_vacation)
        else -> stringResource(Res.string.status_current_week_format, displayCurrentWeek)
    }
    val firstDayText = when (firstDayOfWeekInt) {
        DayOfWeek.SUNDAY.isoDayNumber -> stringResource(Res.string.day_of_week_sunday)
        else -> stringResource(Res.string.day_of_week_monday)
    }
    val showNonCurrentWeekTitle = stringResource(Res.string.item_show_non_current_week)
    val showWeekendsTitle = stringResource(Res.string.item_show_weekends)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UiCard(modifier = Modifier.fillMaxWidth()) {
            UiServiceRow(
                title = stringResource(Res.string.item_set_start_date),
                summary = formattedDate,
                onClick = onSemesterStartDateClick,
            )
            UiServiceRow(
                title = stringResource(Res.string.item_total_weeks),
                summary = stringResource(Res.string.status_total_weeks_format, semesterTotalWeeks),
                onClick = onSemesterTotalWeeksClick,
            )
            UiServiceRow(
                title = stringResource(Res.string.item_current_week),
                summary = weekStatusText,
                onClick = onManualWeekClick,
            )
            UiServiceRow(
                title = stringResource(Res.string.item_first_day_of_week),
                summary = firstDayText,
                onClick = onFirstDayOfWeekClick,
            )
        }
        UiCard(modifier = Modifier.fillMaxWidth()) {
            UiSettingRow(title = showNonCurrentWeekTitle, trailingContent = {
                UiSwitch(
                    checked = showNonCurrentWeek,
                    onCheckedChange = onShowNonCurrentWeekChanged,
                    modifier = Modifier.semantics { contentDescription = showNonCurrentWeekTitle },
                )
            })
            UiSettingRow(title = showWeekendsTitle, trailingContent = {
                UiSwitch(
                    checked = showWeekends,
                    onCheckedChange = onShowWeekendsChanged,
                    modifier = Modifier.semantics { contentDescription = showWeekendsTitle },
                )
            })
        }
    }
}

/**
 * 手动周数选择器对话框
 */
@Composable
fun ManualWeekPickerDialog(
    totalWeeks: Int,
    currentWeek: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    val optionOnVacationText = stringResource(Res.string.title_vacation)

    val weekOptions = listOf(optionOnVacationText) + (1..totalWeeks).map {
        stringResource(Res.string.status_current_week_format, it)
    }

    val initialSelectedValue = when (currentWeek) {
        null -> optionOnVacationText
        else -> stringResource(Res.string.status_current_week_format, currentWeek)
    }

    var dialogSelectedValue by remember { mutableStateOf(initialSelectedValue) }

    UiDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.dialog_title_manual_set_week),
        text = {
            NativeNumberPicker(
                values = weekOptions,
                selectedValue = dialogSelectedValue,
                onValueChange = { newValue ->
                    dialogSelectedValue = newValue
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = UiDialogAction(onClick = {
                val weekNumber = if (dialogSelectedValue == optionOnVacationText) {
                    null
                } else {
                    dialogSelectedValue.filter { it.isDigit() }.toIntOrNull()
                }
                onConfirm(weekNumber)
            }, text = stringResource(Res.string.action_confirm)),
        dismissButton = UiDialogAction(onClick = onDismiss, text = stringResource(Res.string.action_cancel))
    )
}

/**
 * 每周起始日选择器对话框
 */
@Composable
fun DayOfWeekPickerDialog(
    initialDayOfWeekInt: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val dayOfWeekMondayText = stringResource(Res.string.day_of_week_monday)
    val dayOfWeekSundayText = stringResource(Res.string.day_of_week_sunday)

    val dayOptionsMap = mapOf(
        dayOfWeekMondayText to DayOfWeek.MONDAY.isoDayNumber,
        dayOfWeekSundayText to DayOfWeek.SUNDAY.isoDayNumber
    )
    val dayOptions = dayOptionsMap.keys.toList()

    val initialSelectedDayText = dayOptionsMap.entries.firstOrNull { it.value == initialDayOfWeekInt }?.key
        ?: dayOfWeekMondayText

    var dialogSelectedText by remember { mutableStateOf(initialSelectedDayText) }

    UiDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.dialog_title_set_first_day_of_week),
        text = {
            NativeNumberPicker(
                values = dayOptions,
                selectedValue = dialogSelectedText,
                onValueChange = { newValue ->
                    dialogSelectedText = newValue
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = UiDialogAction(onClick = {
                val selectedDayInt = dayOptionsMap[dialogSelectedText] ?: DayOfWeek.MONDAY.isoDayNumber
                onConfirm(selectedDayInt)
            }, text = stringResource(Res.string.action_confirm)),
        dismissButton = UiDialogAction(onClick = onDismiss, text = stringResource(Res.string.action_cancel))
    )
}

/**
 * 数字选择器对话框
 */
@Composable
private fun NumberPickerDialog(
    title: String,
    range: IntRange,
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var dialogSelectedValue by remember { mutableIntStateOf(initialValue.coerceIn(range)) }

    UiDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = {
            NativeNumberPicker(
                values = range.toList(),
                selectedValue = dialogSelectedValue,
                onValueChange = { newValue ->
                    dialogSelectedValue = newValue
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = UiDialogAction(onClick = { onConfirm(dialogSelectedValue) }, text = stringResource(Res.string.action_confirm)),
        dismissButton = UiDialogAction(onClick = onDismiss, text = stringResource(Res.string.action_cancel))
    )
}
