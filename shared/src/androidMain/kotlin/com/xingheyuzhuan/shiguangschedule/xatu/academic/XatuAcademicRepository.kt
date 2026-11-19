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
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException

internal const val XATU_ALL_GRADES = "all-grades"

data class XatuPlanUpdate(val snapshot: XatuAcademicSnapshot, val warning: String?)

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
        val grades = if (semesterId == XATU_ALL_GRADES) service.allGrades() else service.grades(semesterId)
        current.copy(grades = grades, fetchedAt = System.currentTimeMillis()).also {
            snapshots["$account|$semesterId"] = it
            save()
        }
    }

    suspend fun officialWeek(): Int? = service.officialWeek()

    suspend fun currentSemesterId(semesters: List<XatuSemester>): String = XatuAcademicParser.currentSemesterId(
        semesters,
        checkNotNull(service.officialWeek()) { "教务首页未返回当前教学周，请手动选择学期" },
        LocalDate.now(ZoneOffset.ofHours(8))
    )

    suspend fun refreshExams(account: String, semesterId: String): XatuAcademicSnapshot = mutex.withLock {
        load()
        val current = snapshots["$account|$semesterId"] ?: XatuAcademicSnapshot(account, semesterId)
        service.exams(semesterId).let { current.copy(exams = it, fetchedAt = System.currentTimeMillis()) }.also {
            snapshots["$account|$semesterId"] = it
            save()
        }
    }

    suspend fun refreshPlan(account: String): XatuPlanUpdate = mutex.withLock {
        load()
        val key = "$account|all"
        val current = snapshots[key] ?: XatuAcademicSnapshot(account, "all")
        val plan = service.plan()
        var warning: String? = null
        val catalog = try {
            service.catalog()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            warning = "培养方案读取失败：${error.message}"
            current.catalog
        }
        val updated = current.copy(plan = plan, catalog = catalog, fetchedAt = System.currentTimeMillis()).also {
            snapshots[key] = it
            save()
        }
        XatuPlanUpdate(updated, warning)
    }

    suspend fun loadAccountCache(account: String): XatuAcademicCacheFile = mutex.withLock {
        load()
        XatuAcademicCacheFile(semesters, snapshots.values.filter { it.account == account })
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
                val value = decodeAcademicCache(raw, crypto::decrypt)
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
        val temporary = cachePath.parent!! / "academic_cache.json.tmp"
        fileSystem.sink(temporary).buffer().use {
            it.writeUtf8(json.encodeToString(EncryptedAcademicCache(encrypted.encryptedData, encrypted.iv)))
        }
        fileSystem.atomicMove(temporary, cachePath)
    }
}
