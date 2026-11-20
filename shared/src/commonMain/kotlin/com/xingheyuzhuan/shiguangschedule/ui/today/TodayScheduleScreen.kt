package com.xingheyuzhuan.shiguangschedule.ui.today

import com.xingheyuzhuan.shiguangschedule.ui.components.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import com.xingheyuzhuan.shiguangschedule.ui.components.UiButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Scaffold
import com.xingheyuzhuan.shiguangschedule.ui.components.UiActionButton
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Instant
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeek
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.MergedCourseBlock
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.CourseDetailBottomSheet
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.courseSurfaceColor
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.courseTextOn
import com.xingheyuzhuan.shiguangschedule.ui.schedule.displayedCourseName
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.arrow_forward_24px
import shiguangschedule.shared.generated.resources.course_status_finished
import shiguangschedule.shared.generated.resources.course_position_prefix
import shiguangschedule.shared.generated.resources.course_teacher_prefix
import shiguangschedule.shared.generated.resources.label_credit
import shiguangschedule.shared.generated.resources.label_remark
import shiguangschedule.shared.generated.resources.title_today_schedule
import shiguangschedule.shared.generated.resources.week_days_full_names
import kotlinx.coroutines.delay
import kotlin.time.Clock

private const val EMPTY_TIME_PLACEHOLDER = "--:--"

internal fun isCourseFinishedAt(endTime: String?, currentTime: LocalTime): Boolean {
    val end = endTime?.let { LocalTime.Formats.ISO.parseOrNull(it) } ?: return false
    return end < currentTime
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScheduleScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    embedded: Boolean = false,
    viewModel: TodayScheduleViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridStyle by viewModel.gridStyle.collectAsState()
    val isDark = LocalIsDarkTheme.current
    var currentTime by remember {
        mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time)
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
            currentTime = now
            delay(60_000L - now.second * 1_000L - now.nanosecond / 1_000_000L)
        }
    }

    UiScaffold(
        contentWindowInsets = if (embedded) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        containerColor = uiStyleColors().background,
        topBar = {
            if (!embedded) UiTopAppBar(title = stringResource(Res.string.title_today_schedule), onBack = onBack,
                actions = { UiActionButton(onClick = { onNavigate(Destination.CourseSchedule) }, text = "周课表") })
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is TodayUiState.Error -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UiFeedback(state.message, UiFeedbackKind.ERROR)
                    UiActionButton(onClick = viewModel::retry, text = "重新读取课表")
                }
                is TodayUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        UiFeedback("正在读取课表…", UiFeedbackKind.LOADING, Modifier.padding(16.dp))
                    }
                }
                is TodayUiState.Success -> {
                    TodayContent(state, gridStyle, isDark, currentTime, onNavigate = onNavigate, onSelectDate = viewModel::selectDate)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayContent(
    state: TodayUiState.Success,
    gridStyle: ScheduleGridStyle,
    isDark: Boolean,
    currentTime: LocalTime,
    modifier: Modifier = Modifier,
    onNavigate: (Destination) -> Unit,
    onSelectDate: (LocalDate?) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<CourseDisplayModel?>(null) }
    val actualToday = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val subtitle = when (state.status) {
        TodayStatus.NoSemesterConfig -> "未设置开学日期"
        TodayStatus.Vacation -> "学期尚未开始 · ${state.startDate} 开学"
        TodayStatus.SemesterEnded -> "所选日期不在本学期"
        TodayStatus.Normal -> "第 ${state.weekIndex} 教学周"
    }
    LazyColumn(
        modifier.fillMaxSize().uiContentWidth(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val weekdays = stringArrayResource(Res.array.week_days_full_names)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                UiCourseIconButton(vectorResource(Res.drawable.arrow_back_24px), "前一天", tint = uiStyleColors().accent) {
                    onSelectDate(state.today.plus(-1, DateTimeUnit.DAY))
                }
                UiButton(
                    text = "${state.today}  ${weekdays[state.today.dayOfWeek.isoDayNumber - 1]} ▾",
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f),
                )
                UiCourseIconButton(vectorResource(Res.drawable.arrow_forward_24px), "后一天", tint = uiStyleColors().accent) {
                    onSelectDate(state.today.plus(1, DateTimeUnit.DAY))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                UiText(subtitle, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = uiStyleColors().secondaryText)
                if (state.today != actualToday) UiButton("回到今天", { onSelectDate(null) })
            }
        }
        if (state.status == TodayStatus.NoSemesterConfig) item {
            UiButton("设置开学日期", { onNavigate(Destination.CourseStartDateSettings) })
        }
        if (state.status == TodayStatus.Normal && state.courses.isEmpty()) item {
            UiFeedback(if (state.today == actualToday) "今天没有课程" else "所选日期没有课程", UiFeedbackKind.EMPTY)
        }
        itemsIndexed(state.courses, key = { _, model -> model.course.id }) { _, model ->
            CourseTimelineItem(model, gridStyle, isDark, currentTime,
                courseDate = state.today, actualToday = actualToday, onClick = { detail = model })
        }
    }
    if (showDatePicker) {
        UiDatePickerDialog(initialDateMillis = state.today.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
            onDismissRequest = { showDatePicker = false }, onConfirm = {
                onSelectDate(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date)
                showDatePicker = false
            })
    }
    detail?.let { model ->
        TodayCourseDetail(
            model = model,
            onDismiss = { detail = null },
            onEdit = { detail = null; onNavigate(Destination.AddEditCourse(courseId = it)) },
        )
    }
}

