package com.xingheyuzhuan.shiguangschedule.tool

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import okio.use
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import shiguangschedule.shared.generated.resources.Res
import school_index.SchoolIndex

/**
 * 资源与缓存初始化管理器。
 *
 * 负责应用启动时的静态离线资源解压校验，以及过期的分享与下载临时缓存清理。
 */
@Suppress("unused")
@Single(createdAtStart = true)
class ResourceInitializerManager(
    private val fileSystem: FileSystem,
    @Named("FilesDir") private val filesDir: Path,
    @Named("CacheDir") private val cacheDir: Path
) {
    private val targetRepoDir: Path = filesDir / "repo"
    private val shareTempDir: Path = cacheDir / "share_temp"
    private val initializationMutex = Mutex()
    private var initialized = false

    init {
        CoroutineScope(Dispatchers.IO).launch {
            initializeOfflineRepo()
            clearTempCaches()
        }
    }

    /**
     * 校验并解压内置离线适配仓库。
     *
     * @param forceOverwrite 是否强制清除并重新解压覆盖现有本地仓库
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun initializeOfflineRepo(forceOverwrite: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        initializationMutex.withLock {
            runCatching {
                if (initialized && !forceOverwrite) return@runCatching

                val zipBytes = Res.readBytes("files/offline_schools.zip")
                val tempZipFile = filesDir / "temp_offline_schools.zip"

                fileSystem.write(tempZipFile) {
                    write(zipBytes)
                }

                try {
                    installOfflineRepo(fileSystem, tempZipFile, targetRepoDir, forceOverwrite)
                    initialized = true
                } finally {
                    fileSystem.delete(tempZipFile)
                }
            }
        }
    }

    /**
     * 清理应用生成的临时文件与过期缓存目录。
     */
    suspend fun clearTempCaches(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (fileSystem.exists(shareTempDir)) {
                fileSystem.deleteRecursively(shareTempDir)
            }

            val tempSchoolsRepo = cacheDir / "temp_schools_repo"
            val tempIndexRepo = cacheDir / "temp_index_repo"
            if (fileSystem.exists(tempSchoolsRepo)) fileSystem.deleteRecursively(tempSchoolsRepo)
            if (fileSystem.exists(tempIndexRepo)) fileSystem.deleteRecursively(tempIndexRepo)
        }
    }
}

/** Add bundled adapters on upgrades without replacing downloaded scripts or index metadata. */
internal fun installOfflineRepo(fileSystem: FileSystem, zipPath: Path, targetRepoDir: Path, forceOverwrite: Boolean = false) {
    if (forceOverwrite && fileSystem.exists(targetRepoDir)) fileSystem.deleteRecursively(targetRepoDir)
    fileSystem.createDirectories(targetRepoDir)
    fileSystem.openZip(zipPath).use { zipFileSystem ->
        fun unzipDirectory(currentZipPath: Path, targetDir: Path) {
            val entries = zipFileSystem.list(currentZipPath)
            for (entry in entries) {
                val destinationPath = targetDir / entry.name

                if (!destinationPath.toString().startsWith(targetDir.toString())) {
                    throw IllegalArgumentException("Illegal zip path: ${entry.name}")
                }

                val metadata = zipFileSystem.metadata(entry)

                if (metadata.isDirectory) {
                    fileSystem.createDirectories(destinationPath)
                    unzipDirectory(entry, destinationPath)
                } else if (!fileSystem.exists(destinationPath)) {
                    destinationPath.parent?.let { fileSystem.createDirectories(it) }

                    zipFileSystem.source(entry).use { source ->
                        fileSystem.sink(destinationPath).buffer().use { sink ->
                            sink.writeAll(source)
                        }
                    }
                }
            }
        }
        unzipDirectory("/".toPath(), targetRepoDir)
        val indexPath = targetRepoDir / "index/school_index.pb"
        val bundled = zipFileSystem.read("/index/school_index.pb".toPath()) { SchoolIndex.ADAPTER.decode(this) }
        val current = fileSystem.read(indexPath) { SchoolIndex.ADAPTER.decode(this) }
        val schools = current.schools.map { school ->
            val additions = bundled.schools.firstOrNull { it.id == school.id }?.adapters.orEmpty()
                .filter { adapter -> school.adapters.none { it.adapter_id == adapter.adapter_id } }
            school.copy(adapters = school.adapters + additions)
        } + bundled.schools.filter { school -> current.schools.none { it.id == school.id } }
        if (schools != current.schools) {
            fileSystem.write(indexPath) { write(SchoolIndex.ADAPTER.encode(current.copy(schools = schools))) }
        }
    }
}
