package com.xingheyuzhuan.shiguangschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeScheduleRepository
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridStyleComposed.Companion.toComposedStyle
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.ScheduleConfigPackage
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.WeeklyScheduleUiState
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.CourseMergeUtils.gridScaleToTime
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.CourseMergeUtils.mergeCourses
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.CourseMergeUtils.courseTimes
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.acceptsCourseOn
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.WeekSchedulePage
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.formatToHHmm
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.getTodayLocalDate
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.startOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)
@KoinViewModel
class WeeklyScheduleViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val timeScheduleRepository: TimeScheduleRepository,
    private val styleSettingsRepository: StyleSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyScheduleUiState())
    val uiState: StateFlow<WeeklyScheduleUiState> = _uiState.asStateFlow()

    private val _pagerMondayDate = MutableStateFlow(
        getTodayLocalDate().startOfWeek(DayOfWeek.MONDAY)
    )

    private val visibleToday = MutableStateFlow(getTodayLocalDate())

    private val _isDarkTheme = MutableStateFlow(false)

    fun updateDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    private val appSettingsFlow = appSettingsRepository.getAppSettings()
    private val styleFlow = styleSettingsRepository.styleFlow

    private val courseTableConfigFlow = appSettingsFlow.flatMapLatest { settings ->
        val tableId = settings.currentCourseTableId
        if (tableId.isNotEmpty()) {
            appSettingsRepository.getCourseTableConfigFlow(tableId)
        } else {
            flowOf(null)
        }
    }

    private val currentCoursesFlow = combine(
        _pagerMondayDate,
        appSettingsFlow,
        courseTableConfigFlow,
        styleFlow
    ) { date, settings, config, style ->
        val tableId = settings.currentCourseTableId
        val mode = style.toComposedStyle().scheduleMode

        if (config != null && tableId.isNotEmpty()) {
            val window = listOf(
                date.minus(1, DateTimeUnit.WEEK),
                date,
                date.plus(1, DateTimeUnit.WEEK)
            )

            val semesterStart = config.semesterStartDate?.let { LocalDate.parse(it) }
            val holidays = settings.holidays.filter { it.isHoliday }.flatMap { it.dates }.toSet()
            combine(window.map { weekStart ->
                val pageWeek = appSettingsRepository.getWeekIndexAtDate(weekStart, config.semesterStartDate, config.firstDayOfWeek)
                val dates = (0..6).map { weekStart.plus(it, DateTimeUnit.DAY) }
                val courses = when {
                    pageWeek == null || pageWeek !in 1..config.semesterTotalWeeks -> flowOf(emptyList())
                    settings.showNonCurrentWeekCourses -> courseTableRepository.getCoursesWithWeeksByTableId(tableId).map { all ->
                        all.filter { course -> course.weeks.any { it.weekNumber >= pageWeek } }
                    }
                    else -> courseTableRepository.getCoursesWithWeeksByDate(tableId, weekStart, config)
                }
                combine(courses, timeScheduleRepository.observeEffectiveTimeSlots(tableId, dates)) { rows, slots ->
                    weekStart to buildWeekPage(rows, slots, holidays, pageWeek ?: 0, semesterStart, mode)
                }
            }) { results -> results.toMap() }
        } else {
            flowOf(emptyMap())
        }
    }.flatMapLatest { it }

    init {
        viewModelScope.launch {
            val configAndTimeFlow = combine(
                appSettingsFlow,
                courseTableConfigFlow,
                styleFlow,
                _pagerMondayDate
            ) { settings, config, style, mondayDate ->
                ScheduleConfigPackage(settings, config, style, mondayDate)
            }

            combine(configAndTimeFlow, currentCoursesFlow, visibleToday, _isDarkTheme) { configPkg, cache, today, isDark ->
                val config = configPkg.config
                val startDate = config?.semesterStartDate?.let { LocalDate.parse(it) }
                val firstDayOfWeekInt = config?.firstDayOfWeek ?: DayOfWeek.MONDAY.isoDayNumber
                val totalWeeks = config?.semesterTotalWeeks ?: 20
                val page = cache[configPkg.mondayDate]
                val sidebarDate = if (today.startOfWeek(DayOfWeek(firstDayOfWeekInt)) == configPkg.mondayDate) today else configPkg.mondayDate
                val timeSlots = page?.slotsByDay?.get(sidebarDate.dayOfWeek.isoDayNumber).orEmpty()

                val currentWeekNum = appSettingsRepository.getWeekIndexAtDate(
                    targetDate = today,
                    startDateStr = config?.semesterStartDate,
                    firstDayOfWeekInt = firstDayOfWeekInt
                )

                val weekIndex = appSettingsRepository.getWeekIndexAtDate(
                    targetDate = configPkg.mondayDate,
                    startDateStr = config?.semesterStartDate,
                    firstDayOfWeekInt = firstDayOfWeekInt
                )

                val daysUntil = if (startDate != null && today < startDate) {
                    today.daysUntil(startDate).toLong()
                } else 0L

                val currentWeekCourses = page?.courses.orEmpty()
                fixInvalidCourseColors(currentWeekCourses.flatMap { it.courses }, configPkg.style)

                val wallpaperPath = appSettingsRepository.getActiveWallpaperPath(isDark)

                val previousState = _uiState.value

                WeeklyScheduleUiState(
                    style = configPkg.style,
                    showWeekends = config?.showWeekends ?: false,
                    totalWeeks = totalWeeks,
                    courseCache = cache,
                    currentMergedCourses = page?.courses.orEmpty(),
                    timeSlots = timeSlots,
                    isSemesterSet = startDate != null,
                    semesterStartDate = startDate,
                    firstDayOfWeek = firstDayOfWeekInt,
                    weekIndexInPager = weekIndex,
                    currentWeekNumber = currentWeekNum,
                    pagerMondayDate = configPkg.mondayDate,
                    daysUntilStart = daysUntil,
                    floatingCourse = previousState.floatingCourse,
                    floatingSourceWeek = previousState.floatingSourceWeek,
                    floatingDurationMinutes = previousState.floatingDurationMinutes,
                    wallpaperPath = wallpaperPath
                )
            }.collect { _uiState.value = it }
        }
    }

    fun updateToday(date: LocalDate) { visibleToday.value = date }

    fun updatePagerDate(newDate: LocalDate) = _pagerMondayDate.update { newDate }

    fun switchCourseTable(tableId: String) {
        viewModelScope.launch {
            val currentSettings = appSettingsRepository.getAppSettingsOnce()
            val newSettings = currentSettings.copy(currentCourseTableId = tableId)
            appSettingsRepository.insertOrUpdateAppSettings(newSettings)
        }
    }

    private fun fixInvalidCourseColors(courses: List<CourseWithWeeks>, style: ScheduleGridStyle) {
        viewModelScope.launch {
            val validRange = style.courseColorMaps.indices
            courses.forEach { cw ->
                if (cw.course.colorInt !in validRange) {
                    courseTableRepository.updateCourseColor(cw.course.id, style.generateRandomColorIndex())
                }
            }
        }
    }

    /**
     * 进入跨周移动暂存状态
     */
    fun enterFloatingMode(course: CourseWithWeeks, sourceWeek: Int, slots: List<TimeSlot>) {
        val (start, end) = courseTimes(course.course, slots) ?: return
        val duration = (end.toSecondOfDay() - start.toSecondOfDay()) / 60
        _uiState.update {
            it.copy(
                floatingCourse = course,
                floatingSourceWeek = sourceWeek,
                floatingDurationMinutes = duration
            )
        }
    }

    /**
     * 全清空或取消挂起队列
     */
    fun exitFloatingMode() {
        _uiState.update {
            it.copy(
                floatingCourse = null,
                floatingSourceWeek = null,
                floatingDurationMinutes = null
            )
        }
    }

    /**
     * 配合跨周结算的最终持久化落地更新
     */
    fun updateCourseTimeByFloatingGesture(
        targetWeek: Int,
        targetDay: Int,
        startSection: Float,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.acceptsCourseOn(targetWeek, targetDay)) return@launch
            try {
                val courseWrapper = state.floatingCourse ?: return@launch
                val sourceWeek = state.floatingSourceWeek ?: return@launch
                val mode = state.style.toComposedStyle().scheduleMode
                val slots = state.timeSlots

                val currentSettings = appSettingsRepository.getAppSettingsOnce()
                val tableId = currentSettings.currentCourseTableId
                if (tableId.isBlank()) return@launch

                val originalCourse = courseWrapper.course

                val updatedCourseForTime = if (mode == ScheduleModeProto.TIME_24H_MODE) {
                    val duration = state.floatingDurationMinutes ?: return@launch
                    moveTimedCourse(originalCourse, targetDay, gridScaleToTime(startSection, slots, mode), duration)
                } else {
                    val sectionOffset = (originalCourse.endSection ?: 1) - (originalCourse.startSection ?: 1)
                    val newStartSection = startSection.toInt().coerceIn(1, slots.size)
                    val newEndSection = (newStartSection + sectionOffset).coerceIn(1, slots.size)
                    if (newStartSection > newEndSection) return@launch

                    originalCourse.copy(
                        day = targetDay,
                        isCustomTime = false,
                        customStartTime = null,
                        customEndTime = null,
                        startSection = newStartSection,
                        endSection = newEndSection
                    )
                }

                val isNoPositionChange = originalCourse.day == updatedCourseForTime.day &&
                        originalCourse.startSection == updatedCourseForTime.startSection &&
                        originalCourse.endSection == updatedCourseForTime.endSection &&
                        originalCourse.customStartTime == updatedCourseForTime.customStartTime &&
                        originalCourse.customEndTime == updatedCourseForTime.customEndTime

                if (sourceWeek == targetWeek && isNoPositionChange) {
                    return@launch
                }

                val isSingleWeek = courseWrapper.weeks.size <= 1

                if (isSingleWeek) {
                    val weekNumbers = listOf(targetWeek)
                    courseTableRepository.upsertCourse(updatedCourseForTime, weekNumbers)
                } else {
                    val remainingWeeks = courseWrapper.weeks
                        .map { it.weekNumber }
                        .filter { it != sourceWeek }
                    courseTableRepository.upsertCourse(originalCourse, remainingWeeks)

                    val clonedNewId = Uuid.random().toString()
                    val finalClonedCourse = updatedCourseForTime.copy(id = clonedNewId)
                    courseTableRepository.upsertCourse(finalClonedCourse, listOf(targetWeek))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.update {
                    it.copy(
                        floatingCourse = null,
                        floatingSourceWeek = null,
                        floatingDurationMinutes = null
                    )
                }
                onComplete()
            }
        }
    }

    /**
     * 统一持久化调度手势调课方法（拆分并更新单周/多周周次逻辑）
     */
    fun updateCourseTimeByGesture(
        courseId: String,
        targetDay: Int,
        startSection: Float,
        endSection: Float,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val mode = state.style.toComposedStyle().scheduleMode
                val slots = state.timeSlots
                val currentWeek = state.weekIndexInPager ?: return@launch
                if (!state.acceptsCourseOn(currentWeek, targetDay)) return@launch

                val currentSettings = appSettingsRepository.getAppSettingsOnce()
                val tableId = currentSettings.currentCourseTableId
                if (tableId.isBlank()) return@launch

                val allCoursesWithWeeks = courseTableRepository.getCoursesWithWeeksByTableId(tableId).firstOrNull() ?: return@launch
                val targetWrapper = allCoursesWithWeeks.find { it.course.id == courseId } ?: return@launch
                val originalCourse = targetWrapper.course

                val updatedCourseForTime = if (mode == ScheduleModeProto.TIME_24H_MODE) {
                    val newStartTime = gridScaleToTime(startSection, slots, mode)
                    val newEndTime = gridScaleToTime(endSection, slots, mode)

                    originalCourse.copy(
                        day = targetDay,
                        isCustomTime = true,
                        customStartTime = newStartTime.formatToHHmm(),
                        customEndTime = newEndTime.formatToHHmm(),
                        startSection = null,
                        endSection = null
                    )
                } else {
                    val newStartSection = (startSection.toInt() + 1).coerceIn(1, slots.size)
                    val newEndSection = endSection.toInt().coerceIn(1, slots.size)
                    if (newStartSection > newEndSection) return@launch

                    originalCourse.copy(
                        day = targetDay,
                        isCustomTime = false,
                        customStartTime = null,
                        customEndTime = null,
                        startSection = newStartSection,
                        endSection = newEndSection
                    )
                }
                val isNoPositionChange = originalCourse.day == updatedCourseForTime.day &&
                        originalCourse.startSection == updatedCourseForTime.startSection &&
                        originalCourse.endSection == updatedCourseForTime.endSection &&
                        originalCourse.customStartTime == updatedCourseForTime.customStartTime &&
                        originalCourse.customEndTime == updatedCourseForTime.customEndTime

                if (isNoPositionChange) {
                    return@launch
                }

                val isSingleWeek = targetWrapper.weeks.size <= 1

                if (isSingleWeek) {
                    val weekNumbers = targetWrapper.weeks.map { it.weekNumber }
                    courseTableRepository.upsertCourse(updatedCourseForTime, weekNumbers)
                } else {
                    val remainingWeeks = targetWrapper.weeks
                        .map { it.weekNumber }
                        .filter { it != currentWeek }
                    courseTableRepository.upsertCourse(originalCourse, remainingWeeks)

                    val clonedNewId = Uuid.random().toString()
                    val finalClonedCourse = updatedCourseForTime.copy(id = clonedNewId)
                    courseTableRepository.upsertCourse(finalClonedCourse, listOf(currentWeek))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onComplete()
            }
        }
    }
}
internal fun buildWeekPage(
    courses: List<CourseWithWeeks>,
    slotsByDate: Map<LocalDate, List<TimeSlot>>,
    holidays: Set<LocalDate>,
    week: Int,
    semesterStart: LocalDate?,
    mode: ScheduleModeProto
): WeekSchedulePage {
    val byDay = courses.groupBy { it.course.day }
    val blocks = slotsByDate.flatMap { (date, slots) ->
        if (semesterStart == null || date < semesterStart || date in holidays) emptyList()
        else mergeCourses(byDay[date.dayOfWeek.isoDayNumber].orEmpty(), slots, week, mode)
    }
    return WeekSchedulePage(week, blocks, slotsByDate.mapKeys { it.key.dayOfWeek.isoDayNumber })
}

internal fun moveTimedCourse(course: Course, targetDay: Int, start: LocalTime, durationMinutes: Int): Course {
    val endMinute = (start.hour * 60 + start.minute + durationMinutes).coerceAtMost(23 * 60 + 59)
    return course.copy(
        day = targetDay, isCustomTime = true,
        customStartTime = start.formatToHHmm(), customEndTime = LocalTime(endMinute / 60, endMinute % 60).formatToHHmm(),
        startSection = null, endSection = null
    )
}
