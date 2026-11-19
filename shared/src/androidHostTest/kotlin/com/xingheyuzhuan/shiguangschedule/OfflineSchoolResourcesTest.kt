package com.xingheyuzhuan.shiguangschedule

import com.xingheyuzhuan.shiguangschedule.tool.installOfflineRepo
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import okio.FileSystem
import okio.Path.Companion.toPath
import school_index.AdapterCategory
import school_index.School
import school_index.SchoolIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OfflineSchoolResourcesTest {
    @Test
    fun upgradeAddsXatuAndPreservesDownloadedResourcesAndIndexVersion() {
        val parent = Paths.get("build", "tmp").toAbsolutePath()
        Files.createDirectories(parent)
        val folder = Files.createTempDirectory(parent, "offline-schools-").toString().toPath()
        val fs = FileSystem.SYSTEM
        try {
            val assets = Paths.get("assets", "offline_repo")
            val zip = folder / "bundled.zip"
            ZipOutputStream(Files.newOutputStream(Paths.get(zip.toString()))).use { output ->
                Files.walk(assets).use { paths ->
                    paths.filter { Files.isRegularFile(it) }.forEach { file ->
                        output.putNextEntry(ZipEntry(assets.relativize(file).toString().replace('\\', '/')))
                        Files.copy(file, output)
                        output.closeEntry()
                    }
                }
            }
            val repo = folder / "repo"
            fs.createDirectories(repo / "index")
            val downloaded = SchoolIndex(protocol_version = 2, version_id = "downloaded-version", schools = listOf(School(id = "CUSTOM", name = "保留学校")))
            fs.write(repo / "index/school_index.pb") { write(SchoolIndex.ADAPTER.encode(downloaded)) }
            fs.createDirectories(repo / "schools/resources/GLOBAL_TOOLS")
            fs.write(repo / "schools/resources/GLOBAL_TOOLS/school.js") { writeUtf8("downloaded-tool") }

            installOfflineRepo(fs, zip, repo)
            val restored = fs.read(repo / "index/school_index.pb") { SchoolIndex.ADAPTER.decode(this) }
            assertEquals(downloaded.version_id, restored.version_id)
            assertEquals(downloaded.schools.single(), restored.schools.first { it.id == "CUSTOM" })
            val xatu = restored.schools.single { it.id == "XATU" }
            val adapter = xatu.adapters.single { it.category == AdapterCategory.BACHELOR_AND_ASSOCIATE }
            val script = repo / "schools/resources" / xatu.resource_folder / adapter.asset_js_path
            assertTrue(fs.exists(script))
            assertTrue(fs.read(script) { readUtf8() }.contains("runImportFlow();"))
            assertEquals("downloaded-tool", fs.read(repo / "schools/resources/GLOBAL_TOOLS/school.js") { readUtf8() })

            fs.write(script) { writeUtf8("newer-downloaded-xatu") }
            installOfflineRepo(fs, zip, repo)
            assertEquals("newer-downloaded-xatu", fs.read(script) { readUtf8() })
            assertEquals(restored, fs.read(repo / "index/school_index.pb") { SchoolIndex.ADAPTER.decode(this) })
        } finally {
            fs.deleteRecursively(folder)
        }
    }
}
