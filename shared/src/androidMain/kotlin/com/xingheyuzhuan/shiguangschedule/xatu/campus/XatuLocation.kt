package com.xingheyuzhuan.shiguangschedule.xatu.campus

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class XatuCoordinate(val latitude: Double, val longitude: Double, val system: String)

object XatuLocation {
    fun toWgs84(point: XatuCoordinate): XatuCoordinate {
        if (point.system.equals("WGS84", true)) return point.copy(system = "WGS84")
        var result = point.copy(system = "WGS84")
        repeat(8) {
            val converted = toGcj02(result)
            val dLat = converted.latitude - point.latitude
            val dLng = converted.longitude - point.longitude
            result = result.copy(latitude = result.latitude - dLat, longitude = result.longitude - dLng)
            if (max(abs(dLat), abs(dLng)) < 1e-9) return result
        }
        return result
    }

    private fun toGcj02(point: XatuCoordinate): XatuCoordinate {
        val lat = point.latitude
        val lng = point.longitude
        if (lng !in 72.004..137.8347 || lat !in .8293..55.8271) return point.copy(system = "GCJ02")
        val x = lng - 105.0
        val y = lat - 35.0
        var dLat = -100 + 2 * x + 3 * y + .2 * y * y + .1 * x * y + .2 * sqrt(abs(x))
        dLat += (20 * sin(6 * x * PI) + 20 * sin(2 * x * PI)) * 2 / 3
        dLat += (20 * sin(y * PI) + 40 * sin(y / 3 * PI)) * 2 / 3
        dLat += (160 * sin(y / 12 * PI) + 320 * sin(y * PI / 30)) * 2 / 3
        var dLng = 300 + x + 2 * y + .1 * x * x + .1 * x * y + .1 * sqrt(abs(x))
        dLng += (20 * sin(6 * x * PI) + 20 * sin(2 * x * PI)) * 2 / 3
        dLng += (20 * sin(x * PI) + 40 * sin(x / 3 * PI)) * 2 / 3
        dLng += (150 * sin(x / 12 * PI) + 300 * sin(x / 30 * PI)) * 2 / 3
        val rad = lat / 180 * PI
        val magic = 1 - .00669342162296594323 * sin(rad).pow(2)
        val root = sqrt(magic)
        dLat = dLat * 180 / ((6378245 * (1 - .00669342162296594323)) / (magic * root) * PI)
        dLng = dLng * 180 / (6378245 / root * cos(rad) * PI)
        return XatuCoordinate(lat + dLat, lng + dLng, "GCJ02")
    }
}
