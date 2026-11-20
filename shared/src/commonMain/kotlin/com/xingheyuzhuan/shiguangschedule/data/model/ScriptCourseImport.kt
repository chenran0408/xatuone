package com.xingheyuzhuan.shiguangschedule.data.model

import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.serialization.Serializable

/** The JS bridge accepts partial config; file imports continue to use their full config model. */
@Serializable
internal data class ScriptCourseConfig(
    val semesterStartDate: String? = null,
    val semesterTotalWeeks: Int? = null,
    val firstDayOfWeek: Int? = null,
    val defaultClassDuration: Int? = null,
    val defaultBreakDuration: Int? = null,
) {
    fun withSavedDates(saved: CourseTableConfig?, lastCourseWeek: Int) = copy(
        semesterStartDate = semesterStartDate ?: saved?.semesterStartDate,
        semesterTotalWeeks = semesterTotalWeeks ?: saved?.semesterTotalWeeks?.takeIf { it >= lastCourseWeek },
    )

    fun applyTo(previous: CourseTableConfig) = previous.copy(
        semesterStartDate = semesterStartDate ?: previous.semesterStartDate,
        semesterTotalWeeks = semesterTotalWeeks ?: previous.semesterTotalWeeks,
        firstDayOfWeek = firstDayOfWeek ?: previous.firstDayOfWeek,
    )
}

internal class ScriptCourseImport {
    var courses: List<CourseImportExport.ImportCourseJsonModel>? = null
    var slots: List<CourseImportExport.TimeSlotJsonModel>? = null
    var config = ScriptCourseConfig()
    var combo: CourseImportExport.ComboScheduleJsonModel? = null
}

/** Matches the same school year / half-year used by the installed XATU adapter. */
internal fun savedDatesForSemester(config: CourseTableConfig?, selection: String?): CourseTableConfig? {
    val date = config?.semesterStartDate?.let(LocalDate::parse) ?: return null
    val autumn = date.month.number >= 7
    val year = if (autumn) date.year else date.year - 1
    val term = if (autumn) "秋季" else "春季"
    val selectedTerm = selection?.removeSuffix("（当前学期）")
    return config.takeIf { selectedTerm == "${year}-${year + 1}学年 ${term}学期" }
}
