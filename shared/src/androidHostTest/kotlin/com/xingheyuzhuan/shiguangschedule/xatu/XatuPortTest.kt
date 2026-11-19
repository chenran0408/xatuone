package com.xingheyuzhuan.shiguangschedule.xatu

import com.xingheyuzhuan.shiguangschedule.data.repository.XatuBalanceRecord
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuAcademicParser
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuAcademicService
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuExam
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuParseFailure
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuSessionExpired
import com.xingheyuzhuan.shiguangschedule.xatu.academic.startDateTime
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuCalendarParser
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuCoordinate
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuLocation
import com.xingheyuzhuan.shiguangschedule.xatu.campus.summarizeXatuElectricity
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XatuPortTest {
    @Test
    fun expiredEamsRequestUsesCanonicalCasOnceThenRetriesTheProtectedPage() = runBlocking {
        val requests = mutableListOf<String>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests += request.url.toString()
            when (request.url.host) {
                "jwgl2018.xatu.edu.cn" -> {
                    if (requests.size == 1) {
                        response(request, "https://authserver.xatu.edu.cn/authserver/login", "<p>login</p>")
                    } else response(request, request.url.toString(), "<p>当前教学周 8</p>")
                }
                "authserver.xatu.edu.cn" ->
                    response(request, "https://jwgl2018.xatu.edu.cn/eams/homeExt.action", "<p>CAS returned</p>")
                else -> error("Unexpected host")
            }
        }.build()

        assertEquals(8, XatuAcademicService(client).officialWeek())

        assertEquals(3, requests.size)
        assertEquals(requests.first(), requests.last())
        assertEquals(
            "https://authserver.xatu.edu.cn/authserver/login?service=http://jwgl2018.xatu.edu.cn/eams/homeExt.action",
            requests[1]
        )
    }

    @Test
    fun casPasswordFormKeepsSessionExpiredAndDoesNotRetryProtectedPage() = runBlocking {
        val requests = mutableListOf<String>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests += request.url.toString()
            val body = if (request.url.host == "jwgl2018.xatu.edu.cn") {
                response(request, "https://authserver.xatu.edu.cn/authserver/login", "<p>login</p>")
            } else {
                response(request, request.url.toString(), "<form><input name='password' type='password'></form>")
            }
            body
        }.build()

        assertFailsWith<XatuSessionExpired> { XatuAcademicService(client).officialWeek() }
        assertEquals(2, requests.size)
    }

    @Test
    fun networkAndHttpFailuresDoNotTriggerCasFallback() = runBlocking {
        for (httpFailure in listOf(false, true)) {
            val requests = mutableListOf<String>()
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                val request = chain.request()
                requests += request.url.toString()
                if (httpFailure) response(request, request.url.toString(), "failed", 503)
                else throw IOException("DNS lookup failed")
            }.build()
            if (httpFailure) {
                val error = assertFailsWith<IllegalStateException> { XatuAcademicService(client).officialWeek() }
                assertEquals("教务系统返回 HTTP 503", error.message)
            } else {
                assertFailsWith<IOException> { XatuAcademicService(client).officialWeek() }
            }
            assertEquals(1, requests.size)
        }
    }

    @Test
    fun authserverLinkAloneDoesNotMarkAnEamsResponseExpired() = runBlocking {
        var requests = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests++
            val request = chain.request()
            response(request, request.url.toString(), "<p>当前教学周 8</p><a href='https://authserver.xatu.edu.cn/help'>help</a>")
        }.build()
        assertEquals(8, XatuAcademicService(client).officialWeek())
        assertEquals(1, requests)
    }

    @Test
    fun cookieRequestsUseTheBrowserStoreForEachUrlAndKeepEqualsInValues() {
        val requests = mutableListOf<String>()
        val jar = XatuCookieJar(read = { url ->
            requests += url
            if (url.startsWith("https://authserver.xatu.edu.cn/")) "ticket=abc==; session=123" else null
        }, write = { _, _ -> }, flush = {})
        val auth = "https://authserver.xatu.edu.cn/authserver/login".toHttpUrl()
        assertEquals(listOf("abc==", "123"), jar.loadForRequest(auth).map { it.value })
        val other = "https://ehall.xatu.edu.cn/".toHttpUrl()
        assertTrue(jar.loadForRequest(other).isEmpty())
        assertEquals(listOf(auth.toString(), other.toString()), requests)
    }

    @Test
    fun responseCookiesKeepExpiryPathAndSecurityAttributesWhenWrittenBack() {
        val url = "https://authserver.xatu.edu.cn/authserver/login".toHttpUrl()
        val cookie = Cookie.parse(url, "session=new; Path=/authserver; Max-Age=3600; Secure; HttpOnly")!!
        val writes = mutableListOf<Pair<String, String>>()
        var flushes = 0
        val jar = XatuCookieJar({ null }, { target, value -> writes += target to value }, { flushes++ })
        jar.saveFromResponse(url, listOf(cookie))
        assertEquals(listOf(url.toString() to cookie.toString()), writes)
        assertEquals(1, flushes)
        jar.saveFromResponse(url, emptyList())
        assertEquals(1, flushes)
    }

    @Test
    fun unrecognizedResponsesAreFailuresRatherThanSuccessfulEmptyData() {
        val html = "<html><h1>访问受限</h1><p>请连接校园网</p></html>"
        assertFailsWith<XatuParseFailure> { XatuAcademicParser.grades(html) }
        assertFailsWith<XatuParseFailure> { XatuAcademicParser.exams(html) }
        assertTrue(XatuAcademicParser.grades("<p>暂无成绩</p>").grades.isEmpty())
        assertTrue(XatuAcademicParser.exams("<p>没有考试安排</p>").isEmpty())
    }

    @Test
    fun anEmptyProbeCannotHideFailureOfARealExamBatch() = runBlocking {
        fun service(probe: String, actual: String): XatuAcademicService {
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                val request = chain.request()
                val html = when {
                    request.url.encodedPath.endsWith("stdExamTable.action") ->
                        "<select name='examBatch.id'><option value='12'>期末</option></select>"
                    request.url.queryParameter("examBatch.id") == "0" -> probe
                    else -> actual
                }
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK").body(html.toResponseBody()).build()
            }.build()
            return XatuAcademicService(client)
        }
        val empty = "<p>没有考试安排</p>"
        val invalid = "<h1>访问受限</h1>"
        assertFailsWith<XatuParseFailure> { service(empty, invalid).exams("102") }
        // An unsupported probe is not an error when a real batch explicitly has no exams.
        assertTrue(service(invalid, empty).exams("102").isEmpty())
    }

    @Test
    fun dailyBalancesUseBeijingDatesAndTheLatestObservationInsteadOfInputOrder() {
        val history = listOf(
            record("2026-11-01T16:01:00Z", "18"), // Nov 2 in UTC+8
            record("2026-11-01T15:59:00Z", "20"),
            record("2026-11-01T10:00:00Z", "22")
        )
        val days = summarizeXatuElectricity(history)
        assertEquals(listOf("2026-11-01", "2026-11-02"), days.map { it.day.toString() })
        assertEquals(listOf("20", "18"), days.map { it.balance })
        assertEquals("2", days.last().decrease)
    }

    @Test
    fun missedDaysAndRechargesDoNotBecomeDailyUsage() {
        val days = summarizeXatuElectricity(listOf(
            record("2026-11-01T02:00:00Z", "20"),
            record("2026-11-03T02:00:00Z", "16"),
            record("2026-11-04T02:00:00Z", "50"),
            record("2026-11-05T02:00:00Z", "48.5")
        ))
        assertEquals(listOf(null, null, null, "1.5"), days.map { it.decrease })
        assertTrue(summarizeXatuElectricity(emptyList()).isEmpty())
    }

    @Test
    fun savingDeviceCoordinatesPreservesWgs84WithoutApplyingMapConversion() {
        val saved = XatuLocation.checkinLocation("34.2658", "108.9535", true, "WGS84")
        val converted = XatuLocation.toWgs84(XatuCoordinate(saved.latitude, saved.longitude, saved.system))
        assertEquals(saved.latitude, converted.latitude)
        assertEquals(saved.longitude, converted.longitude)
        assertEquals("WGS84", saved.system)
    }

    @Test
    fun examsSortOnlyWithACompleteValidServerDate() {
        fun exam(time: String) = XatuExam("id", "课程", "code", "期末", time, "教室", "12", "")
        assertEquals(LocalDateTime.of(2026, 12, 3, 9, 0), exam("2026-12-03 09:00-11:00").startDateTime())
        assertEquals(LocalDateTime.of(2026, 12, 3, 9, 0), exam("2026年12月3日（星期四）09：00-11：00").startDateTime())
        assertNull(exam("12-03 09:00-11:00").startDateTime())
        assertNull(exam("2026-02-30 09:00").startDateTime())
    }

    @Test
    fun calendarSelectsTheArticleAndOriginalImageRatherThanNavigationAssets() {
        val page = XatuCalendarParser.latest("""
            <a href="/index/xagydxxl.htm">校历</a>
            <a href="/info/1001/2026.htm">2026-2027学年校历</a>
        """)
        assertEquals("https://jwcw.xatu.edu.cn/info/1001/2026.htm", page)
        assertEquals("https://jwcw.xatu.edu.cn/__local/calendar.png", XatuCalendarParser.image("""
            <img src="/images/logo.png"><img src="/images/nav-calendar.jpg">
            <img src="/__local/calendar.png">
        """, page))
        assertFailsWith<IllegalStateException> { XatuCalendarParser.latest("<h1>访问受限</h1>") }
    }

    @Test
    fun calendarInfersTheEndYearOnlyForAnExplicitCrossYearDateRange() {
        val semesters = XatuCalendarParser.semesters("2026年8月31日至1月24日；2027年3月1日至2027年7月18日")
        assertEquals(listOf("2026-08-31", "2027-03-01"), semesters.map { it.start })
        assertEquals(listOf("2027-01-24", "2027-07-18"), semesters.map { it.end })
        assertTrue(XatuCalendarParser.semesters("元旦假期2027年1月1日至1月3日").isEmpty())
    }

    private fun record(instant: String, balance: String) = XatuBalanceRecord(Instant.parse(instant).toEpochMilli(), balance)

    private fun response(request: okhttp3.Request, finalUrl: String, body: String, code: Int = 200) =
        Response.Builder().request(request.newBuilder().url(finalUrl).build()).protocol(Protocol.HTTP_1_1)
            .code(code).message(if (code == 200) "OK" else "Failure").body(body.toResponseBody()).build()
}
