package com.xingheyuzhuan.shiguangschedule.xatu

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.core.okio.OkioStorage
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusSnapshot
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCheckinLocation
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCalendar
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuDormSelection
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import okio.FileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class XatuCampusRepositoryTest {
    private val dorm = XatuDormSelection("1", "项目", "1", "校区", "1", "楼栋", "101", "101")

    @Test
    fun balanceRefreshPersistsOnlyForSelectedDorm() = withRepository { repository ->
        repository.saveDorm(dorm)
        repository.saveBalance(dorm, "28.50", 1000L)
        val saved = repository.snapshot.first()
        assertEquals("28.50", saved.balance)
        assertEquals(1000L, saved.balanceAt)
        assertEquals(listOf("28.50"), saved.balanceHistory.map { it.balance })
    }

    @Test
    fun oldDormResponseCannotOverwriteNewDormHistory() = withRepository { repository ->
        repository.saveDorm(dorm)
        repository.saveBalance(dorm, "28.50", 1000L)
        val nextDorm = dorm.copy(roomId = "102", roomName = "102")
        repository.saveDorm(nextDorm)

        assertFailsWith<IllegalStateException> { repository.saveBalance(dorm, "20.00", 2000L) }
        assertEquals(XatuCampusSnapshot(dorm = nextDorm), repository.snapshot.first())
    }

    @Test
    fun unbindingDormClearsOnlyDormAndItsBalanceHistory() = withRepository { repository ->
        val location = XatuCheckinLocation(true, 34.2, 108.9)
        val calendar = XatuCalendar("校历", "https://jwcw.xatu.edu.cn/", "calendar.jpg", emptyList(), 2000L)
        repository.saveCheckinLocation(location)
        repository.saveCalendar(calendar)
        repository.saveDorm(dorm)
        repository.saveBalance(dorm, "28.50", 1000L)

        repository.clearDorm()

        assertEquals(XatuCampusSnapshot(checkinLocation = location, calendar = calendar), repository.snapshot.first())
        assertFailsWith<IllegalStateException> { repository.saveBalance(dorm, "20.00", 3000L) }
        assertEquals(XatuCampusSnapshot(checkinLocation = location, calendar = calendar), repository.snapshot.first())
    }

    @Test
    fun logoutRemovesDormLocationAndBalanceRecords() = withRepository { repository ->
        repository.saveDorm(dorm)
        repository.saveBalance(dorm, "28.50", 1000L)
        repository.saveCheckinLocation(XatuCheckinLocation(true, 34.2, 108.9))
        repository.clear()
        assertEquals(XatuCampusSnapshot(), repository.snapshot.first())
    }

    private fun withRepository(block: suspend (XatuCampusRepository) -> Unit) = runBlocking {
        val parent = Paths.get("build", "tmp").toAbsolutePath()
        Files.createDirectories(parent)
        val path = parent.resolve("xatu-campus-${UUID.randomUUID()}.preferences_pb")
        val job = SupervisorJob()
        // Android FileStorage uses platform file operations unavailable to a host test.
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer, producePath = { path.toString().toPath() })
        )
        try {
            block(XatuCampusRepository(store))
        } finally {
            job.cancelAndJoin()
            Files.deleteIfExists(path)
        }
    }
}
