package com.xingheyuzhuan.shiguangschedule

import com.xingheyuzhuan.shiguangschedule.ui.schedule.courseDisplayName
import com.xingheyuzhuan.shiguangschedule.ui.schedule.displayedCourseName
import com.xingheyuzhuan.shiguangschedule.ui.today.courseDayStatus
import com.xingheyuzhuan.shiguangschedule.ui.today.TodayStatus
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.CourseImportProgress
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.model.toProto
import com.xingheyuzhuan.shiguangschedule.data.model.toCompose
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import kotlinx.datetime.LocalDate
import kotlin.test.*

class CourseModuleTest {
    @Test fun unknownSemesterCannotBecomeAnEmptyTeachingDay() {
        val date = LocalDate(2026, 11, 7)
        assertEquals(TodayStatus.NoSemesterConfig, courseDayStatus(date, null, null))
        assertEquals(TodayStatus.Vacation, courseDayStatus(date, LocalDate(2027, 2, 22), null))
        assertEquals(TodayStatus.SemesterEnded, courseDayStatus(date, LocalDate(2026, 2, 23), null))
        assertEquals(TodayStatus.Normal, courseDayStatus(date, LocalDate(2026, 8, 31), 10))
    }

    @Test fun numberVisibilityDoesNotRewriteTheStoredNameOrMisreadOrdinaryTitles() {
        val raw = "大学英语Ⅲ | 230056.04"
        assertEquals("230056.04", courseDisplayName(raw).number)
        assertEquals("大学英语Ⅲ", displayedCourseName(raw, true))
        assertEquals(raw, displayedCourseName(raw, false))
        assertEquals("艺术 | 设计", displayedCourseName("艺术 | 设计", true))
        assertNull(courseDisplayName("概率论（A）").number)
    }

    @Test fun displayPreferenceRoundTripsWithoutChangingOtherSettings() {
        val style = ScheduleGridStyle(hideCourseNumber = true, hideTeacher = true, sectionHeightDp = 88f)
        assertEquals(style, ScheduleGridStyleProto.ADAPTER.decode(ScheduleGridStyleProto.ADAPTER.encode(style.toProto())).toCompose())
        assertFalse(ScheduleGridStyleProto().toCompose().hideCourseNumber)
    }

    @Test fun successNeedsSavedCoursesAndCompletionNotASuccessLookingToast() {
        val running = CourseImportProgress(running = true, message = "成功导入")
        assertFalse(running.scriptReturned(null).completed)
        assertNotNull(running.scriptReturned(null).error)
        assertNotNull(running.complete().error)
        val saved = running.copy(savedCount = 3)
        assertFalse(saved.completed)
        assertTrue(saved.complete().completed)
        assertEquals(saved.complete(), saved.complete().scriptReturned(null))
        val failed = saved.copy(error = "磁盘写入失败").complete()
        assertFalse(failed.completed)
        assertEquals("磁盘写入失败", failed.error)
    }
}
