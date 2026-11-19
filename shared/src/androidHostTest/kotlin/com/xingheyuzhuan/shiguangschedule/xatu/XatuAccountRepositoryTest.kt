package com.xingheyuzhuan.shiguangschedule.xatu

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccount
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccountRepository
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XatuAccountRepositoryTest {
    @Test
    fun savedDraftBecomesRestorableAfterLoginAndRemainsEligibleAfterLogout() = withRepository { repository ->
        repository.saveStudentNumber("  12345  ")
        assertEquals(XatuAccount("12345", loggedIn = false, canRestore = false), repository.account.first())

        assertTrue(repository.markLoggedIn("12345"))
        assertEquals(XatuAccount("12345", loggedIn = true, canRestore = true), repository.account.first())

        repository.markLoggedOut("12345")
        assertEquals(XatuAccount("12345", loggedIn = false, canRestore = true), repository.account.first())

        repository.clear()
        assertEquals(XatuAccount("", loggedIn = false, canRestore = false), repository.account.first())
    }

    @Test
    fun staleStudentNumberCannotChangeCurrentAccount() = withRepository { repository ->
        repository.saveLogin("12345")
        repository.saveStudentNumber("67890")

        assertFalse(repository.markLoggedIn("12345"))
        repository.markLoggedOut("12345")
        assertEquals(XatuAccount("67890", loggedIn = false, canRestore = false), repository.account.first())
    }

    private fun withRepository(block: suspend (XatuAccountRepository) -> Unit) = runBlocking {
        val parent = Paths.get("build", "tmp").toAbsolutePath()
        Files.createDirectories(parent)
        val folder = Files.createTempDirectory(parent, "xatu-account-")
        val path = folder.resolve("account.preferences_pb")
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer, producePath = { path.toString().toPath() })
        )
        try {
            block(XatuAccountRepository(store))
        } finally {
            job.cancelAndJoin()
            Files.deleteIfExists(path)
            Files.delete(folder)
        }
    }
}
