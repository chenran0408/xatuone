package com.xingheyuzhuan.shiguangschedule.data.repository

interface XatuSessionRepository {
    suspend fun completeLogin()
    suspend fun restoreSavedLogin(studentNumber: String)
    suspend fun clear()
}
