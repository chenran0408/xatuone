package com.xingheyuzhuan.shiguangschedule

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeek
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.CourseMergeUtils
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.courseTextOn
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.fittingCourseTime
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.scheduleColumnEdge
import com.xingheyuzhuan.shiguangschedule.ui.today.isCourseFinishedAt
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.WeeklyScheduleUiState
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.acceptsCourseOn
import com.xingheyuzhuan.shiguangschedule.ui.schedule.buildWeekPage
import com.xingheyuzhuan.shiguangschedule.ui.schedule.moveTimedCourse
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.calculateSingleSchedulables
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.visibleWeekdays
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.calculateCurrentSectionIndex
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ScheduleRenderingTest {
    @Test
    fun narrowCourseCellsShowAWholeTimeOrAnEllipsis() {
        val interval = "08:30–09:15"
        assertEquals(interval, fittingCourseTime(interval, 11, String::length))
        assertEquals("08:30", fittingCourseTime(interval, 5, String::length))
        assertEquals("…", fittingCourseTime(interval, 4, String::length))
        assertEquals("08:30", fittingCourseTime("08:30", 5, String::length))
    }

    @Test
    fun sevenDayGridUsesEveryPixelAndConflictsStayInsideTheirDay() {
        for (width in listOf(1079, 1080, 1081, 1345)) {
            val edges = (0..7).map { scheduleColumnEdge(width, 7, it) }
            val widths = edges.zipWithNext { left, right -> right - left }
            assertEquals(0, edges.first())
            assertEquals(width, edges.last())
            assertTrue(widths.max() - widths.min() <= 1)
            for (dayWidth in widths) {
                val split = scheduleColumnEdge(dayWidth, 2, 1)
                assertEquals(dayWidth, scheduleColumnEdge(dayWidth, 2, 2))
                assertTrue(kotlin.math.abs(split - (dayWidth - split)) <= 1)
            }
        }
    }

    @Test
    fun automaticCourseTextStaysLegibleOnUserChosenColors() {
        val preferred = Color(0xff243349)
        assertEquals(preferred, courseTextOn(Color.White, preferred))
        for (surface in listOf(Color.DarkGray, Color.Blue, Color.Green, Color.Red)) {
            for (ink in listOf(Color.White, preferred)) {
                val a = surface.luminance() + .05f
                val b = courseTextOn(surface, ink).luminance() + .05f
                assertTrue(maxOf(a, b) / minOf(a, b) >= 4.5f)
            }
        }
    }

    @Test
    fun todayCourseFinishesOnlyAfterItsEndTime() {
        assertFalse(isCourseFinishedAt("09:00", LocalTime(8, 59)))
        assertFalse(isCourseFinishedAt("09:00", LocalTime(9, 0)))
        assertTrue(isCourseFinishedAt("09:00", LocalTime(9, 0, 1)))
    }

    @Test
    fun missingOrInvalidEndTimeDoesNotMarkACourseFinished() {
        assertFalse(isCourseFinishedAt("unknown", LocalTime(9, 0)))
        assertFalse(isCourseFinishedAt(null, LocalTime(9, 0)))
        assertFalse(isCourseFinishedAt(null, LocalTime(0, 0)))
    }

    @Test
    fun overlappingCoursesKeepDetailsAndWeekStateForTheDetailPager() {
        val first = course("first", listOf(3))
        val second = course("second", listOf(4))
        val slots = listOf(
            TimeSlot("table", 1, "08:00", "08:45"),
            TimeSlot("table", 2, "08:55", "09:40"),
        )

        val blocks = CourseMergeUtils.mergeCourses(listOf(first, second), slots, currentWeek = 3)

        assertEquals(2, blocks.size)
        val active = blocks.single { it.courses.single().course.id == "first" }
        val inactive = blocks.single { it.courses.single().course.id == "second" }
        assertFalse(active.isVisualDemoted)
        assertTrue(inactive.isVisualDemoted)
        assertEquals(listOf(first, second), active.clusterCourses)
        assertSame(first, active.courses.single())
        assertSame(second, inactive.courses.single())
        assertEquals(0f, active.startSection)
        assertEquals(2f, active.endSection)
    }

    @Test
    fun customTimeCoursesStillRenderWithoutPresetSlotsIn24HourMode() {
        val source = course("custom", listOf(3)).let {
            it.copy(course = it.course.copy(
                startSection = null,
                endSection = null,
                isCustomTime = true,
                customStartTime = "10:10",
                customEndTime = "11:40",
            ))
        }
        val block = CourseMergeUtils.mergeCourses(
            listOf(source), emptyList(), 3, ScheduleModeProto.TIME_24H_MODE
        ).single()

        assertEquals(10f + 10f / 60f, block.startSection, 0.0001f)
        assertEquals(11f + 40f / 60f, block.endSection, 0.0001f)
        assertTrue(block.needsProportionalRendering)
        assertSame(source, block.courses.single())
        assertTrue(CourseMergeUtils.mergeCourses(emptyList(), emptyList(), 3).isEmpty())
    }

    @Test
    fun hiddenWeekendsStillShowFridayWhenWeekStartsOnSunday() {
        val friday = course("friday", listOf(1)).let { it.copy(course = it.course.copy(day = 5)) }
        val slots = listOf(TimeSlot("t", 1, "08:00", "08:45"), TimeSlot("t", 2, "08:55", "09:40"))
        val blocks = CourseMergeUtils.mergeCourses(listOf(friday), slots, 1)
        val days = visibleWeekdays(firstDayOfWeek = 7, showWeekends = false)
        assertEquals(listOf(1, 2, 3, 4, 5), days)
        val column = calculateSingleSchedulables(blocks, days).single().columnIndex
        assertEquals(5, days[column])
        assertEquals(listOf(7, 1, 2, 3, 4, 5, 6), visibleWeekdays(7, true))
    }

    @Test
    fun aHolidaySkipsOnlyThatDateAndTheOtherDayUsesItsOwnTimetable() {
        val monday = LocalDate(2026, 11, 16)
        val tuesday = LocalDate(2026, 11, 17)
        val mondaySlots = listOf(TimeSlot("m", 1, "08:00", "08:45"), TimeSlot("m", 2, "08:55", "09:40"))
        val tuesdaySlots = listOf(TimeSlot("t", 1, "09:00", "09:45"), TimeSlot("t", 2, "09:55", "10:40"))
        val original = course("monday", listOf(12))
        val rows = listOf(original, original.copy(course = original.course.copy(id = "tuesday", day = 2)))
        val page = buildWeekPage(rows, mapOf(monday to mondaySlots, tuesday to tuesdaySlots),
            setOf(monday), 12, LocalDate(2026, 8, 31), ScheduleModeProto.TIME_24H_MODE)
        val block = page.courses.single()
        assertEquals(2, block.day)
        assertEquals(9f, block.startSection)
        assertEquals(10f + 40f / 60, block.endSection, .0001f)
        assertEquals(tuesdaySlots, page.slotsByDay[2])
        assertEquals(9, CourseMergeUtils.courseTimes(block.courses.single().course, tuesdaySlots)?.first?.hour)
    }

    @Test
    fun midweekSemesterStartOmitsEarlierCoursesButKeepsOpeningDay() {
        val monday = LocalDate(2026, 11, 16)
        val wednesday = LocalDate(2026, 11, 18)
        val slots = listOf(TimeSlot("t", 1, "08:00", "08:45"), TimeSlot("t", 2, "08:55", "09:40"))
        val first = course("monday", listOf(1))
        val openingDay = first.copy(course = first.course.copy(id = "wednesday", day = 3))
        val week = buildWeekPage(listOf(first, openingDay), mapOf(monday to slots, wednesday to slots),
            emptySet(), 1, wednesday, ScheduleModeProto.SECTION_MODE)
        assertEquals(listOf(openingDay), week.courses.flatMap { it.courses })
    }

    @Test
    fun moveTargetsStayInsideTheTeachingWeeksAndRespectASundayWeekStart() {
        val state = WeeklyScheduleUiState(totalWeeks = 12, semesterStartDate = LocalDate(2026, 11, 18), firstDayOfWeek = 7)
        assertFalse(state.acceptsCourseOn(0, 3))
        assertFalse(state.acceptsCourseOn(13, 1))
        assertFalse(state.acceptsCourseOn(1, 7))
        assertFalse(state.acceptsCourseOn(1, 2))
        assertTrue(state.acceptsCourseOn(1, 3))
        assertTrue(state.acceptsCourseOn(2, 7))
        assertTrue(state.acceptsCourseOn(12, 6))
        assertFalse(state.copy(semesterStartDate = null).acceptsCourseOn(1, 3))
    }

    @Test
    fun movingAPresetCourseAcrossWeeksKeepsItsFullDurationIncludingTheBreak() {
        val original = course("course", listOf(1)).course
        val slots = listOf(TimeSlot("t", 1, "08:00", "08:45"), TimeSlot("t", 2, "08:55", "09:40"))
        val (start, end) = requireNotNull(CourseMergeUtils.courseTimes(original, slots))
        val moved = moveTimedCourse(original, 3, LocalTime(10, 0), (end.toSecondOfDay() - start.toSecondOfDay()) / 60)
        assertEquals(LocalTime(10, 0) to LocalTime(11, 40), CourseMergeUtils.courseTimes(moved, emptyList()))
        assertEquals(3, moved.day)
        assertEquals(original.credit, moved.credit)
    }

    @Test
    fun currentSectionFollowsTheClockThroughBreaks() {
        val slots = listOf(TimeSlot("t", 1, "08:00", "08:45"), TimeSlot("t", 2, "08:55", "09:40"))
        assertEquals(1, calculateCurrentSectionIndex(slots, LocalTime(8, 44)))
        assertEquals(-1, calculateCurrentSectionIndex(slots, LocalTime(8, 45)))
        assertEquals(2, calculateCurrentSectionIndex(slots, LocalTime(8, 55)))
    }

    private fun course(id: String, weeks: List<Int>) = CourseWithWeeks(
        course = Course(
            id = id,
            courseTableId = "table",
            name = "长课程名称与课程序号 | 230056.04",
            teacher = "教师甲、教师乙",
            position = "教三楼-长地点名称-301",
            day = 1,
            startSection = 1,
            endSection = 2,
            customStartTime = null,
            customEndTime = null,
            colorInt = 2,
            remark = "保留完整备注",
            credit = 3f,
        ),
        weeks = weeks.map { CourseWeek(id, it) },
    )
}
