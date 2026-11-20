package com.xingheyuzhuan.shiguangschedule

import com.sun.net.httpserver.HttpServer
import com.xingheyuzhuan.shiguangschedule.data.api.webdav.WebDavClient
import com.xingheyuzhuan.shiguangschedule.data.api.webdav.WebDavConfig
import com.xingheyuzhuan.shiguangschedule.tool.ZipUtils
import com.xingheyuzhuan.shiguangschedule.ui.settings.backup.BackupScratchFiles
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupStorageTest {
    @Test
    fun failedCancelledAndParallelImportsLeaveOnlyTheUserArchive() = runBlocking {
        val parent = Files.createDirectories(Paths.get("build", "tmp").toAbsolutePath())
        val cache = Files.createTempDirectory(parent, "backup-storage-").toString().toPath()
        val fs = FileSystem.SYSTEM
        val userZip = cache / "export.zip"
        val archive = ZipUtils.createZip(mapOf("meta.json" to "one".encodeToByteArray()))
        try {
            fs.write(userZip) { write(archive) }
            assertFailsWith<IOException> {
                BackupScratchFiles.readArchive(cache, Buffer().writeUtf8("damaged")) { }
            }
            assertFailsWith<CancellationException> {
                BackupScratchFiles.readArchive(cache, Buffer().write(archive)) { throw CancellationException() }
            }
            val contents = listOf("first", "second").map { value ->
                async(Dispatchers.IO) {
                    val bytes = ZipUtils.createZip(mapOf("meta.json" to value.encodeToByteArray()))
                    BackupScratchFiles.readArchive(cache, Buffer().write(bytes)) { zip ->
                        zip.read("/meta.json".toPath()) { readUtf8() }
                    }
                }
            }.awaitAll()
            assertEquals(listOf("first", "second"), contents)
            assertEquals(listOf(userZip), fs.list(cache))
            assertContentEquals(archive, fs.read(userZip) { readByteArray() })
        } finally {
            fs.deleteRecursively(cache)
        }
    }

    @Test
    fun cleanupRecognizesPreviousBackupsOnlyInPrivateCache() = runBlocking {
        val parent = Files.createDirectories(Paths.get("build", "tmp").toAbsolutePath())
        val root = Files.createTempDirectory(parent, "backup-legacy-").toString().toPath()
        val cache = root / "cache"
        val fs = FileSystem.SYSTEM
        try {
            fs.createDirectory(cache)
            fs.write(root / "meta.json") { writeUtf8("user file") }
            listOf("backup_import_123456.zip", "meta.json", "course_restore.cbor", "xatu-backup-import.tmp",
                "backup_export_123456.zip", "preserve.cbor").forEach { fs.write(cache / it) {} }
            BackupScratchFiles.clean(cache, root)
            assertTrue(fs.exists(root / "meta.json"))
            assertTrue(fs.exists(cache / "backup_import_123456.zip"))
            assertFalse(fs.exists(cache / "xatu-backup-import.tmp"))
            BackupScratchFiles.clean(cache, cache)
            assertEquals(listOf("backup_export_123456.zip", "preserve.cbor"), fs.list(cache).map { it.name }.sorted())
        } finally {
            fs.deleteRecursively(root)
        }
    }

    @Test
    fun webDavTransfersModuleBytesAtTheOriginalRemotePath() = runBlocking {
        val files = ConcurrentHashMap<String, ByteArray>()
        val service = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        service.createContext("/") { exchange ->
            exchange.use {
                val path = exchange.requestURI.path
                when (exchange.requestMethod) {
                    "MKCOL" -> exchange.sendResponseHeaders(201, -1)
                    "PUT" -> {
                        files[path] = exchange.requestBody.use { it.readBytes() }
                        exchange.sendResponseHeaders(201, -1)
                    }
                    "GET" -> {
                        val bytes = files[path]
                        if (bytes == null) exchange.sendResponseHeaders(404, -1)
                        else {
                            exchange.sendResponseHeaders(200, bytes.size.toLong())
                            exchange.responseBody.write(bytes)
                        }
                    }
                }
            }
        }
        service.start()
        try {
            val config = WebDavConfig("http://127.0.0.1:${service.address.port}", "", "", "remote root")
            WebDavClient(config).use { client ->
                val bytes = byteArrayOf(0, 0x7F, -1)
                assertTrue(client.upload("Backup/course.cbor", bytes))
                assertContentEquals(bytes, files["/remote root/Backup/course.cbor"])
                assertContentEquals(bytes, client.download("Backup/course.cbor"))
                assertNull(client.download("Backup/missing.cbor"))
            }
        } finally {
            service.stop(0)
        }
    }
}
