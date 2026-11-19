package com.xingheyuzhuan.shiguangschedule.xatu.academic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import okhttp3.OkHttpClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.koin.core.annotation.Single

class XatuSessionExpired : IllegalStateException("教务登录已失效，请重新登录")

internal const val XATU_CAS_LOGIN_URL =
    "https://authserver.xatu.edu.cn/authserver/login?service=http://jwgl2018.xatu.edu.cn/eams/homeExt.action"
private const val XATU_USER_AGENT = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/130 Mobile Safari/537.36"

@Single
class XatuAcademicService(
    client: OkHttpClient
) {
    private val client = client.newBuilder().addNetworkInterceptor { chain ->
        upgradeXatuAuthRedirect(chain.proceed(chain.request()))
    }.build()
    private val base = "http://jwgl2018.xatu.edu.cn/eams/"
    private val casMutex = Mutex()
    private val sessionGeneration = AtomicLong()

    private data class Page(val html: String, val url: okhttp3.HttpUrl)

    private suspend fun requestPage(request: Request): Page = withContext(Dispatchers.IO) {
        execute(request).use { response ->
            val html = response.body.string()
            if (!response.isSuccessful) error("教务系统返回 HTTP ${response.code}")
            if (isCasLogin(response.request.url, html)) throw XatuSessionExpired()
            Page(html, response.request.url)
        }
    }

    private suspend fun page(path: String, query: Map<String, String> = emptyMap()): String {
        val url = (base + path).toHttpUrl().newBuilder().apply {
            query.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()
        val request = Request.Builder().url(url).header("User-Agent", XATU_USER_AGENT).build()
        val generation = sessionGeneration.get()
        val result = try {
            requestPage(request)
        } catch (expired: XatuSessionExpired) {
            recoverSession(generation)
            requestPage(request)
        }
        check(isEamsDestination(result.url)) { "教务请求未返回教务系统页面" }
        return result.html
    }

    private suspend fun recoverSession(observedGeneration: Long) {
        casMutex.withLock {
            if (sessionGeneration.get() != observedGeneration) return
            val request = Request.Builder()
                .url(XATU_CAS_LOGIN_URL)
                .header("User-Agent", XATU_USER_AGENT)
                .build()
            val page = requestPage(request)
            check(isEamsDestination(page.url)) { "CAS 未返回教务系统页面" }
            sessionGeneration.incrementAndGet()
        }
    }

    private fun isCasLogin(url: okhttp3.HttpUrl, html: String): Boolean =
        url.host == "authserver.xatu.edu.cn" ||
            (html.contains("password", ignoreCase = true) &&
                Jsoup.parse(html).selectFirst("form input[type=password], form input[name=password]") != null)

    private fun isEamsDestination(url: okhttp3.HttpUrl): Boolean =
        url.host == "jwgl2018.xatu.edu.cn" && url.encodedPath.startsWith("/eams/")

    private suspend fun execute(request: Request): Response = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) continuation.resume(response)
                else response.close()
            }
        })
    }

    suspend fun grades(semesterId: String): XatuGradePage {
        val shell = page("teach/grade/course/person.action", mapOf("semester.id" to semesterId))
        val resolved = Regex("semesterCalendar\\s*\\(\\s*\\{[^}]*?value\\s*:\\s*[\"'](\\d+)").find(shell)?.groupValues?.get(1) ?: semesterId
        return XatuAcademicParser.grades(page("teach/grade/course/person!search.action", mapOf("semesterId" to resolved, "projectType" to "", "looked" to "yes")))
    }

    suspend fun allGrades(): XatuGradePage = XatuAcademicParser.grades(
        page("teach/grade/course/person!historyCourseGrade.action", mapOf("projectType" to "MAJOR"))
    )

    suspend fun plan(): XatuStudyPlan = XatuAcademicParser.plan(page("myPlanCompl.action"))

    suspend fun catalog(): List<XatuCourseInfo> = XatuAcademicParser.catalog(page("myPlan.action"))

    suspend fun exams(semesterId: String): List<XatuExam> {
        val batches = Jsoup.parse(page("stdExamTable.action", mapOf("semester.id" to semesterId)))
            .select("select[name=examBatch.id] option[value]")
            .map { it.attr("value") }
            .filter { it != "0" && it.toLongOrNull() != null }
        var failure: XatuParseFailure? = null
        for (batch in (listOf("0") + batches).distinct()) {
            try {
                val exams = XatuAcademicParser.exams(
                    page("stdExamTable!examTable.action", mapOf("examBatch.id" to batch, "semester.id" to semesterId))
                )
                if (exams.isNotEmpty()) return exams
            } catch (error: XatuParseFailure) {
                // Batch 0 is a probe; a failed real batch must not erase cached exams.
                if (batch != "0" || batches.isEmpty()) failure = error
            }
        }
        failure?.let { throw it }
        return emptyList()
    }

    suspend fun semesters(): List<XatuSemester> {
        val html = page("dataQuery.action", mapOf("tagId" to "", "dataType" to "semesterCalendar"))
        return XatuAcademicParser.semesters(html)
    }

    suspend fun officialWeek(): Int? {
        val html = page("homeExt!main.action")
        val patterns = listOf(
            """<span>\s*(\d{1,2})\s*</span>\s*<i>\s*/\s*\d+\s*</i>[\s\S]{0,80}?当前教学周""",
            """当前教学周[\s\S]{0,80}?<span>\s*(\d{1,2})\s*</span>""",
            """当前教学周\s*[:：]?\s*(\d{1,2})""",
            """第\s*(\d{1,2})\s*周"""
        )
        return patterns.firstNotNullOfOrNull { Regex(it).find(html)?.groupValues?.get(1)?.toIntOrNull() }
    }
}

// EAMS redirects expired sessions to HTTP CAS, which itself redirects to HTTPS.
// Upgrade that verified host before OkHttp follows it; keep Android's HTTP policy strict.
internal fun upgradeXatuAuthRedirect(response: Response): Response {
    val location = response.header("Location")?.let { response.request.url.resolve(it) }
    if (location?.host != "authserver.xatu.edu.cn" || location.scheme != "http") return response
    return response.newBuilder()
        .header("Location", location.newBuilder().scheme("https").build().toString())
        .build()
}
