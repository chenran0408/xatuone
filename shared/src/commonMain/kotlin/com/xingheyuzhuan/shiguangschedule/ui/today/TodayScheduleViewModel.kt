package com.xingheyuzhuan.shiguangschedule.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeScheduleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock

@KoinViewModel
class TodayScheduleViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val styleSettingsRepository: StyleSettingsRepository,
    private val timeScheduleRepository: TimeScheduleRepository
) : ViewModel() {

    companion object {
        private const val DEFAULT_SEMESTER_TOTAL_WEEKS = 20
        private const val MAX_TIME_SORT_KEY = "99:99"
    }

    val gridStyle: StateFlow<ScheduleGridStyle> = styleSettingsRepository.styleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleGridStyle())

    private val currentDate = flow {
        while (true) {
            emit(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
            delay(60_000L)
        }
    }.distinctUntilChanged()

    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    fun selectDate(date: LocalDate?) { selectedDate.value = date }

    private val refresh = MutableStateFlow(0)
    fun retry() { refresh.update { it + 1 } }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TodayUiState> = refresh.flatMapLatest { combine(
        appSettingsRepository.getAppSettings(),
        combine(currentDate, selectedDate) { today, selected -> selected ?: today }
    ) { settings, today -> settings to today }
        .flatMapLatest { (settings, today) ->
            val tableId = settings.currentCourseTableId
            val dayOfWeek = today.dayOfWeek.isoDayNumber

            combine(
                appSettingsRepository.getCourseTableConfigFlow(tableId),
                timeScheduleRepository.observeEffectiveTimeSlots(tableId, today)
            ) { config: CourseTableConfig?, timeSlots: List<TimeSlot> ->

                val startDate = config?.semesterStartDate?.let {
                    try { LocalDate.parse(it) } catch (e: Exception) { null }
                }

                val totalWeeks = config?.semesterTotalWeeks ?: DEFAULT_SEMESTER_TOTAL_WEEKS
                val firstDayOfWeek = config?.firstDayOfWeek ?: DayOfWeek.MONDAY.isoDayNumber

                val weekIndex = appSettingsRepository.getWeekIndexAtDate(today, config?.semesterStartDate, firstDayOfWeek)
                    ?.takeIf { it in 1..totalWeeks }

                // 判定所选日期是否处于生效的假期中
                val isSkippedDay = settings.holidays.any { holiday ->
                    holiday.isHoliday && holiday.dates.contains(today)
                }

                val status = courseDayStatus(today, startDate, weekIndex)

                // 记录状态与精准计算所需的物理配置
                DataSnapshot(
                    status = status,
                    weekIndex = weekIndex,
                    timeSlots = timeSlots,
                    startDate = startDate,
                    totalWeeks = totalWeeks,
                    firstDayOfWeek = firstDayOfWeek,
                    isSkippedDay = isSkippedDay
                )
            }.flatMapLatest { snapshot ->
                // 只有 Normal 状态且不是跳过日期时才查询数据库
                if (snapshot.status == TodayStatus.Normal && snapshot.weekIndex != null && !snapshot.isSkippedDay) {
                    courseTableRepository.getCoursesForDay(tableId, snapshot.weekIndex, dayOfWeek).map { courses ->
                        createSuccessState(courses, snapshot, today)
                    }
                } else {
                    // 如果是跳过日期或非正常学期状态，直接返回空课程列表
                    flowOf(createSuccessState(emptyList(), snapshot, today))
                }
            }
        }.onStart { emit(TodayUiState.Loading) }.catch { emit(TodayUiState.Error("课表读取失败：${it.message}")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState.Loading)

    /**
     * 内部辅助快照类
     */
    private data class DataSnapshot(
        val status: TodayStatus,
        val weekIndex: Int?,
        val timeSlots: List<TimeSlot>,
        val startDate: LocalDate?,
        val totalWeeks: Int,
        val firstDayOfWeek: Int,
        val isSkippedDay: Boolean
    )

    private fun createSuccessState(
        courses: List<CourseWithWeeks>,
        snapshot: DataSnapshot,
        today: LocalDate
    ): TodayUiState {
        val slotMap = snapshot.timeSlots.associateBy { it.number }

        val displayModels = courses.map { item ->
            val startSlot = slotMap[item.course.startSection]
            val endSlot = slotMap[item.course.endSection]
            CourseDisplayModel(
                course = item.course,
                startTime = item.course.customStartTime ?: startSlot?.startTime,
                endTime = item.course.customEndTime ?: endSlot?.endTime,
                weeks = item.weeks.map { it.weekNumber }
            )
        }.sortedWith(
            compareBy<CourseDisplayModel> { it.startTime ?: MAX_TIME_SORT_KEY }
                .thenBy { it.endTime ?: MAX_TIME_SORT_KEY }
        )

        return TodayUiState.Success(
            courses = displayModels,
            weekIndex = snapshot.weekIndex ?: 0,
            today = today,
            status = snapshot.status,
            startDate = snapshot.startDate,
            totalWeeks = snapshot.totalWeeks,
            firstDayOfWeek = snapshot.firstDayOfWeek
        )
    }
}

data class CourseDisplayModel(
    val course: Course,
    val startTime: String?,
    val endTime: String?,
    val weeks: List<Int> = emptyList()
)

enum class TodayStatus { Normal, NoSemesterConfig, SemesterEnded, Vacation }

sealed class TodayUiState {
    data object Loading : TodayUiState()
    data class Error(val message: String) : TodayUiState()
    data class Success(
        val courses: List<CourseDisplayModel>,
        val weekIndex: Int,
        val today: LocalDate,
        val status: TodayStatus,
        val startDate: LocalDate?,
        val totalWeeks: Int,
        val firstDayOfWeek: Int
    ) : TodayUiState()
}

internal fun courseDayStatus(date: LocalDate, start: LocalDate?, week: Int?): TodayStatus = when {
    start == null -> TodayStatus.NoSemesterConfig
    date < start -> TodayStatus.Vacation
    week == null -> TodayStatus.SemesterEnded
    else -> TodayStatus.Normal
}
