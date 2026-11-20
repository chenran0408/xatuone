package com.xingheyuzhuan.shiguangschedule.ui.schedule

import com.xingheyuzhuan.shiguangschedule.ui.components.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.UiActionButton
import com.xingheyuzhuan.shiguangschedule.ui.components.UiSelectionCard
import androidx.compose.runtime.saveable.rememberSaveable
import com.xingheyuzhuan.shiguangschedule.ui.today.TodayScheduleScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.xatuCourseImport
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.navigation.AddEditCourseChannel
import com.xingheyuzhuan.shiguangschedule.navigation.PresetCourseData
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.CourseDetailBottomSheet
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.FloatingCourseBar
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGrid
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridActions
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridStyleComposed
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridViewState
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.WeekSelectorBottomSheet
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.rememberScheduleGridState
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.acceptsCourseOn
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.MergedCourseBlock
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.startOfWeek
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.calculateCurrentSectionIndex
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.text_no_courses_this_week
import shiguangschedule.shared.generated.resources.action_select_table
import shiguangschedule.shared.generated.resources.format_week_display
import shiguangschedule.shared.generated.resources.snackbar_add_course_within_semester
import shiguangschedule.shared.generated.resources.swap_horiz_24px
import shiguangschedule.shared.generated.resources.tune_24px
import shiguangschedule.shared.generated.resources.title_current_week
import shiguangschedule.shared.generated.resources.title_semester_not_set
import shiguangschedule.shared.generated.resources.title_vacation
import shiguangschedule.shared.generated.resources.title_vacation_until_start
import kotlin.time.Clock

