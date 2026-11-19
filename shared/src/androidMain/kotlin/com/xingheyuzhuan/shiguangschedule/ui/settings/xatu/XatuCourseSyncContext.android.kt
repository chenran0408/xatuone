package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccount
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccountRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuSessionRepository
import com.xingheyuzhuan.shiguangschedule.ui.components.*
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuAcademicRepository
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuSessionExpired
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinViewModel

data class XatuCourseSyncContextState(
    val account: XatuAccount? = null,
    val semester: String? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val requiresLogin: Boolean = false,
)

@KoinViewModel
class XatuCourseSyncContextViewModel(
    accounts: XatuAccountRepository,
    academic: XatuAcademicRepository,
    session: XatuSessionRepository,
) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    private val _state = MutableStateFlow(XatuCourseSyncContextState())
    val state = _state.asStateFlow()
    private var manualLoginAccount: String? = null
    init {
        viewModelScope.launch {
            combine(accounts.account.distinctUntilChanged(), refresh) { account, _ -> account }.collectLatest { account ->
                _state.value = XatuCourseSyncContextState(
                    account = account,
                    loading = account.loggedIn || (account.canRestore && manualLoginAccount != account.studentNumber),
                    error = if (!account.loggedIn && manualLoginAccount == account.studentNumber) "请重新登录教务账号" else null,
                    requiresLogin = !account.loggedIn && manualLoginAccount == account.studentNumber,
                )
                if (!account.loggedIn) {
                    if (!account.canRestore || manualLoginAccount == account.studentNumber) return@collectLatest
                    try {
                        session.restoreSavedLogin(account.studentNumber)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: XatuSessionExpired) {
                        manualLoginAccount = account.studentNumber
                        _state.update { it.copy(loading = false, error = error.message, requiresLogin = true) }
                    } catch (error: Exception) {
                        _state.update { it.copy(loading = false, error = error.message) }
                    }
                    return@collectLatest
                }
                manualLoginAccount = null
                try {
                    val semesters = academic.semesters()
                    val id = academic.currentSemesterId(semesters)
                    _state.value = XatuCourseSyncContextState(account, semesters.first { it.id == id }.name, loading = false)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: XatuSessionExpired) {
                    manualLoginAccount = account.studentNumber
                    _state.update { it.copy(loading = false, error = error.message, requiresLogin = true) }
                    accounts.markLoggedOut(account.studentNumber)
                } catch (error: Exception) {
                    _state.value = XatuCourseSyncContextState(account, loading = false, error = error.message)
                }
            }
        }
    }
    fun refresh() { refresh.update { it + 1 } }
}

@Composable
actual fun XatuCourseSyncContext(onAccount: () -> Unit) {
    val viewModel: XatuCourseSyncContextViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val account = state.account
        val restoring = account?.canRestore == true && !account.loggedIn && state.loading
        UiText(account?.let { "教务账号：${it.studentNumber.ifBlank { "未设置" }}" } ?: "正在读取账号…", style = MaterialTheme.typography.bodySmall)
        UiText(state.semester?.let { "当前学期：$it" } ?: when {
            restoring -> "正在恢复教务登录…"
            state.loading -> "正在确认当前学期…"
            else -> "当前学期尚未确认"
        }, style = MaterialTheme.typography.bodySmall)
        state.error?.let { UiText(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UiButton(if (state.requiresLogin) "重新登录" else "账号管理", onAccount, Modifier.weight(1f))
            UiButton("重新确认", viewModel::refresh, Modifier.weight(1f), enabled = !state.loading)
        }
    }
}
