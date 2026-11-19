package com.xingheyuzhuan.shiguangschedule.xatu

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.StartScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.homeCourseAt
import com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.upcomingXatuExam
import com.xingheyuzhuan.shiguangschedule.ui.today.CourseDisplayModel
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuExam
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuBalanceRecord
import com.xingheyuzhuan.shiguangschedule.xatu.campus.xatuElectricityTrend
import com.xingheyuzhuan.shiguangschedule.xatu.campus.recentXatuElectricityChange
import com.xingheyuzhuan.shiguangschedule.xatu.campus.xatuElectricityWindow
import java.time.LocalDateTime
import kotlin.test.*

class XatuHomeOverviewTest {
    @Test
    fun currentCourseHasPriorityOverFutureCoursesAndEndsAtItsActualBoundary() {
        val active = course("active", "08:00", "09:00")
        val next = course("next", "10:00", "11:00")
        assertSame(active, homeCourseAt(listOf(next, active), "08:30:00"))
        assertSame(active, homeCourseAt(listOf(active, next), "09:00:00"))
        assertSame(next, homeCourseAt(listOf(active, next), "09:00:01"))
        assertNull(homeCourseAt(listOf(active, next), "11:00:01"))
    }

    @Test
    fun unknownTimesAreNotPresentedAsACompletedDay() {
        val unknown = course("unknown", null, null)
        assertSame(unknown, homeCourseAt(listOf(unknown), "19:00:00"))
        val malformed = course("malformed", "unknown", "unknown")
        assertSame(malformed, homeCourseAt(listOf(malformed), "19:00:00"))
        assertNull(homeCourseAt(emptyList(), "19:00:00"))
    }

    @Test
    fun nearestExamUsesACompleteFutureDateAndDoesNotAlterTheFullList() {
        fun exam(id: String, time: String) = XatuExam(id, id, "", "", time, "教室", "", "")
        val later = exam("later", "2026-12-10 10:00-12:00")
        val next = exam("next", "2026-12-03 09:00-11:00")
        val records = listOf(later, exam("past", "2026-11-01 09:00"), exam("unknown", "12-01 09:00"), next)
        assertSame(next, upcomingXatuExam(records, LocalDateTime.of(2026, 11, 7, 12, 0)))
        assertNull(upcomingXatuExam(records, LocalDateTime.of(2027, 1, 1, 0, 0)))
        assertEquals(4, records.size)
    }

    @Test
    fun newInstallsStartAtHomeWhileSavedTimetablePreferencesAreKept() {
        assertEquals(StartScreen.CAMPUS_HOME, AppSettingsModel.fromPreferences(emptyPreferences(), "").startScreen)
        listOf(StartScreen.COURSE_SCHEDULE, StartScreen.TODAY_SCHEDULE).forEach { previous ->
            val prefs = preferencesOf(stringPreferencesKey("start_screen") to previous.value)
            assertEquals(previous, AppSettingsModel.fromPreferences(prefs, "").startScreen)
        }
    }

    @Test
    fun trendKeepsOnlyActualReadingsInTimeOrderWithoutFillingMissingDays() {
        val history = listOf(XatuBalanceRecord(10, "18.00"), XatuBalanceRecord(1, "20.00"), XatuBalanceRecord(100, "35.00"))
        assertEquals(listOf(1L, 10L, 100L), xatuElectricityTrend(history).map { it.at })
        assertEquals("17.00", recentXatuElectricityChange(history)?.toPlainString())
        assertEquals(listOf(10L, 1L, 100L), history.map { it.at })
        assertNull(recentXatuElectricityChange(history.take(1)))
        assertTrue(xatuElectricityTrend(emptyList()).isEmpty())
    }

    @Test
    fun recentBalanceChangeKeepsItsSignAndDoesNotBecomeDailyUsage() {
        val records = (1L..10L).map { XatuBalanceRecord(it, "${20 - it}.50") }
        assertEquals((4L..10L).toList(), xatuElectricityTrend(records.reversed()).map { it.at })
        assertEquals("-1.00", recentXatuElectricityChange(records)?.toPlainString())
        assertEquals("0.00", recentXatuElectricityChange(listOf(records.last(), XatuBalanceRecord(11, "10.50")))?.toPlainString())
    }

    @Test
    fun balanceWindowFiltersObservedSnapshotsWithoutCreatingUsageRecords() {
        val now = 30L * 24 * 60 * 60 * 1000
        val history = listOf(
            XatuBalanceRecord(now - 31L * 24 * 60 * 60 * 1000, "60.00"),
            XatuBalanceRecord(now - 29L * 24 * 60 * 60 * 1000, "55.00"),
            XatuBalanceRecord(now - 6L * 24 * 60 * 60 * 1000, "48.00"),
            XatuBalanceRecord(now, "43.00"),
        )
        assertEquals(listOf("48.00", "43.00"), xatuElectricityWindow(history, 7, now).map { it.balance })
        assertEquals(listOf("55.00", "48.00", "43.00"), xatuElectricityWindow(history, 30, now).map { it.balance })
    }

    private fun course(id: String, start: String?, end: String?) = CourseDisplayModel(
        Course(id, "table", id, "教师", "教室", 1, 1, 2, false, null, null, 0), start, end
    )
}
