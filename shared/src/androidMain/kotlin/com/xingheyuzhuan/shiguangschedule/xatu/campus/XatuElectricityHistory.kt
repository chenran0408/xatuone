// Daily-balance deduction adapted from XATUHelper-Android (Kelejun, MIT).
package com.xingheyuzhuan.shiguangschedule.xatu.campus

import com.xingheyuzhuan.shiguangschedule.data.repository.XatuBalanceRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.math.BigDecimal

data class XatuDailyElectricity(val day: LocalDate, val balance: String, val decrease: String?)

/** Only observed readings, in their actual chronological order. */
fun xatuElectricityTrend(history: List<XatuBalanceRecord>): List<XatuBalanceRecord> = history.sortedBy { it.at }.takeLast(7)

/** Observed balance snapshots inside a calendar-time window. */
fun xatuElectricityWindow(history: List<XatuBalanceRecord>, days: Int, now: Long = System.currentTimeMillis()): List<XatuBalanceRecord> {
    val since = now - days * 24L * 60L * 60L * 1000L
    return history.filter { it.at in since..now }.sortedBy { it.at }
}

/** A signed balance change, not an estimate of electricity usage. */
fun recentXatuElectricityChange(history: List<XatuBalanceRecord>): BigDecimal? {
    val recent = xatuElectricityTrend(history).takeLast(2)
    return if (recent.size == 2) recent.last().balance.toBigDecimal() - recent.first().balance.toBigDecimal() else null
}

/** Last observed balance each day. Missing days and balance increases are not usage estimates. */
fun summarizeXatuElectricity(history: List<XatuBalanceRecord>): List<XatuDailyElectricity> {
    val days = history.groupBy { Instant.ofEpochMilli(it.at).atOffset(ZoneOffset.ofHours(8)).toLocalDate() }
        .map { (day, records) -> day to records.maxBy { it.at }.balance }.sortedBy { it.first }
    return days.mapIndexed { index, (day, balance) ->
        val previous = days.getOrNull(index - 1)
        val decrease = if (previous?.first?.plusDays(1) == day) {
            (previous.second.toBigDecimal() - balance.toBigDecimal()).takeIf { it.signum() >= 0 }?.toPlainString()
        } else null
        XatuDailyElectricity(day, balance, decrease)
    }
}
