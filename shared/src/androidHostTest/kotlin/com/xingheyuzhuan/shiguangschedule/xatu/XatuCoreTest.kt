package com.xingheyuzhuan.shiguangschedule.xatu

import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuAcademicParser
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuCoordinate
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XatuCoreTest {
    @Test
    fun wgs84CoordinatesRemainStable() {
        val point = XatuCoordinate(34.2658, 108.9535, "WGS84")
        val converted = XatuLocation.toWgs84(point)

        assertEquals(point.latitude, converted.latitude, 1e-9)
        assertEquals(point.longitude, converted.longitude, 1e-9)
        assertEquals("WGS84", converted.system)
    }

    @Test
    fun gcj02CoordinatesConvertBackNearOriginalPoint() {
        val gps = XatuCoordinate(34.2658, 108.9535, "WGS84")
        val gcj = XatuCoordinate(34.2690, 108.9600, "GCJ02")
        val converted = XatuLocation.toWgs84(gcj)

        assertEquals("WGS84", converted.system)
        assertTrue(converted.latitude in 34.24..34.30)
        assertTrue(converted.longitude in 108.92..109.00)
        assertTrue(kotlin.math.abs(converted.latitude - gps.latitude) < 0.01)
        assertTrue(kotlin.math.abs(converted.longitude - gps.longitude) < 0.01)
    }

    @Test
    fun semesterParserPrefersStructuredRecordsAndDeduplicatesIds() {
        val html = """
            [{"id": "12", "schoolYear": "2026-2027", "name": "秋季学期", "startDate": "2026-09-01"},
             {"id": "12", "schoolYear": "2026-2027", "name": "秋季学期"},
             {"id": "11", "schoolYear": "2025-2026", "name": "春季学期"}]
        """.trimIndent()

        val semesters = XatuAcademicParser.semesters(html)

        assertEquals(listOf("12", "11"), semesters.map { it.id })
        assertEquals("2026-2027学年秋季学期", semesters.first().name)
        assertEquals("2026-09-01", semesters.first().startDate)
    }
}
