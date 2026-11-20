package com.xingheyuzhuan.shiguangschedule

import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport
import com.xingheyuzhuan.shiguangschedule.data.model.ScriptCourseConfig
import com.xingheyuzhuan.shiguangschedule.data.model.savedDatesForSemester
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScriptCourseImportTest {
    @Test fun historicalTermNeedsANewWeekCountWhenLaterClassesAreAdded() {
        val saved = CourseTableConfig("table", semesterStartDate = "2024-09-02", semesterTotalWeeks = 16)
        val history = ScriptCourseConfig()
        assertEquals(16, history.withSavedDates(saved, lastCourseWeek = 16).semesterTotalWeeks)
        val extended = history.withSavedDates(saved, lastCourseWeek = 18)
        assertEquals("2024-09-02", extended.semesterStartDate)
        assertNull(extended.semesterTotalWeeks)
    }

    @Test fun currentAdapterOnlyUpdatesSchoolDates() {
        val previous = CourseTableConfig("table", showWeekends = true, firstDayOfWeek = 7,
            semesterStartDate = "2026-02-23", semesterTotalWeeks = 20)
        val patch = CourseImportExport.json.decodeFromString<ScriptCourseConfig>(
            """{"semesterStartDate":"2026-08-31","semesterTotalWeeks":26}"""
        )
        assertEquals(previous.copy(semesterStartDate = "2026-08-31", semesterTotalWeeks = 26), patch.applyTo(previous))
        assertNull(patch.defaultClassDuration)
        assertNull(patch.defaultBreakDuration)
    }

    @Test fun reimportKeepsDatesOnlyForTheSelectedAcademicYearAndTerm() {
        val autumn = CourseTableConfig("table", semesterStartDate = "2026-08-31", semesterTotalWeeks = 26)
        val spring = autumn.copy(semesterStartDate = "2027-02-22", semesterTotalWeeks = 21)
        assertEquals(autumn, savedDatesForSemester(autumn, "2026-2027学年 秋季学期（当前学期）"))
        assertEquals(spring, savedDatesForSemester(spring, "2026-2027学年 春季学期"))
        assertNull(savedDatesForSemester(autumn, "2024-2025学年 秋季学期"))
        assertNull(savedDatesForSemester(autumn, "2026-2027学年 春季学期"))
        assertNull(savedDatesForSemester(autumn.copy(semesterStartDate = null), "2026-2027学年 秋季学期"))
    }
}
