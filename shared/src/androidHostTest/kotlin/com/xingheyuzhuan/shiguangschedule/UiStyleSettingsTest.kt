package com.xingheyuzhuan.shiguangschedule

import androidx.datastore.preferences.core.emptyPreferences
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.test.Test
import kotlin.test.assertEquals

class UiStyleSettingsTest {
    @Test
    fun stylePreferenceIsReadAndDefaultsToMaterial() {
        assertEquals(UiStyle.MATERIAL, AppSettingsModel.fromPreferences(emptyPreferences(), "").uiStyle)
    }

    @Test
    fun storedStyleIsRestored() {
        UiStyle.entries.forEach { style ->
            val preferences = androidx.datastore.preferences.core.preferencesOf(
                stringPreferencesKey("ui_style") to style.value
            )
            assertEquals(style, AppSettingsModel.fromPreferences(preferences, "").uiStyle)
        }
    }
}
