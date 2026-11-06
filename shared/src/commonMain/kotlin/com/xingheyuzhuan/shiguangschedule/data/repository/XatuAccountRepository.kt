package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

data class XatuAccount(val studentNumber: String, val loggedIn: Boolean)

@Single
class XatuAccountRepository(
    @Named("XatuAccount") private val dataStore: DataStore<Preferences>
) {
    private val studentNumberKey = stringPreferencesKey("student_number")
    private val loggedInKey = stringPreferencesKey("logged_in")

    val account: Flow<XatuAccount> = dataStore.data.map { preferences ->
        XatuAccount(
            studentNumber = preferences[studentNumberKey].orEmpty(),
            loggedIn = preferences[loggedInKey] == "true"
        )
    }

    suspend fun saveStudentNumber(studentNumber: String) {
        dataStore.edit { preferences ->
            preferences[studentNumberKey] = studentNumber.trim()
        }
    }

    suspend fun markLoggedIn() {
        dataStore.edit { preferences -> preferences[loggedInKey] = "true" }
    }

    suspend fun markLoggedOut() {
        dataStore.edit { preferences -> preferences[loggedInKey] = "false" }
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
