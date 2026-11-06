package com.xingheyuzhuan.shiguangschedule.xatu

import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuAcademicParser
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuCoordinate
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XatuCoreTest {
    @Test
    fun wgs84CoordinatesRemainStable() {
        val point = XatuCoordinate(34.2658, 108.9535, "WGS84")
        val converted = XatuLocation.toWgs84(point)

        assertEquals(point.latitude, converted.latitude, 1e-9)
        assertEquals(point.longitude, converted.longitude, 1e-9)
        assertEquals("WGS84", converted.system)
    }

    @Test
    fun gcj02CoordinatesConvertBackNearOriginalPoint() {
        val gps = XatuCoordinate(34.2658, 108.9535, "WGS84")
        val gcj = XatuCoordinate(34.2690, 108.9600, "GCJ02")
        val converted = XatuLocation.toWgs84(gcj)

        assertEquals("WGS84", converted.system)
        assertTrue(converted.latitude in 34.24..34.30)
        assertTrue(converted.longitude in 108.92..109.00)
        assertTrue(kotlin.math.abs(converted.latitude - gps.latitude) < 0.01)
        assertTrue(kotlin.math.abs(converted.longitude - gps.longitude) < 0.01)
    }

    @Test
    fun semesterParserPrefersStructuredRecordsAndDeduplicatesIds() {
        val html = """
            [{"id": "12", "schoolYear": "2026-2027", "name": "秋季学期", "startDate": "2026-09-01"},
             {"id": "12", "schoolYear": "2026-2027", "name": "秋季学期"},
             {"id": "11", "schoolYear": "2025-2026", "name": "春季学期"}]
        """.trimIndent()

        val semesters = XatuAcademicParser.semesters(html)

        assertEquals(listOf("12", "11"), semesters.map { it.id })
        assertEquals("2026-2027学年秋季学期", semesters.first().name)
        assertEquals("2026-09-01", semesters.first().startDate)
    }

    @Test
    fun gradeParserKeepsCourseIdentityAndSummary() {
        val html = """
            <div>课程门数：1 总学分：3 平均绩点：3.5</div>
            <table><tr><th>学年学期</th><th>课程序号</th><th>课程名称</th><th>学分</th><th>成绩</th><th>绩点</th></tr>
            <tr><td>2026-2027秋季</td><td>230056.04</td><td>大学英语Ⅲ</td><td>3</td><td>92</td><td>3.5</td></tr></table>
        """.trimIndent()

        val page = XatuAcademicParser.grades(html)

        assertEquals(1, page.grades.size)
        assertEquals("230056.04", page.grades.single().code)
        assertEquals("大学英语Ⅲ", page.grades.single().course)
        assertEquals(1, page.summary.count)
        assertEquals(3.0, page.summary.credits)
        assertEquals(3.5, page.summary.gpa)
    }

    @Test
    fun examParserCombinesDateAndTimeAndBuildsStableId() {
        val html = """
            <table><tr><th>课程名称</th><th>日期</th><th>时间</th><th>考场</th><th>座位号</th></tr>
            <tr><td>高等数学</td><td>2027-01-08</td><td>09:00-11:00</td><td>教1-101</td><td>12</td></tr></table>
        """.trimIndent()

        val exam = XatuAcademicParser.exams(html).single()

        assertEquals("高等数学", exam.course)
        assertEquals("2027-01-08 09:00-11:00", exam.time)
        assertEquals("教1-101", exam.room)
        assertEquals("12", exam.seat)
        assertEquals("|高等数学||2027-01-08 09:00-11:00|教1-101", exam.id)
    }

    @Test
    fun planParserBuildsSectionsAndCourseCompletion() {
        val html = """
            <div>姓名：张三 专业/专业方向：计算机科学 GPA：3.2 要求学分/实修学分：160/120</div>
            <table>
              <tr class="darkColumn"><td>通识教育</td><td>30</td><td>24</td></tr>
              <tr><td>1</td><td>230001</td><td>程序设计</td><td>4</td><td>4</td><td>4.0</td><td>是</td></tr>
            </table>
        """.trimIndent()

        val plan = XatuAcademicParser.plan(html)

        assertEquals("张三", plan.studentName)
        assertEquals("计算机科学", plan.major)
        assertEquals(160.0, plan.requiredCredits)
        assertEquals(120.0, plan.earnedCredits)
        assertEquals("程序设计", plan.sections.single().courses.single().name)
        assertTrue(plan.sections.single().courses.single().passed)
    }

    @Test
    fun catalogParserReadsCourseMetadata() {
        val html = """
            <table><tr><td>230002</td><td class="course">数据结构</td><td>3</td><td>48</td><td></td><td></td><td></td><td></td><td></td><td>2026秋</td><td></td><td>是</td></tr></table>
        """.trimIndent()

        val course = XatuAcademicParser.catalog(html).single()

        assertEquals("230002", course.code)
        assertEquals("数据结构", course.name)
        assertEquals("必修", course.nature)
        assertEquals("3", course.credits)
        assertEquals("48", course.hours)
    }
}
