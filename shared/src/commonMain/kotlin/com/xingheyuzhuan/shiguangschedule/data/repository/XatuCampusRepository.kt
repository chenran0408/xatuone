package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Serializable
data class XatuDormSelection(
    val projectId: String,
    val projectName: String,
    val areaId: String,
    val areaName: String,
    val buildingId: String,
    val buildingName: String,
    val roomId: String,
    val roomName: String
)

@Serializable
data class XatuCalendarSemester(val label: String, val start: String, val end: String)

@Serializable
data class XatuCalendar(
    val title: String,
    val pageUrl: String,
    val imageUrl: String,
    val semesters: List<XatuCalendarSemester>,
    val fetchedAt: Long,
    val imagePath: String = "",
    val rawText: String = ""
)

@Serializable
data class XatuBalanceRecord(val at: Long, val balance: String)

@Serializable
data class XatuCheckinLocation(
    val enabled: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val system: String = "GCJ02"
)

@Serializable
data class XatuCampusSnapshot(
    val dorm: XatuDormSelection? = null,
    val balance: String? = null,
    val balanceAt: Long = 0,
    val balanceHistory: List<XatuBalanceRecord> = emptyList(),
    val checkinLocation: XatuCheckinLocation = XatuCheckinLocation(),
    val calendar: XatuCalendar? = null
)

@Single
class XatuCampusRepository(
    @Named("XatuCampus") private val dataStore: DataStore<Preferences>
) {
    private val snapshotKey = stringPreferencesKey("snapshot")
    private val json = Json { ignoreUnknownKeys = true }

    val snapshot: Flow<XatuCampusSnapshot> = dataStore.data.map {
        it[snapshotKey]?.let(json::decodeFromString) ?: XatuCampusSnapshot()
    }

    suspend fun saveDorm(dorm: XatuDormSelection) = update { copy(dorm = dorm, balance = null, balanceAt = 0, balanceHistory = emptyList()) }

    suspend fun clearDorm() = update { copy(dorm = null, balance = null, balanceAt = 0, balanceHistory = emptyList()) }

    suspend fun saveBalance(balance: String, checkedAt: Long) = update {
        copy(
            balance = balance,
            balanceAt = checkedAt,
            balanceHistory = (balanceHistory + XatuBalanceRecord(checkedAt, balance)).takeLast(400)
        )
    }

    suspend fun saveCalendar(calendar: XatuCalendar) = update { copy(calendar = calendar) }

    suspend fun saveCheckinLocation(location: XatuCheckinLocation) = update { copy(checkinLocation = location) }

    suspend fun clear() {
        dataStore.edit { it.remove(snapshotKey) }
    }

    private suspend fun update(transform: XatuCampusSnapshot.() -> XatuCampusSnapshot) {
        dataStore.edit { preferences ->
            val current = preferences[snapshotKey]?.let(json::decodeFromString) ?: XatuCampusSnapshot()
            preferences[snapshotKey] = json.encodeToString(current.transform())
        }
    }
}
