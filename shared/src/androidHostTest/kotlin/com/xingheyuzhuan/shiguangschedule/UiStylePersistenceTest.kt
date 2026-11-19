package com.xingheyuzhuan.shiguangschedule

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.preferencesOf
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

class UiStylePersistenceTest {
    @Test
    fun eachStyleSurvivesStoreReopenWithoutChangingOtherPreferences() = runBlocking {
        val parent = Paths.get("build", "tmp").toAbsolutePath()
        Files.createDirectories(parent)
        val folder = Files.createTempDirectory(parent, "ui-style-")
        val path = folder.resolve("settings.preferences_pb").toString().toPath()
        val original = preferencesOf(
            AppSettingsModel.KEY_CURRENT_COURSE_TABLE_ID to "selected-table",
            AppSettingsModel.KEY_THEME_MODE to "DARK",
            AppSettingsModel.KEY_REMINDER_ENABLED to true,
            AppSettingsModel.KEY_REMIND_BEFORE_MINUTES to 12
        )
        try {
            withStore(path) { store -> store.updateData { original } }
            for (style in UiStyle.entries) {
                withStore(path) { store ->
                    val repository = AppSettingsRepository(
                        store, unusedDependency(), unusedDependency(), unusedDependency(), path.parent!!, FileSystem.SYSTEM
                    )
                    repository.updateUiStyle(style)
                    assertEquals(style.value, store.data.first()[AppSettingsModel.KEY_UI_STYLE])
                }
                withStore(path) { store ->
                    val restored = store.data.first()
                    assertEquals(style, AppSettingsModel.fromPreferences(restored, "").uiStyle)
                    assertEquals(original.asMap(), restored.asMap().filterKeys { it != AppSettingsModel.KEY_UI_STYLE })
                }
            }
        } finally {
            Files.deleteIfExists(folder.resolve("settings.preferences_pb"))
            Files.delete(folder)
        }
    }

    private suspend fun withStore(path: Path, block: suspend (DataStore<Preferences>) -> Unit) {
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer, producePath = { path })
        )
        try {
            block(store)
        } finally {
            job.cancelAndJoin()
        }
    }

    // A style write must not touch course DAOs or the separate timetable style store.
    private inline fun <reified T> unusedDependency(): T = Proxy.newProxyInstance(
        T::class.java.classLoader, arrayOf(T::class.java)
    ) { _, method, _ -> error("风格更新不应调用 ${method.name}") } as T
}
