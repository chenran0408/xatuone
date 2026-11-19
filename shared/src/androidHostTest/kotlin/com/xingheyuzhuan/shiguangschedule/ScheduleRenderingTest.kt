package com.xingheyuzhuan.shiguangschedule

import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeek
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.CourseMergeUtils
import com.xingheyuzhuan.shiguangschedule.ui.today.isCourseFinishedAt
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ScheduleRenderingTest {
    @Test
    fun todayCourseFinishesOnlyAfterItsEndTime() {
        assertFalse(isCourseFinishedAt("09:00", LocalTime(8, 59)))
        assertFalse(isCourseFinishedAt("09:00", LocalTime(9, 0)))
        assertTrue(isCourseFinishedAt("09:00", LocalTime(9, 0, 1)))
    }

    @Test
    fun todayUnknownTimesKeepTheExistingDisplayBehavior() {
        assertFalse(isCourseFinishedAt("unknown", LocalTime(9, 0)))
        assertTrue(isCourseFinishedAt(null, LocalTime(9, 0)))
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
