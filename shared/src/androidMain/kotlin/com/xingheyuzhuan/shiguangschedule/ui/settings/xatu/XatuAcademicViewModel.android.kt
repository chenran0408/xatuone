package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccountRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuSessionRepository
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuAcademicRepository
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuExam
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuGradePage
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuCourseInfo
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuStudyPlan
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuSemester
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XATU_ALL_GRADES
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuSessionExpired
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class XatuAcademicUiState(
    val page: String = "",
    val grades: XatuGradePage? = null,
    val exams: List<XatuExam>? = null,
    val plan: XatuStudyPlan? = null,
    val catalog: List<XatuCourseInfo> = emptyList(),
    val semesters: List<XatuSemester> = emptyList(),
    val selectedSemesterId: String? = null,
    val officialWeek: Int? = null,
    val fetchedAt: Long? = null,
    val loading: Boolean = false,
    val requiresLogin: Boolean = false,
    val status: String = ""
)

@KoinViewModel
class XatuAcademicViewModel(
    private val academic: XatuAcademicRepository,
    private val accountRepository: XatuAccountRepository,
    private val session: XatuSessionRepository,
    appSettingsRepository: AppSettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(XatuAcademicUiState())
    val uiState: StateFlow<XatuAcademicUiState> = _uiState.asStateFlow()
    @OptIn(ExperimentalCoroutinesApi::class)
    val courseConfig = appSettingsRepository.getAppSettings()
        .map { it.currentCourseTableId }
        .distinctUntilChanged()
        .flatMapLatest(appSettingsRepository::getCourseTableConfigFlow)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private var request: Job? = null
    private val selectedSemesters = mutableMapOf<Pair<String, String>, String>()
    private var manualLoginAccount: String? = null

    fun load(page: String) {
        request?.cancel()
        request = viewModelScope.launch {
            val account = accountRepository.account.first()
            if (!account.loggedIn && manualLoginAccount == account.studentNumber && _uiState.value.page == page) {
                _uiState.update { it.copy(loading = false) }
                return@launch
            }
            _uiState.value = XatuAcademicUiState(page = page, loading = true, status = "正在读取教务数据…")
            if (!account.loggedIn || account.studentNumber.isBlank()) {
                if (account.canRestore && manualLoginAccount != account.studentNumber) {
                    _uiState.update { it.copy(status = "正在恢复教务登录…") }
                    try {
                        session.restoreSavedLogin(account.studentNumber)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: XatuSessionExpired) {
                        manualLoginAccount = account.studentNumber
                        _uiState.update { it.copy(loading = false, status = error.message.orEmpty(), requiresLogin = true) }
                    } catch (error: Exception) {
                        _uiState.update { it.copy(loading = false, status = "读取失败：${error.message ?: "未知错误"}") }
                    }
                } else {
                    _uiState.update { it.copy(loading = false, status = "请先登录教务账号", requiresLogin = manualLoginAccount == account.studentNumber) }
                }
                return@launch
            }
            manualLoginAccount = null
            try {
                when (page) {
                    "plan" -> loadPlan(account.studentNumber)
                    "week" -> {
                        val week = academic.officialWeek()
                        _uiState.update { it.copy(officialWeek = week, status = if (week == null) "教务首页未返回当前教学周" else "已更新") }
                    }
                    else -> loadSemesterData(page, account.studentNumber)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: XatuSessionExpired) {
                manualLoginAccount = account.studentNumber
                _uiState.update { it.copy(status = error.message.orEmpty(), requiresLogin = true) }
                accountRepository.markLoggedOut(account.studentNumber)
            } catch (error: Exception) {
                _uiState.update { it.copy(status = "读取失败：${error.message ?: "未知错误"}") }
            } finally {
                if (isActive) _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun cancel() {
        request?.cancel()
        request = null
        _uiState.update { it.copy(loading = false) }
    }

    fun selectSemester(id: String) {
        val page = _uiState.value.page
        if (page != "grades" && page != "exams") return
        request?.cancel()
        request = viewModelScope.launch {
            val account = accountRepository.account.first()
            if (!account.loggedIn || account.studentNumber.isBlank()) return@launch
            try {
                loadSemester(page, account.studentNumber, id, _uiState.value.semesters)
            } catch (error: CancellationException) {
                throw error
            } catch (error: XatuSessionExpired) {
                manualLoginAccount = account.studentNumber
                _uiState.update { it.copy(status = error.message.orEmpty(), requiresLogin = true) }
                accountRepository.markLoggedOut(account.studentNumber)
            } catch (error: Exception) {
                _uiState.update { it.copy(status = "读取失败：${error.message ?: "未知错误"}") }
            } finally {
                if (isActive) _uiState.update { it.copy(loading = false) }
            }
        }
    }

    private suspend fun loadPlan(account: String) {
        val cached = academic.loadCached(account, "all")
        _uiState.update { it.copy(plan = cached?.plan, catalog = cached?.catalog.orEmpty(), fetchedAt = cached?.fetchedAt, status = if (cached?.plan != null) "正在更新…" else "正在读取学业完成情况…") }
        val updated = academic.refreshPlan(account)
        _uiState.update { it.copy(plan = updated.snapshot.plan, catalog = updated.snapshot.catalog, fetchedAt = updated.snapshot.fetchedAt, status = updated.warning ?: "已更新") }
    }

    private suspend fun loadSemesterData(page: String, account: String) {
        val previous = selectedSemesters[account to page]
        val cache = academic.loadAccountCache(account)
        val cached = cache.snapshots
            .filter { previous == null || it.semesterId == previous }
            .filter { if (page == "grades") it.grades != null else it.exams.isNotEmpty() }
            .maxByOrNull { it.fetchedAt }
        if (cached != null) {
            _uiState.update { it.copy(
                semesters = cache.semesters, selectedSemesterId = cached.semesterId,
                grades = if (page == "grades") cached.grades else null,
                exams = if (page == "exams") cached.exams else null,
                fetchedAt = cached.fetchedAt, status = "正在确认学期…"
            ) }
        }
        val semesters = academic.semesters()
        val selected = previous?.takeIf { id -> semesters.any { it.id == id } || (page == "grades" && id == XATU_ALL_GRADES) }
            ?: if (semesters.isNotEmpty()) academic.currentSemesterId(semesters) else if (page == "grades") XATU_ALL_GRADES else null
        if (selected == null) _uiState.update { it.copy(
            semesters = semesters, selectedSemesterId = null, grades = null, exams = null,
            fetchedAt = null, status = "暂无可选学期"
        ) }
        if (selected != null) loadSemester(page, account, selected, semesters)
    }

    private suspend fun loadSemester(page: String, account: String, semesterId: String, semesters: List<XatuSemester>) {
        selectedSemesters[account to page] = semesterId
        _uiState.update {
            val sameSemester = it.selectedSemesterId == semesterId
            it.copy(
                semesters = semesters,
                selectedSemesterId = semesterId,
                grades = if (sameSemester) it.grades else null,
                exams = if (sameSemester) it.exams else null,
                fetchedAt = if (sameSemester) it.fetchedAt else null,
                loading = true,
                requiresLogin = false,
                status = if (page == "grades") "正在读取成绩…" else "正在读取考试安排…",
            )
        }
        val cached = academic.loadCached(account, semesterId)
        if (page == "grades") {
            _uiState.update { it.copy(selectedSemesterId = semesterId, grades = cached?.grades, exams = null, fetchedAt = cached?.fetchedAt, status = if (cached?.grades != null) "正在更新…" else "正在读取成绩…") }
            val updated = academic.refreshGrades(account, semesterId)
            _uiState.update { it.copy(grades = updated.grades, fetchedAt = updated.fetchedAt, status = "已更新：${semesterName(semesterId)}") }
        } else {
            _uiState.update { it.copy(selectedSemesterId = semesterId, grades = null, exams = cached?.exams, fetchedAt = cached?.fetchedAt, status = if (cached != null) "正在更新…" else "正在读取考试安排…") }
            val updated = academic.refreshExams(account, semesterId)
            _uiState.update { it.copy(exams = updated.exams, fetchedAt = updated.fetchedAt, status = "已更新：${semesterName(semesterId)}") }
        }
    }

    private fun semesterName(id: String) = if (id == XATU_ALL_GRADES) "全部历史" else _uiState.value.semesters.firstOrNull { it.id == id }?.name.orEmpty()
}
