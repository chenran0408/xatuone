package com.xingheyuzhuan.shiguangschedule.xatu.academic

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

@Single
class XatuAcademicService(
    private val cookieManager: CookieManager,
    private val client: OkHttpClient
) {
    private val base = "http://jwgl2018.xatu.edu.cn/eams/"

    private suspend fun page(path: String, query: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val url = (base + path).toHttpUrl().newBuilder().apply {
            query.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()
        val request = Request.Builder()
            .url(url)
            .header("Cookie", cookieManager.getCookie(url.toString()).orEmpty())
            .header("User-Agent", "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/130 Mobile Safari/537.36")
            .build()
        execute(request).use { response ->
            val html = response.body.string()
            if (!response.isSuccessful) error("教务系统返回 HTTP ${response.code}")
            if (html.contains("name=\"password\"") || html.contains("authserver")) throw XatuSessionExpired()
            html
        }
    }

    private suspend fun execute(request: Request): Response = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: java.io.IOException) {
                if (continuation.isActive) continuation.resumeWithException(error)
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
            .filter { it.toLongOrNull() != null }
        for (batch in (listOf("0") + batches).distinct()) {
            val exams = XatuAcademicParser.exams(
                page("stdExamTable!examTable.action", mapOf("examBatch.id" to batch, "semester.id" to semesterId))
            )
            if (exams.isNotEmpty()) return exams
        }
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
