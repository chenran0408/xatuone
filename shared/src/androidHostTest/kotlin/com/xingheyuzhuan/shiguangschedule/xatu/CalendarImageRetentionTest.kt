package com.xingheyuzhuan.shiguangschedule.xatu

import com.xingheyuzhuan.shiguangschedule.xatu.campus.pruneCalendarImages
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarImageRetentionTest {
    @Test
    fun refreshKeepsCurrentOriginalAndOnlyPrunesOwnedImageFiles() {
        val root = Files.createTempDirectory("calendar-retention").toFile()
        try {
            val files = File(root, "files").also { it.mkdir() }
            val cache = File(root, "cache").also { it.mkdir() }
            val current = File(files, "xatu-calendar-${"a".repeat(64)}.image")
            current.writeText("new")
            File(files, "xatu-calendar-${"b".repeat(64)}.image").writeText("old")
            File(files, "xatu-calendar-notes.image").writeText("personal")
            File(cache, "xatu-calendar-original-${"c".repeat(64)}.image").writeText("recovered")
            File(cache, "export.zip").writeText("export")

            pruneCalendarImages(files, cache, current)

            assertEquals(setOf(current.name, "xatu-calendar-notes.image"), files.list()!!.toSet())
            assertEquals("new", current.readText())
            assertEquals(listOf("export.zip"), cache.list()!!.toList())
        } finally {
            root.deleteRecursively()
        }
    }
}
