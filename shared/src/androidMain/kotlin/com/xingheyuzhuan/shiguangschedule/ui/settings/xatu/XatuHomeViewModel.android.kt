package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccount
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccountRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusSnapshot
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuSessionRepository
import com.xingheyuzhuan.shiguangschedule.xatu.academic.*
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuCampusOption
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuCampusService
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuDorm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

data class XatuHomeUiState(
    val account: XatuAccount? = null,
    val officialWeek: Int? = null,
    val exams: List<XatuExam>? = null,
    val examSemester: String = "",
    val cacheUpdatedAt: Long? = null,
    val loading: Boolean = true,
    val requiresLogin: Boolean = false,
    val error: String? = null,
)

internal fun upcomingXatuExam(exams: List<XatuExam>, now: LocalDateTime): XatuExam? = exams
    .mapNotNull { exam -> exam.startDateTime()?.takeIf { it >= now }?.let { it to exam } }
    .minByOrNull { it.first }?.second

@KoinViewModel
class XatuHomeViewModel(
    private val academic: XatuAcademicRepository,
    private val accountRepository: XatuAccountRepository,
    private val campus: XatuCampusRepository,
    private val electricity: com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuElectricityRepository,
    private val session: XatuSessionRepository,
) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    private val _uiState = MutableStateFlow(XatuHomeUiState())
    val uiState = _uiState.asStateFlow()
    val campusSnapshot: StateFlow<XatuCampusSnapshot?> = campus.snapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val electricityRefresh = MutableStateFlow(0)
    val electricityState = electricity.state
    private var observedDate = LocalDate.now(ZoneOffset.ofHours(8))
    private var manualLoginAccount: String? = null

    init {
        viewModelScope.launch {
            combine(accountRepository.account.distinctUntilChanged(), refresh) { account, _ -> account }
                .collectLatest { account -> load(account) }
        }
        viewModelScope.launch {
            combine(campus.snapshot.map { it.dorm }.distinctUntilChanged(), electricityRefresh) { dorm, _ -> dorm }
                .collectLatest { selection ->
                    if (selection != null) electricity.refresh(selection)
                }
        }
    }

    fun refresh() { refresh.update { it + 1 } }

    fun refreshElectricity() {
        electricityRefresh.update { it + 1 }
    }

    fun onDateChanged(date: LocalDate) {
        if (observedDate != date) {
            observedDate = date
            refresh()
        }
    }

    private suspend fun load(account: XatuAccount) {
        if (!account.loggedIn) {
            val previous = _uiState.value.takeIf { it.account?.studentNumber == account.studentNumber }
            if (account.canRestore && manualLoginAccount != account.studentNumber) {
                _uiState.value = (previous ?: XatuHomeUiState()).copy(account = account, loading = true, error = null, requiresLogin = false)
                try {
                    session.restoreSavedLogin(account.studentNumber)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: XatuSessionExpired) {
                    manualLoginAccount = account.studentNumber
                    _uiState.update { it.copy(loading = false, requiresLogin = true, error = error.message) }
                } catch (error: Exception) {
                    _uiState.update { it.copy(loading = false, error = "教务摘要更新失败：${error.message}") }
                }
            } else {
                _uiState.value = (previous ?: XatuHomeUiState()).copy(
                    account = account,
                    loading = false,
                    requiresLogin = manualLoginAccount == account.studentNumber,
                    error = previous?.error.takeIf { manualLoginAccount == account.studentNumber },
                )
            }
            return
        }
        manualLoginAccount = null
        _uiState.value = if (_uiState.value.account == account) _uiState.value.copy(loading = true, requiresLogin = false, error = null)
            else XatuHomeUiState(account = account)
        try {
            val cache = academic.loadAccountCache(account.studentNumber)
            val cached = cache.snapshots.filter { it.exams.isNotEmpty() }.maxByOrNull { it.fetchedAt }
            if (cached != null) _uiState.update { it.copy(
                exams = cached.exams,
                examSemester = cache.semesters.firstOrNull { it.id == cached.semesterId }?.name.orEmpty(),
                cacheUpdatedAt = cached.fetchedAt,
            ) }
            val week = checkNotNull(academic.officialWeek()) { "教务未返回当前教学周" }
            _uiState.update { it.copy(officialWeek = week) }
            val semesters = academic.semesters()
            val semester = XatuAcademicParser.currentSemesterId(semesters, week, LocalDate.now(ZoneOffset.ofHours(8)))
            val updated = academic.refreshExams(account.studentNumber, semester)
            _uiState.update { it.copy(
                exams = updated.exams,
                examSemester = semesters.first { it.id == semester }.name,
                cacheUpdatedAt = updated.fetchedAt,
            ) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: XatuSessionExpired) {
            manualLoginAccount = account.studentNumber
            _uiState.update { it.copy(requiresLogin = true, error = error.message) }
            accountRepository.markLoggedOut(account.studentNumber)
        } catch (error: Exception) {
            val message = "教务摘要更新失败：${error.message}"
            _uiState.update { it.copy(error = message) }
        } finally {
            _uiState.update { it.copy(loading = false) }
        }
    }
}
