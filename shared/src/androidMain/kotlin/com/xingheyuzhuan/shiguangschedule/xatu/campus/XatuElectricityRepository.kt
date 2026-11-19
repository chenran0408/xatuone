package com.xingheyuzhuan.shiguangschedule.xatu.campus

import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuDormSelection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

data class XatuElectricityQueryState(
    val dorm: XatuDormSelection? = null,
    val loading: Boolean = false,
    val checkedAt: Long = 0,
    val error: String? = null,
    val requiresLogin: Boolean = false,
    val noData: Boolean = false,
)

/** Home and details share request feedback as well as the existing persisted readings. */
@Single
class XatuElectricityRepository(
    private val campus: XatuCampusRepository,
    private val service: XatuCampusService,
) {
    private val _state = MutableStateFlow(XatuElectricityQueryState())
    val state = _state.asStateFlow()

    suspend fun refresh(selection: XatuDormSelection) {
        if (_state.value.loading && _state.value.dorm == selection) return
        val request = XatuElectricityQueryState(dorm = selection, loading = true)
        _state.value = request
        try {
            val dorm = XatuDorm(
                XatuCampusOption(selection.projectId, selection.projectName),
                XatuCampusOption(selection.areaId, selection.areaName),
                XatuCampusOption(selection.buildingId, selection.buildingName),
                XatuCampusOption(selection.roomId, selection.roomName),
            )
            val balance = service.electricityBalance(dorm)
            val checkedAt = System.currentTimeMillis()
            campus.saveBalance(selection, balance, checkedAt)
            if (_state.value === request) _state.value = request.copy(loading = false, checkedAt = checkedAt)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (_state.value === request) _state.value = request.copy(
                loading = false, error = error.message ?: "电量查询失败",
                requiresLogin = error is XatuPaySessionExpired,
                noData = error is XatuElectricityNoData,
            )
        } finally {
            if (_state.value === request) _state.value = XatuElectricityQueryState(dorm = selection)
        }
    }
}