@Composable
internal fun TodayCourseDetail(model: CourseDisplayModel, onDismiss: () -> Unit, onEdit: (String) -> Unit) {
    val course = CourseWithWeeks(model.course, model.weeks.map { CourseWeek(model.course.id, it) })
    CourseDetailBottomSheet(
        MergedCourseBlock(model.course.day, 0f, 1f, listOf(course)),
        onDismissRequest = onDismiss,
        onEditClick = onEdit,
    )
}

@Composable
fun CourseTimelineItem(
    model: CourseDisplayModel,
    gridStyle: ScheduleGridStyle,
    isDark: Boolean,
    currentTime: LocalTime,
    courseDate: LocalDate,
    actualToday: LocalDate,
    onClick: () -> Unit,
) {
    val isFinished = courseDate < actualToday || (courseDate == actualToday && isCourseFinishedAt(model.endTime, currentTime))

    val colorPair = gridStyle.courseColorMaps.getOrElse(model.course.colorInt) {
        ScheduleGridStyle.DEFAULT_COLOR_MAPS[0]
    }
    val themeColor = if (isDark) colorPair.dark else colorPair.light
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val style = LocalUiStyle.current
    val indication = if (style == UiStyle.MATERIAL) LocalIndication.current else null
    val finishedLabel = stringResource(Res.string.course_status_finished)
    val colors = uiStyleColors()
    val surface = colors.surface.compositeOver(colors.background)
    val containerColor = if (style == UiStyle.MIUIX) surface else
        courseSurfaceColor(style, themeColor, surface, isDark).let {
            if (isFinished) it.copy(alpha = .65f).compositeOver(surface) else it
        }
    val cardText = courseTextOn(containerColor, colors.onSurface)
    val cardSecondary = courseTextOn(containerColor, colors.secondaryText)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                if (isFinished) stateDescription = finishedLabel
            },
    ) {
        if (style != UiStyle.MIUIX) {
            Column(
                modifier = Modifier.width(65.dp * LocalDensity.current.fontScale).padding(top = 4.dp),
                horizontalAlignment = Alignment.End,
            ) {
                UiText(
                    text = model.startTime ?: EMPTY_TIME_PLACEHOLDER,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        textDecoration = if (isFinished) TextDecoration.LineThrough else null,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = uiStyleColors().onSurface,
                )
                UiText(
                    text = model.endTime ?: EMPTY_TIME_PLACEHOLDER,
                    style = MaterialTheme.typography.labelSmall,
                    color = uiStyleColors().secondaryText,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            UiScheduleCourseCard(
                modifier = Modifier.fillMaxWidth().clickable(
                    interactionSource = interactionSource,
                    indication = indication,
                    onClickLabel = "查看课程详情",
                    role = Role.Button,
                    onClick = onClick,
                ),
                cornerRadius = if (style == UiStyle.MATERIAL) 12.dp else 16.dp,
                containerColor = containerColor,
                raised = false,
                pressed = pressed,
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    UiText(
                        text = displayedCourseName(model.course.name, gridStyle.hideCourseNumber),
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (isFinished) TextDecoration.LineThrough else null
                        ),
                        fontWeight = FontWeight.Bold,
                        color = cardText,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (style == UiStyle.MIUIX) {
                        Row(
                            Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(8.dp).background(themeColor, CircleShape))
                            UiText(
                                "${model.startTime ?: EMPTY_TIME_PLACEHOLDER}–${model.endTime ?: EMPTY_TIME_PLACEHOLDER}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cardSecondary,
                            )
                        }
                    }

                    if (!gridStyle.hideLocation && model.course.position.isNotBlank()) {
                        UiText(
                            text = stringResource(Res.string.course_position_prefix, model.course.position),
                            style = MaterialTheme.typography.bodySmall,
                            color = cardSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (!gridStyle.hideTeacher && model.course.teacher.isNotBlank()) {
                        UiText(
                            text = stringResource(Res.string.course_teacher_prefix, model.course.teacher),
                            style = MaterialTheme.typography.bodySmall,
                            color = cardSecondary
                        )
                    }

                    val credit = model.course.credit
                    if (credit != null && credit > 0f) {
                        val creditValStr = if (credit % 1f == 0f) {
                            credit.toInt().toString()
                        } else {
                            credit.toString()
                        }
                        val creditLabel = stringResource(Res.string.label_credit)
                        UiText(
                            text = "$creditLabel: $creditValStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = cardSecondary
                        )
                    }
                }
            }
            model.course.remark?.takeIf { it.isNotBlank() }?.let { remark ->
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, start = 4.dp)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(8.dp)
                ) {
                    Column {
                        UiText(
                            text = stringResource(Res.string.label_remark),
                            style = MaterialTheme.typography.labelSmall,
                            color = uiStyleColors().secondaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        UiText(
                            text = remark,
                            style = MaterialTheme.typography.bodySmall,
                            color = uiStyleColors().secondaryText,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
