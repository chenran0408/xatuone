package com.xingheyuzhuan.shiguangschedule.data.db.widget

import java.time.LocalDateTime
import java.time.format.DateTimeParseException

fun WidgetCourse.alarmDateTimeOrNull(time: String): LocalDateTime? = try {
    LocalDateTime.parse("${date}T$time")
} catch (_: DateTimeParseException) {
    null
}

/** 缺少对应节次的课程仍可在桌面显示，只给能确定开始时间的未来课程分配闹钟。 */
fun upcomingCourseReminders(
    courses: List<WidgetCourse>,
    now: LocalDateTime,
    minutesBefore: Int,
    maxCount: Int
): List<Pair<WidgetCourse, LocalDateTime>> = courses.asSequence()
    .filterNot { it.isSkipped }
    .mapNotNull { course ->
        val trigger = course.alarmDateTimeOrNull(course.startTime)?.minusMinutes(minutesBefore.toLong())
        if (trigger != null && trigger.isAfter(now)) course to trigger else null
    }
    .take(maxCount)
    .toList()
