package com.xingheyuzhuan.shiguangschedule.ui.schedule

/** XATU 适配器将序号保存在课程名的最后一个“ | ”后，原始名称不改写。 */
internal data class CourseDisplayName(val title: String, val number: String?)

internal fun courseDisplayName(name: String): CourseDisplayName {
    val match = Regex("^(.+) \\| ([0-9]+\\.[0-9]+)$").matchEntire(name)
    return if (match == null) CourseDisplayName(name, null)
    else CourseDisplayName(match.groupValues[1], match.groupValues[2])
}

internal fun displayedCourseName(name: String, hideNumber: Boolean): String =
    if (hideNumber) courseDisplayName(name).title else name
