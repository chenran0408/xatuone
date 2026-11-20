package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.room3.withWriteTransaction
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTimeBinding
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTimeBinding.TargetType
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTimeBindingDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.MainAppDatabase
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlotDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTableCombo
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTableComboDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTableComboRule
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTableDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * 作息调度数据仓库
 * 负责管理课表专属作息、公共作息、组合作息以及作息绑定的组合计算逻辑
 */
@Single
class TimeScheduleRepository(
    private val database: MainAppDatabase,
    private val timeTableDao: TimeTableDao,
    private val timeSlotDao: TimeSlotDao,
    private val courseTimeBindingDao: CourseTimeBindingDao,
    private val timeTableComboDao: TimeTableComboDao
) {

    // 动态作息计算与观察

    /**
     * 实时观察指定课表在特定日期的生效节次列表
     */
    fun observeEffectiveTimeSlots(
        courseTableId: String,
        targetDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    ): Flow<List<TimeSlot>> = observeEffectiveTimeSlots(courseTableId, listOf(targetDate)).map { it.getValue(targetDate) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeEffectiveTimeSlots(courseTableId: String, dates: List<LocalDate>): Flow<Map<LocalDate, List<TimeSlot>>> =
        courseTimeBindingDao.observeBindingByCourseTableId(courseTableId).flatMapLatest { binding ->
            if (binding?.targetType == TargetType.COMBO) {
                observeComboEffectiveTimeSlots(binding.targetId, courseTableId, dates)
            } else {
                timeSlotDao.getTimeSlotsByTimeTableId(binding?.targetId ?: courseTableId).map { slots ->
                    dates.associateWith { slots }
                }
            }
        }

    /**
     * 单次获取指定课表在特定日期的生效节次列表
     */
    suspend fun getEffectiveTimeSlotsOnce(
        tableId: String,
        date: LocalDate
    ): List<TimeSlot> {
        val binding = courseTimeBindingDao.getBindingByCourseTableId(tableId)
            ?: return timeSlotDao.getTimeSlotsOnceByTimeTableId(tableId)

        if (binding.targetType == TargetType.SINGLE) {
            return timeSlotDao.getTimeSlotsOnceByTimeTableId(binding.targetId)
        }

        val combo = timeTableComboDao.getComboById(binding.targetId)
        val rules = timeTableComboDao.getRuleListByComboId(binding.targetId)

        val comboBaseTableId = combo?.baseTimeTableId ?: tableId
        val comboBaseSlots = timeSlotDao.getTimeSlotsOnceByTimeTableId(comboBaseTableId)

        val matchedTargetId = matchComboTargetTableId(rules, date)

        if (matchedTargetId == null || matchedTargetId == comboBaseTableId) {
            return comboBaseSlots
        }

        val targetSlots = timeSlotDao.getTimeSlotsOnceByTimeTableId(matchedTargetId)
        return alignTimeSlots(baseSlots = comboBaseSlots, targetSlots = targetSlots, targetTableId = matchedTargetId)
    }

    /**
     * 监听作息规则变化触发信号
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTimeScheduleFlowByTableId(tableId: String): Flow<Unit> {
        return courseTimeBindingDao.observeBindingByCourseTableId(tableId).distinctUntilChanged().flatMapLatest { binding ->
            if (binding?.targetType == TargetType.COMBO) {
                combine(
                    timeTableComboDao.getComboByIdFlow(binding.targetId).distinctUntilChanged(),
                    timeTableComboDao.getRulesByComboId(binding.targetId).distinctUntilChanged()
                ) { combo, rules ->
                    rules.map { it.targetTimeTableId } + (combo?.baseTimeTableId ?: tableId)
                }.flatMapLatest { sourceIds ->
                    timeSlotDao.observeSlotsForTimeTables(sourceIds.distinct()).distinctUntilChanged()
                }
            } else {
                timeSlotDao.getTimeSlotsByTimeTableId(binding?.targetId ?: tableId).distinctUntilChanged()
            }
        }.map { Unit }
    }

    /**
     * 观察组合作息计算后的生效节次数据
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeComboEffectiveTimeSlots(
        comboId: String,
        courseTableId: String,
        dates: List<LocalDate>
    ): Flow<Map<LocalDate, List<TimeSlot>>> {
        return combine(
            timeTableComboDao.getComboByIdFlow(comboId),
            timeTableComboDao.getRulesByComboId(comboId)
        ) { combo, rules ->
            val baseId = combo?.baseTimeTableId ?: courseTableId
            baseId to dates.associateWith { matchComboTargetTableId(rules, it) ?: baseId }
        }.distinctUntilChanged().flatMapLatest { (baseId, idsByDate) ->
            val tableIds = (idsByDate.values + baseId).distinct()
            timeSlotDao.observeSlotsForTimeTables(tableIds).map { slots ->
                val slotsById = slots.groupBy { it.timeTableId }
                val base = slotsById[baseId].orEmpty()
                val aligned = tableIds.associateWith { id ->
                    if (id == baseId) base else alignTimeSlots(base, slotsById[id].orEmpty(), id)
                }
                idsByDate.mapValues { aligned.getValue(it.value) }
            }
        }
    }

    /**
     * 节次对齐算法：以 baseSlots 结构为基准骨架，将 targetSlots 的时间配置对齐过去
     */
    private fun alignTimeSlots(
        baseSlots: List<TimeSlot>,
        targetSlots: List<TimeSlot>,
        targetTableId: String
    ): List<TimeSlot> {
        if (baseSlots.isEmpty()) return targetSlots
        if (targetSlots.isEmpty()) return baseSlots

        val targetMap = targetSlots.associateBy { it.number }

        return baseSlots.map { baseSlot ->
            val targetSlot = targetMap[baseSlot.number]
            if (targetSlot != null) {
                baseSlot.copy(
                    startTime = targetSlot.startTime,
                    endTime = targetSlot.endTime,
                    alias = targetSlot.alias,
                    timeTableId = targetTableId
                )
            } else {
                baseSlot
            }
        }
    }

    /**
     * 匹配日期对应的组合作息规则
     */
    private fun matchComboTargetTableId(
        rules: List<TimeTableComboRule>,
        targetDate: LocalDate
    ): String? {
        if (rules.isEmpty()) return null
        val currentDateStr = targetDate.toString()
        return rules.firstOrNull { currentDateStr in it.startDate..it.endDate }?.targetTimeTableId
    }

    // 基础作息与节次 CRUD

    fun getTimeTableById(timeTableId: String): Flow<TimeTable?> {
        return timeTableDao.observeTimeTableById(timeTableId)
    }

    fun getTimeSlotsByTimeTableId(timeTableId: String): Flow<List<TimeSlot>> {
        return timeSlotDao.getTimeSlotsByTimeTableId(timeTableId)
    }

    /**
     * 保存专属作息及其关联的节次列表
     */
    suspend fun saveExclusiveTimeTable(timeTable: TimeTable, timeSlots: List<TimeSlot>) {
        database.withWriteTransaction {
            timeTableDao.insertOrUpdate(timeTable)
            timeSlotDao.deleteTimeSlotsGreaterThan(timeTable.id, timeSlots.size)
            if (timeSlots.isNotEmpty()) {
                timeSlotDao.insertAll(timeSlots)
            }
        }
    }

    // 课表作息绑定管理

    suspend fun getBinding(courseTableId: String): CourseTimeBinding? {
        return courseTimeBindingDao.getBindingByCourseTableId(courseTableId)
    }

    suspend fun bindCourseTableToTimeSchedule(courseTableId: String, targetType: TargetType, targetId: String) {
        val binding = CourseTimeBinding(
            courseTableId = courseTableId,
            targetType = targetType,
            targetId = targetId
        )
        courseTimeBindingDao.insertOrUpdate(binding)
    }

    // 公共作息与组合作息管理

    fun getAllPublicTimeTables(): Flow<List<TimeTable>> {
        return timeTableDao.getAllPublicTimeTables()
    }

    /**
     * 保存公共作息及其关联的节次列表
     */
    suspend fun savePublicTimeTable(timeTable: TimeTable, timeSlots: List<TimeSlot>) {
        database.withWriteTransaction {
            timeTableDao.insertOrUpdate(timeTable)
            timeSlotDao.deleteAllTimeSlotsByTimeTableId(timeTable.id)
            if (timeSlots.isNotEmpty()) {
                timeSlotDao.insertAll(timeSlots)
            }
        }
    }

    /**
     * 删除公共作息并级联解除关联关系
     */
    suspend fun deletePublicTimeTable(id: String) {
        database.withWriteTransaction {
            timeTableComboDao.clearBaseTimeTableId(id)
            timeTableComboDao.deleteRulesByTargetTimeTableId(id)
            courseTimeBindingDao.deleteByTargetId(id)
            timeSlotDao.deleteAllTimeSlotsByTimeTableId(id)
            timeTableDao.deleteTimeTableById(id)
        }
    }

    fun getAllCombos(): Flow<List<TimeTableCombo>> {
        return timeTableComboDao.getAllCombos()
    }

    fun getComboRules(comboId: String): Flow<List<TimeTableComboRule>> {
        return timeTableComboDao.getRulesByComboId(comboId)
    }

    /**
     * 保存组合作息及其替换规则
     */
    suspend fun saveTimeTableCombo(combo: TimeTableCombo, rules: List<TimeTableComboRule>) {
        database.withWriteTransaction {
            timeTableComboDao.insertCombo(combo)
            timeTableComboDao.replaceComboRules(combo.id, rules)
        }
    }

    /**
     * 删除组合作息并清除绑定的规则与绑定状态
     */
    suspend fun deleteCombo(id: String) {
        database.withWriteTransaction {
            courseTimeBindingDao.deleteByTargetId(id)
            timeTableComboDao.deleteComboById(id)
            timeTableComboDao.deleteRulesByComboId(id)
        }
    }
}
