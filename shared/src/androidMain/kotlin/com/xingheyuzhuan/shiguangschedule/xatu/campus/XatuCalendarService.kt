package com.xingheyuzhuan.shiguangschedule.xatu.campus

import android.graphics.BitmapFactory
import android.content.Context
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCalendar
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCalendarSemester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.koin.mp.KoinPlatform
import java.io.File
import java.time.LocalDate

class XatuCalendarService(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun latest(): XatuCalendar = withContext(Dispatchers.IO) {
        val index = get(INDEX, INDEX)
        val page = Jsoup.parse(index, INDEX).select("a[href]")
            .first { "校历" in it.text() && it.absUrl("href") != INDEX }
            .absUrl("href")
        val html = get(page, page)
        val document = Jsoup.parse(html, page)
        val image = document.select("img[src]").firstOrNull { "__local" in it.attr("src") }
            ?: document.select("img[src]").firstOrNull { it.hasAttr("orisrc") }
            ?: document.select("img[src]").first { Regex("(?i)\\.(jpg|jpeg|png)(?:\\?|$)").containsMatchIn(it.attr("src")) }
        val imageUrl = image.absUrl(if (image.hasAttr("orisrc")) "orisrc" else "src")
        val imageBytes = getBytes(imageUrl, page)
        val imageFile = File(KoinPlatform.getKoin().get<Context>().filesDir, "xatu-calendar.jpg")
        imageFile.writeBytes(imageBytes)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val rawText = bitmap?.let { XatuCalendarOcr.read(it).also { _ -> it.recycle() } }.orEmpty()
        val spans = semesters(document.text()).ifEmpty { semesters(rawText) }
        XatuCalendar(
            title = document.title().substringBefore("-西安工业"),
            pageUrl = page,
            imageUrl = imageUrl,
            semesters = spans,
            fetchedAt = System.currentTimeMillis(),
            imagePath = imageFile.absolutePath,
            rawText = rawText
        )
    }

    private fun getBytes(url: String, referer: String): ByteArray {
        val request = Request.Builder().url(url).header("Referer", referer).header("User-Agent", USER_AGENT).build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("教务处校历图片返回 HTTP ${response.code}")
            response.body.bytes()
        }
    }

    private fun get(url: String, referer: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Referer", referer)
            .header("User-Agent", USER_AGENT)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("教务处校历返回 HTTP ${response.code}")
            response.body.string()
        }
    }

    private fun semesters(text: String): List<XatuCalendarSemester> {
        val compact = text.replace(Regex("\\s+"), "")
        val regex = Regex("""(\d{4})年(\d{1,2})月(\d{1,2})日[-－—–~～至到]+(?:(\d{4})年)?(\d{1,2})月(\d{1,2})日""")
        return regex.findAll(compact).mapNotNull { match ->
            val values = match.groupValues
            val start = runCatching { LocalDate.of(values[1].toInt(), values[2].toInt(), values[3].toInt()) }.getOrNull()
                ?: return@mapNotNull null
            val endYear = values[4].toIntOrNull() ?: (start.year + if (values[5].toInt() < start.monthValue) 1 else 0)
            val end = runCatching { LocalDate.of(endYear, values[5].toInt(), values[6].toInt()) }.getOrNull()
                ?: return@mapNotNull null
            if (end.toEpochDay() - start.toEpochDay() !in 30..260) return@mapNotNull null
            start to end
        }.distinctBy { it.first to it.second }.take(4).mapIndexed { index, (start, end) ->
            XatuCalendarSemester("第${index + 1}学期", start.toString(), end.toString())
        }.toList()
    }

    private companion object {
        const val INDEX = "https://jwcw.xatu.edu.cn/index/xagydxxl.htm"
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/130 Mobile Safari/537.36"
    }
}
