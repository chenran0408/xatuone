package com.xingheyuzhuan.shiguangschedule.xatu

import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuAcademicParser
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuCoordinate
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuAcademicCacheFile
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuSemester
import com.xingheyuzhuan.shiguangschedule.xatu.academic.decodeAcademicCache
import java.time.LocalDate
import com.xingheyuzhuan.shiguangschedule.xatu.academic.upgradeXatuAuthRedirect
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import kotlin.test.assertSame

class XatuCoreTest {
    @Test
    fun academicAuthRedirectUsesHttpsAndKeepsTheServiceParameter() {
        val response = authRedirectResponse("http://authserver.xatu.edu.cn/authserver/login?service=http%3A%2F%2Fjwgl2018.xatu.edu.cn%2Feams%2FhomeExt.action")
        val secure = upgradeXatuAuthRedirect(response)
        assertEquals(302, secure.code)
        assertEquals(
            "https://authserver.xatu.edu.cn/authserver/login?service=http%3A%2F%2Fjwgl2018.xatu.edu.cn%2Feams%2FhomeExt.action",
            secure.header("Location")
        )
    }

    @Test
    fun academicAuthUpgradeDoesNotRewriteOtherRedirects() {
        listOf(
            "https://authserver.xatu.edu.cn/authserver/login",
            "http://jwgl2018.xatu.edu.cn/eams/home.action",
            "http://authserver.xatu.edu.cn.example.com/login",
            "/eams/home.action"
        ).forEach { location ->
            val response = authRedirectResponse(location)
            assertSame(response, upgradeXatuAuthRedirect(response))
        }
    }

    private fun authRedirectResponse(location: String): Response = Response.Builder()
        .request(Request.Builder().url("http://jwgl2018.xatu.edu.cn/eams/homeExt!main.action").build())
        .protocol(Protocol.HTTP_1_1)
        .code(302)
        .message("Found")
        .header("Location", location)
        .build()

    @Test
    fun teachingWeekSelectsCurrentSemesterInsteadOfFutureListHead() {
        val semesters = listOf(
            XatuSemester("104", "2027-2028学年1"),
            XatuSemester("103", "2026-2027学年2"),
            XatuSemester("102", "2026-2027学年1"),
            XatuSemester("90", "2025-2026学年2"),
            XatuSemester("89", "2025-2026学年1")
        )
        assertEquals("102", XatuAcademicParser.currentSemesterId(semesters, 10, LocalDate.of(2026, 11, 7)))
        assertEquals("90", XatuAcademicParser.currentSemesterId(semesters, 3, LocalDate.of(2026, 3, 20)))
        // January still belongs to the autumn semester that started the previous year.
        assertEquals("89", XatuAcademicParser.currentSemesterId(semesters, 20, LocalDate.of(2026, 1, 16)))
    }

    @Test
    fun missingCurrentSemesterDoesNotSelectAnUnrelatedFutureSemester() {
        assertFailsWith<IllegalStateException> {
            XatuAcademicParser.currentSemesterId(listOf(XatuSemester("104", "2027-2028学年1")), 10, LocalDate.of(2026, 11, 7))
        }
    }

    @Test
    fun gradeSummaryReadsSchoolTotalRowInsteadOfFirstSemesterYear() {
        val syntheticHtml = """
            <table><thead><tr><th>学年度</th><th>学期</th><th>门数</th><th>总学分</th><th>平均绩点</th></tr></thead>
            <tbody><tr><td>2018-2019</td><td>1</td><td>8</td><td>19.50</td><td>3.25</td></tr>
            <tr><th colspan="2">在校汇总</th><th>42</th><th>102.75</th><th>2.75</th></tr>
            <tr><th colspan="5">统计时间:2021-01-01 12:00</th></tr></tbody></table>
        """.trimIndent()
        val summary = XatuAcademicParser.summary(syntheticHtml)
        assertEquals(42, summary.count)
        assertEquals(102.75, summary.credits)
        assertEquals(2.75, summary.gpa)
    }

    @Test
    fun gradeSummaryDoesNotReadTableHeadersAsLabelledValues() {
        val summary = XatuAcademicParser.summary("""
            <table><tr><th>学年度</th><th>学期</th><th>门数</th><th>总学分</th><th>平均绩点</th></tr>
            <tr><td>2023-2024</td><td>1</td><td>13</td><td>24.5</td><td>2.89</td></tr></table>
        """)
        assertEquals(null, summary.gpa)
        assertEquals(null, summary.credits)
        assertEquals(null, summary.count)
    }

    @Test
    fun enablingCheckinUsesCurrentInputCoordinates() {
        val location = XatuLocation.checkinLocation("34.2658", "108.9535", true)
        assertTrue(location.enabled)
        assertEquals(34.2658, location.latitude)
        assertEquals(108.9535, location.longitude)
        assertEquals("GCJ02", location.system)
    }

