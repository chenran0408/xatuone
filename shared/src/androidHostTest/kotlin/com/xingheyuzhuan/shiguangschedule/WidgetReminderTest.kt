package com.xingheyuzhuan.shiguangschedule

import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.data.db.widget.upcomingCourseReminders
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTimeBinding
import com.xingheyuzhuan.shiguangschedule.data.db.main.MainAppDatabaseConstructor
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeScheduleRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.widgetTeachingWeek
import java.lang.reflect.Proxy
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WidgetReminderTest {
    @Test
    fun editingTheBoundPublicTimetableEmitsAnotherSnapshotWithoutRebinding() = runBlocking {
        val original = TimeSlot("public", 1, "08:00", "08:45")
        val slots = MutableStateFlow(listOf(original))
        val binding = CourseTimeBinding("courses", CourseTimeBinding.TargetType.SINGLE, "public")
        val repository = TimeScheduleRepository(
            MainAppDatabaseConstructor.initialize(), dao(),
            dao { name, args ->
                check(name == "getTimeSlotsByTimeTableId" && args.single() == "public")
                slots
            },
            dao { name, _ ->
                check(name == "observeBindingByCourseTableId")
                flowOf(binding)
            }, dao()
        )
        val starts = withTimeout(3000) {
            repository.getTimeScheduleFlowByTableId("courses").map {
                val start = slots.value.single().startTime
                if (start == "08:00") slots.value = listOf(original.copy(startTime = "09:00", endTime = "09:45"))
                start
            }.take(2).toList()
        }
        assertEquals(listOf("08:00", "09:00"), starts)
    }

    private inline fun <reified T> dao(
        crossinline call: (String, Array<out Any?>) -> Any? = { method, _ -> error("Unexpected DAO call $method") }
    ): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
        call(method.name, args.orEmpty())
    } as T

    @Test
    fun weekOneStartsOnTheConfiguredOpeningDateEvenWhenWeeksStartEarlier() {
        val start = LocalDate(2026, 9, 2) // 周三开学，每周从周日开始
        fun week(day: Int) = widgetTeachingWeek(LocalDate(2026, 9, day), start, 7, 2)

        assertNull(week(1))
        assertEquals(1, week(2))
        assertEquals(1, week(5))
        assertEquals(2, week(6))
        assertNull(week(13))
    }

    @Test
    fun missingTimeHolidaysAndPastCoursesDoNotTakeTheNextAlarmSlot() {
        val first = WidgetCourse("a", "课程", "", "", "08:00", "08:45", date = "2026-09-02", colorInt = 0)
        val next = first.copy(id = "b", date = "2026-09-03", startTime = "00:05", endTime = "00:50")
        val courses = listOf(
            first.copy(startTime = ""),
            first,
            first.copy(startTime = "20:00", isSkipped = true),
            next
        )

        val alarms = upcomingCourseReminders(courses, LocalDateTime.parse("2026-09-02T18:00"), 15, 1)

        assertEquals(listOf(next to LocalDateTime.parse("2026-09-02T23:50")), alarms)
    }
}
