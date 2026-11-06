package com.xingheyuzhuan.shiguangschedule.xatu.academic

import kotlinx.serialization.Serializable
import org.jsoup.Jsoup

@Serializable
data class XatuExam(
    val id: String,
    val course: String,
    val code: String,
    val name: String,
    val time: String,
    val room: String,
    val seat: String,
    val note: String
)

@Serializable
data class XatuGradePart(val name: String, val score: String)
@Serializable
data class XatuGrade(
    val id: String,
    val course: String,
    val code: String,
    val credit: String,
    val score: String,
    val gpa: String,
    val term: String,
    val parts: List<XatuGradePart>
)

@Serializable
data class XatuGradeSummary(val count: Int? = null, val credits: Double? = null, val gpa: Double? = null)
@Serializable
data class XatuGradePage(val grades: List<XatuGrade>, val summary: XatuGradeSummary)

@Serializable
data class XatuPlanCourse(
    val id: String,
    val code: String,
    val name: String,
    val requiredCredits: Double,
    val earnedCredits: Double,
    val score: String,
    val passed: Boolean
)

@Serializable
data class XatuPlanSection(
    val id: String,
    val title: String,
    val requiredCredits: Double,
    val earnedCredits: Double,
    val courses: List<XatuPlanCourse> = emptyList(),
    val children: List<XatuPlanSection> = emptyList()
)

@Serializable
data class XatuStudyPlan(
    val studentName: String,
    val major: String,
    val gpa: String,
    val requiredCredits: Double,
    val earnedCredits: Double,
    val sections: List<XatuPlanSection>
)

@Serializable
data class XatuCourseInfo(
    val code: String,
    val name: String,
    val nature: String,
    val credits: String,
    val hours: String,
    val term: String
)

@Serializable
data class XatuSemester(val id: String, val name: String, val startDate: String? = null)
@Serializable
data class XatuAcademicSnapshot(
    val account: String,
    val semesterId: String,
    val grades: XatuGradePage? = null,
    val exams: List<XatuExam> = emptyList(),
    val plan: XatuStudyPlan? = null,
    val catalog: List<XatuCourseInfo> = emptyList(),
    val fetchedAt: Long = System.currentTimeMillis()
)

@Serializable
internal data class XatuAcademicCacheFile(
    val semesters: List<XatuSemester> = emptyList(),
    val snapshots: List<XatuAcademicSnapshot> = emptyList()
)

