package com.xingheyuzhuan.shiguangschedule.ui.settings.backup

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.openZip
import okio.use

internal object BackupScratchFiles {
    private const val IMPORT = "xatu-backup-import.tmp"
    private val lock = Mutex()
    private val legacyImport = Regex("""backup_import_[1-9][0-9]{5}\.zip""")
    private val legacyWebDav = setOf(
        "meta.json", "course.cbor", "style.cbor", "app_settings.cbor",
        "meta_restore.json", "course_restore.cbor", "style_restore.cbor", "app_settings_restore.cbor",
    )

    suspend fun clean(cacheDir: Path, oldTempDir: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY) = lock.withLock {
        val fs = FileSystem.SYSTEM
        fs.delete(cacheDir / IMPORT)
        if (!fs.exists(oldTempDir) || fs.canonicalize(oldTempDir) != fs.canonicalize(cacheDir)) return@withLock
        fs.list(oldTempDir).filter { it.name in legacyWebDav || legacyImport.matches(it.name) }
            .forEach { if (fs.metadata(it).isRegularFile) fs.delete(it) }
    }

    // Okio's indexed ZIP reader needs a file. A single scoped file also bounds leftovers if Android kills the process.
    suspend fun <T> readArchive(cacheDir: Path, source: BufferedSource, read: (FileSystem) -> T): T = lock.withLock {
        val fs = FileSystem.SYSTEM
        val path = cacheDir / IMPORT
        try {
            fs.write(path) { writeAll(source) }
            currentCoroutineContext().ensureActive()
            fs.openZip(path).use(read)
        } finally {
            fs.delete(path)
        }
    }
}