private const val INFINITE_PAGER_CENTER = Int.MAX_VALUE / 2

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeeklyScheduleScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    onNavHideFractionChanged: (Float) -> Unit = {},
    viewModel: WeeklyScheduleViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var dayMode by rememberSaveable { mutableStateOf(false) }

    val isDark = LocalIsDarkTheme.current
    LaunchedEffect(isDark) {
        viewModel.updateDarkTheme(isDark)
    }

    val initialTime = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    val clock = remember {
        flow {
            while (true) {
                val time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                emit(time)
                delay(60_000L - time.second * 1000L - time.nanosecond / 1_000_000L)
            }
        }
    }
    val now by clock.collectAsStateWithLifecycle(initialValue = initialTime)
    val today = now.date
    val anchorDate = rememberSaveable { today.toString() }
    val firstDay = DayOfWeek(uiState.firstDayOfWeek)
    val baseWeek = remember(anchorDate, firstDay) { LocalDate.parse(anchorDate).startOfWeek(firstDay) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarMsg = stringResource(Res.string.snackbar_add_course_within_semester)

    val pagerState = rememberPagerState(
        initialPage = INFINITE_PAGER_CENTER,
        pageCount = { Int.MAX_VALUE }
    )

    var previousDate by rememberSaveable { mutableStateOf(today.toString()) }
    LaunchedEffect(today, baseWeek) {
        viewModel.updateToday(today)
        val oldPage = INFINITE_PAGER_CENTER + baseWeek.daysUntil(LocalDate.parse(previousDate).startOfWeek(firstDay)) / 7
        val newPage = INFINITE_PAGER_CENTER + baseWeek.daysUntil(today.startOfWeek(firstDay)) / 7
        if (newPage != oldPage && pagerState.currentPage == oldPage) pagerState.scrollToPage(newPage)
        previousDate = today.toString()
    }
    LaunchedEffect(pagerState.currentPage, baseWeek) {
        val offset = pagerState.currentPage - INFINITE_PAGER_CENTER
        viewModel.updatePagerDate(baseWeek.plus(offset, DateTimeUnit.WEEK))
    }

    var showWeekSelector by remember { mutableStateOf(false) }
    var showTableSwitcher by remember { mutableStateOf(false) }
    var isGridHolding by remember { mutableStateOf(false) }
    var selectedBlockForDetail by remember { mutableStateOf<MergedCourseBlock?>(null) }

    val systemFontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val composedStyle by remember(uiState.style, systemFontScale) {
        derivedStateOf {
            with(ScheduleGridStyleComposed) { uiState.style.toComposedStyle() }.let {
                it.copy(
                    timeColumnWidth = it.timeColumnWidth * systemFontScale,
                    dayHeaderHeight = it.dayHeaderHeight * systemFontScale,
                    sectionHeight = it.sectionHeight * systemFontScale,
                )
            }
        }
    }

    val floatingCourse = uiState.floatingCourse

    val snackbarHostState = remember { SnackbarHostState() }

    val customTextColor = composedStyle.pageTextColor ?: uiStyleColors().onSurface
    val customSubTextColor = customTextColor.copy(alpha = 0.7f)
    val uiStyle = LocalUiStyle.current

    val (displayTitle, titleTextColor) = when {
        !uiState.isSemesterSet || uiState.semesterStartDate == null -> {
            stringResource(Res.string.title_semester_not_set) to MaterialTheme.colorScheme.error
        }
        uiState.daysUntilStart > 0 -> {
            stringResource(Res.string.title_vacation_until_start, uiState.daysUntilStart.toString()) to customTextColor
        }
        uiState.weekIndexInPager != null && uiState.weekIndexInPager!! in 1..uiState.totalWeeks -> {
            stringResource(Res.string.title_current_week, uiState.weekIndexInPager.toString()) to customTextColor
        }
        else -> {
            stringResource(Res.string.title_vacation) to customTextColor
        }
    }

    val navHideFraction = if (!dayMode && floatingCourse != null) 1f else 0f

    LaunchedEffect(navHideFraction) {
        onNavHideFractionChanged(navHideFraction)
    }

    val windowSize = LocalWindowInfo.current.containerSize
    val shortLandscape = with(LocalDensity.current) { windowSize.width > windowSize.height && windowSize.height.toDp() < 480.dp }

    Box(modifier = Modifier.fillMaxSize().background(
        if (uiState.wallpaperPath == null) uiStyleColors().background else Color.Transparent
    )) {
        uiState.wallpaperPath?.let { path ->
            AsyncImage(
                model = path,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        UiScaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            topBar = {
                    Column(Modifier.uiContentWidth()) {
                        UiTopAppBar(title = if (uiStyle == UiStyle.MIUIX) displayTitle else "$displayTitle ▾", titleColor = titleTextColor, containerColor = Color.Transparent, onTitleClick = {
                            if (!uiState.isSemesterSet || uiState.semesterStartDate == null) onNavigate(Destination.CourseStartDateSettings)
                            else showWeekSelector = true
                        }, actions = {
                            UiCourseIconButton(
                                vectorResource(Res.drawable.swap_horiz_24px),
                                stringResource(Res.string.action_select_table),
                                tint = customTextColor,
                                inToolbar = true,
                            ) { showTableSwitcher = true }
                        })
                        val courseActions: @Composable RowScope.() -> Unit = {
                            UiButton("同步课表", { onNavigate(xatuCourseImport) }, Modifier.weight(1f))
                            UiButton("课表设置", { onNavigate(Destination.CourseTools) }, Modifier.weight(1f),
                                contentColor = uiStyleColors().secondaryText)
                        }
                        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (uiStyle == UiStyle.MATERIAL) {
                                val picker: @Composable (Modifier, Boolean) -> Unit = { modifier, compact ->
                                    SingleChoiceSegmentedButtonRow(modifier) {
                                        listOf("日视图", "周视图").forEachIndexed { index, label ->
                                            SegmentedButton(
                                                selected = dayMode == (index == 0),
                                                onClick = { dayMode = index == 0 },
                                                shape = SegmentedButtonDefaults.itemShape(index, 2),
                                                modifier = if (compact) Modifier.semantics { contentDescription = label } else Modifier,
                                            ) { Text(if (compact) label.take(1) else label) }
                                        }
                                    }
                                }
                                BoxWithConstraints(Modifier.fillMaxWidth()) {
                                    val inline = maxWidth / systemFontScale >= if (shortLandscape) 300.dp else 320.dp
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (!inline) picker(Modifier.fillMaxWidth(), false)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (inline) picker(Modifier.weight(1.55f), true)
                                            Button(
                                                { onNavigate(xatuCourseImport) },
                                                Modifier.weight(1f).then(if (inline) Modifier.semantics { contentDescription = "同步课程表" } else Modifier),
                                                contentPadding = PaddingValues(horizontal = if (inline) 8.dp else 12.dp),
                                            ) { Text("同步课表") }
                                            OutlinedButton(
                                                { onNavigate(Destination.CourseTools) },
                                                Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = if (inline) 8.dp else 12.dp),
                                            ) { Text("课表设置") }
                                        }
                                    }
                                }
                            } else if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
                                val fontScale = LocalDensity.current.fontScale
                                BoxWithConstraints(Modifier.fillMaxWidth()) {
                                    if (maxWidth / fontScale >= 328.dp) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            MiuixCourseViewTabs(dayMode, { dayMode = it }, Modifier.weight(1.65f))
                                            UiButton("同步课表", { onNavigate(xatuCourseImport) }, Modifier.weight(1f))
                                            UiCourseIconButton(
                                                vectorResource(Res.drawable.tune_24px),
                                                "课表设置",
                                                tint = uiStyleColors().secondaryText,
                                                inToolbar = true,
                                            ) { onNavigate(Destination.CourseTools) }
                                        }
                                    } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        MiuixCourseViewTabs(dayMode, { dayMode = it }, Modifier.fillMaxWidth())
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = courseActions)
                                    }
                                }
                            } else {
                                val fontScale = LocalDensity.current.fontScale
                                BoxWithConstraints(Modifier.fillMaxWidth()) {
                                    if (maxWidth / fontScale >= 334.dp) Row(Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        CouiCourseViewTabs(dayMode, { dayMode = it }, Modifier.weight(1f))
                                        UiButton("同步课表", { onNavigate(xatuCourseImport) }, Modifier.width(96.dp))
                                        UiCourseIconButton(
                                            vectorResource(Res.drawable.tune_24px),
                                            "课表设置",
                                            tint = uiStyleColors().secondaryText,
                                        ) { onNavigate(Destination.CourseTools) }
                                    } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CouiCourseViewTabs(dayMode, { dayMode = it }, Modifier.fillMaxWidth())
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = courseActions)
                                    }
                                }
                            }
                        }
                    }
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    snackbar = { data ->
                        if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) MiuixCourseSnackbar(data)
                        else if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) CouiCourseSnackbar(data)
                        else androidx.compose.material3.Snackbar(data)
                    }
                )
            }
        ) { scaffoldInnerPadding ->

            if (dayMode) {
                Box(Modifier.padding(scaffoldInnerPadding)) {
                    TodayScheduleScreen(onNavigate, onBack, embedded = true)
                }
            } else HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(
                        start = scaffoldInnerPadding.calculateStartPadding(LayoutDirection.Ltr),
                        top = scaffoldInnerPadding.calculateTopPadding(),
                        end = scaffoldInnerPadding.calculateEndPadding(LayoutDirection.Ltr),
                        bottom = scaffoldInnerPadding.calculateBottomPadding()
                    )
                    .fillMaxSize(),
                beyondViewportPageCount = 1,
                userScrollEnabled = !isGridHolding
            ) { pageIndex ->

                val pageMondayDate = remember(pageIndex, baseWeek) {
                    baseWeek.plus(pageIndex - INFINITE_PAGER_CENTER, DateTimeUnit.WEEK)
                }

                val pageYearString = remember(pageMondayDate) {
                    pageMondayDate.year.toString()
                }

                val pageDateStrings = remember(pageMondayDate) {
                    (0..6).map { i ->
                        val d = pageMondayDate.plus(i.toLong(), DateTimeUnit.DAY)
                        val month = d.month.number.toString().padStart(2, '0')
                        val day = d.day.toString().padStart(2, '0')
                        "$month-$day"
                    }
                }

                val pageTodayIndex = remember(pageMondayDate, today) {
                    val weekDates = (0..6).map { pageMondayDate.plus(it.toLong(), DateTimeUnit.DAY) }
                    weekDates.indexOf(today)
                }

                val page = uiState.courseCache[pageMondayDate]
                val pageCourses = page?.courses.orEmpty()
                val sidebarDate = if (pageTodayIndex >= 0) today else pageMondayDate
                val pageSlots = page?.slotsByDay?.get(sidebarDate.dayOfWeek.isoDayNumber).orEmpty()
                val activeSection = if (pageTodayIndex >= 0) calculateCurrentSectionIndex(pageSlots, now.time) else -1
                val activeHour = if (pageTodayIndex >= 0) now.hour else -1
                val gridState = rememberScheduleGridState()

                val weekIndex = page?.week
                val totalWeeks = uiState.totalWeeks
                val weekStr = if (weekIndex != null && weekIndex in 1..totalWeeks) {
                    stringResource(Res.string.format_week_display, weekIndex)
                } else {
                    null
                }

                val gridViewState = remember(pageDateStrings, pageYearString, uiState, pageCourses, pageTodayIndex, weekStr, activeSection, activeHour) {
                    ScheduleGridViewState(
                        dates = pageDateStrings,
                        currentYear = pageYearString,
                        currentWeek = weekStr,
                        timeSlots = pageSlots,
                        dayTimeSlots = page?.slotsByDay.orEmpty(),
                        mergedCourses = pageCourses,
                        showWeekends = uiState.showWeekends,
                        todayIndex = pageTodayIndex,
                        firstDayOfWeek = uiState.firstDayOfWeek,
                        currentSectionIndex = activeSection,
                        currentHour = activeHour
                    )
                }

                val gridActions = remember(uiState, page, snackbarMsg) {
                    object : ScheduleGridActions {
                        override fun onCourseBlockClicked(block: MergedCourseBlock) {
                            selectedBlockForDetail = block
                        }

                        override fun onGridCellClicked(day: Int, section: Int) {
                            val currentWeek = page?.week ?: 0
                            if (!uiState.acceptsCourseOn(currentWeek, day)) {
                                coroutineScope.launch { snackbarHostState.showSnackbar(snackbarMsg) }
                                return
                            }
                            if (floatingCourse != null) {
                                val startSec = section.toFloat()
                                coroutineScope.launch {
                                    viewModel.updateCourseTimeByFloatingGesture(
                                        targetWeek = currentWeek,
                                        targetDay = day,
                                        startSection = startSec
                                    )
                                }
                            } else {
                                coroutineScope.launch {
                                    val currentWeekSet = setOf(currentWeek)
                                    val presetData = if (composedStyle.scheduleMode == ScheduleModeProto.TIME_24H_MODE) {
                                        val startHour = section.coerceIn(0, 23)
                                        val endHour = (startHour + 1) % 24

                                        val startTimeStr = "${startHour.toString().padStart(2, '0')}:00"
                                        val endTimeStr = "${endHour.toString().padStart(2, '0')}:00"

                                        PresetCourseData(
                                            day = day,
                                            isCustomTime = true,
                                            customStartTime = startTimeStr,
                                            customEndTime = endTimeStr,
                                            presetWeeks = currentWeekSet
                                        )
                                    } else {
                                        PresetCourseData(
                                            day = day,
                                            startSection = section,
                                            endSection = section,
                                            isCustomTime = false,
                                            presetWeeks = currentWeekSet
                                        )
                                    }

                                    AddEditCourseChannel.sendEvent(presetData)
                                    onNavigate(Destination.AddEditCourse())
                                }
                            }
                        }

                        override fun onTimeSlotClicked() {
                            onNavigate(Destination.TimeScheduleManagement)
                        }

                        override fun onHoldStateChanged(isHolding: Boolean) {
                            isGridHolding = isHolding
                        }

                        override fun onCourseMovedWithinGrid(
                            block: MergedCourseBlock,
                            newDay: Int,
                            newStartSection: Float,
                            newEndSection: Float
                        ) {
                            val currentWeek = page?.week ?: 0
                            if (uiState.acceptsCourseOn(currentWeek, newDay)) {
                                block.courses.firstOrNull()?.course?.id?.let { courseId ->
                                    coroutineScope.launch {
                                        viewModel.updateCourseTimeByGesture(
                                            courseId = courseId,
                                            targetDay = newDay,
                                            startSection = newStartSection,
                                            endSection = newEndSection
                                        )
                                    }
                                }
                            } else {
                                coroutineScope.launch { snackbarHostState.showSnackbar(snackbarMsg) }
                            }
                        }

                        override fun onCourseTimeAdjusted(
                            block: MergedCourseBlock,
                            newStart: Float,
                            newEnd: Float
                        ) {
                            val currentWeek = page?.week ?: 0
                            if (uiState.acceptsCourseOn(currentWeek, block.day)) {
                                block.courses.firstOrNull()?.course?.id?.let { courseId ->
                                    coroutineScope.launch {
                                        viewModel.updateCourseTimeByGesture(
                                            courseId = courseId,
                                            targetDay = block.day,
                                            startSection = newStart,
                                            endSection = newEnd
                                        )
                                    }
                                }
                            } else {
                                coroutineScope.launch { snackbarHostState.showSnackbar(snackbarMsg) }
                            }
                        }

                        override fun onInitiateFloatingMode(block: MergedCourseBlock) {
                            val targetCourseWrapper = block.courses.firstOrNull()
                            val currentWeek = page?.week ?: 0
                            if (targetCourseWrapper != null && uiState.acceptsCourseOn(currentWeek, block.day)) {
                                viewModel.enterFloatingMode(
                                    course = targetCourseWrapper,
                                    sourceWeek = currentWeek,
                                    slots = page?.slotsByDay?.get(block.day).orEmpty()
                                )
                            }
                        }
                    }
                }

                Box(Modifier.fillMaxSize().clipToBounds()) {
                    ScheduleGrid(
                        state = gridState,
                        viewState = gridViewState,
                        actions = gridActions,
                        style = composedStyle,
                        modifier = if (uiStyle != UiStyle.MATERIAL && uiState.wallpaperPath == null)
                            Modifier.padding(top = 4.dp).clip(uiScheduleCourseShape(12.dp)).background(uiStyleColors().surface)
                        else Modifier,
                    )
                    if (uiState.isSemesterSet) {
                        if (page == null) {
                            CircularProgressIndicator(
                                color = uiStyleColors().accent,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else if (pageCourses.isEmpty()) {
                            UiText(
                                text = stringResource(Res.string.text_no_courses_this_week),
                                style = MaterialTheme.typography.bodyMedium,
                                color = customSubTextColor,
                                modifier = Modifier.align(Alignment.Center).padding(16.dp)
                            )
                        }
                    }
                }
            }
        }

        FloatingCourseBar(
            floatingCourse = floatingCourse,
            onCancelClick = { viewModel.exitFloatingMode() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }

    // 周次选择弹窗
    if (showWeekSelector) {
        WeekSelectorBottomSheet(
            totalWeeks = uiState.totalWeeks,
            currentWeek = uiState.currentWeekNumber ?: 1,
            selectedWeek = uiState.weekIndexInPager ?: (uiState.currentWeekNumber ?: 1),
            onWeekSelected = { week ->
                val currentWeekAtPage = uiState.weekIndexInPager ?: 1
                val offset = week - currentWeekAtPage
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + offset)
                }
                dayMode = false
                showWeekSelector = false
            },
            onDismissRequest = { showWeekSelector = false }
        )
    }

    // 课表切换弹窗
    if (showTableSwitcher) {
        CourseTablePickerDialog(
            title = stringResource(Res.string.action_select_table),
            onDismissRequest = { showTableSwitcher = false },
            onTableSelected = { table: CourseTable ->
                viewModel.switchCourseTable(table.id)
                showTableSwitcher = false
            }
        )
    }

    // 课程详情弹窗
    if (selectedBlockForDetail != null) {
        CourseDetailBottomSheet(
            block = selectedBlockForDetail!!,
            onDismissRequest = { selectedBlockForDetail = null },
            onEditClick = { courseId ->
                selectedBlockForDetail = null
                onNavigate(Destination.AddEditCourse(courseId = courseId))
            }
        )
    }
}