object XatuAcademicParser {
    fun semesters(html: String): List<XatuSemester> {
        val records = Regex("""\{[^{}]*\}""").findAll(html).mapNotNull { match ->
            fun field(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
                Regex("""[\"']?${Regex.escape(key)}[\"']?\s*:\s*(?:[\"']([^\"']*)[\"']|(\d+))""")
                    .find(match.value)?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } }
            }
            val id = field("id") ?: return@mapNotNull null
            val year = field("schoolYear", "school_year", "academicYear", "year").orEmpty()
            val name = field("name", "termName", "term", "title").orEmpty()
            if (year.isEmpty() && name.isEmpty()) return@mapNotNull null
            XatuSemester(id, if (year.isEmpty()) name else "${year}学年$name", field("startDate", "start_date", "beginDate", "start"))
        }.toList()
        val fallback = Jsoup.parse(html).select("[val], option[value]").mapNotNull {
            val id = it.attr("val").ifEmpty { it.attr("value") }
            if (id.toLongOrNull() != null && it.text().isNotBlank()) XatuSemester(id, it.text().trim()) else null
        }
        return (records.ifEmpty { fallback }).distinctBy { it.id }.sortedByDescending { it.id.toLongOrNull() ?: 0 }
    }

    fun exams(html: String): List<XatuExam> {
        val table = XatuHtmlTable.find(html) { h ->
            h.any { "课程名称" in it || "科目" in it } && h.any { "时间" in it || "日期" in it || "考场" in it }
        } ?: return emptyList()
        fun col(vararg words: String) = table.header.indexOfFirst { words.any { word -> word in it } }
        val courseCol = col("课程名称", "科目")
        val timeCol = col("考试时间", "时间", "日期")
        val dateCol = col("日期")
        return table.rows.mapNotNull { row ->
            fun at(index: Int) = row.getOrNull(index).orEmpty()
            val course = at(courseCol)
            if (course.isBlank()) return@mapNotNull null
            var time = at(timeCol)
            val date = at(dateCol)
            if (date.isNotBlank() && date != time && !time.contains(date)) time = "$date $time".trim()
            val room = at(col("考场", "教室", "地点"))
            if (time.isBlank() && room.isBlank()) return@mapNotNull null
            val code = at(col("课程序号", "课程代码", "课程号"))
            val name = at(col("考试名称"))
            XatuExam("$code|$course|$name|$time|$room", course, code, name, time, room, at(col("座位")), at(col("备注", "说明")))
        }.distinctBy { it.id }
    }

    fun grades(html: String): XatuGradePage {
        val summary = summary(html)
        val table = XatuHtmlTable.find(html) { h ->
            h.any { "课程名称" in it || "科目" in it } && (h.any { "成绩" in it || "绩点" in it } || (h.any { it.trim() == "学期" || it.trim() == "学年学期" } && h.any { "学分" in it }))
        } ?: return XatuGradePage(emptyList(), summary)
        fun col(vararg words: String) = table.header.indexOfFirst { words.any { word -> word in it } }
        val scoreCol = col("总评", "最终").takeIf { it >= 0 } ?: table.header.indexOfFirst { it == "成绩" || it == "总成绩" }
        val creditCol = table.header.indexOfFirst { "学分" in it && "绩点" !in it }
        val repeated = mutableMapOf<String, Int>()
        return XatuGradePage(table.rows.mapNotNull { row ->
            fun at(index: Int) = row.getOrNull(index).orEmpty()
            val course = at(col("课程名称", "科目"))
            if (course.isBlank()) return@mapNotNull null
            val code = at(col("课程序号", "课程代码", "课程号"))
            val term = at(col("学年学期", "学期", "学年"))
            val parts = table.header.mapIndexedNotNull { i, label -> if (i != scoreCol && ("成绩" in label || label == "其他") && at(i).isNotBlank()) XatuGradePart(label, at(i)) else null }
            val identity = "$term|$code|$course"
            val occurrence = repeated.getOrDefault(identity, 0)
            repeated[identity] = occurrence + 1
            XatuGrade(if (occurrence == 0) identity else "$identity|$occurrence", course, code, at(creditCol), at(scoreCol), at(col("绩点")), term, parts)
        }.distinctBy { it.id }, summary)
    }

    fun summary(html: String): XatuGradeSummary {
        val text = org.jsoup.Jsoup.parse(html).text()
        fun number(vararg labels: String): Double? = labels.firstNotNullOfOrNull { label ->
            Regex("${Regex.escape(label)}[:：\\s]*([0-9]+(?:\\.[0-9]+)?)").find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        }
        return XatuGradeSummary(number("课程门数", "课程数", "门数")?.toInt(), number("总学分", "合计学分", "学分合计", "已修学分"), number("平均学分绩点", "平均绩点", "平均GPA"))
    }

    private data class SectionBuilder(
        val id: String,
        val title: String,
        val required: Double,
        val earned: Double,
        val courses: MutableList<XatuPlanCourse> = mutableListOf(),
        val children: MutableList<SectionBuilder> = mutableListOf()
    ) {
        fun freeze(): XatuPlanSection = XatuPlanSection(id, title, required, earned, courses.toList(), children.map { it.freeze() })
    }

    fun plan(html: String): XatuStudyPlan {
        val doc = org.jsoup.Jsoup.parse(html)
        val text = doc.text()
        val roots = mutableListOf<SectionBuilder>()
        var top: SectionBuilder? = null
        var child: SectionBuilder? = null
        var grandchild: SectionBuilder? = null
        var serial = 0
        for (row in doc.select("tr")) {
            val values = row.children().filter { it.tagName() == "td" || it.tagName() == "th" }
                .map { it.text().replace('\u00a0', ' ').trim() }
            if (values.isEmpty()) continue
            if (row.hasClass("darkColumn")) {
                val title = values[0].replace(Regex("""(?<=\S)\s*[（(]所有.*$"""), "").trim()
                if (title.isBlank()) continue
                val numbers = values.drop(1).mapNotNull(String::toDoubleOrNull)
                val section = SectionBuilder("section-${serial++}", title, numbers.getOrElse(0) { 0.0 }, numbers.getOrElse(1) { 0.0 })
                when {
                    Regex("""^\d+[ 　]""").containsMatchIn(title) && child != null -> { child.children.add(section); grandchild = section }
                    Regex("""^[（(][一二三四五六七八九十]+[)）]""").containsMatchIn(title) && top != null -> { top.children.add(section); child = section; grandchild = null }
                    else -> { roots.add(section); top = section; child = null; grandchild = null }
                }
            } else if (values.size >= 7 && values[0].toIntOrNull() != null && values[2].isNotBlank()) {
                (grandchild ?: child ?: top)?.courses?.add(
                    XatuPlanCourse("${(grandchild ?: child ?: top)!!.id}|${values[1]}|${values[0]}", values[1], values[2], values[3].toDoubleOrNull() ?: 0.0, values[4].toDoubleOrNull() ?: 0.0, values[5], values[6].contains("是") || values[6] == "通过")
                )
            }
        }
        check(roots.isNotEmpty()) { "没有识别到学业完成情况" }
        val totals = Regex("""要求学分/实修学分\s*[:：]\s*([\d.]+)\s*/\s*([\d.]+)""").find(text)
        fun field(pattern: String) = Regex(pattern).find(text)?.groupValues?.get(1).orEmpty()
        return XatuStudyPlan(
            field("""姓名\s*[:：]\s*(\S+)"""),
            field("""专业/专业方向\s*[:：]\s*(\S+)"""),
            field("""GPA\s*[:：]\s*([\d.]+)"""),
            totals?.groupValues?.get(1)?.toDoubleOrNull() ?: roots.sumOf { it.required },
            totals?.groupValues?.get(2)?.toDoubleOrNull() ?: roots.sumOf { it.earned },
            roots.map { it.freeze() }
        )
    }

    fun catalog(html: String): List<XatuCourseInfo> {
        val result = org.jsoup.Jsoup.parse(html).select("tr").mapNotNull { row ->
            val cells = row.children().filter { it.tagName() == "td" || it.tagName() == "th" }
            val index = cells.indexOfFirst { it.hasClass("course") }
            if (index < 0) return@mapNotNull null
            fun at(offset: Int) = cells.getOrNull(index + offset)?.text()?.trim().orEmpty()
            if (at(1).toDoubleOrNull() == null) return@mapNotNull null
            XatuCourseInfo(at(-1), at(0).replace(Regex("""^\d+\s+"""), ""), when { "是" in at(10) -> "必修"; "否" in at(10) -> "选修"; else -> "" }, at(1), at(2), at(8))
        }.distinctBy { it.code.ifBlank { it.name } }
        check(result.isNotEmpty()) { "没有识别到教学计划课程" }
        return result
    }
}

internal data class XatuHtmlTable(val header: List<String>, val rows: List<List<String>>) {
    companion object {
        fun find(html: String, matches: (List<String>) -> Boolean): XatuHtmlTable? {
            for (table in org.jsoup.Jsoup.parse(html).select("table")) {
                val rows = table.select("tr").filter { it.closest("table") == table }
                val index = rows.indexOfFirst { row -> row.children().map { it.text().trim() }.let(matches) }
                if (index >= 0) {
                    val header = rows[index].children().map { it.text().trim() }
                    return XatuHtmlTable(header, rows.drop(index + 1).map { it.children().map { cell -> cell.text().replace('\u00a0', ' ').trim() } })
                }
            }
            return null
        }
    }
}
