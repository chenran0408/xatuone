package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccountRepository
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuAcademicRepository
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuExam
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuGradePage
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuCourseInfo
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuStudyPlan
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuSemester
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
    val status: String = ""
)

@KoinViewModel
class XatuAcademicViewModel(
    private val academic: XatuAcademicRepository,
    private val accountRepository: XatuAccountRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(XatuAcademicUiState())
    val uiState: StateFlow<XatuAcademicUiState> = _uiState.asStateFlow()
    private var request: Job? = null

    fun load(page: String) {
        request?.cancel()
        request = viewModelScope.launch {
            val account = accountRepository.account.first()
            _uiState.value = XatuAcademicUiState(page = page, status = "正在读取教务数据…")
            if (!account.loggedIn || account.studentNumber.isBlank()) {
                _uiState.update { it.copy(status = "请先登录教务账号") }
                return@launch
            }
            try {
                if (page == "plan") loadPlan(account.studentNumber) else loadSemesterData(page, account.studentNumber)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(status = error.message ?: "读取教务数据失败，已保留本地缓存") }
            }
        }
    }

    fun selectSemester(id: String) {
        val page = _uiState.value.page
        if (page != "grades" && page != "exams") return
        request?.cancel()
        request = viewModelScope.launch {
            try {
                loadSemester(page, accountRepository.account.first().studentNumber, id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(status = error.message ?: "读取失败，已保留本地缓存") }
            }
        }
    }

    private suspend fun loadPlan(account: String) {
        val cached = academic.loadCached(account, "all")
        _uiState.update { it.copy(plan = cached?.plan, catalog = cached?.catalog.orEmpty(), status = if (cached?.plan != null) "已显示缓存，正在更新…" else "正在读取学业完成情况…") }
        val updated = academic.refreshPlan(account)
        _uiState.update { it.copy(plan = updated.plan, catalog = updated.catalog, status = "已更新") }
    }

    private suspend fun loadSemesterData(page: String, account: String) {
        val semesters = academic.semesters()
        val selected = semesters.firstOrNull()?.id
        _uiState.update { it.copy(semesters = semesters, selectedSemesterId = selected, status = if (semesters.isEmpty()) "暂无可选学期" else "") }
        if (selected != null) loadSemester(page, account, selected)
    }

    private suspend fun loadSemester(page: String, account: String, semesterId: String) {
        _uiState.update { it.copy(selectedSemesterId = semesterId, grades = null, exams = null) }
        val cached = academic.loadCached(account, semesterId)
        if (page == "grades") {
            _uiState.update { it.copy(grades = cached?.grades, status = if (cached?.grades != null) "已显示缓存，正在更新…" else "正在读取成绩…") }
            val updated = academic.refreshGrades(account, semesterId)
            _uiState.update { it.copy(grades = updated.grades, status = "已更新：${semesterName(semesterId)}") }
        } else {
            _uiState.update { it.copy(exams = cached?.exams, status = if (cached != null) "已显示缓存，正在更新…" else "正在读取考试安排…") }
            val updated = academic.refreshExams(account, semesterId)
            _uiState.update { it.copy(exams = updated.exams, status = "已更新：${semesterName(semesterId)}") }
        }
    }

    private fun semesterName(id: String) = _uiState.value.semesters.firstOrNull { it.id == id }?.name.orEmpty()
}
