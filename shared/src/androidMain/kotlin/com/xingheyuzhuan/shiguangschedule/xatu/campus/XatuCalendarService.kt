package com.xingheyuzhuan.shiguangschedule.xatu.campus

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AtomicFile
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCalendar
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.File
import java.security.MessageDigest
import org.koin.core.annotation.Single

@Single
class XatuCalendarService(
    private val client: OkHttpClient,
    private val context: Context,
    private val campus: XatuCampusRepository
) {
    private val imageLock = Mutex()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        imageLock.withLock {
            val latest = fetchLatest()
            campus.saveCalendar(latest)
            pruneCalendarImages(context.filesDir, context.cacheDir, File(latest.imagePath))
        }
    }

    private suspend fun fetchLatest(): XatuCalendar {
        val page = XatuCalendarParser.latest(get(XatuCalendarParser.INDEX, XatuCalendarParser.INDEX))
        val html = get(page, page)
        val document = Jsoup.parse(html, page)
        val imageUrl = XatuCalendarParser.image(html, page)
        val imageBytes = getBytes(imageUrl, page)
        val bounds = imageBounds(imageBytes)
        var sample = 1
        while (bounds.outWidth / sample > 4096 || bounds.outHeight / sample > 4096 ||
            bounds.outWidth.toLong() * bounds.outHeight / sample / sample > 12_000_000) sample *= 2
        val bitmap = checkNotNull(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample })) { "无法打开校历原图" }
        val rawText = try { XatuCalendarOcr.read(bitmap) } finally { bitmap.recycle() }
        val spans = XatuCalendarParser.semesters(document.text()).ifEmpty { XatuCalendarParser.semesters(rawText) }
        val imageFile = File(context.filesDir, "xatu-calendar-${digest(imageBytes)}.image")
        if (!imageFile.isFile) saveImage(imageFile, imageBytes)
        return XatuCalendar(
            title = document.title().substringBefore("-西安工业"),
            pageUrl = page,
            imageUrl = imageUrl,
            semesters = spans,
            fetchedAt = System.currentTimeMillis(),
            imagePath = imageFile.absolutePath,
            rawText = rawText
        )
    }

    suspend fun originalImage(calendar: XatuCalendar): String = withContext(Dispatchers.IO) {
        imageLock.withLock { resolveOriginal(calendar) }
    }

    private fun resolveOriginal(calendar: XatuCalendar): String {
        if (calendar.imagePath.isNotBlank()) {
            val saved = File(calendar.imagePath)
            if (saved.isFile) return saved.absolutePath
        }

        check(calendar.imageUrl.isNotBlank()) { "校历未提供原图地址" }
        val source = "${calendar.pageUrl}\n${calendar.imageUrl}\n${calendar.fetchedAt}"
        val recovered = File(context.cacheDir, "xatu-calendar-original-${digest(source.encodeToByteArray())}.image")
        if (!recovered.isFile) {
            val imageBytes = getBytes(calendar.imageUrl, calendar.pageUrl)
            imageBounds(imageBytes)
            saveImage(recovered, imageBytes)
        }
        return recovered.absolutePath
    }

    private fun imageBounds(imageBytes: ByteArray): BitmapFactory.Options {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, bounds)
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "校历原图格式异常" }
        return bounds
    }

    private fun saveImage(file: File, imageBytes: ByteArray) {
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            stream.write(imageBytes)
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
    }

    private fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    private fun getBytes(url: String, referer: String): ByteArray {
        val request = Request.Builder().url(url).header("Referer", referer).header("User-Agent", USER_AGENT).build()
        return client.newCall(request).execute().use { response ->
            response.requireCalendarAccess()
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
            response.requireCalendarAccess()
            if (!response.isSuccessful) error("教务处校历返回 HTTP ${response.code}")
            response.body.string()
        }
    }

    private fun Response.requireCalendarAccess() {
        if (code == 403 || request.url.encodedPath == "/system/resource/code/auth/auth.htm") {
            error("校历访问受限，请连接校园网后重试")
        }
    }

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/130 Mobile Safari/537.36"
    }
}

internal fun pruneCalendarImages(filesDir: File, cacheDir: File, current: File) {
    val original = Regex("xatu-calendar-[0-9a-f]{64}\\.image")
    val recovered = Regex("xatu-calendar-original-[0-9a-f]{64}\\.image")
    filesDir.listFiles { file -> file != current && file.isFile && original.matches(file.name) }
        ?.forEach { it.delete() }
    cacheDir.listFiles { file -> file.isFile && recovered.matches(file.name) }
        ?.forEach { it.delete() }
}
