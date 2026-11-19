package com.xingheyuzhuan.shiguangschedule.xatu

import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccountRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuSessionRepository
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.AndroidWebViewController
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuAcademicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single(binds = [XatuSessionRepository::class])
class AndroidXatuSessionRepository(
    private val account: XatuAccountRepository,
    private val campus: XatuCampusRepository,
    private val academic: XatuAcademicRepository,
) : XatuSessionRepository {
    private val sessionChange = Mutex()

    override suspend fun completeLogin() = sessionChange.withLock {
        val studentNumber = account.account.first().studentNumber
        checkNotNull(academic.officialWeek()) { "尚未确认教务登录，请完成网页验证后重试" }
        saveVerifiedLogin(studentNumber)
    }

    override suspend fun restoreSavedLogin(studentNumber: String) = sessionChange.withLock {
        val saved = account.account.first()
        if (saved.studentNumber != studentNumber || !saved.canRestore) throw CancellationException("教务账号已变更")
        if (saved.loggedIn) return@withLock
        checkNotNull(academic.officialWeek()) { "教务未返回当前教学周，暂无法确认登录状态" }
        saveVerifiedLogin(studentNumber)
    }

    private suspend fun saveVerifiedLogin(studentNumber: String) {
        AndroidWebViewController.shared.persistSession()
        if (!account.markLoggedIn(studentNumber)) throw CancellationException("教务账号已变更")
    }

    override suspend fun clear() = sessionChange.withLock {
        val studentNumber = account.account.first().studentNumber
        account.clear()
        try {
            if (studentNumber.isNotBlank()) academic.clearAccount(studentNumber)
            campus.clear()
        } finally {
            withContext(NonCancellable) { AndroidWebViewController.shared.clearSession() }
        }
    }
}
