package com.xingheyuzhuan.shiguangschedule.xatu.academic

import java.time.LocalDateTime

/** Only complete server dates are sortable; never invent a missing year. */
fun XatuExam.startDateTime(): LocalDateTime? {
    val match = Regex("""(\d{4})[-/.年](\d{1,2})[-/.月](\d{1,2})日?\s*[^\d]*?(\d{1,2})[:：](\d{2})""").find(time) ?: return null
    val values = match.groupValues.drop(1).map(String::toInt)
    return runCatching { LocalDateTime.of(values[0], values[1], values[2], values[3], values[4]) }.getOrNull()
}
