package com.xingheyuzhuan.shiguangschedule.xatu.academic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.xingheyuzhuan.shiguangschedule.tool.SecureCrypto
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class XatuAcademicRepository(
    private val service: XatuAcademicService,
    private val crypto: SecureCrypto,
    @Named("FilesDir") private val filesDir: Path,
    private val fileSystem: FileSystem
) {
    private val mutex = Mutex()
    private val snapshots = mutableMapOf<String, XatuAcademicSnapshot>()
    private var semesters: List<XatuSemester> = emptyList()
    private var loaded = false
    private val json = Json { ignoreUnknownKeys = true }
    private val cachePath = filesDir / "xatu" / "academic_cache.json"

    suspend fun semesters(force: Boolean = false): List<XatuSemester> = mutex.withLock {
        load()
        if (force || semesters.isEmpty()) semesters = service.semesters()
        save()
        semesters
    }

    suspend fun refreshGrades(account: String, semesterId: String): XatuAcademicSnapshot = mutex.withLock {
        load()
        val current = snapshots["$account|$semesterId"] ?: XatuAcademicSnapshot(account, semesterId)
        service.grades(semesterId).let { current.copy(grades = it) }.also {
            snapshots["$account|$semesterId"] = it
            save()
        }
    }

    suspend fun refreshExams(account: String, semesterId: String): XatuAcademicSnapshot = mutex.withLock {
        load()
        val current = snapshots["$account|$semesterId"] ?: XatuAcademicSnapshot(account, semesterId)
        service.exams(semesterId).let { current.copy(exams = it) }.also {
            snapshots["$account|$semesterId"] = it
            save()
        }
    }

    suspend fun refreshPlan(account: String): XatuAcademicSnapshot = mutex.withLock {
        load()
        val key = "$account|all"
        val current = snapshots[key] ?: XatuAcademicSnapshot(account, "all")
        val plan = service.plan()
        val catalog = service.catalog()
        current.copy(plan = plan, catalog = catalog).also {
            snapshots[key] = it
            save()
        }
    }

    suspend fun loadCached(account: String, semesterId: String): XatuAcademicSnapshot? = mutex.withLock {
        load()
        snapshots["$account|$semesterId"]
    }

    suspend fun clearAccount(account: String) = mutex.withLock {
        load()
        snapshots.keys.removeAll { it.startsWith("$account|") }
        save()
    }

    private suspend fun load() {
        if (loaded) return
        withContext(Dispatchers.IO) {
            if (fileSystem.exists(cachePath)) {
                val raw = fileSystem.source(cachePath).buffer().use { it.readUtf8() }
                val value = runCatching {
                    val encrypted = json.decodeFromString<EncryptedAcademicCache>(raw)
                    val decrypted = crypto.decrypt(encrypted.encryptedData, encrypted.iv)
                        ?: error("学术缓存解密失败")
                    json.decodeFromString<XatuAcademicCacheFile>(decrypted)
                }.getOrElse {
                    json.decodeFromString<XatuAcademicCacheFile>(raw)
                }
                semesters = value.semesters
                snapshots.clear()
                value.snapshots.forEach { snapshots["${it.account}|${it.semesterId}"] = it }
            }
            loaded = true
        }
    }

    private suspend fun save() = withContext(Dispatchers.IO) {
        fileSystem.createDirectories(cachePath.parent!!)
        val value = XatuAcademicCacheFile(semesters, snapshots.values.toList())
        val encrypted = crypto.encrypt(json.encodeToString(value)) ?: error("学术缓存加密失败")
        fileSystem.sink(cachePath).buffer().use {
            it.writeUtf8(json.encodeToString(EncryptedAcademicCache(encrypted.encryptedData, encrypted.iv)))
        }
    }
}

@kotlinx.serialization.Serializable
private data class EncryptedAcademicCache(val encryptedData: String, val iv: String)