    @Test
    fun invalidCheckinCoordinatesCannotBeSaved() {
        listOf("" to "108", "NaN" to "108", "91" to "108", "34" to "181", "34" to "Infinity").forEach { (lat, lng) ->
            assertFailsWith<IllegalArgumentException> { XatuLocation.checkinLocation(lat, lng, true) }
        }
    }

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
            <table><tr><th>学年学期</th><th>课程序号</th><th>课程名称</th><th>学分</th><th>平时成绩</th><th>期末成绩</th><th>成绩</th><th>绩点</th></tr>
            <tr><td>2026-2027秋季</td><td>230056.04</td><td>大学英语Ⅲ</td><td>3</td><td>95</td><td>90</td><td>92</td><td>3.5</td></tr></table>
        """.trimIndent()

        val page = XatuAcademicParser.grades(html)

        assertEquals(1, page.grades.size)
        assertEquals("230056.04", page.grades.single().code)
        assertEquals("大学英语Ⅲ", page.grades.single().course)
        assertEquals("92", page.grades.single().score)
        assertEquals(listOf("平时成绩" to "95", "期末成绩" to "90"), page.grades.single().parts.map { it.name to it.score })
        assertEquals(1, page.summary.count)
        assertEquals(3.0, page.summary.credits)
        assertEquals(3.5, page.summary.gpa)
    }

    @Test
    fun examParserCombinesDateAndTimeAndBuildsStableId() {
        val html = """
            <table><tr><th>课程名称</th><th>课程序号</th><th>考试名称</th><th>日期</th><th>时间</th><th>考场</th><th>座位号</th><th>备注</th></tr>
            <tr><td>高等数学</td><td>230001.01</td><td>期末考试</td><td>2027-01-08</td><td>09:00-11:00</td><td>教1-101</td><td>12</td><td>携带学生证</td></tr></table>
        """.trimIndent()

        val exam = XatuAcademicParser.exams(html).single()

        assertEquals("高等数学", exam.course)
        assertEquals("2027-01-08 09:00-11:00", exam.time)
        assertEquals("教1-101", exam.room)
        assertEquals("12", exam.seat)
        assertEquals("230001.01", exam.code)
        assertEquals("期末考试", exam.name)
        assertEquals("携带学生证", exam.note)
        assertEquals("230001.01|高等数学|期末考试|2027-01-08 09:00-11:00|教1-101", exam.id)
    }

    @Test
    fun planParserBuildsSectionsAndCourseCompletion() {
        val html = """
            <div>姓名：张三 专业/专业方向：计算机科学 GPA：3.2 要求学分/实修学分：160/120</div>
            <table>
              <tr class="darkColumn"><td>通识教育</td><td>30</td><td>24</td></tr>
              <tr><td>1</td><td>230001</td><td>程序设计</td><td>4</td><td>4</td><td>4.0</td><td>是</td></tr>
              <tr><td>2</td><td>230002</td><td>数据结构</td><td>3</td><td>0</td><td></td><td>否</td></tr>
            </table>
        """.trimIndent()

        val plan = XatuAcademicParser.plan(html)

        assertEquals("张三", plan.studentName)
        assertEquals("计算机科学", plan.major)
        assertEquals(160.0, plan.requiredCredits)
        assertEquals(120.0, plan.earnedCredits)
        assertEquals("程序设计", plan.sections.single().courses.first().name)
        assertTrue(plan.sections.single().courses.first().passed)
        assertFalse(plan.sections.single().courses.last().passed)
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
        assertEquals("2026秋", course.term)
    }

    @Test
    fun academicCacheReadsLegacyPlaintext() {
        val cache = XatuAcademicCacheFile(semesters = listOf(XatuSemester("12", "2026-2027秋季学期")))
        val raw = Json.encodeToString(cache)

        val decoded = decodeAcademicCache(raw) { _, _ -> error("legacy cache must not be decrypted") }

        assertEquals(cache, decoded)
    }

    @Test
    fun academicCacheReadsEncryptedFormat() {
        val cache = XatuAcademicCacheFile(semesters = listOf(XatuSemester("12", "2026-2027秋季学期")))
        val raw = """{"encryptedData":"ciphertext","iv":"nonce"}"""

        val decoded = decodeAcademicCache(raw) { encryptedData, iv ->
            assertEquals("ciphertext", encryptedData)
            assertEquals("nonce", iv)
            Json.encodeToString(cache)
        }

        assertEquals(cache, decoded)
    }

    @Test
    fun academicCacheDoesNotTreatUndecryptableCiphertextAsLegacyData() {
        val raw = """{"encryptedData":"ciphertext","iv":"nonce"}"""

        assertFailsWith<IllegalStateException> {
            decodeAcademicCache(raw) { _, _ -> null }
        }
    }
}
