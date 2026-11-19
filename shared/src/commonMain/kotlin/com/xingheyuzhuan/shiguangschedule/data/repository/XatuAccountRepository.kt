package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

data class XatuAccount(val studentNumber: String, val loggedIn: Boolean, val canRestore: Boolean = loggedIn)

@Single
class XatuAccountRepository(
    @Named("XatuAccount") private val dataStore: DataStore<Preferences>
) {
    private val studentNumberKey = stringPreferencesKey("student_number")
    private val loggedInKey = stringPreferencesKey("logged_in")

    val account: Flow<XatuAccount> = dataStore.data.map { preferences ->
        val studentNumber = preferences[studentNumberKey].orEmpty()
        val loginStatus = preferences[loggedInKey]
        XatuAccount(
            studentNumber = studentNumber,
            loggedIn = loginStatus == "true",
            canRestore = studentNumber.isNotBlank() && loginStatus != null,
        )
    }

    suspend fun saveStudentNumber(studentNumber: String) {
        val number = studentNumber.trim()
        dataStore.edit { preferences ->
            if (preferences[studentNumberKey] != number) preferences.remove(loggedInKey)
            preferences[studentNumberKey] = number
        }
    }

    suspend fun markLoggedIn(studentNumber: String): Boolean {
        val updated = dataStore.edit { preferences ->
            if (studentNumber.isNotBlank() && preferences[studentNumberKey] == studentNumber) {
                preferences[loggedInKey] = "true"
            }
        }
        return updated[studentNumberKey] == studentNumber && updated[loggedInKey] == "true"
    }

    suspend fun markLoggedOut(studentNumber: String) {
        dataStore.edit { preferences ->
            if (preferences[studentNumberKey] == studentNumber && preferences[loggedInKey] == "true") {
                preferences[loggedInKey] = "false"
            }
        }
    }

    suspend fun saveLogin(studentNumber: String) {
        dataStore.edit { preferences ->
            preferences[studentNumberKey] = studentNumber.trim()
            preferences[loggedInKey] = "true"
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
