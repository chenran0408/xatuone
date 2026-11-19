// Adapted from XATUHelper-Android (Kelejun, MIT); license bundled in assets/licenses.
package com.xingheyuzhuan.shiguangschedule.xatu.campus

import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCalendarSemester
import org.jsoup.Jsoup
import java.time.LocalDate

object XatuCalendarParser {
    const val INDEX = "https://jwcw.xatu.edu.cn/index/xagydxxl.htm"

    fun latest(html: String): String = Jsoup.parse(html, INDEX).select("a[href]")
        .firstOrNull { "校历" in it.text() && ("/info/" in it.absUrl("href") || "/news/content.jsp" in it.absUrl("href")) }
        ?.absUrl("href") ?: error("校历列表未找到正文，请确认校园网访问权限")

    fun image(html: String, page: String): String {
        val images = Jsoup.parse(html, page).select("img[src]")
        val image = images.firstOrNull { "__local" in it.attr("src") }
            ?: images.firstOrNull { it.hasAttr("orisrc") }
            ?: images.firstOrNull { node ->
                Regex("(?i)\\.(jpg|jpeg|png)(?:\\?|$)").containsMatchIn(node.attr("src")) &&
                    listOf("logo", "nav-", "bottom-icon").none { it in node.attr("src") }
            }
        return image?.absUrl(if (image.hasAttr("orisrc")) "orisrc" else "src")
            ?.takeIf { it.isNotBlank() } ?: error("校历正文未找到原图，请查看来源网页")
    }

    fun semesters(text: String): List<XatuCalendarSemester> {
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
}
